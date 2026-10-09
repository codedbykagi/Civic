package com.civic.app.data.auth

import android.content.Context
import android.content.SharedPreferences
import java.util.UUID

/**
 * Persists the signed-in identity across restarts.
 *
 * Plain app-private SharedPreferences: on Android this file is inside the app sandbox, unreadable by other apps.
 * It is excluded from cloud backup in AndroidManifest.xml so tokens never leave the device. If this app ever
 * handles anything more sensitive than a report feed, move the two token keys to the Keystore.
 */
class SessionStore(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("civic_session", Context.MODE_PRIVATE)

    /** Stable per-install id. Doubles as the device profile's identity and as the sync idempotency key. */
    val installId: String
        get() = prefs.getString(KEY_INSTALL_ID, null) ?: UUID.randomUUID().toString().also {
            prefs.edit().putString(KEY_INSTALL_ID, it).apply()
        }

    fun readAccount(): Account? {
        val id = prefs.getString(KEY_USER_ID, null) ?: return null
        val displayName = prefs.getString(KEY_DISPLAY_NAME, null) ?: return null
        return Account(
            id = id,
            username = prefs.getString(KEY_USERNAME, null) ?: displayName.toUsername(),
            displayName = displayName,
            avatar = prefs.getString(KEY_AVATAR, null),
            bio = prefs.getString(KEY_BIO, null),
            email = prefs.getString(KEY_EMAIL, null),
            isCloud = prefs.getBoolean(KEY_IS_CLOUD, false),
        )
    }

    fun writeAccount(account: Account) = prefs.edit()
        .putString(KEY_USER_ID, account.id)
        .putString(KEY_USERNAME, account.username)
        .putString(KEY_DISPLAY_NAME, account.displayName)
        .putString(KEY_AVATAR, account.avatar)
        .putString(KEY_BIO, account.bio)
        .putString(KEY_EMAIL, account.email)
        .putBoolean(KEY_IS_CLOUD, account.isCloud)
        .apply()

    fun readSession(): CloudSession? {
        val userId = prefs.getString(KEY_USER_ID, null) ?: return null
        val access = prefs.getString(KEY_ACCESS_TOKEN, null) ?: return null
        val refresh = prefs.getString(KEY_REFRESH_TOKEN, null) ?: return null
        return CloudSession(userId, access, refresh, prefs.getLong(KEY_EXPIRES_AT, 0))
    }

    fun writeSession(session: CloudSession) = prefs.edit()
        .putString(KEY_ACCESS_TOKEN, session.accessToken)
        .putString(KEY_REFRESH_TOKEN, session.refreshToken)
        .putLong(KEY_EXPIRES_AT, session.expiresAtMillis)
        .apply()

    /** Signing out drops the tokens but keeps the install id, so queued local reports keep their identity. */
    fun clearSession() = prefs.edit()
        .remove(KEY_ACCESS_TOKEN)
        .remove(KEY_REFRESH_TOKEN)
        .remove(KEY_EXPIRES_AT)
        .apply()

    fun clearAll() = prefs.edit()
        .remove(KEY_USER_ID)
        .remove(KEY_USERNAME)
        .remove(KEY_DISPLAY_NAME)
        .remove(KEY_AVATAR)
        .remove(KEY_BIO)
        .remove(KEY_EMAIL)
        .remove(KEY_IS_CLOUD)
        .remove(KEY_ACCESS_TOKEN)
        .remove(KEY_REFRESH_TOKEN)
        .remove(KEY_EXPIRES_AT)
        .apply()

    private companion object {
        const val KEY_INSTALL_ID = "install_id"
        const val KEY_USER_ID = "user_id"
        const val KEY_USERNAME = "username"
        const val KEY_DISPLAY_NAME = "display_name"
        const val KEY_AVATAR = "avatar"
        const val KEY_BIO = "bio"
        const val KEY_EMAIL = "email"
        const val KEY_IS_CLOUD = "is_cloud"
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
        const val KEY_EXPIRES_AT = "expires_at"
    }
}

/**
 * Derives a handle from a display name: lowercase, letters/digits/underscore only, 3–20 chars.
 * Matches the `username` check constraint in infra/supabase/schema.sql, so a locally chosen handle stays valid
 * if the profile is later pushed to the server.
 */
fun String.toUsername(): String {
    // ASCII only, deliberately: Char.isLetterOrDigit() is true for 'ë' and for Devanagari, and the server's
    // check constraint would then reject the whole profile. A name with nothing ASCII in it falls back below.
    val cleaned = lowercase().map { if (it in 'a'..'z' || it in '0'..'9') it else '_' }
        .joinToString("")
        .trim('_')
        .replace(Regex("_+"), "_")
        .take(20)
    return if (cleaned.isEmpty()) "civic_user" else cleaned.padEnd(3, '1')
}
