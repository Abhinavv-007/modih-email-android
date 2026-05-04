package com.modih.mail.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "modih_prefs")

class PreferencesManager(private val context: Context) {

    companion object {
        private val BROWSER_TOKEN = stringPreferencesKey("browser_token")
        private val CURRENT_INBOX_ID = stringPreferencesKey("current_inbox_id")
        private val CURRENT_INBOX_EMAIL = stringPreferencesKey("current_inbox_email")
        private val CURRENT_INBOX_OWNER_TOKEN = stringPreferencesKey("current_inbox_owner_token")
        private val CURRENT_INBOX_CREATED_AT = longPreferencesKey("current_inbox_created_at")
        private val CURRENT_INBOX_EXPIRES_AT = longPreferencesKey("current_inbox_expires_at")
        private val SESSION_INBOXES_JSON = stringPreferencesKey("session_inboxes_json")
        private val RESERVED_ALIASES_JSON = stringPreferencesKey("reserved_aliases_json")
        private val USER_UID = stringPreferencesKey("user_uid")
        private val USER_EMAIL = stringPreferencesKey("user_email")
        private val USER_PLAN = stringPreferencesKey("user_plan")
    }

    suspend fun getBrowserToken(): String {
        val prefs = context.dataStore.data.map { it[BROWSER_TOKEN] }
        var token: String? = null
        prefs.collect { token = it }
        if (token == null) {
            token = UUID.randomUUID().toString()
            context.dataStore.edit { it[BROWSER_TOKEN] = token!! }
        }
        return token!!
    }

    suspend fun saveCurrentInbox(id: String, email: String, ownerToken: String, createdAt: Long, expiresAt: Long) {
        context.dataStore.edit { prefs ->
            prefs[CURRENT_INBOX_ID] = id
            prefs[CURRENT_INBOX_EMAIL] = email
            prefs[CURRENT_INBOX_OWNER_TOKEN] = ownerToken
            prefs[CURRENT_INBOX_CREATED_AT] = createdAt
            prefs[CURRENT_INBOX_EXPIRES_AT] = expiresAt
        }
    }

    suspend fun clearCurrentInbox() {
        context.dataStore.edit { prefs ->
            prefs.remove(CURRENT_INBOX_ID)
            prefs.remove(CURRENT_INBOX_EMAIL)
            prefs.remove(CURRENT_INBOX_OWNER_TOKEN)
            prefs.remove(CURRENT_INBOX_CREATED_AT)
            prefs.remove(CURRENT_INBOX_EXPIRES_AT)
        }
    }

    val currentInboxFlow: Flow<SavedInbox?> = context.dataStore.data.map { prefs ->
        val id = prefs[CURRENT_INBOX_ID] ?: return@map null
        val expiresAt = prefs[CURRENT_INBOX_EXPIRES_AT] ?: 0L
        if (expiresAt > 0 && System.currentTimeMillis() / 1000 > expiresAt) return@map null
        SavedInbox(
            id = id,
            email = prefs[CURRENT_INBOX_EMAIL] ?: "",
            ownerToken = prefs[CURRENT_INBOX_OWNER_TOKEN] ?: "",
            createdAt = prefs[CURRENT_INBOX_CREATED_AT] ?: 0L,
            expiresAt = expiresAt
        )
    }

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
