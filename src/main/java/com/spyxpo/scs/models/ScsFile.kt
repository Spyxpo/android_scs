package com.spyxpo.scs.models

import com.google.gson.annotations.SerializedName

/**
 * Represents file metadata in SCS storage.
 */
data class ScsFile(
    @SerializedName("id")
    val id: String,

    @SerializedName("name")
    val name: String,

    @SerializedName("path")
    val path: String? = null,

    @SerializedName("folder")
    val folder: String? = null,

    @SerializedName("mimeType")
    val mimeType: String? = null,

    @SerializedName("size")
    val size: Long? = null,

    @SerializedName("url")
    val url: String? = null,

    @SerializedName("downloadUrl")
    val downloadUrl: String? = null,

    @SerializedName("createdAt")
    val createdAt: String? = null,

    @SerializedName("updatedAt")
    val updatedAt: String? = null,

    @SerializedName("metadata")
    val metadata: Map<String, Any>? = null
) {
    /**
     * Check if this is an image file
     */
    fun isImage(): Boolean {
        return mimeType?.startsWith("image/") == true
    }

    /**
     * Check if this is a video file
     */
    fun isVideo(): Boolean {
        return mimeType?.startsWith("video/") == true
    }

    /**
     * Check if this is an audio file
     */
    fun isAudio(): Boolean {
        return mimeType?.startsWith("audio/") == true
    }

    /**
     * Check if this is a PDF file
     */
    fun isPdf(): Boolean {
        return mimeType == "application/pdf"
    }

    /**
     * Get the file extension
     */
    fun getExtension(): String? {
        return name.substringAfterLast('.', "").takeIf { it.isNotEmpty() }
    }

    /**
     * Get human-readable file size
     */
    fun getFormattedSize(): String {
        val bytes = size ?: return "Unknown"
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
            else -> "${bytes / (1024 * 1024 * 1024)} GB"
        }
    }

    companion object {
        /**
         * Create a file from a JSON map
         */
        @Suppress("UNCHECKED_CAST")
        fun fromMap(map: Map<String, Any?>): ScsFile {
            return ScsFile(
                id = map["id"] as? String ?: map["_id"] as? String ?: "",
                name = map["name"] as? String ?: "",
                path = map["path"] as? String,
                folder = map["folder"] as? String,
                mimeType = map["mimeType"] as? String,
                size = (map["size"] as? Number)?.toLong(),
                url = map["url"] as? String,
                downloadUrl = map["downloadUrl"] as? String,
                createdAt = map["createdAt"] as? String,
                updatedAt = map["updatedAt"] as? String,
                metadata = map["metadata"] as? Map<String, Any>
            )
        }
    }
}

/**
 * Response from listing files
 */
data class FilesListResponse(
    @SerializedName("files")
    val files: List<ScsFile>,

    @SerializedName("total")
    val total: Int? = null,

    @SerializedName("limit")
    val limit: Int? = null,

    @SerializedName("skip")
    val skip: Int? = null
)

/**
 * Response from file upload
 */
data class FileUploadResponse(
    @SerializedName("file")
    val file: ScsFile,

    @SerializedName("message")
    val message: String? = null
)
