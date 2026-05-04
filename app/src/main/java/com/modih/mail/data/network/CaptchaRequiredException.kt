package com.modih.mail.data.network

/**
 * Thrown by [com.modih.mail.data.repository.MailRepository.createInbox] when
 * the server returns HTTP 428 with `code: "CAPTCHA_REQUIRED"`.
 *
 * The Cloudflare backend gates free-tier inbox creation on a Turnstile
 * challenge once the user crosses `turnstileAt - 1` daily creations. The
 * mobile app doesn't have a native Turnstile widget yet, so the UI catches
 * this exception and routes the user to the web flow at https://modih.in
 * to complete the challenge there.
 *
 * Carrying [creationsToday] / [maxCreations] so the UI can show the right
 * "X of Y daily inboxes used" copy without an extra round-trip.
 */
class CaptchaRequiredException(
    val creationsToday: Int,
    val maxCreations: Int,
    message: String = "Daily verification challenge required."
) : Exception(message)
