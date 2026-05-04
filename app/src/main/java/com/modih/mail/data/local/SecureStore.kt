package com.modih.mail.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Keystore-backed encrypted SharedPreferences for tokens that the handoff
 * doc explicitly calls out as "passwords" — owner tokens for inbox access,
 * the operator's admin secret if they ever entered one, and any future
 * Developer-plan API keys.
 *
 * Backed by AES-256-GCM with a master key wrapped by the AndroidKeyStore.
 * Stored at rest in /data/data/com.modih.mail/shared_prefs/modih_secure.xml,
 * but the values are ciphertext so a raw filesystem dump on a rooted device
 * does not expose the tokens.
 *
 * Non-sensitive prefs (current inbox metadata, plan, last-known UID) keep
 * living in DataStore — only secrets move here.
 */
class SecureStore(context: Context) {

    private val prefs: SharedPreferences = run {
        val masterKey = MasterKey.Builder(context.applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context.applicationContext,
            FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun get(key: String): String? = prefs.getString(key, null)?.takeIf { it.isNotBlank() }

    fun put(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val FILE_NAME = "modih_secure"

        const val KEY_OWNER_TOKEN = "current_inbox_owner_token"
        const val KEY_ADMIN_SECRET = "admin_secret"
    }
}
