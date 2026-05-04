package com.modih.mail.data.network

import com.modih.mail.BuildConfig
import okhttp3.CertificatePinner
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import java.util.concurrent.TimeUnit
import kotlin.math.min

/**
 * Single shared OkHttpClient for the whole app. Centralised so the
 * security knobs (cert pinning, 429 backoff) all live in one place.
 *
 *  - Cert pinning: only enforced in release builds, and only for modih.in.
 *    We pin the current Google Trust Services intermediate + two roots so
 *    a leaf rotation on its own doesn't brick the app, but a self-signed /
 *    rogue-CA MITM cert is still rejected. Debug builds skip pinning so
 *    devs can run a local proxy.
 *
 *  - 429 backoff: when the server returns 429 the body has Retry-After
 *    (set by the per-IP brute-force rate limiter on the backend). We
 *    honour it once, with a hard ceiling, so a single misbehaving call
 *    can't spin forever.
 */
object NetworkClient {

    /** Hostname we pin against. Other hosts (Firebase, Google) are NOT pinned. */
    private const val PINNED_HOST = "modih.in"

    /**
     * Pins are SubjectPublicKeyInfo SHA-256 hashes (base64).
     *
     *  - WE1: current intermediate (Google Trust Services), as observed on the
     *    live cert chain at the time of release. Most likely to rotate first.
     *  - GTS Root R4: current root (ECDSA). Long-lived.
     *  - GTS Root R3: sibling ECDSA root, kept as a backup so we survive a
     *    Google-internal root reissuance without bricking installed clients.
     *
     * If you regenerate these, run:
     *
     *   echo | openssl s_client -servername modih.in -connect modih.in:443 \
     *     -showcerts 2>/dev/null \
     *     | openssl x509 -pubkey -noout \
     *     | openssl pkey -pubin -outform DER \
     *     | openssl dgst -sha256 -binary | base64
     */
    private val PINS = arrayOf(
        "sha256/kIdp6NNEd8wsugYyyIYFsi1ylMCED3hZbSR8ZFsa/A4=", // GTS WE1 (intermediate)
        "sha256/mEflZT5enoR1FuXLgYYGqnVEoZvmf9c2bVBpiOjYQ0c=", // GTS Root R4
        "sha256/IWvkpCSxIlrCw3Ax5TeP3hXEC7gWZsBgyxLvJ8Ar3Lc="  // GTS Root R3 (backup)
    )

    private const val MAX_RETRY_AFTER_MS = 30_000L
    private const val DEFAULT_BACKOFF_MS = 1_500L

    val client: OkHttpClient by lazy {
        val builder = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .callTimeout(60, TimeUnit.SECONDS) // generous: backoff may eat 30s
            .retryOnConnectionFailure(true)
            .addInterceptor(RateLimitBackoffInterceptor())

        if (!BuildConfig.DEBUG) {
            val pinnerBuilder = CertificatePinner.Builder()
            for (p in PINS) pinnerBuilder.add(PINNED_HOST, p)
            // Subdomains (none today, but futureproofing — admin worker etc).
            for (p in PINS) pinnerBuilder.add("*.$PINNED_HOST", p)
            builder.certificatePinner(pinnerBuilder.build())
        }

        builder.build()
    }

    /**
     * Honour 429 Retry-After exactly once, with a sane upper bound. Anything
     * the server returns above [MAX_RETRY_AFTER_MS] is ignored — we'd rather
     * surface the 429 than freeze the UI for a minute.
     */
    private class RateLimitBackoffInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            val first = chain.proceed(request)
            if (first.code != 429) return first

            val retryAfterHeader = first.header("Retry-After")
            val sleepMs = parseRetryAfter(retryAfterHeader) ?: DEFAULT_BACKOFF_MS
            val capped = min(sleepMs, MAX_RETRY_AFTER_MS)
            first.close()

            try {
                Thread.sleep(capped)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                throw e
            }
            return chain.proceed(request)
        }

        private fun parseRetryAfter(value: String?): Long? {
            if (value.isNullOrBlank()) return null
            // Per RFC 7231 the value is either delta-seconds or HTTP-date.
            // The backend always sends seconds, so handle that fast path.
            val seconds = value.trim().toLongOrNull()
            return seconds?.times(1000)
        }
    }
}
