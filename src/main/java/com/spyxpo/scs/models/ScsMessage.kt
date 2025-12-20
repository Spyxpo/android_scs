package com.spyxpo.scs.models

import com.google.gson.annotations.SerializedName

/**
 * Represents a push notification message in SCS messaging.
 */
data class ScsMessage(
    @SerializedName("id")
    val id: String,

    @SerializedName("title")
    val title: String,

    @SerializedName("body")
    val body: String,

    @SerializedName("topic")
    val topic: String? = null,

    @SerializedName("data")
    val data: Map<String, Any>? = null,

    @SerializedName("sentAt")
    val sentAt: String? = null,

    @SerializedName("createdAt")
    val createdAt: String? = null
) {
    companion object {
        @Suppress("UNCHECKED_CAST")
        fun fromMap(map: Map<String, Any?>): ScsMessage {
            return ScsMessage(
                id = map["id"] as? String ?: map["_id"] as? String ?: "",
                title = map["title"] as? String ?: "",
                body = map["body"] as? String ?: "",
                topic = map["topic"] as? String,
                data = map["data"] as? Map<String, Any>,
                sentAt = map["sentAt"] as? String,
                createdAt = map["createdAt"] as? String
            )
        }
    }
}

/**
 * Represents a device token for push notifications.
 */
data class DeviceToken(
    @SerializedName("token")
    val token: String,

    @SerializedName("platform")
    val platform: String,

    @SerializedName("topics")
    val topics: List<String>? = null,

    @SerializedName("createdAt")
    val createdAt: String? = null
) {
    companion object {
        const val PLATFORM_ANDROID = "android"
        const val PLATFORM_IOS = "ios"
        const val PLATFORM_WEB = "web"
    }
}

/**
 * Represents a messaging topic.
 */
data class MessageTopic(
    @SerializedName("name")
    val name: String,

    @SerializedName("subscriberCount")
    val subscriberCount: Int? = null,

    @SerializedName("createdAt")
    val createdAt: String? = null
)

/**
 * Response from listing messages
 */
data class MessagesListResponse(
    @SerializedName("messages")
    val messages: List<ScsMessage>,

    @SerializedName("total")
    val total: Int? = null
)

/**
 * Response from listing topics
 */
data class TopicsListResponse(
    @SerializedName("topics")
    val topics: List<MessageTopic>
)

/**
 * Response from sending a message
 */
data class SendMessageResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("messageId")
    val messageId: String? = null,

    @SerializedName("message")
    val message: String? = null
)
