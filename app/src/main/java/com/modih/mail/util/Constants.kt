package com.modih.mail.util

object Constants {
    const val BASE_URL = "https://modih.in"
    const val API_BASE = "$BASE_URL/api"
    const val PRIVACY_URL = "$BASE_URL/privacy.html"
    const val TERMS_URL = "$BASE_URL/terms.html"
    const val LINKEDIN_URL = "https://www.linkedin.com/in/abhnv07/"
    const val AUTHOR_URL = "https://abhnv.in"

    const val FREE_MAX_INBOXES_PER_DAY = 3
    const val FREE_MAX_ACTIVE_INBOXES = 1
    const val FREE_RETENTION_HOURS = 3
    const val FREE_RESERVED_ALIASES = 1

    const val PRO_MAX_INBOXES_PER_DAY = 25
    const val PRO_MAX_ACTIVE_INBOXES = 10
    const val PRO_RETENTION_DAYS = 7
    const val PRO_RESERVED_ALIASES = 3

    const val DEV_RETENTION_DAYS = 30
    const val DEV_CREATES_PER_MONTH = 5000
    const val DEV_READS_PER_MONTH = 50000

    const val REFRESH_INTERVAL_MS = 5000L
}
