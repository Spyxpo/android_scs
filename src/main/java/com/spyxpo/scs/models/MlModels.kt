package com.spyxpo.scs.models

import com.google.gson.annotations.SerializedName

/**
 * Result from text recognition (OCR).
 */
data class TextRecognitionResult(
    @SerializedName("text")
    val text: String,

    @SerializedName("confidence")
    val confidence: Double? = null,

    @SerializedName("blocks")
    val blocks: List<TextBlock>? = null
) {
    companion object {
        @Suppress("UNCHECKED_CAST")
        fun fromMap(map: Map<String, Any?>): TextRecognitionResult {
            return TextRecognitionResult(
                text = map["text"] as? String ?: "",
                confidence = (map["confidence"] as? Number)?.toDouble(),
                blocks = (map["blocks"] as? List<Map<String, Any?>>)?.map { TextBlock.fromMap(it) }
            )
        }
    }
}

/**
 * A block of recognized text.
 */
data class TextBlock(
    @SerializedName("text")
    val text: String,

    @SerializedName("confidence")
    val confidence: Double? = null,

    @SerializedName("boundingBox")
    val boundingBox: BoundingBox? = null
) {
    companion object {
        @Suppress("UNCHECKED_CAST")
        fun fromMap(map: Map<String, Any?>): TextBlock {
            return TextBlock(
                text = map["text"] as? String ?: "",
                confidence = (map["confidence"] as? Number)?.toDouble(),
                boundingBox = (map["boundingBox"] as? Map<String, Any?>)?.let { BoundingBox.fromMap(it) }
            )
        }
    }
}

/**
 * Bounding box coordinates.
 */
data class BoundingBox(
    @SerializedName("x")
    val x: Int,

    @SerializedName("y")
    val y: Int,

    @SerializedName("width")
    val width: Int,

    @SerializedName("height")
    val height: Int
) {
    companion object {
        fun fromMap(map: Map<String, Any?>): BoundingBox {
            return BoundingBox(
                x = (map["x"] as? Number)?.toInt() ?: 0,
                y = (map["y"] as? Number)?.toInt() ?: 0,
                width = (map["width"] as? Number)?.toInt() ?: 0,
                height = (map["height"] as? Number)?.toInt() ?: 0
            )
        }
    }
}

/**
 * Result from image labeling.
 */
data class ImageLabelingResult(
    @SerializedName("labels")
    val labels: List<ImageLabel>,

    @SerializedName("processedAt")
    val processedAt: String? = null
) {
    companion object {
        @Suppress("UNCHECKED_CAST")
        fun fromMap(map: Map<String, Any?>): ImageLabelingResult {
            return ImageLabelingResult(
                labels = (map["labels"] as? List<Map<String, Any?>>)?.map { ImageLabel.fromMap(it) }
                    ?: emptyList(),
                processedAt = map["processedAt"] as? String
            )
        }
    }
}

/**
 * A label detected in an image.
 */
data class ImageLabel(
    @SerializedName("label")
    val label: String,

    @SerializedName("confidence")
    val confidence: Double,

    @SerializedName("category")
    val category: String? = null
) {
    companion object {
        fun fromMap(map: Map<String, Any?>): ImageLabel {
            return ImageLabel(
                label = map["label"] as? String ?: "",
                confidence = (map["confidence"] as? Number)?.toDouble() ?: 0.0,
                category = map["category"] as? String
            )
        }
    }
}

/**
 * ML service statistics.
 */
data class MlStats(
    @SerializedName("textRecognitionCount")
    val textRecognitionCount: Int,

    @SerializedName("imageLabelingCount")
    val imageLabelingCount: Int,

    @SerializedName("totalRequests")
    val totalRequests: Int
)
