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

    // ==================== AI AGENTS ====================

    /**
     * Create a new AI agent
     *
     * @param name Agent name
     * @param instructions System instructions for the agent
     * @param description Agent description
     * @param model AI model to use
     * @param tools List of tool IDs
     * @param temperature Temperature (0-1)
     * @param maxTokens Maximum tokens
     * @param metadata Additional metadata
     * @return Created agent
     */
    suspend fun createAgent(
        name: String,
        instructions: String? = null,
        description: String? = null,
        model: String? = null,
        tools: List<String>? = null,
        temperature: Double? = null,
        maxTokens: Int? = null,
        metadata: Map<String, Any?>? = null
    ): Agent {
        val body = mutableMapOf<String, Any?>("name" to name)
        instructions?.let { body["instructions"] = it }
        description?.let { body["description"] = it }
        model?.let { body["model"] = it }
        tools?.let { body["tools"] = it }
        temperature?.let { body["temperature"] = it }
        maxTokens?.let { body["maxTokens"] = it }
        metadata?.let { body["metadata"] = it }

        val response = httpClient.post("/ai/agents", body)

        @Suppress("UNCHECKED_CAST")
        val map = gson.fromJson<Map<String, Any?>>(response, Map::class.java)
        val agentMap = map["agent"] as? Map<String, Any?> ?: map
        return Agent.fromMap(agentMap)
    }

    /**
     * List all agents
     *
     * @param limit Maximum number of agents
     * @param offset Number to skip
     * @param status Filter by status
     * @return List of agents
     */
    suspend fun listAgents(
        limit: Int? = null,
        offset: Int? = null,
        status: String? = null
    ): List<Agent> {
        val params = mutableListOf<String>()
        limit?.let { params.add("limit=$it") }
        offset?.let { params.add("offset=$it") }
        status?.let { params.add("status=$it") }

        val queryString = if (params.isNotEmpty()) "?${params.joinToString("&")}" else ""
        val response = httpClient.get("/ai/agents$queryString")

        val agents = response.getAsJsonArray("agents") ?: JsonArray()
        return agents.map { element ->
            @Suppress("UNCHECKED_CAST")
            val map = gson.fromJson<Map<String, Any?>>(element, Map::class.java)
            Agent.fromMap(map)
        }
    }

    /**
     * Get an agent by ID
     *
     * @param agentId Agent ID
     * @return Agent details
     */
    suspend fun getAgent(agentId: String): Agent {
        val response = httpClient.get("/ai/agents/$agentId")

        @Suppress("UNCHECKED_CAST")
        val map = gson.fromJson<Map<String, Any?>>(response.toString(), Map::class.java)
        val agentMap = map["agent"] as? Map<String, Any?> ?: map
        return Agent.fromMap(agentMap)
    }

    /**
     * Update an agent
     *
     * @param agentId Agent ID
     * @param name Agent name
     * @param instructions System instructions
     * @param description Agent description
     * @param model AI model to use
     * @param tools List of tool IDs
     * @param temperature Temperature (0-1)
     * @param maxTokens Maximum tokens
     * @param metadata Additional metadata
     * @param status Agent status
     * @return Updated agent
     */
    suspend fun updateAgent(
        agentId: String,
        name: String? = null,
        instructions: String? = null,
        description: String? = null,
        model: String? = null,
        tools: List<String>? = null,
        temperature: Double? = null,
        maxTokens: Int? = null,
        metadata: Map<String, Any?>? = null,
        status: String? = null
    ): Agent {
        val body = mutableMapOf<String, Any?>()
        name?.let { body["name"] = it }
        instructions?.let { body["instructions"] = it }
        description?.let { body["description"] = it }
        model?.let { body["model"] = it }
        tools?.let { body["tools"] = it }
        temperature?.let { body["temperature"] = it }
        maxTokens?.let { body["maxTokens"] = it }
        metadata?.let { body["metadata"] = it }
        status?.let { body["status"] = it }

        val response = httpClient.put("/ai/agents/$agentId", body)

        @Suppress("UNCHECKED_CAST")
        val map = gson.fromJson<Map<String, Any?>>(response, Map::class.java)
        val agentMap = map["agent"] as? Map<String, Any?> ?: map
        return Agent.fromMap(agentMap)
    }

    /**
     * Delete an agent
     *
     * @param agentId Agent ID
     */
    suspend fun deleteAgent(agentId: String) {
        httpClient.delete("/ai/agents/$agentId")
    }

    /**
     * Run an agent with input
     *
     * @param agentId Agent ID
     * @param input User input message
     * @param sessionId Session ID for conversation continuity
     * @param context Additional context data
     * @return Agent response with output and session ID
     */
    suspend fun runAgent(
        agentId: String,
        input: String,
        sessionId: String? = null,
        context: Map<String, Any?>? = null
    ): AgentRunResponse {
        val body = mutableMapOf<String, Any?>("input" to input)
        sessionId?.let { body["sessionId"] = it }
        context?.let { body["context"] = it }

        val response = httpClient.post("/ai/agents/$agentId/run", body)

        @Suppress("UNCHECKED_CAST")
        val map = gson.fromJson<Map<String, Any?>>(response, Map::class.java)
        return AgentRunResponse.fromMap(map)
    }

    /**
     * List sessions for an agent
     *
     * @param agentId Agent ID
     * @param limit Maximum number of sessions
     * @param offset Number to skip
     * @return List of sessions
     */
    suspend fun listAgentSessions(
        agentId: String,
        limit: Int? = null,
        offset: Int? = null
    ): List<AgentSession> {
        val params = mutableListOf<String>()
        limit?.let { params.add("limit=$it") }
        offset?.let { params.add("offset=$it") }

        val queryString = if (params.isNotEmpty()) "?${params.joinToString("&")}" else ""
        val response = httpClient.get("/ai/agents/$agentId/sessions$queryString")

        val sessions = response.getAsJsonArray("sessions") ?: JsonArray()
        return sessions.map { element ->
            @Suppress("UNCHECKED_CAST")
            val map = gson.fromJson<Map<String, Any?>>(element, Map::class.java)
            AgentSession.fromMap(map)
        }
    }

    /**
     * Get an agent session with full message history
     *
     * @param agentId Agent ID
     * @param sessionId Session ID
     * @return Session with messages
     */
    suspend fun getAgentSession(agentId: String, sessionId: String): AgentSession {
        val response = httpClient.get("/ai/agents/$agentId/sessions/$sessionId")

        @Suppress("UNCHECKED_CAST")
        val map = gson.fromJson<Map<String, Any?>>(response.toString(), Map::class.java)
        val sessionMap = map["session"] as? Map<String, Any?> ?: map
        return AgentSession.fromMap(sessionMap)
    }

    /**
     * Delete an agent session
     *
     * @param agentId Agent ID
     * @param sessionId Session ID
     */
    suspend fun deleteAgentSession(agentId: String, sessionId: String) {
        httpClient.delete("/ai/agents/$agentId/sessions/$sessionId")
    }

    // Agent Tools

    /**
     * Define a tool that agents can use
     *
     * @param name Tool name
     * @param description Tool description
     * @param parameters JSON schema for tool parameters
     * @return Created tool
     */
    suspend fun defineTool(
        name: String,
        description: String? = null,
        parameters: Map<String, Any?>? = null
    ): AgentTool {
        val body = mutableMapOf<String, Any?>("name" to name)
        description?.let { body["description"] = it }
        parameters?.let { body["parameters"] = it }

        val response = httpClient.post("/ai/tools", body)

        @Suppress("UNCHECKED_CAST")
        val map = gson.fromJson<Map<String, Any?>>(response, Map::class.java)
        val toolMap = map["tool"] as? Map<String, Any?> ?: map
        return AgentTool.fromMap(toolMap)
    }

    /**
     * List all defined tools
     *
     * @return List of tools
     */
    suspend fun listTools(): List<AgentTool> {
        val response = httpClient.get("/ai/tools")

        val tools = response.getAsJsonArray("tools") ?: JsonArray()
        return tools.map { element ->
            @Suppress("UNCHECKED_CAST")
            val map = gson.fromJson<Map<String, Any?>>(element, Map::class.java)
            AgentTool.fromMap(map)
        }
    }

    /**
     * Delete a tool
     *
     * @param toolId Tool ID
     */
    suspend fun deleteTool(toolId: String) {
        httpClient.delete("/ai/tools/$toolId")
    }

    // ==================== TTS & STT ====================

    /**
     * Convert text to speech
     *
     * @param text Text to convert to speech
     * @param voice Optional voice preset (defaults to 'v2/en_speaker_6')
     * @return TTSResponse with base64 encoded audio data
     */
    suspend fun textToSpeech(
        text: String,
        voice: String? = null
    ): TTSResponse {
        val body = buildJsonObject {
            put("text", text)
            voice?.let { put("voice", it) }
        }

        val response = httpClient.post("/ai/tts", body)
        return TTSResponse(
            success = response["success"]?.asBoolean ?: false,
            audio = response["audio"]?.asString ?: "",
            format = response["format"]?.asString ?: "wav",
            sampleRate = response["sample_rate"]?.asInt ?: 24000
        )
    }

    /**
     * Convert speech to text
     *
     * @param audio Base64 encoded audio data
     * @return STTResponse with transcribed text
     */
    suspend fun speechToText(audio: String): STTResponse {
        val body = buildJsonObject {
            put("audio", audio)
        }

        val response = httpClient.post("/ai/stt", body)
        return STTResponse(
            success = response["success"]?.asBoolean ?: false,
            text = response["text"]?.asString ?: ""
        )
    }

    // ==================== PROVIDER SETTINGS ====================

    /**
     * Get the LLM provider configured for this project.
     *
     * Supported providers: huggingface, openai, groq, anthropic, google,
     *   together, mistral, openrouter, custom
     *
     * @return Map with "settings" (current config) and "supportedProviders" list.
     *         The API key is never returned — check "hasApiKey" instead.
     */
    suspend fun getProviderSettings(): Map<String, Any?> {
        return httpClient.get("/ai/settings/provider")
    }

    /**
     * Configure which LLM provider this project uses.
     *
     * @param input [UpdateProviderInput] with provider, apiKey, and optional model/baseUrl.
     * @return Map with "message" confirming the update.
     *
     * Example:
     * ```kotlin
     * scs.ai.updateProviderSettings(UpdateProviderInput(
     *     provider = "huggingface",
     *     apiKey   = "hf_...",
     *     model    = "meta-llama/Llama-3.2-3B-Instruct"
     * ))
     * ```
     */
    suspend fun updateProviderSettings(input: UpdateProviderInput): Map<String, Any?> {
        val body = buildJsonObject {
            put("provider", input.provider)
            put("apiKey", input.apiKey)
            input.model?.let { put("model", it) }
            input.baseUrl?.let { put("baseUrl", it) }
        }
        return httpClient.put("/ai/settings/provider", body)
    }
}

/**
 * TTS response data class
 */
data class TTSResponse(
    val success: Boolean,
    val audio: String,
    val format: String,
    val sampleRate: Int
)

/**
 * STT response data class
 */
data class STTResponse(
    val success: Boolean,
    val text: String
)

// ==================== PROVIDER SETTINGS ====================

/**
 * Current LLM provider configuration for a project.
 * The API key is never returned — only [hasApiKey] indicates one is set.
 */
data class AiProviderSettings(
    val provider: String,
    val model: String,
    val baseUrl: String,
    val hasApiKey: Boolean
)

/**
 * Input for updating the LLM provider configuration.
 *
 * Supported providers: huggingface, openai, groq, anthropic, google,
 *   together, mistral, openrouter, custom
 */
data class UpdateProviderInput(
    /** Provider ID — e.g. "huggingface", "openai", "groq" */
    val provider: String,
    /** API key or token for the provider.
     *  Hugging Face: get a free token at huggingface.co/settings/tokens */
    val apiKey: String,
    /** Default model ID (optional; provider default used if null) */
    val model: String? = null,
    /** Custom base URL — only needed when provider = "custom" */
    val baseUrl: String? = null
)
