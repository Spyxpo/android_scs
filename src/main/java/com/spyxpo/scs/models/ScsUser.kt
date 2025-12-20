package com.spyxpo.scs.models

import com.google.gson.annotations.SerializedName

/**
 * Represents a user in the SCS authentication system.
 */
data class ScsUser(
    @SerializedName("uid")
    val uid: String,

    @SerializedName("email")
    val email: String,

    @SerializedName("displayName")
    val displayName: String? = null,

    @SerializedName("role")
    val role: String = "user",

    @SerializedName("disabled")
    val disabled: Boolean = false,

    @SerializedName("customData")
    val customData: Map<String, Any>? = null,

    @SerializedName("createdAt")
    val createdAt: String? = null,

    @SerializedName("lastLoginAt")
    val lastLoginAt: String? = null
) {
    /**
     * Check if the user is an admin
     */
    fun isAdmin(): Boolean = role == "admin"

    /**
     * Check if the user account is active
     */
    fun isActive(): Boolean = !disabled

    companion object {
        /**
         * Create a user from a JSON map
         */
        fun fromMap(map: Map<String, Any?>): ScsUser {
            @Suppress("UNCHECKED_CAST")
            return ScsUser(
                uid = map["uid"] as? String ?: "",
                email = map["email"] as? String ?: "",
                displayName = map["displayName"] as? String,
                role = map["role"] as? String ?: "user",
                disabled = map["disabled"] as? Boolean ?: false,
                customData = map["customData"] as? Map<String, Any>,
                createdAt = map["createdAt"] as? String,
                lastLoginAt = map["lastLoginAt"] as? String
            )
        }
    }
}

/**
 * Response from authentication operations
 */
data class AuthResponse(
    @SerializedName("user")
    val user: ScsUser,

    @SerializedName("token")
    val token: String,

    @SerializedName("message")
    val message: String? = null
)

/**
 * Response from listing users (admin)
 */
data class UsersListResponse(
    @SerializedName("users")
    val users: List<ScsUser>,

    @SerializedName("total")
    val total: Int,

    @SerializedName("limit")
    val limit: Int,

    @SerializedName("skip")
    val skip: Int
)
