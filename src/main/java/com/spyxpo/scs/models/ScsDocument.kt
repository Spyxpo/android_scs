package com.spyxpo.scs.models

import com.google.gson.annotations.SerializedName

/**
 * Represents a document in the SCS database.
 */
data class ScsDocument(
    @SerializedName("id")
    val id: String,

    @SerializedName("data")
    val data: Map<String, Any?>,

    @SerializedName("createdAt")
    val createdAt: String? = null,

    @SerializedName("updatedAt")
    val updatedAt: String? = null
) {
    /**
     * Get a field value from the document
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> get(field: String): T? {
        return data[field] as? T
    }

    /**
     * Get a string field
     */
    fun getString(field: String): String? {
        return data[field] as? String
    }

    /**
     * Get an integer field
     */
    fun getInt(field: String): Int? {
        return (data[field] as? Number)?.toInt()
    }

    /**
     * Get a long field
     */
    fun getLong(field: String): Long? {
        return (data[field] as? Number)?.toLong()
    }

    /**
     * Get a double field
     */
    fun getDouble(field: String): Double? {
        return (data[field] as? Number)?.toDouble()
    }

    /**
     * Get a boolean field
     */
    fun getBoolean(field: String): Boolean? {
        return data[field] as? Boolean
    }

    /**
     * Get a list field
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> getList(field: String): List<T>? {
        return data[field] as? List<T>
    }

    /**
     * Get a map field
     */
    @Suppress("UNCHECKED_CAST")
    fun getMap(field: String): Map<String, Any?>? {
        return data[field] as? Map<String, Any?>
    }

    /**
     * Check if document contains a field
     */
    fun contains(field: String): Boolean {
        return data.containsKey(field)
    }

    companion object {
        /**
         * Create a document from a JSON map
         */
        @Suppress("UNCHECKED_CAST")
        fun fromMap(map: Map<String, Any?>): ScsDocument {
            val id = map["id"] as? String ?: map["_id"] as? String ?: ""
            val data = (map["data"] as? Map<String, Any?>) ?: map.filterKeys {
                it !in listOf("id", "_id", "createdAt", "updatedAt")
            }
            return ScsDocument(
                id = id,
                data = data,
                createdAt = map["createdAt"] as? String,
                updatedAt = map["updatedAt"] as? String
            )
        }
    }
}

/**
 * Response from querying documents
 */
data class DocumentsResponse(
    @SerializedName("documents")
    val documents: List<ScsDocument>,

    @SerializedName("total")
    val total: Int? = null,

    @SerializedName("limit")
    val limit: Int? = null,

    @SerializedName("skip")
    val skip: Int? = null
)

/**
 * Query filter for database queries
 */
data class QueryFilter(
    val field: String,
    val operator: String,
    val value: Any?
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "field" to field,
        "operator" to operator,
        "value" to value
    )
}

/**
 * Sort order for database queries
 */
data class QueryOrderBy(
    val field: String,
    val direction: String = "asc"
) {
    fun toMap(): Map<String, String> = mapOf(
        "field" to field,
        "direction" to direction
    )
}
