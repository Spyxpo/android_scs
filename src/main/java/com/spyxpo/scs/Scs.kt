package com.spyxpo.scs

import android.content.Context
import com.spyxpo.scs.services.*
import com.spyxpo.scs.utils.ScsHttpClient
import com.spyxpo.scs.utils.SessionStorage

/**
 * Main entry point for the SCS (Spyxpo Cloud Services) SDK.
 *
 * Initialize the SDK with your configuration:
 * ```kotlin
 * val scs = Scs.initialize(context, ScsConfig(
 *     apiKey = "your-api-key",
 *     projectId = "your-project-id",
 *     baseUrl = "https://your-scs-server.com"
 * ))
 * ```
 *
 * Then access services:
 * ```kotlin
 * // Authentication
 * val user = scs.auth.login("email@example.com", "password")
 *
 * // Database
 * val docs = scs.database.collection("users").get()
 *
 * // Storage
 * scs.storage.upload(file, "uploads")
 *
 * // And more...
 * ```
 */
class Scs private constructor(
    private val context: Context,
    val config: ScsConfig
) {
    private val sessionStorage = SessionStorage(context)
    internal val httpClient = ScsHttpClient(config, sessionStorage)

    /**
     * Authentication service for user registration, login, and management
     */
    val auth: AuthService by lazy { AuthService(httpClient, sessionStorage) }

    /**
     * Database service for document storage and querying
     */
    val database: DatabaseService by lazy { DatabaseService(httpClient) }

    /**
     * Storage service for file uploads and downloads
     */
    val storage: StorageService by lazy { StorageService(httpClient) }

    /**
     * Realtime database service for real-time data synchronization
     */
    val realtime: RealtimeService by lazy { RealtimeService(config, sessionStorage) }

    /**
     * Cloud messaging service for push notifications
     */
    val messaging: MessagingService by lazy { MessagingService(httpClient) }

    /**
     * Remote configuration service for dynamic app configuration
     */
    val remoteConfig: RemoteConfigService by lazy { RemoteConfigService(httpClient) }

    /**
     * Cloud functions service for serverless function invocation
     */
    val functions: FunctionsService by lazy { FunctionsService(httpClient, config.projectId) }

    /**
     * Machine learning service for text recognition and image labeling
     */
    val ml: MlService by lazy { MlService(httpClient) }

    /**
     * AI service for chat, text completion, and image generation
     */
    val ai: AiService by lazy { AiService(httpClient) }

    /**
     * Call service for voice/video calls, group calls, and live streaming
     */
    val calls: CallService by lazy { CallService(httpClient, config) }

    /**
     * Check if a user is currently logged in
     */
    fun isLoggedIn(): Boolean = sessionStorage.getToken() != null

    /**
     * Get the current auth token (if any)
     */
    fun getAuthToken(): String? = sessionStorage.getToken()

    /**
     * Clear all local data (logout)
     */
    fun clearSession() {
        sessionStorage.clear()
        realtime.disconnect()
    }

    companion object {
        @Volatile
        private var instance: Scs? = null

        /**
         * Initialize the SCS SDK. Must be called before using any services.
         *
         * @param context Android application context
         * @param config SDK configuration
         * @return The initialized SCS instance
         */
        fun initialize(context: Context, config: ScsConfig): Scs {
            return instance ?: synchronized(this) {
                instance ?: Scs(context.applicationContext, config).also {
                    instance = it
                }
            }
        }

        /**
         * Get the current SCS instance.
         *
         * @throws IllegalStateException if SDK has not been initialized
         */
        fun getInstance(): Scs {
            return instance ?: throw IllegalStateException(
                "SCS SDK has not been initialized. Call Scs.initialize() first."
            )
        }

        /**
         * Check if the SDK has been initialized
         */
        fun isInitialized(): Boolean = instance != null

        /**
         * Reset the SDK instance (for testing purposes)
         */
        internal fun reset() {
            instance?.clearSession()
            instance = null
        }
    }
}
