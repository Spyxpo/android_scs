package com.spyxpo.scs.services

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.spyxpo.scs.ScsException
import com.spyxpo.scs.models.AuthResponse
import com.spyxpo.scs.models.ScsUser
import com.spyxpo.scs.models.UsersListResponse
import com.spyxpo.scs.utils.ScsHttpClient
import com.spyxpo.scs.utils.SessionStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Service for authentication operations.
 *
 * Example usage:
 * ```kotlin
 * // Register a new user
 * val user = scs.auth.register("email@example.com", "password", "John Doe")
 *
 * // Login
 * val user = scs.auth.login("email@example.com", "password")
 *
 * // Observe auth state
 * scs.auth.currentUserFlow.collect { user ->
 *     // Handle user state changes
 * }
 *
 * // Logout
 * scs.auth.logout()
 * ```
 */
class AuthService(
    private val httpClient: ScsHttpClient,
    private val sessionStorage: SessionStorage
) {
    private val gson = Gson()

    private val _currentUserFlow = MutableStateFlow<ScsUser?>(sessionStorage.getUser())

    /**
     * Flow of the current user state. Emits null when logged out.
     */
    val currentUserFlow: Flow<ScsUser?> = _currentUserFlow.asStateFlow()

    private val _currentUserLiveData = MutableLiveData<ScsUser?>(sessionStorage.getUser())

    /**
     * LiveData of the current user state. Emits null when logged out.
     */
    val currentUserLiveData: LiveData<ScsUser?> = _currentUserLiveData

    /**
     * Get the current user (if logged in)
     */
    val currentUser: ScsUser?
        get() = _currentUserFlow.value

    /**
     * Check if a user is currently logged in
     */
    val isLoggedIn: Boolean
        get() = sessionStorage.hasSession()

    /**
     * Register a new user
     *
     * @param email User's email address
     * @param password User's password
     * @param displayName Optional display name
     * @param customData Optional custom data to store with the user
     * @return The registered user
     */
    suspend fun register(
        email: String,
        password: String,
        displayName: String? = null,
        customData: Map<String, Any>? = null
    ): ScsUser {
        val body = mutableMapOf<String, Any?>(
            "email" to email,
            "password" to password
        )
        displayName?.let { body["displayName"] = it }
        customData?.let { body["customData"] = it }

        val response = httpClient.post("/auth/project/register", body)
        val authResponse = gson.fromJson(response, AuthResponse::class.java)

        // Save session
        sessionStorage.saveToken(authResponse.token)
        sessionStorage.saveUser(authResponse.user)

        // Update state
        _currentUserFlow.value = authResponse.user
        _currentUserLiveData.postValue(authResponse.user)

        return authResponse.user
    }

    /**
     * Login with email and password
     *
     * @param email User's email address
     * @param password User's password
     * @return The logged in user
     */
    suspend fun login(email: String, password: String): ScsUser {
        val body = mapOf(
            "email" to email,
            "password" to password
        )

        val response = httpClient.post("/auth/project/login", body)
        val authResponse = gson.fromJson(response, AuthResponse::class.java)

        // Save session
        sessionStorage.saveToken(authResponse.token)
        sessionStorage.saveUser(authResponse.user)

        // Update state
        _currentUserFlow.value = authResponse.user
        _currentUserLiveData.postValue(authResponse.user)

        return authResponse.user
    }

    /**
     * Get the current user from the server
     *
     * @return The current user
     */
    suspend fun getCurrentUser(): ScsUser {
        val response = httpClient.get("/auth/project/me")
        val user = gson.fromJson(response.getAsJsonObject("user"), ScsUser::class.java)

        // Update local state
        sessionStorage.saveUser(user)
        _currentUserFlow.value = user
        _currentUserLiveData.postValue(user)

        return user
    }

    /**
     * Update the current user's profile
     *
     * @param displayName New display name
     * @param customData New custom data (merges with existing)
     * @return The updated user
     */
    suspend fun updateProfile(
        displayName: String? = null,
        customData: Map<String, Any>? = null
    ): ScsUser {
        val body = mutableMapOf<String, Any?>()
        displayName?.let { body["displayName"] = it }
        customData?.let { body["customData"] = it }

        val response = httpClient.put("/auth/project/profile", body)
        val user = gson.fromJson(response.getAsJsonObject("user"), ScsUser::class.java)

        // Update local state
        sessionStorage.saveUser(user)
        _currentUserFlow.value = user
        _currentUserLiveData.postValue(user)

        return user
    }

    /**
     * Change the current user's password
     *
     * @param currentPassword Current password
     * @param newPassword New password
     */
    suspend fun changePassword(currentPassword: String, newPassword: String) {
        val body = mapOf(
            "currentPassword" to currentPassword,
            "newPassword" to newPassword
        )

        httpClient.put("/auth/project/password", body)
    }

    /**
     * Delete the current user's account
     */
    suspend fun deleteAccount() {
        httpClient.delete("/auth/project/account")
        logout()
    }

    /**
     * Logout the current user
     */
    fun logout() {
        sessionStorage.clear()
        _currentUserFlow.value = null
        _currentUserLiveData.postValue(null)
    }

    // ==================== Admin Methods ====================

    /**
     * List all users (admin only)
     *
     * @param limit Maximum number of users to return
     * @param skip Number of users to skip
     * @return List of users with pagination info
     */
    suspend fun listUsers(limit: Int = 50, skip: Int = 0): UsersListResponse {
        val params = mapOf(
            "limit" to limit.toString(),
            "skip" to skip.toString()
        )

        val response = httpClient.get("/auth/project/users", params)
        return gson.fromJson(response, UsersListResponse::class.java)
    }

    /**
     * Get a user by UID (admin only)
     *
     * @param uid User's unique identifier
     * @return The user
     */
    suspend fun getUser(uid: String): ScsUser {
        val response = httpClient.get("/auth/project/users/$uid")
        return gson.fromJson(response.getAsJsonObject("user"), ScsUser::class.java)
    }

    /**
     * Disable a user account (admin only)
     *
     * @param uid User's unique identifier
     */
    suspend fun disableUser(uid: String) {
        val body = mapOf("disabled" to true)
        httpClient.put("/auth/project/users/$uid/status", body)
    }

    /**
     * Enable a user account (admin only)
     *
     * @param uid User's unique identifier
     */
    suspend fun enableUser(uid: String) {
        val body = mapOf("disabled" to false)
        httpClient.put("/auth/project/users/$uid/status", body)
    }

    /**
     * Delete a user (admin only)
     *
     * @param uid User's unique identifier
     */
    suspend fun deleteUser(uid: String) {
        httpClient.delete("/auth/project/users/$uid")
    }

    /**
     * Reload the current user from the server
     */
    suspend fun reload(): ScsUser? {
        return if (isLoggedIn) {
            getCurrentUser()
        } else {
            null
        }
    }
}
