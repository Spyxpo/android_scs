package com.spyxpo.scs.services

import com.google.gson.Gson
import com.spyxpo.scs.models.ImageLabelingResult
import com.spyxpo.scs.models.MlStats
import com.spyxpo.scs.models.TextRecognitionResult
import com.spyxpo.scs.utils.ScsHttpClient
import java.io.File

/**
 * Service for machine learning operations.
 *
 * Example usage:
 * ```kotlin
 * // Text recognition (OCR) from a file
 * val imageFile = File("/path/to/image.jpg")
 * val result = scs.ml.recognizeText(imageFile)
 * println("Recognized text: ${result.text}")
 *
 * // Text recognition from bytes
 * val bytes = // ... load image bytes
 * val result = scs.ml.recognizeTextFromBytes(bytes, "image.jpg")
 *
 * // Image labeling
 * val labels = scs.ml.labelImage(imageFile)
 * labels.labels.forEach { label ->
 *     println("${label.label}: ${label.confidence}")
 * }
 *
 * // Get ML statistics
 * val stats = scs.ml.getStats()
 * ```
 */
class MlService(private val httpClient: ScsHttpClient) {

    private val gson = Gson()

    /**
     * Recognize text in an image (OCR)
     *
     * @param file Image file
     * @return Text recognition result
     */
    suspend fun recognizeText(file: File): TextRecognitionResult {
        val response = httpClient.uploadFile("/ml/text-recognition", file, "image")
        return parseTextRecognitionResult(response)
    }

    /**
     * Recognize text in an image from bytes (OCR)
     *
     * @param bytes Image bytes
     * @param filename Filename with extension (for mime type detection)
     * @return Text recognition result
     */
    suspend fun recognizeTextFromBytes(
        bytes: ByteArray,
        filename: String = "image.jpg"
    ): TextRecognitionResult {
        val response = httpClient.uploadBytes("/ml/text-recognition", bytes, filename, "image")
        return parseTextRecognitionResult(response)
    }

    /**
     * Label objects in an image
     *
     * @param file Image file
     * @return Image labeling result
     */
    suspend fun labelImage(file: File): ImageLabelingResult {
        val response = httpClient.uploadFile("/ml/image-labeling", file, "image")
        return parseImageLabelingResult(response)
    }

    /**
     * Label objects in an image from bytes
     *
     * @param bytes Image bytes
     * @param filename Filename with extension (for mime type detection)
     * @return Image labeling result
     */
    suspend fun labelImageFromBytes(
        bytes: ByteArray,
        filename: String = "image.jpg"
    ): ImageLabelingResult {
        val response = httpClient.uploadBytes("/ml/image-labeling", bytes, filename, "image")
        return parseImageLabelingResult(response)
    }

    /**
     * Get ML service statistics
     *
     * @return ML statistics
     */
    suspend fun getStats(): MlStats {
        val response = httpClient.get("/ml/stats")
        return gson.fromJson(response, MlStats::class.java)
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseTextRecognitionResult(response: com.google.gson.JsonObject): TextRecognitionResult {
        val resultObj = response.getAsJsonObject("result")
            ?: response

        val map = gson.fromJson<Map<String, Any?>>(resultObj, Map::class.java)
        return TextRecognitionResult.fromMap(map)
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseImageLabelingResult(response: com.google.gson.JsonObject): ImageLabelingResult {
        val resultObj = response.getAsJsonObject("result")
            ?: response

        val map = gson.fromJson<Map<String, Any?>>(resultObj, Map::class.java)
        return ImageLabelingResult.fromMap(map)
    }
}
