package com.spyxpo.scs.services

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.spyxpo.scs.models.ConfigParameter
import com.spyxpo.scs.models.ConfigVersion
import com.spyxpo.scs.utils.ScsHttpClient

/**
 * Service for remote configuration.
 *
 * Example usage:
 * ```kotlin
 * // Fetch and activate config
 * scs.remoteConfig.fetchAndActivate()
 *
 * // Get config values
 * val welcomeMessage = scs.remoteConfig.getString("welcome_message", "Hello!")
 * val maxItems = scs.remoteConfig.getInt("max_items", 10)
 * val featureEnabled = scs.remoteConfig.getBoolean("new_feature", false)
 *
 * // Admin: Set a parameter
 * scs.remoteConfig.setParameter("welcome_message", "Welcome to our app!", "string")
 *
 * // Admin: Publish config
 * scs.remoteConfig.publish()
 * ```
 */
class RemoteConfigService(private val httpClient: ScsHttpClient) {

    private val gson = Gson()
    private var cachedConfig: Map<String, Any?> = emptyMap()
    private var configVersion: Int? = null

    /**
     * Fetch the remote configuration and activate it
     *
     * @return True if new config was fetched
     */
    suspend fun fetchAndActivate(): Boolean {
        val response = httpClient.get("/remoteConfig/fetch")

        val parameters = response.getAsJsonObject("parameters")
        val version = response.get("version")?.asInt

        if (parameters != null) {
            cachedConfig = gson.fromJson(parameters, Map::class.java) as Map<String, Any?>
            configVersion = version
            return true
        }

        return false
    }

    /**
     * Get a string value from config
     *
     * @param key Parameter key
     * @param defaultValue Default value if not found
     * @return The config value
     */
    fun getString(key: String, defaultValue: String = ""): String {
        return cachedConfig[key]?.toString() ?: defaultValue
    }

    /**
     * Get an integer value from config
     *
     * @param key Parameter key
     * @param defaultValue Default value if not found
     * @return The config value
     */
    fun getInt(key: String, defaultValue: Int = 0): Int {
        val value = cachedConfig[key]
        return when (value) {
            is Number -> value.toInt()
            is String -> value.toIntOrNull() ?: defaultValue
            else -> defaultValue
        }
    }

    /**
     * Get a long value from config
     *
     * @param key Parameter key
     * @param defaultValue Default value if not found
     * @return The config value
     */
    fun getLong(key: String, defaultValue: Long = 0L): Long {
        val value = cachedConfig[key]
        return when (value) {
            is Number -> value.toLong()
            is String -> value.toLongOrNull() ?: defaultValue
            else -> defaultValue
        }
    }

    /**
     * Get a double value from config
     *
     * @param key Parameter key
     * @param defaultValue Default value if not found
     * @return The config value
     */
    fun getDouble(key: String, defaultValue: Double = 0.0): Double {
        val value = cachedConfig[key]
        return when (value) {
            is Number -> value.toDouble()
            is String -> value.toDoubleOrNull() ?: defaultValue
            else -> defaultValue
        }
    }

    /**
     * Get a boolean value from config
     *
     * @param key Parameter key
     * @param defaultValue Default value if not found
     * @return The config value
     */
    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        val value = cachedConfig[key]
        return when (value) {
            is Boolean -> value
            is String -> value.equals("true", ignoreCase = true)
            is Number -> value.toInt() != 0
            else -> defaultValue
        }
    }

    /**
     * Get a JSON value from config
     *
     * @param key Parameter key
     * @return The config value as JsonObject, or null
     */
    fun getJson(key: String): JsonObject? {
        val value = cachedConfig[key] ?: return null
        return try {
            gson.toJsonTree(value).asJsonObject
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Get all config parameters
     *
     * @return Map of all config parameters
     */
    fun getAll(): Map<String, Any?> = cachedConfig.toMap()

    /**
     * Get the current config version
     */
    fun getVersion(): Int? = configVersion

    // ==================== Admin Methods ====================

    /**
     * List all config parameters (admin)
     *
     * @return List of config parameters
     */
    suspend fun listParameters(): List<ConfigParameter> {
        val response = httpClient.get("/remoteConfig/params")
        val params = response.getAsJsonArray("parameters") ?: JsonArray()

        return params.map { element ->
            gson.fromJson(element, ConfigParameter::class.java)
        }
    }

    /**
     * Get a specific parameter (admin)
     *
     * @param key Parameter key
     * @return The parameter
     */
    suspend fun getParameter(key: String): ConfigParameter {
        val response = httpClient.get("/remoteConfig/params/$key")
        return gson.fromJson(response.getAsJsonObject("parameter"), ConfigParameter::class.java)
    }

    /**
     * Set a config parameter (admin)
     *
     * @param key Parameter key
     * @param value Parameter value
     * @param type Value type (string, number, boolean, json)
     * @param description Optional description
     */
    suspend fun setParameter(
        key: String,
        value: Any,
        type: String = ConfigParameter.TYPE_STRING,
        description: String? = null
    ) {
        val body = mutableMapOf<String, Any?>(
            "key" to key,
            "value" to value,
            "type" to type
        )
        description?.let { body["description"] = it }

        httpClient.post("/remoteConfig/params", body)
    }

    /**
     * Update a config parameter (admin)
     *
     * @param key Parameter key
     * @param value New value
     * @param description Optional new description
     */
    suspend fun updateParameter(
        key: String,
        value: Any,
        description: String? = null
    ) {
        val body = mutableMapOf<String, Any?>("value" to value)
        description?.let { body["description"] = it }

        httpClient.put("/remoteConfig/params/$key", body)
    }

    /**
     * Delete a config parameter (admin)
     *
     * @param key Parameter key
     */
    suspend fun deleteParameter(key: String) {
        httpClient.delete("/remoteConfig/params/$key")
    }

    /**
     * Publish the current config (admin)
     *
     * @return The new config version
     */
    suspend fun publish(): ConfigVersion {
        val response = httpClient.post("/remoteConfig/publish")
        return gson.fromJson(response.getAsJsonObject("version"), ConfigVersion::class.java)
    }

    /**
     * List all config versions (admin)
     *
     * @return List of config versions
     */
    suspend fun listVersions(): List<ConfigVersion> {
        val response = httpClient.get("/remoteConfig/versions")
        val versions = response.getAsJsonArray("versions") ?: JsonArray()

        return versions.map { element ->
            gson.fromJson(element, ConfigVersion::class.java)
        }
    }

    /**
     * Rollback to a previous version (admin)
     *
     * @param versionId Version ID to rollback to
     */
    suspend fun rollback(versionId: String) {
        httpClient.post("/remoteConfig/versions/$versionId/rollback")
    }

    /**
     * Clear the local config cache
     */
    fun clearCache() {
        cachedConfig = emptyMap()
        configVersion = null
    }
}
