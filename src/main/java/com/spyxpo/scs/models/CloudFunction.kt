package com.spyxpo.scs.models

import com.google.gson.annotations.SerializedName

/**
 * Represents a cloud function in SCS.
 */
data class CloudFunction(
    @SerializedName("id")
    val id: String,

    @SerializedName("name")
    val name: String,

    @SerializedName("description")
    val description: String? = null,

    @SerializedName("runtime")
    val runtime: String? = null,

    @SerializedName("handler")
    val handler: String? = null,

    @SerializedName("timeout")
    val timeout: Int? = null,

    @SerializedName("memory")
    val memory: Int? = null,

    @SerializedName("status")
    val status: String? = null,

    @SerializedName("createdAt")
    val createdAt: String? = null,

    @SerializedName("updatedAt")
    val updatedAt: String? = null
) {
    companion object {
        const val STATUS_ACTIVE = "active"
        const val STATUS_INACTIVE = "inactive"
        const val STATUS_DEPLOYING = "deploying"
        const val STATUS_ERROR = "error"

        @Suppress("UNCHECKED_CAST")
        fun fromMap(map: Map<String, Any?>): CloudFunction {
            return CloudFunction(
                id = map["id"] as? String ?: map["_id"] as? String ?: "",
                name = map["name"] as? String ?: "",
                description = map["description"] as? String,
                runtime = map["runtime"] as? String,
                handler = map["handler"] as? String,
                timeout = (map["timeout"] as? Number)?.toInt(),
                memory = (map["memory"] as? Number)?.toInt(),
                status = map["status"] as? String,
                createdAt = map["createdAt"] as? String,
                updatedAt = map["updatedAt"] as? String
            )
        }
    }
}

/**
 * Result from invoking a cloud function.
 */
data class FunctionResult(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("data")
    val data: Any? = null,

    @SerializedName("error")
    val error: String? = null,

    @SerializedName("executionTime")
    val executionTime: Long? = null
) {
    /**
     * Get data as a specific type
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> getDataAs(): T? = data as? T

    /**
     * Get data as a map
     */
    @Suppress("UNCHECKED_CAST")
    fun getDataAsMap(): Map<String, Any?>? = data as? Map<String, Any?>
}

/**
 * Function log entry.
 */
data class FunctionLog(
    @SerializedName("timestamp")
    val timestamp: String,

    @SerializedName("level")
    val level: String,

    @SerializedName("message")
    val message: String
)

/**
 * Response from listing functions
 */
data class FunctionsListResponse(
    @SerializedName("functions")
    val functions: List<CloudFunction>
)

/**
 * Response from getting function logs
 */
data class FunctionLogsResponse(
    @SerializedName("logs")
    val logs: List<FunctionLog>
)
