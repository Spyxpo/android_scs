package com.spyxpo.scs.models

import com.google.gson.annotations.SerializedName

/**
 * Represents a remote configuration parameter.
 */
data class ConfigParameter(
    @SerializedName("key")
    val key: String,

    @SerializedName("value")
    val value: Any?,

    @SerializedName("type")
    val type: String,

    @SerializedName("description")
    val description: String? = null,

    @SerializedName("createdAt")
    val createdAt: String? = null,

    @SerializedName("updatedAt")
    val updatedAt: String? = null
) {
    /**
     * Get value as string
     */
    fun asString(): String? = value?.toString()

    /**
     * Get value as integer
     */
    fun asInt(): Int? = (value as? Number)?.toInt()

    /**
     * Get value as long
     */
    fun asLong(): Long? = (value as? Number)?.toLong()

    /**
     * Get value as double
     */
    fun asDouble(): Double? = (value as? Number)?.toDouble()

    /**
     * Get value as boolean
     */
    fun asBoolean(): Boolean? = value as? Boolean

    companion object {
        const val TYPE_STRING = "string"
        const val TYPE_NUMBER = "number"
        const val TYPE_BOOLEAN = "boolean"
        const val TYPE_JSON = "json"

        @Suppress("UNCHECKED_CAST")
        fun fromMap(map: Map<String, Any?>): ConfigParameter {
            return ConfigParameter(
                key = map["key"] as? String ?: "",
                value = map["value"],
                type = map["type"] as? String ?: TYPE_STRING,
                description = map["description"] as? String,
                createdAt = map["createdAt"] as? String,
                updatedAt = map["updatedAt"] as? String
            )
        }
    }
}

/**
 * Represents a remote configuration version.
 */
data class ConfigVersion(
    @SerializedName("id")
    val id: String,

    @SerializedName("version")
    val version: Int,

    @SerializedName("parameters")
    val parameters: Map<String, Any?>? = null,

    @SerializedName("isActive")
    val isActive: Boolean = false,

    @SerializedName("createdAt")
    val createdAt: String? = null,

    @SerializedName("publishedAt")
    val publishedAt: String? = null
)

/**
 * Response from listing config parameters
 */
data class ConfigParamsResponse(
    @SerializedName("parameters")
    val parameters: List<ConfigParameter>
)

/**
 * Response from listing config versions
 */
data class ConfigVersionsResponse(
    @SerializedName("versions")
    val versions: List<ConfigVersion>
)

/**
 * Response from fetching active config
 */
data class FetchConfigResponse(
    @SerializedName("parameters")
    val parameters: Map<String, Any?>,

    @SerializedName("version")
    val version: Int? = null,

    @SerializedName("fetchedAt")
    val fetchedAt: String? = null
)
