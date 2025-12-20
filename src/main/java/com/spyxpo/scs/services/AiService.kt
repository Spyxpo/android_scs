package com.spyxpo.scs.services

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.spyxpo.scs.models.*
import com.spyxpo.scs.utils.ScsHttpClient

/**
 * Service for AI operations (chat, completion, image generation).
 *
 * Example usage:
 * ```kotlin
 * // Chat with AI
 * val messages = listOf(
 *     ChatMessage.system("You are a helpful assistant."),
 *     ChatMessage.user("What is the capital of France?")
 * )
 * val response = scs.ai.chat(messages)
 * println(response.content)
 *
 * // Text completion
 * val completion = scs.ai.complete("Once upon a time")
 * println(completion.content)
 *
 * // Image generation
 * val image = scs.ai.generateImage("A sunset over mountains")
 * println(image.imageUrl)
 *
 * // List available models
 * val models = scs.ai.listModels()
 * ```
 */
class AiService(private val httpClient: ScsHttpClient) {

    private val gson = Gson()

    /**
     * Chat with an AI model
     *
     * @param messages Conversation messages
     * @param model Model to use (optional)
     * @param temperature Sampling temperature (0.0-2.0)
     * @param maxTokens Maximum tokens in response
     * @return Chat response
     */
    suspend fun chat(
        messages: List<ChatMessage>,
        model: String? = null,
        temperature: Double? = null,
        maxTokens: Int? = null
    ): ChatResponse {
        val body = mutableMapOf<String, Any?>(
            "messages" to messages.map { mapOf("role" to it.role, "content" to it.content) }
        )
        model?.let { body["model"] = it }
        temperature?.let { body["temperature"] = it }
        maxTokens?.let { body["maxTokens"] = it }

        val response = httpClient.post("/ai/chat", body)

        @Suppress("UNCHECKED_CAST")
        val map = gson.fromJson<Map<String, Any?>>(response, Map::class.java)
        return ChatResponse.fromMap(map)
    }

    /**
     * Simple chat with a single message
     *
     * @param message User message
     * @param systemPrompt Optional system prompt
     * @param model Model to use
     * @return Chat response content
     */
    suspend fun ask(
        message: String,
        systemPrompt: String? = null,
        model: String? = null
    ): String {
        val messages = mutableListOf<ChatMessage>()
        systemPrompt?.let { messages.add(ChatMessage.system(it)) }
        messages.add(ChatMessage.user(message))

        val response = chat(messages, model)
        return response.content
    }

    /**
     * Continue a conversation
     *
     * @param conversationHistory Previous messages in the conversation
     * @param newMessage New user message
     * @param model Model to use
     * @return Updated conversation with assistant response
     */
    suspend fun continueConversation(
        conversationHistory: List<ChatMessage>,
        newMessage: String,
        model: String? = null
    ): Pair<List<ChatMessage>, ChatResponse> {
        val messages = conversationHistory.toMutableList()
        messages.add(ChatMessage.user(newMessage))

        val response = chat(messages, model)
        messages.add(ChatMessage.assistant(response.content))

        return Pair(messages, response)
    }

    /**
     * Complete a text prompt
     *
     * @param prompt Text prompt to complete
     * @param model Model to use (optional)
     * @param temperature Sampling temperature (0.0-2.0)
     * @param maxTokens Maximum tokens in response
     * @return Completion response
     */
    suspend fun complete(
        prompt: String,
        model: String? = null,
        temperature: Double? = null,
        maxTokens: Int? = null
    ): CompletionResponse {
        val body = mutableMapOf<String, Any?>("prompt" to prompt)
        model?.let { body["model"] = it }
        temperature?.let { body["temperature"] = it }
        maxTokens?.let { body["maxTokens"] = it }

        val response = httpClient.post("/ai/complete", body)

        @Suppress("UNCHECKED_CAST")
        val map = gson.fromJson<Map<String, Any?>>(response, Map::class.java)
        return CompletionResponse.fromMap(map)
    }

    /**
     * Generate an image from a text prompt
     *
     * @param prompt Image description
     * @param size Image size (e.g., "512x512", "1024x1024")
     * @param quality Image quality (e.g., "standard", "hd")
     * @param model Model to use (optional)
     * @return Image generation response
     */
    suspend fun generateImage(
        prompt: String,
        size: String? = null,
        quality: String? = null,
        model: String? = null
    ): ImageGenerationResponse {
        val body = mutableMapOf<String, Any?>("prompt" to prompt)
        size?.let { body["size"] = it }
        quality?.let { body["quality"] = it }
        model?.let { body["model"] = it }

        val response = httpClient.post("/ai/generate-image", body)

        @Suppress("UNCHECKED_CAST")
        val map = gson.fromJson<Map<String, Any?>>(response, Map::class.java)
        return ImageGenerationResponse.fromMap(map)
    }

    /**
     * List available AI models
     *
     * @return List of available models
     */
    suspend fun listModels(): List<AiModel> {
        val response = httpClient.get("/ai/models")
        val models = response.getAsJsonArray("models") ?: JsonArray()

        return models.map { element ->
            @Suppress("UNCHECKED_CAST")
            val map = gson.fromJson<Map<String, Any?>>(element, Map::class.java)
            AiModel.fromMap(map)
        }
    }

    /**
     * Pull (download) an AI model
     *
     * @param modelName Name of the model to pull
     */
    suspend fun pullModel(modelName: String) {
        val body = mapOf("name" to modelName)
        httpClient.post("/ai/models/pull", body)
    }

    /**
     * Builder for creating a chat conversation
     */
    class ChatBuilder {
        private val messages = mutableListOf<ChatMessage>()
        private var model: String? = null
        private var temperature: Double? = null
        private var maxTokens: Int? = null

        fun system(content: String): ChatBuilder {
            messages.add(ChatMessage.system(content))
            return this
        }

        fun user(content: String): ChatBuilder {
            messages.add(ChatMessage.user(content))
            return this
        }

        fun assistant(content: String): ChatBuilder {
            messages.add(ChatMessage.assistant(content))
            return this
        }

        fun model(model: String): ChatBuilder {
            this.model = model
            return this
        }

        fun temperature(temperature: Double): ChatBuilder {
            this.temperature = temperature
            return this
        }

        fun maxTokens(maxTokens: Int): ChatBuilder {
            this.maxTokens = maxTokens
            return this
        }

        internal fun build(): ChatRequest {
            return ChatRequest(messages, model, temperature, maxTokens)
        }
    }

    internal data class ChatRequest(
        val messages: List<ChatMessage>,
        val model: String?,
        val temperature: Double?,
        val maxTokens: Int?
    )

    /**
     * Create a chat builder for fluent API
     */
    fun chatBuilder(): ChatBuilder = ChatBuilder()

    /**
     * Execute a chat request from a builder
     */
    suspend fun chat(builder: ChatBuilder): ChatResponse {
        val request = builder.build()
        return chat(request.messages, request.model, request.temperature, request.maxTokens)
    }
}
