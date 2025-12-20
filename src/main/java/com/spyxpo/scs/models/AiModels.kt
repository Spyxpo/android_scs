package com.spyxpo.scs.models

import com.google.gson.annotations.SerializedName

/**
 * A message in an AI chat conversation.
 */
data class ChatMessage(
    @SerializedName("role")
    val role: String,

    @SerializedName("content")
    val content: String
) {
    companion object {
        const val ROLE_SYSTEM = "system"
        const val ROLE_USER = "user"
        const val ROLE_ASSISTANT = "assistant"

        fun system(content: String) = ChatMessage(ROLE_SYSTEM, content)
        fun user(content: String) = ChatMessage(ROLE_USER, content)
        fun assistant(content: String) = ChatMessage(ROLE_ASSISTANT, content)
    }
}

/**
 * Response from AI chat.
 */
data class ChatResponse(
    @SerializedName("content")
    val content: String,

    @SerializedName("model")
    val model: String? = null,

    @SerializedName("usage")
    val usage: TokenUsage? = null,

    @SerializedName("finishReason")
    val finishReason: String? = null
) {
    companion object {
        fun fromMap(map: Map<String, Any?>): ChatResponse {
            @Suppress("UNCHECKED_CAST")
            return ChatResponse(
                content = map["content"] as? String
                    ?: map["response"] as? String
                    ?: (map["message"] as? Map<String, Any?>)?.get("content") as? String
                    ?: "",
                model = map["model"] as? String,
                usage = (map["usage"] as? Map<String, Any?>)?.let { TokenUsage.fromMap(it) },
                finishReason = map["finishReason"] as? String
            )
        }
    }
}

/**
 * Response from AI text completion.
 */
data class CompletionResponse(
    @SerializedName("content")
    val content: String,

    @SerializedName("model")
    val model: String? = null,

    @SerializedName("usage")
    val usage: TokenUsage? = null
) {
    companion object {
        fun fromMap(map: Map<String, Any?>): CompletionResponse {
            @Suppress("UNCHECKED_CAST")
            return CompletionResponse(
                content = map["content"] as? String
                    ?: map["response"] as? String
                    ?: map["text"] as? String
                    ?: "",
                model = map["model"] as? String,
                usage = (map["usage"] as? Map<String, Any?>)?.let { TokenUsage.fromMap(it) }
            )
        }
    }
}

/**
 * Response from AI image generation.
 */
data class ImageGenerationResponse(
    @SerializedName("imageUrl")
    val imageUrl: String? = null,

    @SerializedName("imageBase64")
    val imageBase64: String? = null,

    @SerializedName("revisedPrompt")
    val revisedPrompt: String? = null
) {
    companion object {
        fun fromMap(map: Map<String, Any?>): ImageGenerationResponse {
            return ImageGenerationResponse(
                imageUrl = map["imageUrl"] as? String
                    ?: map["url"] as? String
                    ?: map["image_url"] as? String,
                imageBase64 = map["imageBase64"] as? String
                    ?: map["base64"] as? String
                    ?: map["image_base64"] as? String,
                revisedPrompt = map["revisedPrompt"] as? String
                    ?: map["revised_prompt"] as? String
            )
        }
    }
}

/**
 * Token usage information.
 */
data class TokenUsage(
    @SerializedName("promptTokens")
    val promptTokens: Int,

    @SerializedName("completionTokens")
    val completionTokens: Int,

    @SerializedName("totalTokens")
    val totalTokens: Int
) {
    companion object {
        fun fromMap(map: Map<String, Any?>): TokenUsage {
            return TokenUsage(
                promptTokens = (map["promptTokens"] as? Number)?.toInt()
                    ?: (map["prompt_tokens"] as? Number)?.toInt()
                    ?: 0,
                completionTokens = (map["completionTokens"] as? Number)?.toInt()
                    ?: (map["completion_tokens"] as? Number)?.toInt()
                    ?: 0,
                totalTokens = (map["totalTokens"] as? Number)?.toInt()
                    ?: (map["total_tokens"] as? Number)?.toInt()
                    ?: 0
            )
        }
    }
}

/**
 * An AI model available for use.
 */
data class AiModel(
    @SerializedName("name")
    val name: String,

    @SerializedName("size")
    val size: Long? = null,

    @SerializedName("modifiedAt")
    val modifiedAt: String? = null,

    @SerializedName("digest")
    val digest: String? = null,

    @SerializedName("details")
    val details: ModelDetails? = null
) {
    companion object {
        @Suppress("UNCHECKED_CAST")
        fun fromMap(map: Map<String, Any?>): AiModel {
            return AiModel(
                name = map["name"] as? String ?: "",
                size = (map["size"] as? Number)?.toLong(),
                modifiedAt = map["modifiedAt"] as? String ?: map["modified_at"] as? String,
                digest = map["digest"] as? String,
                details = (map["details"] as? Map<String, Any?>)?.let { ModelDetails.fromMap(it) }
            )
        }
    }
}

/**
 * Details about an AI model.
 */
data class ModelDetails(
    @SerializedName("format")
    val format: String? = null,

    @SerializedName("family")
    val family: String? = null,

    @SerializedName("parameterSize")
    val parameterSize: String? = null,

    @SerializedName("quantizationLevel")
    val quantizationLevel: String? = null
) {
    companion object {
        fun fromMap(map: Map<String, Any?>): ModelDetails {
            return ModelDetails(
                format = map["format"] as? String,
                family = map["family"] as? String,
                parameterSize = map["parameterSize"] as? String ?: map["parameter_size"] as? String,
                quantizationLevel = map["quantizationLevel"] as? String ?: map["quantization_level"] as? String
            )
        }
    }
}

/**
 * Response from listing AI models.
 */
data class ModelsListResponse(
    @SerializedName("models")
    val models: List<AiModel>
)
