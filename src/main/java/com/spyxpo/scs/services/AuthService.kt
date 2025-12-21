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

    // ==================== OAuth and Social Sign-In Methods ====================

    /**
     * Sign in with Google
     *
     * @param idToken Google ID token from Google Sign-In
     * @param accessToken Optional Google access token
     * @return The signed in user
     */
    suspend fun signInWithGoogle(idToken: String, accessToken: String? = null): ScsUser {
        val body = mutableMapOf<String, Any?>("idToken" to idToken)
        accessToken?.let { body["accessToken"] = it }

        val response = httpClient.post("/auth/project/oauth/google", body)
        val authResponse = gson.fromJson(response, AuthResponse::class.java)

        sessionStorage.saveToken(authResponse.token)
        sessionStorage.saveUser(authResponse.user)

        _currentUserFlow.value = authResponse.user
        _currentUserLiveData.postValue(authResponse.user)

        return authResponse.user
    }

    /**
     * Sign in with Facebook
     *
     * @param accessToken Facebook access token from Facebook Login
     * @return The signed in user
     */
    suspend fun signInWithFacebook(accessToken: String): ScsUser {
        val body = mapOf("accessToken" to accessToken)

        val response = httpClient.post("/auth/project/oauth/facebook", body)
        val authResponse = gson.fromJson(response, AuthResponse::class.java)

        sessionStorage.saveToken(authResponse.token)
        sessionStorage.saveUser(authResponse.user)

        _currentUserFlow.value = authResponse.user
        _currentUserLiveData.postValue(authResponse.user)

        return authResponse.user
    }

    /**
     * Sign in with Apple
     *
     * @param identityToken Apple identity token
     * @param authorizationCode Optional Apple authorization code
     * @param fullName Optional user's full name (first sign-in only)
     * @return The signed in user
     */
    suspend fun signInWithApple(
        identityToken: String,
        authorizationCode: String? = null,
        fullName: String? = null
    ): ScsUser {
        val body = mutableMapOf<String, Any?>("identityToken" to identityToken)
        authorizationCode?.let { body["authorizationCode"] = it }
        fullName?.let { body["fullName"] = it }

        val response = httpClient.post("/auth/project/oauth/apple", body)
        val authResponse = gson.fromJson(response, AuthResponse::class.java)

        sessionStorage.saveToken(authResponse.token)
        sessionStorage.saveUser(authResponse.user)

        _currentUserFlow.value = authResponse.user
        _currentUserLiveData.postValue(authResponse.user)

        return authResponse.user
    }

    /**
     * Sign in with GitHub
     *
     * @param code GitHub OAuth authorization code
     * @param redirectUri Optional redirect URI used in OAuth flow
     * @return The signed in user
     */
    suspend fun signInWithGitHub(code: String, redirectUri: String? = null): ScsUser {
        val body = mutableMapOf<String, Any?>("code" to code)
        redirectUri?.let { body["redirectUri"] = it }

        val response = httpClient.post("/auth/project/oauth/github", body)
        val authResponse = gson.fromJson(response, AuthResponse::class.java)

        sessionStorage.saveToken(authResponse.token)
        sessionStorage.saveUser(authResponse.user)

        _currentUserFlow.value = authResponse.user
        _currentUserLiveData.postValue(authResponse.user)

        return authResponse.user
    }

    /**
     * Sign in with Twitter/X
     *
     * @param oauthToken Twitter OAuth token
     * @param oauthTokenSecret Twitter OAuth token secret
     * @return The signed in user
     */
    suspend fun signInWithTwitter(oauthToken: String, oauthTokenSecret: String): ScsUser {
        val body = mapOf(
            "oauthToken" to oauthToken,
            "oauthTokenSecret" to oauthTokenSecret
        )

        val response = httpClient.post("/auth/project/oauth/twitter", body)
        val authResponse = gson.fromJson(response, AuthResponse::class.java)

        sessionStorage.saveToken(authResponse.token)
        sessionStorage.saveUser(authResponse.user)

        _currentUserFlow.value = authResponse.user
        _currentUserLiveData.postValue(authResponse.user)

        return authResponse.user
    }

    /**
     * Sign in with Microsoft
     *
     * @param accessToken Microsoft access token
     * @param idToken Optional Microsoft ID token
     * @return The signed in user
     */
    suspend fun signInWithMicrosoft(accessToken: String, idToken: String? = null): ScsUser {
        val body = mutableMapOf<String, Any?>("accessToken" to accessToken)
        idToken?.let { body["idToken"] = it }

        val response = httpClient.post("/auth/project/oauth/microsoft", body)
        val authResponse = gson.fromJson(response, AuthResponse::class.java)

        sessionStorage.saveToken(authResponse.token)
        sessionStorage.saveUser(authResponse.user)

        _currentUserFlow.value = authResponse.user
        _currentUserLiveData.postValue(authResponse.user)

        return authResponse.user
    }

    /**
     * Sign in anonymously
     * Creates a temporary anonymous account that can be linked to a permanent account later
     *
     * @param customData Optional custom data to store with the anonymous user
     * @return The signed in user
     */
    suspend fun signInAnonymously(customData: Map<String, Any>? = null): ScsUser {
        val body = mutableMapOf<String, Any?>()
        customData?.let { body["customData"] = it }

        val response = httpClient.post("/auth/project/anonymous", body)
        val authResponse = gson.fromJson(response, AuthResponse::class.java)

        sessionStorage.saveToken(authResponse.token)
        sessionStorage.saveUser(authResponse.user)

        _currentUserFlow.value = authResponse.user
        _currentUserLiveData.postValue(authResponse.user)

        return authResponse.user
    }

    /**
     * Send phone verification code
     *
     * @param phoneNumber Phone number in E.164 format (e.g., +1234567890)
     * @param recaptchaToken Optional reCAPTCHA token for verification
     * @return Verification ID to use with signInWithPhoneNumber
     */
    suspend fun sendPhoneVerificationCode(
        phoneNumber: String,
        recaptchaToken: String? = null
    ): String {
        val body = mutableMapOf<String, Any?>("phoneNumber" to phoneNumber)
        recaptchaToken?.let { body["recaptchaToken"] = it }

        val response = httpClient.post("/auth/project/phone/send-code", body)
        return response.get("verificationId").asString
    }

    /**
     * Sign in with phone number
     *
     * @param verificationId Verification ID from sendPhoneVerificationCode
     * @param code SMS verification code
     * @return The signed in user
     */
    suspend fun signInWithPhoneNumber(verificationId: String, code: String): ScsUser {
        val body = mapOf(
            "verificationId" to verificationId,
            "code" to code
        )

        val response = httpClient.post("/auth/project/phone/verify", body)
        val authResponse = gson.fromJson(response, AuthResponse::class.java)

        sessionStorage.saveToken(authResponse.token)
        sessionStorage.saveUser(authResponse.user)

        _currentUserFlow.value = authResponse.user
        _currentUserLiveData.postValue(authResponse.user)

        return authResponse.user
    }

    /**
     * Sign in with a custom token
     *
     * @param token Custom JWT token generated by your backend
     * @return The signed in user
     */
    suspend fun signInWithCustomToken(token: String): ScsUser {
        val body = mapOf("token" to token)

        val response = httpClient.post("/auth/project/custom-token", body)
        val authResponse = gson.fromJson(response, AuthResponse::class.java)

        sessionStorage.saveToken(authResponse.token)
        sessionStorage.saveUser(authResponse.user)

        _currentUserFlow.value = authResponse.user
        _currentUserLiveData.postValue(authResponse.user)

        return authResponse.user
    }

    /**
     * Link an OAuth provider to the current account
     *
     * @param provider Provider name (google, facebook, apple, github, twitter, microsoft)
     * @param credentials Provider-specific credentials
     * @return Updated user
     */
    suspend fun linkProvider(provider: String, credentials: Map<String, Any>): ScsUser {
        val response = httpClient.post("/auth/project/link/$provider", credentials)
        val user = gson.fromJson(response.getAsJsonObject("user"), ScsUser::class.java)

        sessionStorage.saveUser(user)
        _currentUserFlow.value = user
        _currentUserLiveData.postValue(user)

        return user
    }

    /**
     * Unlink an OAuth provider from the current account
     *
     * @param provider Provider name to unlink
     * @return Updated user
     */
    suspend fun unlinkProvider(provider: String): ScsUser {
        val response = httpClient.post("/auth/project/unlink/$provider", emptyMap<String, Any>())
        val user = gson.fromJson(response.getAsJsonObject("user"), ScsUser::class.java)

        sessionStorage.saveUser(user)
        _currentUserFlow.value = user
        _currentUserLiveData.postValue(user)

        return user
    }

    /**
     * Get available sign-in methods for an email
     *
     * @param email Email address to check
     * @return List of sign-in methods for this email
     */
    suspend fun fetchSignInMethodsForEmail(email: String): List<String> {
        val response = httpClient.post("/auth/project/providers", mapOf("email" to email))
        val methods = response.getAsJsonArray("methods")
        return methods?.map { it.asString } ?: emptyList()
    }

    /**
     * Send password reset email
     *
     * @param email Email address to send reset link to
     */
    suspend fun sendPasswordResetEmail(email: String) {
        httpClient.post("/auth/project/password-reset", mapOf("email" to email))
    }

    /**
     * Confirm password reset with code
     *
     * @param code Password reset code from email
     * @param newPassword New password
     */
    suspend fun confirmPasswordReset(code: String, newPassword: String) {
        val body = mapOf(
            "code" to code,
            "newPassword" to newPassword
        )
        httpClient.post("/auth/project/password-reset/confirm", body)
    }

    /**
     * Send email verification
     */
    suspend fun sendEmailVerification() {
        httpClient.post("/auth/project/verify-email", emptyMap<String, Any>())
    }

    /**
     * Verify email with code
     *
     * @param code Email verification code
     */
    suspend fun verifyEmail(code: String) {
        httpClient.post("/auth/project/verify-email/confirm", mapOf("code" to code))
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
