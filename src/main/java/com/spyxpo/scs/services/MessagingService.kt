package com.spyxpo.scs.services

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.spyxpo.scs.models.*
import com.spyxpo.scs.utils.ScsHttpClient

/**
 * Service for cloud messaging (push notifications).
 *
 * Example usage:
 * ```kotlin
 * // Register device token
 * scs.messaging.registerToken(fcmToken, "android")
 *
 * // Subscribe to a topic
 * scs.messaging.subscribeToTopic(fcmToken, "news")
 *
 * // Send to topic
 * scs.messaging.sendToTopic("news", "Breaking News", "Something happened!", mapOf("url" to "..."))
 *
 * // Send to specific device
 * scs.messaging.sendToToken(deviceToken, "Hello", "Personal message")
 *
 * // Unsubscribe from topic
 * scs.messaging.unsubscribeFromTopic(fcmToken, "news")
 *
 * // Unregister token
 * scs.messaging.unregisterToken(fcmToken)
 * ```
 */
class MessagingService(private val httpClient: ScsHttpClient) {

    private val gson = Gson()

    // ==================== Token Management ====================

    /**
     * Register a device token for push notifications
     *
     * @param token Device token (FCM token for Android)
     * @param platform Platform identifier (android, ios, web)
     */
    suspend fun registerToken(token: String, platform: String = DeviceToken.PLATFORM_ANDROID) {
        val body = mapOf(
            "token" to token,
            "platform" to platform
        )
        httpClient.post("/messaging/tokens/register", body)
    }

    /**
     * Unregister a device token
     *
     * @param token Device token to unregister
     */
    suspend fun unregisterToken(token: String) {
        val body = mapOf("token" to token)
        httpClient.post("/messaging/tokens/unregister", body)
    }

    /**
     * List all registered tokens
     *
     * @return List of device tokens
     */
    suspend fun listTokens(): List<DeviceToken> {
        val response = httpClient.get("/messaging/tokens")
        val tokens = response.getAsJsonArray("tokens") ?: JsonArray()

        return tokens.map { element ->
            gson.fromJson(element, DeviceToken::class.java)
        }
    }

    // ==================== Topic Management ====================

    /**
     * Create a new topic
     *
     * @param name Topic name
     */
    suspend fun createTopic(name: String) {
        val body = mapOf("name" to name)
        httpClient.post("/messaging/topics", body)
    }

    /**
     * List all topics
     *
     * @return List of topics
     */
    suspend fun listTopics(): List<MessageTopic> {
        val response = httpClient.get("/messaging/topics")
        val topics = response.getAsJsonArray("topics") ?: JsonArray()

        return topics.map { element ->
            gson.fromJson(element, MessageTopic::class.java)
        }
    }

    /**
     * Delete a topic
     *
     * @param name Topic name
     */
    suspend fun deleteTopic(name: String) {
        httpClient.delete("/messaging/topics/$name")
    }

    /**
     * Subscribe a token to a topic
     *
     * @param token Device token
     * @param topic Topic name
     */
    suspend fun subscribeToTopic(token: String, topic: String) {
        val body = mapOf(
            "token" to token,
            "topic" to topic
        )
        httpClient.post("/messaging/topics/subscribe", body)
    }

    /**
     * Unsubscribe a token from a topic
     *
     * @param token Device token
     * @param topic Topic name
     */
    suspend fun unsubscribeFromTopic(token: String, topic: String) {
        val body = mapOf(
            "token" to token,
            "topic" to topic
        )
        httpClient.post("/messaging/topics/unsubscribe", body)
    }

    // ==================== Sending Messages ====================

    /**
     * Send a message to a topic
     *
     * @param topic Topic name
     * @param title Notification title
     * @param body Notification body
     * @param data Optional custom data
     * @return Send result
     */
    suspend fun sendToTopic(
        topic: String,
        title: String,
        body: String,
        data: Map<String, Any>? = null
    ): SendMessageResponse {
        val requestBody = mutableMapOf<String, Any>(
            "topic" to topic,
            "title" to title,
            "body" to body
        )
        data?.let { requestBody["data"] = it }

        val response = httpClient.post("/messaging/send", requestBody)
        return gson.fromJson(response, SendMessageResponse::class.java)
    }

    /**
     * Send a message to a specific device
     *
     * @param token Device token
     * @param title Notification title
     * @param body Notification body
     * @param data Optional custom data
     * @return Send result
     */
    suspend fun sendToToken(
        token: String,
        title: String,
        body: String,
        data: Map<String, Any>? = null
    ): SendMessageResponse {
        val requestBody = mutableMapOf<String, Any>(
            "token" to token,
            "title" to title,
            "body" to body
        )
        data?.let { requestBody["data"] = it }

        val response = httpClient.post("/messaging/send/token", requestBody)
        return gson.fromJson(response, SendMessageResponse::class.java)
    }

    /**
     * Send a message to multiple tokens
     *
     * @param tokens List of device tokens
     * @param title Notification title
     * @param body Notification body
     * @param data Optional custom data
     * @return Send result
     */
    suspend fun sendToTokens(
        tokens: List<String>,
        title: String,
        body: String,
        data: Map<String, Any>? = null
    ): SendMessageResponse {
        val requestBody = mutableMapOf<String, Any>(
            "tokens" to tokens,
            "title" to title,
            "body" to body
        )
        data?.let { requestBody["data"] = it }

        val response = httpClient.post("/messaging/send", requestBody)
        return gson.fromJson(response, SendMessageResponse::class.java)
    }

    // ==================== Message History ====================

    /**
     * List sent messages
     *
     * @param limit Maximum number of messages to return
     * @param skip Number of messages to skip
     * @return List of messages
     */
    suspend fun listMessages(limit: Int? = null, skip: Int? = null): List<ScsMessage> {
        val params = mutableMapOf<String, String>()
        limit?.let { params["limit"] = it.toString() }
        skip?.let { params["skip"] = it.toString() }

        val response = httpClient.get("/messaging/messages", params)
        val messages = response.getAsJsonArray("messages") ?: JsonArray()

        return messages.map { element ->
            gson.fromJson(element, ScsMessage::class.java)
        }
    }

    /**
     * Get a specific message
     *
     * @param messageId Message ID
     * @return The message
     */
    suspend fun getMessage(messageId: String): ScsMessage {
        val response = httpClient.get("/messaging/messages/$messageId")
        return gson.fromJson(response.getAsJsonObject("message"), ScsMessage::class.java)
    }

    /**
     * Delete a message
     *
     * @param messageId Message ID
     */
    suspend fun deleteMessage(messageId: String) {
        httpClient.delete("/messaging/messages/$messageId")
    }
}
