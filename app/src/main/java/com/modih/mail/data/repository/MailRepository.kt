package com.modih.mail.data.repository

import com.modih.mail.data.model.*
import com.modih.mail.util.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Single client used by every repository instance. The Cloudflare Pages
 * backend is keep-alive friendly so we want to reuse connections instead
 * of building a new pool per call.
 */
private val sharedHttpClient: OkHttpClient by lazy {
    OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()
}

class MailRepository {
    private val client: OkHttpClient = sharedHttpClient
    private val jsonMedia = "application/json".toMediaType()

    // ──────────────────────────────────────────────────────────────────
    // INBOX
    // ──────────────────────────────────────────────────────────────────

    suspend fun createInbox(
        prefix: String? = null,
        browserToken: String,
        authToken: String? = null,
        ownerToken: String? = null,
        turnstileToken: String? = null
    ): Result<Inbox> = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject()
            if (!prefix.isNullOrBlank()) body.put("prefix", prefix)
            if (!turnstileToken.isNullOrBlank()) body.put("turnstile_token", turnstileToken)

            val builder = Request.Builder()
                .url("${Constants.API_BASE}/inbox")
                .post(body.toString().toRequestBody(jsonMedia))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("X-Browser-Token", browserToken)

            if (!authToken.isNullOrBlank()) builder.header("Authorization", "Bearer $authToken")
            if (!ownerToken.isNullOrBlank()) builder.header("X-Owner-Token", ownerToken)

            val response = client.newCall(builder.build()).execute()
            val responseBody = response.body?.string().orEmpty()
            val json = if (responseBody.isBlank()) JSONObject() else JSONObject(responseBody)

            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    Exception(parseErrorMessage(json, "Failed to create inbox (${response.code})"))
                )
            }

            val data = json.optJSONObject("data") ?: json
            Result.success(parseInbox(data))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * GET /api/messages?inbox_id=xxx returns
     *   { success, data: { inbox: {...}, messages: [...], count: n } }
     */
    suspend fun getMessages(
        inboxId: String,
        ownerToken: String,
        authToken: String? = null,
        apiKey: String? = null
    ): Result<MessagesPage> = withContext(Dispatchers.IO) {
        try {
            val builder = Request.Builder()
                .url("${Constants.API_BASE}/messages?inbox_id=${urlEncode(inboxId)}")
                .get()
                .header("Accept", "application/json")
                .header("X-Owner-Token", ownerToken)

            if (!authToken.isNullOrBlank()) builder.header("Authorization", "Bearer $authToken")
            if (!apiKey.isNullOrBlank()) builder.header("X-API-Key", apiKey)

            val response = client.newCall(builder.build()).execute()
            val responseBody = response.body?.string().orEmpty()
            val json = if (responseBody.isBlank()) JSONObject() else JSONObject(responseBody)

            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    Exception(parseErrorMessage(json, "Failed to fetch messages (${response.code})"))
                )
            }

            val data = json.optJSONObject("data") ?: json
            val inboxObj = data.optJSONObject("inbox")
            val inbox = inboxObj?.let {
                Inbox(
                    id = it.optString("id", inboxId),
                    email = it.optString("email", ""),
                    createdAt = it.optLong("created_at", 0L),
                    expiresAt = it.optLong("expires_at", 0L),
                    ownerToken = ""
                )
            }
            val messagesArray = data.optJSONArray("messages") ?: JSONArray()
            val messages = (0 until messagesArray.length()).map { i ->
                parseMessage(messagesArray.getJSONObject(i), inboxId)
            }
            Result.success(
                MessagesPage(
                    inbox = inbox,
                    messages = messages,
                    count = data.optInt("count", messages.size)
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteInbox(
        inboxId: String,
        ownerToken: String,
        authToken: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val builder = Request.Builder()
                .url("${Constants.API_BASE}/inbox?id=${urlEncode(inboxId)}")
                .delete()
                .header("Accept", "application/json")
                .header("X-Owner-Token", ownerToken)

            if (!authToken.isNullOrBlank()) builder.header("Authorization", "Bearer $authToken")

            val response = client.newCall(builder.build()).execute()
            val responseBody = response.body?.string().orEmpty()
            val json = if (responseBody.isBlank()) JSONObject() else JSONObject(responseBody)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(
                Exception(parseErrorMessage(json, "Delete failed (${response.code})"))
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteMessage(
        inboxId: String,
        messageId: String,
        ownerToken: String,
        authToken: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val builder = Request.Builder()
                .url(
                    "${Constants.API_BASE}/messages" +
                        "?inbox_id=${urlEncode(inboxId)}" +
                        "&id=${urlEncode(messageId)}"
                )
                .delete()
                .header("Accept", "application/json")
                .header("X-Owner-Token", ownerToken)

            if (!authToken.isNullOrBlank()) builder.header("Authorization", "Bearer $authToken")

            val response = client.newCall(builder.build()).execute()
            val responseBody = response.body?.string().orEmpty()
            val json = if (responseBody.isBlank()) JSONObject() else JSONObject(responseBody)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(
                Exception(parseErrorMessage(json, "Delete failed (${response.code})"))
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteAllMessages(
        inboxId: String,
        ownerToken: String,
        authToken: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val builder = Request.Builder()
                .url("${Constants.API_BASE}/messages?inbox_id=${urlEncode(inboxId)}")
                .delete()
                .header("Accept", "application/json")
                .header("X-Owner-Token", ownerToken)

            if (!authToken.isNullOrBlank()) builder.header("Authorization", "Bearer $authToken")

            val response = client.newCall(builder.build()).execute()
            val responseBody = response.body?.string().orEmpty()
            val json = if (responseBody.isBlank()) JSONObject() else JSONObject(responseBody)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(
                Exception(parseErrorMessage(json, "Delete failed (${response.code})"))
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ──────────────────────────────────────────────────────────────────
    // AUTH / PROFILE
    // ──────────────────────────────────────────────────────────────────

    suspend fun getUserProfile(authToken: String): Result<UserPlan> = withContext(Dispatchers.IO) {
        try {
            val request = authRequestBuilder("${Constants.API_BASE}/auth/me", authToken)
                .get()
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string().orEmpty()
            val json = if (body.isBlank()) JSONObject() else JSONObject(body)

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception(parseErrorMessage(json, "Auth check failed")))
            }

            Result.success(
                UserPlan(
                    uid = json.optString("uid", ""),
                    email = json.optString("email", ""),
                    plan = Plan.fromString(json.optString("plan", "free")),
                    emailVerified = json.optBoolean("email_verified", false)
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ──────────────────────────────────────────────────────────────────
    // DEVELOPER
    // ──────────────────────────────────────────────────────────────────

    suspend fun getDeveloperUsage(authToken: String): Result<UsageStats> = withContext(Dispatchers.IO) {
        try {
            val request = authRequestBuilder("${Constants.API_BASE}/developer/usage", authToken)
                .get()
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string().orEmpty()
            val json = if (body.isBlank()) JSONObject() else JSONObject(body)

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception(parseErrorMessage(json, "Failed to load usage stats")))
            }

            val creates = json.optJSONObject("inbox_creates") ?: JSONObject()
            val reads = json.optJSONObject("message_reads") ?: JSONObject()
            Result.success(
                UsageStats(
                    inboxesCreated = creates.optInt("used", 0),
                    inboxesLimit = creates.optInt("limit", Constants.DEV_CREATES_PER_MONTH),
                    readsUsed = reads.optInt("used", 0),
                    readsLimit = reads.optInt("limit", Constants.DEV_READS_PER_MONTH),
                    apiKeysActive = 0,
                    apiKeysLimit = 10,
                    resetsAt = json.optLong("resets_at", 0L)
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDeveloperKeys(authToken: String): Result<List<ApiKey>> = withContext(Dispatchers.IO) {
        try {
            val request = authRequestBuilder("${Constants.API_BASE}/developer/keys", authToken)
                .get()
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string().orEmpty()
            val json = if (body.isBlank()) JSONObject() else JSONObject(body)

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception(parseErrorMessage(json, "Failed to load API keys")))
            }

            val data = json.optJSONObject("data") ?: json
            val keys = data.optJSONArray("keys") ?: JSONArray()
            Result.success(
                (0 until keys.length()).map { index ->
                    parseApiKey(keys.getJSONObject(index))
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createDeveloperKey(authToken: String, name: String): Result<ApiKey> = withContext(Dispatchers.IO) {
        try {
            val requestBody = JSONObject()
                .put("name", name)
                .toString()
                .toRequestBody(jsonMedia)

            val request = authRequestBuilder("${Constants.API_BASE}/developer/keys", authToken)
                .post(requestBody)
                .header("Content-Type", "application/json")
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string().orEmpty()
            val json = if (body.isBlank()) JSONObject() else JSONObject(body)

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception(parseErrorMessage(json, "Failed to create API key")))
            }

            val data = json.optJSONObject("data") ?: json
            Result.success(parseApiKey(data))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun revokeDeveloperKey(authToken: String, keyId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val request = authRequestBuilder(
                "${Constants.API_BASE}/developer/keys?id=${urlEncode(keyId)}",
                authToken
            )
                .delete()
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string().orEmpty()
            val json = if (body.isBlank()) JSONObject() else JSONObject(body)

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception(parseErrorMessage(json, "Failed to revoke API key")))
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteAccountData(authToken: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val request = authRequestBuilder("${Constants.API_BASE}/auth/account", authToken)
                .delete()
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string().orEmpty()
            val json = if (body.isBlank()) JSONObject() else JSONObject(body)

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception(parseErrorMessage(json, "Failed to delete account data")))
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ──────────────────────────────────────────────────────────────────
    // CONTACT / SUPPORT
    // ──────────────────────────────────────────────────────────────────

    suspend fun sendContactMessage(
        name: String,
        email: String,
        message: String,
        browserToken: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject()
                .put("name", name)
                .put("email", email)
                .put("message", message)

            val request = Request.Builder()
                .url("${Constants.API_BASE}/contact")
                .post(payload.toString().toRequestBody(jsonMedia))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("X-Browser-Token", browserToken)
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string().orEmpty()
            val json = if (body.isBlank()) JSONObject() else JSONObject(body)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception(parseErrorMessage(json, "Could not send message (${response.code})")))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ──────────────────────────────────────────────────────────────────
    // ADMIN
    // ──────────────────────────────────────────────────────────────────

    suspend fun fetchAdminSnapshot(
        adminSecret: String,
        filter: String = "all",
        emailQuery: String = "",
        rangeKey: String = "7d"
    ): Result<AdminSnapshot> = withContext(Dispatchers.IO) {
        try {
            val params = buildString {
                append("filter=").append(urlEncode(filter))
                if (emailQuery.isNotBlank()) {
                    append("&email=").append(urlEncode(emailQuery))
                }
                append("&range=").append(urlEncode(rangeKey))
            }
            val request = Request.Builder()
                .url("${Constants.API_BASE}/admin/users?$params")
                .get()
                .header("Accept", "application/json")
                .header("X-Admin-Secret", adminSecret)
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string().orEmpty()
            val json = if (body.isBlank()) JSONObject() else JSONObject(body)

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception(parseErrorMessage(json, "Admin auth failed (${response.code})")))
            }

            Result.success(parseAdminSnapshot(json, rangeKey))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun adminUpdatePlan(
        adminSecret: String,
        uid: String,
        plan: String,
        duration: String = "lifetime"
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject()
                .put("uid", uid)
                .put("plan", plan)
                .put("duration", duration)

            val request = Request.Builder()
                .url("${Constants.API_BASE}/admin/users")
                .patch(payload.toString().toRequestBody(jsonMedia))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("X-Admin-Secret", adminSecret)
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string().orEmpty()
            val json = if (body.isBlank()) JSONObject() else JSONObject(body)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception(parseErrorMessage(json, "Update failed (${response.code})")))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ──────────────────────────────────────────────────────────────────
    // HELPERS
    // ──────────────────────────────────────────────────────────────────

    private fun authRequestBuilder(url: String, authToken: String): Request.Builder =
        Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .header("Authorization", "Bearer $authToken")

    private fun urlEncode(value: String): String =
        java.net.URLEncoder.encode(value, "UTF-8")

    private fun parseErrorMessage(json: JSONObject, fallback: String): String {
        val nested = json.optJSONObject("error")
        return nested?.optString("message")?.takeIf { it.isNotBlank() }
            ?: nested?.optString("code")?.takeIf { it.isNotBlank() }
            ?: json.optString("error").takeIf { it.isNotBlank() }
            ?: json.optString("message").takeIf { it.isNotBlank() }
            ?: fallback
    }

    private fun parseInbox(json: JSONObject): Inbox = Inbox(
        id = json.optString("id", ""),
        email = json.optString("email", ""),
        createdAt = json.optLong("created_at", System.currentTimeMillis() / 1000),
        expiresAt = json.optLong("expires_at", 0),
        ownerToken = json.optString("owner_token", ""),
        plan = json.optString("plan", "free"),
        creationsToday = json.optInt("creations_today", 0),
        maxCreations = json.optInt("max_creations", 3)
    )

    private fun parseMessage(json: JSONObject, inboxId: String): MailMessage = MailMessage(
        id = json.optString("id", ""),
        inboxId = json.optString("inbox_id", inboxId),
        from = json.optString("from_address").ifBlank { json.optString("from", "Unknown") },
        fromName = json.optString("from_name").takeIf { it.isNotBlank() },
        subject = json.optString("subject", "(No Subject)"),
        bodyHtml = json.optString("body_html", ""),
        bodyText = json.optString("body_text", ""),
        receivedAt = json.optLong("received_at", System.currentTimeMillis() / 1000),
        otp = json.optString("otp").takeIf { it.isNotBlank() }
    )

    private fun parseApiKey(json: JSONObject): ApiKey = ApiKey(
        id = json.optString("id", ""),
        name = json.optString("name", "Default Key"),
        keyPrefix = json.optString("key_prefix", ""),
        createdAt = json.optLong("created_at", System.currentTimeMillis() / 1000),
        lastUsed = if (json.isNull("last_used_at")) null else json.optLong("last_used_at"),
        isActive = json.optInt("is_active", 1) == 1 || json.optBoolean("is_active", false),
        plainKey = json.optString("key").takeIf { it.isNotBlank() }
    )

    private fun parseAdminSnapshot(json: JSONObject, rangeKey: String): AdminSnapshot {
        val users = json.optJSONArray("users") ?: JSONArray()
        val statsObj = json.optJSONObject("stats") ?: JSONObject()
        val activityObj = json.optJSONObject("activity") ?: JSONObject()
        val analyticsSource = json.optString("analytics_source", "live_tables")

        val parsedUsers = (0 until users.length()).map { i -> parseAdminUser(users.getJSONObject(i)) }

        val stats = AdminStats(
            totalUsers = statsObj.optJSONObject("users")?.optInt("total", parsedUsers.size) ?: parsedUsers.size,
            proUsers = statsObj.optJSONObject("users")?.optInt("pro", 0) ?: 0,
            developerUsers = statsObj.optJSONObject("users")?.optInt("developer", 0) ?: 0,
            activeInboxes = statsObj.optJSONObject("inboxes")?.optInt("active", 0) ?: 0,
            totalInboxesCreated = statsObj.optJSONObject("inboxes")?.optInt("total", 0) ?: 0,
            totalMessagesReceived = statsObj.optJSONObject("messages")?.optInt("total", 0) ?: 0,
            rangeKey = rangeKey
        )

        val flat = mutableListOf<AdminActivity>()
        listOf("inboxes" to "inbox", "messages" to "message", "api" to "api", "logins" to "login")
            .forEach { (key, label) ->
                val arr = activityObj.optJSONArray(key) ?: return@forEach
                for (i in 0 until arr.length()) {
                    val row = arr.getJSONObject(i)
                    flat += AdminActivity(
                        type = label,
                        actor = row.optString("actor")
                            .ifBlank { row.optString("email") }
                            .ifBlank { row.optString("uid", "Unknown") },
                        subject = row.optString("subject")
                            .ifBlank { row.optString("inbox_email") }
                            .ifBlank { row.optString("endpoint", "") },
                        ip = row.optString("ip").takeIf { it.isNotBlank() },
                        createdAt = row.optLong("created_at", 0L)
                    )
                }
            }
        flat.sortByDescending { it.createdAt }

        return AdminSnapshot(
            users = parsedUsers,
            stats = stats,
            activity = flat.take(40),
            analyticsSource = analyticsSource
        )
    }

    private fun parseAdminUser(json: JSONObject): AdminUser = AdminUser(
        uid = json.optString("uid", ""),
        email = json.optString("email", ""),
        plan = json.optString("plan", "free"),
        planExpiresAt = if (json.isNull("plan_expires_at")) null else json.optLong("plan_expires_at"),
        planSource = json.optString("plan_source").takeIf { it.isNotBlank() },
        createdAt = json.optLong("created_at", 0L),
        updatedAt = json.optLong("updated_at", 0L),
        activeInboxes = json.optInt("active_inboxes", 0),
        totalInboxes = json.optInt("total_inboxes", 0),
        totalMessages = json.optInt("total_messages", 0)
    )
}
