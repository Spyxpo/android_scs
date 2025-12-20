package com.spyxpo.scs.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import com.spyxpo.scs.models.ScsUser

/**
 * Secure storage for session data including auth tokens and user info.
 * Uses Android's EncryptedSharedPreferences for secure storage.
 */
class SessionStorage(context: Context) {

    private val gson = Gson()
    private val prefs: SharedPreferences

    init {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        prefs = EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /**
     * Save the authentication token
     */
    fun saveToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    /**
     * Get the stored authentication token
     */
    fun getToken(): String? {
        return prefs.getString(KEY_TOKEN, null)
    }

    /**
     * Remove the authentication token
     */
    fun removeToken() {
        prefs.edit().remove(KEY_TOKEN).apply()
    }

    /**
     * Save the current user
     */
    fun saveUser(user: ScsUser) {
        val userJson = gson.toJson(user)
        prefs.edit().putString(KEY_USER, userJson).apply()
    }

    /**
     * Get the stored user
     */
    fun getUser(): ScsUser? {
        val userJson = prefs.getString(KEY_USER, null) ?: return null
        return try {
            gson.fromJson(userJson, ScsUser::class.java)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Remove the stored user
     */
    fun removeUser() {
        prefs.edit().remove(KEY_USER).apply()
    }

    /**
     * Clear all stored session data
     */
    fun clear() {
        prefs.edit().clear().apply()
    }

    /**
     * Check if there is an active session
     */
    fun hasSession(): Boolean {
        return getToken() != null
    }

    companion object {
        private const val PREFS_NAME = "scs_session"
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_USER = "current_user"
    }
}
