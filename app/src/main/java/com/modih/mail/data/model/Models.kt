package com.modih.mail.data.model

data class UserPlan(
    val uid: String = "",
    val email: String = "",
    val plan: Plan = Plan.FREE
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
    val subject: String,
    val bodyHtml: String,
    val bodyText: String,
    val receivedAt: Long,
    val otp: String? = null
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
    val apiKeysLimit: Int = 0
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
