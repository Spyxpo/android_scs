package com.spyxpo.scs.services

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.spyxpo.scs.models.ScsFile
import com.spyxpo.scs.utils.ScsHttpClient
import java.io.File

/**
 * Service for file storage operations.
 *
 * Example usage:
 * ```kotlin
 * // Upload a file
 * val file = File("/path/to/image.jpg")
 * val metadata = scs.storage.upload(file, "images")
 *
 * // Upload bytes
 * val bytes = byteArrayOf(...)
 * val metadata = scs.storage.uploadBytes(bytes, "document.pdf", "documents")
 *
 * // List files
 * val files = scs.storage.list("images")
 *
 * // Get download URL
 * val url = scs.storage.getDownloadUrl(fileId)
 *
 * // Download file
 * val bytes = scs.storage.download(fileId)
 *
 * // Delete file
 * scs.storage.delete(fileId)
 * ```
 */
class StorageService(private val httpClient: ScsHttpClient) {

    private val gson = Gson()

    /**
     * Upload a file
     *
     * @param file The file to upload
     * @param folder Optional folder path
     * @return File metadata
     */
    suspend fun upload(file: File, folder: String? = null): ScsFile {
        val additionalFields = folder?.let { mapOf("folder" to it) }
        val response = httpClient.uploadFile("/storage/upload", file, "file", additionalFields)
        return parseFile(response)
    }

    /**
     * Upload bytes as a file
     *
     * @param bytes File content as bytes
     * @param filename Name for the file
     * @param folder Optional folder path
     * @return File metadata
     */
    suspend fun uploadBytes(
        bytes: ByteArray,
        filename: String,
        folder: String? = null
    ): ScsFile {
        val additionalFields = folder?.let { mapOf("folder" to it) }
        val response = httpClient.uploadBytes("/storage/upload", bytes, filename, "file", additionalFields)
        return parseFile(response)
    }

    /**
     * List files in storage
     *
     * @param folder Optional folder to list
     * @param limit Maximum number of files to return
     * @param skip Number of files to skip
     * @return List of file metadata
     */
    suspend fun list(
        folder: String? = null,
        limit: Int? = null,
        skip: Int? = null
    ): List<ScsFile> {
        val params = mutableMapOf<String, String>()
        folder?.let { params["folder"] = it }
        limit?.let { params["limit"] = it.toString() }
        skip?.let { params["skip"] = it.toString() }

        val response = httpClient.get("/storage/files", params)
        return parseFiles(response.getAsJsonArray("files") ?: JsonArray())
    }

    /**
     * Get file metadata
     *
     * @param fileId File ID
     * @return File metadata
     */
    suspend fun getMetadata(fileId: String): ScsFile {
        val response = httpClient.get("/storage/files/$fileId/metadata")
        return parseFile(response)
    }

    /**
     * Get the download URL for a file
     *
     * @param fileId File ID
     * @return Download URL
     */
    suspend fun getDownloadUrl(fileId: String): String {
        val response = httpClient.get("/storage/files/$fileId")
        return response.get("url")?.asString
            ?: response.get("downloadUrl")?.asString
            ?: ""
    }

    /**
     * Download a file as bytes
     *
     * @param fileId File ID
     * @return File content as bytes
     */
    suspend fun download(fileId: String): ByteArray {
        return httpClient.downloadFile("/storage/files/$fileId/download")
    }

    /**
     * Delete a file
     *
     * @param fileId File ID
     */
    suspend fun delete(fileId: String) {
        httpClient.delete("/storage/files/$fileId")
    }

    /**
     * Create a folder
     *
     * @param path Folder path
     */
    suspend fun createFolder(path: String) {
        val body = mapOf("path" to path)
        httpClient.post("/storage/folders", body)
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseFile(response: com.google.gson.JsonObject): ScsFile {
        val fileObj = response.getAsJsonObject("file")
            ?: response.getAsJsonObject("data")
            ?: response

        val map = gson.fromJson<Map<String, Any?>>(fileObj, Map::class.java)
        return ScsFile.fromMap(map)
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseFiles(array: JsonArray): List<ScsFile> {
        return array.map { element ->
            val obj = element.asJsonObject
            val map = gson.fromJson<Map<String, Any?>>(obj, Map::class.java)
            ScsFile.fromMap(map)
        }
    }
}
