---
name: testing-modih-mail-android
description: Test the MODiH Mail Android app against the live backend at https://modih.in. Use this skill when verifying releases, security hardening, or backend-wiring changes in modih-email-android. Covers envelope contracts, cert-pin freshness, 429/Retry-After, the admin gate, and the VM constraints that block emulator-based testing.
---

# Testing MODiH Mail Android

## VM constraints to design plans around

- **No `/dev/kvm`, only 2 cores / 7.8 GB RAM.** Don't budget time for a real Android emulator. Boot would take 20+ min and Firebase Auth needs a Play Services image registered to the prod Firebase project, which is not provisionable from the VM. **Plan around this** — design tests that prove the change without a running app.
- **Backend at `https://modih.in` is reachable.** This is where the highest-signal tests live. Most v1.x changes are about backend wiring + envelope parsing, all directly testable with curl.
- **Cloudflare per-IP rate-limit on `POST /api/inbox` is sticky.** Even after 5+ minutes idle, the limiter keeps returning `429 retry-after: 60`. Don't burn your budget on probes you don't need — use one fresh `browser_token` per session for a clean T1/T2/T9 run. **`/api/contact` and `/api/messages` are in different buckets** and stay reachable, so use those for ongoing envelope checks once you've spent the `/api/inbox` budget.
- **`MODIH_ADMIN_SECRET` is required for the admin happy-path.** If not provided, only the 401 gate is testable. Always ask the user up front using the three-option pattern (skip / temp / save).

## Quick reference — tests that work

### APK identity
```
~/android-sdk/build-tools/35.0.0/aapt dump badging ~/modih-mail-vX.Y.Z.apk | head -3
```
Expect `package: name='com.modih.mail' versionCode=N versionName='X.Y.Z' … sdkVersion='26' targetSdkVersion='35'`.

### Cert pin freshness (release builds only — debug skips pinning)
```
echo | openssl s_client -servername modih.in -connect modih.in:443 -showcerts 2>/dev/null \
  | awk '/-----BEGIN CERTIFICATE-----/,/-----END CERTIFICATE-----/' \
  | csplit -z -s -f cert -b "%02d.pem" - '/-----BEGIN CERTIFICATE-----/' '{*}'
for f in cert*.pem; do
  pin=$(openssl x509 -in "$f" -pubkey -noout | openssl pkey -pubin -outform DER \
        | openssl dgst -sha256 -binary | base64)
  subj=$(openssl x509 -in "$f" -noout -subject)
  echo "  sha256/$pin  ($subj)"
done
```
At least one of the SPKI hashes must match a pin in `app/src/main/java/com/modih/mail/data/network/NetworkClient.kt`. **Pin the intermediate + at least one root, NOT the leaf** — leaves rotate frequently.

### `/api/inbox` envelope (use one fresh browser_token, hit it once — the limiter is sticky)
```
BT=$(uuidgen)
curl -sS -X POST https://modih.in/api/inbox \
  -H 'Content-Type: application/json' -H "X-Browser-Token: $BT" --data '{}'
```
Expect HTTP 201 with `{success: true, data: {id, email, owner_token, created_at, expires_at, plan, creations_today, max_creations}}`. `email` ends with `@modih.in`. `owner_token` is non-empty. **The free tier has `turnstileAt: 2`, so the 2nd create on the same browser_token returns HTTP 428 with `error.code:"CAPTCHA_REQUIRED"`** — useful for testing the captcha branch.

### `/api/messages?inbox_id=…` envelope (works even when /api/inbox is rate-limited)
With a real owner token: HTTP 200 + `{success: true, data: {inbox: {…}, messages: […], count}}`.
With bogus inbox: HTTP 404 + `{success: false, error: {code: "INBOX_NOT_FOUND", …}, meta: {request_id}}`. The 404 path verifies the same envelope shape `MailRepository.parseErrorMessage` reads, which is enough if the happy-path is blocked.

### `/api/admin/users` 401 gate
```
curl -sS -o /dev/null -w "%{http_code}\n" "https://modih.in/api/admin/users?filter=all&range=24h"
curl -sS -o /dev/null -w "%{http_code}\n" -H "X-Admin-Secret: bogus" "https://modih.in/api/admin/users?filter=all&range=24h"
```
Both must return 401 with `{"error":"Unauthorized"}`.

### `Retry-After` on 429 (the contract `RateLimitBackoffInterceptor` consumes)
```
for i in 1 2 3; do
  curl -sS -o /dev/null -D /tmp/h.txt -w "%{http_code}\n" -X POST https://modih.in/api/inbox \
    -H 'Content-Type: application/json' -H "X-Browser-Token: probe-$RANDOM" --data '{}'
  grep -i '^retry-after:' /tmp/h.txt
done
```
If rate-limited (likely), every 429 should carry `retry-after: 60`. The interceptor at `NetworkClient.kt:81-99` parses this case-insensitively, sleeps once (capped at 30s), retries once.

## Source-level checks (use when runtime testing is blocked)

For security hardening PRs you can't drive on an emulator, grep the rendering path directly:

- `MessageDetailScreen.kt` should set `javaScriptEnabled=false`, `domStorageEnabled=false`, `mixedContentMode=MIXED_CONTENT_NEVER_ALLOW`, `blockNetworkLoads=!loadImages`, `loadsImagesAutomatically=loadImages`, and call `loadDataWithBaseURL("about:blank", …)`.
- `SecureStore.kt` should use `EncryptedSharedPreferences.create(…, MasterKey, AES256_SIV, AES256_GCM)`.
- `PreferencesManager.kt` should route `OWNER_TOKEN` and `ADMIN_SECRET` reads/writes through `secureStore.put/get/remove`, with a `migrateLegacy()` helper for upgrading from older plaintext versions.
- `MailRepository.kt:17` should be `private val client: OkHttpClient = NetworkClient.client` (single shared client).
- `MailRepository.kt:51-59` should map 428 to `CaptchaRequiredException(creations_today, max_creations)`.

## Things to verify on a real phone (cannot do from VM)

- v1.1→v1.2 token migration end-to-end (install old APK, sign in, update over the top, verify same inbox loads).
- Encrypted prefs at rest: `adb shell run-as com.modih.mail cat shared_prefs/modih_secure.xml` should show base64 ciphertext, not the literal owner token.
- Cert pinning rejection: Burp/mitmproxy with user-trust CA → every `modih.in` call fails with `SSLPeerUnverifiedException`.
- WebView image opt-in: open a message with external `<img>`, confirm blank until **Load images** is tapped.
- CAPTCHA card UI on free tier (2nd create of the day shows the gold card + **Open verification** browser launch).
- App-side 429 retry: trigger limiter, observe single ~60s pause, then success.

## Build / lint

- `./gradlew :app:assembleRelease --no-daemon` — APK at `app/build/outputs/apk/release/app-release.apk` (debug-signed; v1.0 didn't have a release keystore wired up).
- `./gradlew :app:lint --no-daemon` — Android lint, runs in the env's `maintenance` step.
- `isMinifyEnabled = false` in `build.gradle.kts` so the dex isn't obfuscated. Despite that, **`grep` / `strings` / Python-regex on the unzipped `classes.dex` returned 0 hits** even for known-present needles like `"modih.in"`. Likely a quirk of how Kotlin string constants are stored (probably indirected via `string_data_off` rather than inlined as ASCII spans). **Don't rely on dex-internal grep for evidence** — use aapt for metadata + git diff for source presence.

## Devin secrets needed

- `MODIH_ADMIN_SECRET` (plain) — required for the admin happy-path test (`/api/admin/users` 200 + table render). Without it, only the 401 gate is testable. The handoff doc explicitly says the production app should never know this secret — it's strictly for testing the operator endpoint.

## Pitfalls to avoid

- **Don't bank on `/tmp/`** for evidence files between turns — it gets wiped. Use `~/test-evidence/` or another path under `$HOME`.
- **Don't curl with `%header{…}`** in older curl versions — it isn't expanded. Use `-D /tmp/headers.txt` and grep instead.
- **Don't try to brute the Cloudflare limiter open** by waiting and retrying — 5+ min waits don't clear it. Once you've spent your `/api/inbox` budget, the only path to a clean inbox-create test is a different egress IP (i.e., the user's phone).
- **Don't claim emulator success** — if you say a test passed in an emulator, it didn't run; we don't have one.
