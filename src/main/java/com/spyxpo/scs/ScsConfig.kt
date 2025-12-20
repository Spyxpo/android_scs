package com.spyxpo.scs

/**
 * Configuration class for the SCS SDK.
 *
 * @property apiKey The API key for authenticating with the SCS backend
 * @property projectId The project ID for your SCS project
 * @property baseUrl The base URL of the SCS backend (defaults to localhost for development)
 */
data class ScsConfig(
    val apiKey: String,
    val projectId: String,
    val baseUrl: String = "http://localhost:3001"
) {
    init {
        require(apiKey.isNotBlank()) { "API key cannot be blank" }
        require(projectId.isNotBlank()) { "Project ID cannot be blank" }
        require(baseUrl.isNotBlank()) { "Base URL cannot be blank" }
    }

    /**
     * Get the full API URL
     */
    fun getApiUrl(): String = "$baseUrl/api"

    companion object {
        /**
         * Create a config for local development
         */
        fun local(apiKey: String, projectId: String): ScsConfig {
            return ScsConfig(
                apiKey = apiKey,
                projectId = projectId,
                baseUrl = "http://10.0.2.2:3001" // Android emulator localhost
            )
        }

        /**
         * Create a config for production
         */
        fun production(apiKey: String, projectId: String, baseUrl: String): ScsConfig {
            return ScsConfig(
                apiKey = apiKey,
                projectId = projectId,
                baseUrl = baseUrl
            )
        }
    }
}
