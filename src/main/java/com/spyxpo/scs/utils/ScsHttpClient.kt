package com.spyxpo.scs.utils

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import com.spyxpo.scs.ScsConfig
import com.spyxpo.scs.ScsException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * HTTP client for making API requests to the SCS backend.
 * Handles authentication headers, request/response serialization, and error handling.
 */
class ScsHttpClient(
    private val config: ScsConfig,
    private val sessionStorage: SessionStorage
) {
    private val gson = Gson()

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val originalRequest = chain.request()
            val requestBuilder = originalRequest.newBuilder()
                .header("X-API-Key", config.apiKey)
                .header("Content-Type", "application/json")

            // Add auth token if available
            sessionStorage.getToken()?.let { token ->
                requestBuilder.header("Authorization", "Bearer $token")
            }

            chain.proceed(requestBuilder.build())
        }
        .build()

    private val baseUrl: String get() = config.getApiUrl()

    /**
     * Make a GET request
     */
    suspend fun get(
        endpoint: String,
        queryParams: Map<String, String>? = null
    ): JsonObject = withContext(Dispatchers.IO) {
        val urlBuilder = buildUrl(endpoint)
        queryParams?.forEach { (key, value) ->
            urlBuilder.addQueryParameter(key, value)
        }

        val request = Request.Builder()
            .url(urlBuilder.build())
            .get()
            .build()

        executeRequest(request)
    }

    /**
     * Make a POST request with JSON body
     */
    suspend fun post(
        endpoint: String,
        body: Any? = null
    ): JsonObject = withContext(Dispatchers.IO) {
        val jsonBody = if (body != null) gson.toJson(body) else "{}"
        val requestBody = jsonBody.toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(buildUrl(endpoint).build())
            .post(requestBody)
            .build()

        executeRequest(request)
    }

    /**
     * Make a PUT request with JSON body
     */
    suspend fun put(
        endpoint: String,
        body: Any? = null
    ): JsonObject = withContext(Dispatchers.IO) {
        val jsonBody = if (body != null) gson.toJson(body) else "{}"
        val requestBody = jsonBody.toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(buildUrl(endpoint).build())
            .put(requestBody)
            .build()

        executeRequest(request)
    }

    /**
     * Make a PATCH request with JSON body
     */
    suspend fun patch(
        endpoint: String,
        body: Any? = null
    ): JsonObject = withContext(Dispatchers.IO) {
        val jsonBody = if (body != null) gson.toJson(body) else "{}"
        val requestBody = jsonBody.toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(buildUrl(endpoint).build())
            .patch(requestBody)
            .build()

        executeRequest(request)
    }

    /**
     * Make a DELETE request
     */
    suspend fun delete(
        endpoint: String
    ): JsonObject = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(buildUrl(endpoint).build())
            .delete()
            .build()

        executeRequest(request)
    }

    /**
     * Upload a file with multipart form data
     */
    suspend fun uploadFile(
        endpoint: String,
        file: File,
        fieldName: String = "file",
        additionalFields: Map<String, String>? = null
    ): JsonObject = withContext(Dispatchers.IO) {
        val mimeType = getMimeType(file.name)

        val multipartBuilder = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                fieldName,
                file.name,
                file.asRequestBody(mimeType.toMediaType())
            )

        additionalFields?.forEach { (key, value) ->
            multipartBuilder.addFormDataPart(key, value)
        }

        val request = Request.Builder()
            .url(buildUrl(endpoint).build())
            .post(multipartBuilder.build())
            .header("X-API-Key", config.apiKey)
            .apply {
                sessionStorage.getToken()?.let { token ->
                    header("Authorization", "Bearer $token")
                }
            }
            .build()

        // Use a separate client without Content-Type header for multipart
        val multipartClient = client.newBuilder().build()
        executeRequestWithClient(multipartClient, request)
    }

    /**
     * Upload bytes as a file
     */
    suspend fun uploadBytes(
        endpoint: String,
        bytes: ByteArray,
        filename: String,
        fieldName: String = "file",
        additionalFields: Map<String, String>? = null
    ): JsonObject = withContext(Dispatchers.IO) {
        val mimeType = getMimeType(filename)

        val multipartBuilder = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                fieldName,
                filename,
                bytes.toRequestBody(mimeType.toMediaType())
            )

        additionalFields?.forEach { (key, value) ->
            multipartBuilder.addFormDataPart(key, value)
        }

        val request = Request.Builder()
            .url(buildUrl(endpoint).build())
            .post(multipartBuilder.build())
            .header("X-API-Key", config.apiKey)
            .apply {
                sessionStorage.getToken()?.let { token ->
                    header("Authorization", "Bearer $token")
                }
            }
            .build()

        val multipartClient = client.newBuilder().build()
        executeRequestWithClient(multipartClient, request)
    }

    /**
     * Download a file as bytes
     */
    suspend fun downloadFile(endpoint: String): ByteArray = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(buildUrl(endpoint).build())
            .get()
            .build()

        suspendCancellableCoroutine { continuation ->
            val call = client.newCall(request)

            continuation.invokeOnCancellation {
                call.cancel()
            }

            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    continuation.resumeWithException(
                        ScsException(
                            message = e.message ?: "Network error",
                            code = ScsException.NETWORK_ERROR
                        )
                    )
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        if (!it.isSuccessful) {
                            continuation.resumeWithException(
                                ScsException.fromApiError(
                                    it.code,
                                    it.body?.string(),
                                    "Download failed"
                                )
                            )
                            return
                        }

                        val bytes = it.body?.bytes()
                        if (bytes != null) {
                            continuation.resume(bytes)
                        } else {
                            continuation.resumeWithException(
                                ScsException(
                                    message = "Empty response",
                                    code = ScsException.UNKNOWN_ERROR
                                )
                            )
                        }
                    }
                }
            })
        }
    }

    private fun buildUrl(endpoint: String): HttpUrl.Builder {
        val cleanEndpoint = endpoint.trimStart('/')
        return HttpUrl.parse("$baseUrl/$cleanEndpoint")?.newBuilder()
            ?: throw ScsException(
                message = "Invalid URL: $baseUrl/$cleanEndpoint",
                code = ScsException.INVALID_ARGUMENT
            )
    }

    private suspend fun executeRequest(request: Request): JsonObject {
        return executeRequestWithClient(client, request)
    }

    private suspend fun executeRequestWithClient(
        httpClient: OkHttpClient,
        request: Request
    ): JsonObject = suspendCancellableCoroutine { continuation ->
        val call = httpClient.newCall(request)

        continuation.invokeOnCancellation {
            call.cancel()
        }

        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                continuation.resumeWithException(
                    ScsException(
                        message = e.message ?: "Network error",
                        code = ScsException.NETWORK_ERROR
                    )
                )
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val responseBody = it.body?.string()

                    if (!it.isSuccessful) {
                        continuation.resumeWithException(
                            ScsException.fromApiError(
                                it.code,
                                responseBody,
                                "Request failed"
                            )
                        )
                        return
                    }

                    try {
                        val jsonObject = if (responseBody.isNullOrBlank()) {
                            JsonObject()
                        } else {
                            gson.fromJson(responseBody, JsonObject::class.java)
                                ?: JsonObject()
                        }
                        continuation.resume(jsonObject)
                    } catch (e: Exception) {
                        // If response isn't JSON, wrap it
                        val wrapper = JsonObject()
                        wrapper.addProperty("data", responseBody)
                        continuation.resume(wrapper)
                    }
                }
            }
        })
    }

    private fun getMimeType(filename: String): String {
        val extension = filename.substringAfterLast('.', "").lowercase()
        return when (extension) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            "svg" -> "image/svg+xml"
            "pdf" -> "application/pdf"
            "json" -> "application/json"
            "xml" -> "application/xml"
            "txt" -> "text/plain"
            "html", "htm" -> "text/html"
            "css" -> "text/css"
            "js" -> "application/javascript"
            "mp3" -> "audio/mpeg"
            "mp4" -> "video/mp4"
            "zip" -> "application/zip"
            else -> "application/octet-stream"
        }
    }

    /**
     * Parse JSON response to a specific type
     */
    inline fun <reified T> parseResponse(json: JsonObject): T {
        return gson.fromJson(json, T::class.java)
    }

    /**
     * Parse JSON response to a list of specific type
     */
    inline fun <reified T> parseResponseList(json: JsonObject, key: String): List<T> {
        val array = json.getAsJsonArray(key)
        val type = object : TypeToken<List<T>>() {}.type
        return gson.fromJson(array, type)
    }
}
