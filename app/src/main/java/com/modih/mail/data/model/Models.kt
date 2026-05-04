package com.modih.mail.data.model

data class UserPlan(
    val uid: String = "",
    val email: String = "",
    val plan: Plan = Plan.FREE,
    val emailVerified: Boolean = false
)

enum class Plan(val label: String, val maxReservedAliases: Int) {
    FREE("Free", 1),
    PRO("Pro", 3),
    DEVELOPER("Developer", 3);

    companion object {
        fun fromString(s: String): Plan = when (s.lowercase()) {
            "pro" -> PRO
            "developer" -> DEVELOPER
            else -> FREE
        }
    }
}

data class Inbox(
    val id: String,
    val email: String,
    val createdAt: Long,
    val expiresAt: Long,
    val ownerToken: String,
    val plan: String = "free",
    val creationsToday: Int = 0,
    val maxCreations: Int = 3
)

data class MailMessage(
    val id: String,
    val inboxId: String,
    val from: String,
    val fromName: String? = null,
    val subject: String,
    val bodyHtml: String,
    val bodyText: String,
    val receivedAt: Long,
    val otp: String? = null
) {
    /** Display-friendly sender like `Acme <hello@acme.com>` or just the address. */
    val displaySender: String
        get() = if (!fromName.isNullOrBlank() && fromName != from) "$fromName <$from>" else from
}

data class MessagesPage(
    val inbox: Inbox?,
    val messages: List<MailMessage>,
    val count: Int
)

data class ReservedAlias(
    val id: String,
    val prefix: String,
    val email: String,
    val createdAt: Long,
    val isActive: Boolean = true
)

data class ApiKey(
    val id: String,
    val name: String,
    val keyPrefix: String,
    val createdAt: Long,
    val lastUsed: Long?,
    val isActive: Boolean,
    val plainKey: String? = null
)

data class UsageStats(
    val inboxesCreated: Int = 0,
    val inboxesLimit: Int = 0,
    val readsUsed: Int = 0,
    val readsLimit: Int = 0,
    val apiKeysActive: Int = 0,
    val apiKeysLimit: Int = 0,
    val resetsAt: Long = 0
)

data class PricingPlan(
    val name: String,
    val tagline: String,
    val monthlyPrice: Int,
    val quarterlyPrice: Int,
    val yearlyPrice: Int,
    val features: List<PlanFeature>,
    val isRecommended: Boolean = false
)

data class PlanFeature(
    val text: String,
    val included: Boolean
)

data class BillingPeriod(
    val label: String,
    val key: String,
    val discount: String? = null
)

// ──────────────────────────────────────────────────────────────────────────
// Admin
// ──────────────────────────────────────────────────────────────────────────

data class AdminUser(
    val uid: String,
    val email: String,
    val plan: String,
    val planExpiresAt: Long? = null,
    val planSource: String? = null,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val activeInboxes: Int = 0,
    val totalInboxes: Int = 0,
    val totalMessages: Int = 0
)

data class AdminStats(
    val totalUsers: Int = 0,
    val proUsers: Int = 0,
    val developerUsers: Int = 0,
    val activeInboxes: Int = 0,
    val totalInboxesCreated: Int = 0,
    val totalMessagesReceived: Int = 0,
    val rangeKey: String = "7d"
)

data class AdminActivity(
    val type: String,        // "inbox", "message", "api", "login"
    val actor: String,
    val subject: String,
    val ip: String? = null,
    val createdAt: Long = 0
)

data class AdminSnapshot(
    val users: List<AdminUser> = emptyList(),
    val stats: AdminStats = AdminStats(),
    val activity: List<AdminActivity> = emptyList(),
    val analyticsSource: String = "live_tables"
)
