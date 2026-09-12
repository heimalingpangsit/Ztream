package com.zaaam.zreming.data.local

import android.content.Context
import androidx.core.content.edit
import com.zaaam.zreming.data.model.UserDto
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences("ztream_session", Context.MODE_PRIVATE)

    fun save(token: String, user: UserDto) {
        prefs.edit {
            putString("token", token)
            putString("user_id", user.id)
            putString("username", user.username)
            putString("role", user.role)
            putBoolean("vip", user.vip)
            putString("expires_at", user.expiresAt)
        }
    }

    fun getToken(): String? = prefs.getString("token", null)

    fun saveRememberedUsername(username: String) {
        prefs.edit { putString("remembered_username", username) }
    }

    fun getRememberedUsername(): String? = prefs.getString("remembered_username", null)

    fun clearRememberedUsername() {
        prefs.edit { remove("remembered_username") }
    }

    fun getLastSeenNotificationId(): String? = prefs.getString("last_seen_notif_id", null)

    fun saveLastSeenNotificationId(id: String) {
        prefs.edit { putString("last_seen_notif_id", id) }
    }

    fun getUser(): UserDto? {
        val id = prefs.getString("user_id", null) ?: return null
        val username = prefs.getString("username", null) ?: return null
        return UserDto(
            id = id,
            username = username,
            role = prefs.getString("role", "USER") ?: "USER",
            vip = prefs.getBoolean("vip", false),
            expiresAt = prefs.getString("expires_at", null),
        )
    }

    fun isLoggedIn(): Boolean = getToken() != null

    fun clear() {
        prefs.edit { clear() }
    }
}
