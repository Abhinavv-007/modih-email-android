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

class MailRepository {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val jsonMedia = "application/json".toMediaType()

    suspend fun createInbox(
        prefix: String? = null,
        browserToken: String,
        authToken: String? = null,
        ownerToken: String? = null
    ): Result<Inbox> = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject()
            if (prefix != null) body.put("prefix", prefix)

            val builder = Request.Builder()
                .url("${Constants.API_BASE}/inbox")
                .post(body.toString().toRequestBody(jsonMedia))
                .header("Content-Type", "application/json")
                .header("X-Browser-Token", browserToken)

            if (authToken != null) builder.header("Authorization", "Bearer $authToken")
            if (ownerToken != null) builder.header("X-Owner-Token", ownerToken)

            val response = client.newCall(builder.build()).execute()
            val responseBody = response.body?.string() ?: "{}"
            val json = JSONObject(responseBody)

            if (!response.isSuccessful) {
                val error = json.optJSONObject("error")
                val message = error?.optString("message") ?: "Failed to create inbox"
                return@withContext Result.failure(Exception(message))
            }

            val data = json.optJSONObject("data") ?: json
            Result.success(parseInbox(data))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMessages(
        inboxId: String,
        ownerToken: String,
        authToken: String? = null
    ): Result<List<MailMessage>> = withContext(Dispatchers.IO) {
        try {
            val builder = Request.Builder()
                .url("${Constants.API_BASE}/inbox/$inboxId/messages")
                .get()
                .header("X-Owner-Token", ownerToken)

            if (authToken != null) builder.header("Authorization", "Bearer $authToken")

            val response = client.newCall(builder.build()).execute()
            val responseBody = response.body?.string() ?: "{}"
            val json = JSONObject(responseBody)

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Failed to fetch messages"))
            }

            val dataArray = json.optJSONArray("data") ?: JSONArray()
            val messages = (0 until dataArray.length()).map { i ->
                parseMessage(dataArray.getJSONObject(i))
            }
            Result.success(messages)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMessage(
        inboxId: String,
        messageId: String,
        ownerToken: String,
        authToken: String? = null
    ): Result<MailMessage> = withContext(Dispatchers.IO) {
        try {
            val builder = Request.Builder()
                .url("${Constants.API_BASE}/inbox/$inboxId/messages/$messageId")
                .get()
                .header("X-Owner-Token", ownerToken)

            if (authToken != null) builder.header("Authorization", "Bearer $authToken")

            val response = client.newCall(builder.build()).execute()
            val responseBody = response.body?.string() ?: "{}"
            val json = JSONObject(responseBody)

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Failed to fetch message"))
            }

            val data = json.optJSONObject("data") ?: json
            Result.success(parseMessage(data))
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
                .url("${Constants.API_BASE}/inbox/$inboxId")
                .delete()
                .header("X-Owner-Token", ownerToken)

            if (authToken != null) builder.header("Authorization", "Bearer $authToken")

            val response = client.newCall(builder.build()).execute()
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("Delete failed"))
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
                .url("${Constants.API_BASE}/inbox/$inboxId/messages/$messageId")
                .delete()
                .header("X-Owner-Token", ownerToken)

            if (authToken != null) builder.header("Authorization", "Bearer $authToken")

            val response = client.newCall(builder.build()).execute()
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("Delete failed"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserProfile(authToken: String): Result<UserPlan> = withContext(Dispatchers.IO) {
        try {
            val request = authRequestBuilder("${Constants.API_BASE}/auth/me", authToken)
                .get()
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: "{}"
            val json = JSONObject(body)

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Auth check failed"))
            }

            Result.success(
                UserPlan(
                    uid = json.optString("uid", ""),
                    email = json.optString("email", ""),
                    plan = Plan.fromString(json.optString("plan", "free"))
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDeveloperUsage(authToken: String): Result<UsageStats> = withContext(Dispatchers.IO) {
        try {
            val request = authRequestBuilder("${Constants.API_BASE}/developer/usage", authToken)
                .get()
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: "{}"
            val json = JSONObject(body)

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
                    apiKeysLimit = 10
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
            val body = response.body?.string() ?: "{}"
            val json = JSONObject(body)

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
            val body = response.body?.string() ?: "{}"
            val json = JSONObject(body)

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
                "${Constants.API_BASE}/developer/keys?id=$keyId",
                authToken
            )
                .delete()
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: "{}"
            val json = JSONObject(body)

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
            val body = response.body?.string() ?: "{}"
            val json = JSONObject(body)

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception(parseErrorMessage(json, "Failed to delete account data")))
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun authRequestBuilder(url: String, authToken: String): Request.Builder =
        Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $authToken")

    private fun parseErrorMessage(json: JSONObject, fallback: String): String {
        val nested = json.optJSONObject("error")
        return nested?.optString("message")
            ?: nested?.optString("code")
            ?: json.optString("error", fallback)
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

    private fun parseMessage(json: JSONObject): MailMessage = MailMessage(
        id = json.optString("id", ""),
        inboxId = json.optString("inbox_id", ""),
        from = json.optString("from", "Unknown"),
        subject = json.optString("subject", "(No Subject)"),
        bodyHtml = json.optString("body_html", ""),
        bodyText = json.optString("body_text", ""),
        receivedAt = json.optLong("received_at", System.currentTimeMillis() / 1000),
        otp = json.optString("otp", null)
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
}
