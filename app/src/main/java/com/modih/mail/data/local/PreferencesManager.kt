package com.modih.mail.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "modih_prefs")

/**
 * Hybrid prefs:
 *  - Non-sensitive values live in DataStore (browser_token UUID, inbox metadata,
 *    user plan, list of session inboxes, etc.) — Flow-friendly, easy to observe.
 *  - Sensitive values (owner tokens, admin secret) live in [SecureStore], which
 *    wraps EncryptedSharedPreferences with an AndroidKeyStore-backed master key.
 *
 * The handoff doc explicitly calls out: "Owner tokens... store in
 * EncryptedSharedPreferences or the Android Keystore. Never in plain
 * SharedPreferences." Since DataStore is just SharedPreferences-on-Files
 * underneath, owner tokens used to be at risk on a rooted device. This
 * class also handles a one-shot migration of any legacy owner_token /
 * admin_secret values left in DataStore by v1.0 / v1.1.
 */
class PreferencesManager(private val context: Context) {

    private val secureStore: SecureStore by lazy { SecureStore(context.applicationContext) }

    companion object {
        private val BROWSER_TOKEN = stringPreferencesKey("browser_token")
        private val CURRENT_INBOX_ID = stringPreferencesKey("current_inbox_id")
        private val CURRENT_INBOX_EMAIL = stringPreferencesKey("current_inbox_email")

        // Legacy DataStore key — kept only for the one-shot migration to SecureStore.
        private val LEGACY_OWNER_TOKEN = stringPreferencesKey("current_inbox_owner_token")
        private val LEGACY_ADMIN_SECRET = stringPreferencesKey("admin_secret")

        private val CURRENT_INBOX_CREATED_AT = longPreferencesKey("current_inbox_created_at")
        private val CURRENT_INBOX_EXPIRES_AT = longPreferencesKey("current_inbox_expires_at")
        private val SESSION_INBOXES_JSON = stringPreferencesKey("session_inboxes_json")
        private val RESERVED_ALIASES_JSON = stringPreferencesKey("reserved_aliases_json")
        private val USER_UID = stringPreferencesKey("user_uid")
        private val USER_EMAIL = stringPreferencesKey("user_email")
        private val USER_PLAN = stringPreferencesKey("user_plan")
    }

    suspend fun getBrowserToken(): String {
        val existing = context.dataStore.data.map { it[BROWSER_TOKEN] }.first()
        if (!existing.isNullOrBlank()) return existing
        val token = UUID.randomUUID().toString()
        context.dataStore.edit { it[BROWSER_TOKEN] = token }
        return token
    }

    suspend fun saveCurrentInbox(id: String, email: String, ownerToken: String, createdAt: Long, expiresAt: Long) {
        secureStore.put(SecureStore.KEY_OWNER_TOKEN, ownerToken)
        context.dataStore.edit { prefs ->
            prefs[CURRENT_INBOX_ID] = id
            prefs[CURRENT_INBOX_EMAIL] = email
            prefs[CURRENT_INBOX_CREATED_AT] = createdAt
            prefs[CURRENT_INBOX_EXPIRES_AT] = expiresAt
            // Strip any legacy plaintext owner_token left over from v1.0/v1.1.
            prefs.remove(LEGACY_OWNER_TOKEN)
        }
    }

    suspend fun clearCurrentInbox() {
        secureStore.remove(SecureStore.KEY_OWNER_TOKEN)
        context.dataStore.edit { prefs ->
            prefs.remove(CURRENT_INBOX_ID)
            prefs.remove(CURRENT_INBOX_EMAIL)
            prefs.remove(LEGACY_OWNER_TOKEN)
            prefs.remove(CURRENT_INBOX_CREATED_AT)
            prefs.remove(CURRENT_INBOX_EXPIRES_AT)
        }
    }

    val currentInboxFlow: Flow<SavedInbox?> = context.dataStore.data.map { prefs ->
        val id = prefs[CURRENT_INBOX_ID] ?: return@map null
        val expiresAt = prefs[CURRENT_INBOX_EXPIRES_AT] ?: 0L
        if (expiresAt > 0 && System.currentTimeMillis() / 1000 > expiresAt) return@map null
        // Prefer the encrypted store; fall back to legacy DataStore value (and
        // migrate it forward) for installs upgrading from v1.0/v1.1.
        val ownerToken = secureStore.get(SecureStore.KEY_OWNER_TOKEN)
            ?: migrateLegacy(prefs[LEGACY_OWNER_TOKEN], SecureStore.KEY_OWNER_TOKEN, LEGACY_OWNER_TOKEN)
            ?: ""
        SavedInbox(
            id = id,
            email = prefs[CURRENT_INBOX_EMAIL] ?: "",
            ownerToken = ownerToken,
            createdAt = prefs[CURRENT_INBOX_CREATED_AT] ?: 0L,
            expiresAt = expiresAt
        )
    }

    suspend fun currentInboxOnce(): SavedInbox? = currentInboxFlow.first()

    suspend fun saveUserPlan(uid: String, email: String, plan: String) {
        context.dataStore.edit { prefs ->
            prefs[USER_UID] = uid
            prefs[USER_EMAIL] = email
            prefs[USER_PLAN] = plan
        }
    }

    suspend fun clearUser() {
        context.dataStore.edit { prefs ->
            prefs.remove(USER_UID)
            prefs.remove(USER_EMAIL)
            prefs.remove(USER_PLAN)
        }
    }

    val userPlanFlow: Flow<SavedUser?> = context.dataStore.data.map { prefs ->
        val uid = prefs[USER_UID] ?: return@map null
        SavedUser(
            uid = uid,
            email = prefs[USER_EMAIL] ?: "",
            plan = prefs[USER_PLAN] ?: "free"
        )
    }

    // ─── Admin gate ────────────────────────────────────────────────────────
    val adminSecretFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        secureStore.get(SecureStore.KEY_ADMIN_SECRET)
            ?: migrateLegacy(prefs[LEGACY_ADMIN_SECRET], SecureStore.KEY_ADMIN_SECRET, LEGACY_ADMIN_SECRET)
    }

    suspend fun adminSecretOnce(): String? = adminSecretFlow.first()

    suspend fun saveAdminSecret(secret: String) {
        secureStore.put(SecureStore.KEY_ADMIN_SECRET, secret)
        // Defensive: if any legacy plaintext copy is still around, drop it.
        context.dataStore.edit { prefs -> prefs.remove(LEGACY_ADMIN_SECRET) }
    }

    suspend fun clearAdminSecret() {
        secureStore.remove(SecureStore.KEY_ADMIN_SECRET)
        context.dataStore.edit { prefs -> prefs.remove(LEGACY_ADMIN_SECRET) }
    }

    /**
     * One-shot migration from a legacy plaintext value to SecureStore.
     * Returns the value (if any) so the caller can use it on this read,
     * and schedules a delete of the legacy entry on the next write.
     *
     * The legacy delete is intentionally fire-and-forget — DataStore needs
     * a suspend `edit` and we can't suspend from inside `map { }`. The next
     * `saveCurrentInbox` / `saveAdminSecret` call removes the stale key.
     */
    private fun migrateLegacy(
        legacyValue: String?,
        secureKey: String,
        @Suppress("UNUSED_PARAMETER") legacyDataStoreKey: Preferences.Key<String>
    ): String? {
        val v = legacyValue?.takeIf { it.isNotBlank() } ?: return null
        secureStore.put(secureKey, v)
        return v
    }
}

data class SavedInbox(
    val id: String,
    val email: String,
    val ownerToken: String,
    val createdAt: Long,
    val expiresAt: Long
)

data class SavedUser(
    val uid: String,
    val email: String,
    val plan: String
)
