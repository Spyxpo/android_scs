package com.spyxpo.scs.services

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.spyxpo.scs.ScsConfig
import com.spyxpo.scs.ScsException
import com.spyxpo.scs.utils.SessionStorage
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Service for realtime database operations.
 *
 * Example usage:
 * ```kotlin
 * // Get a database reference
 * val ref = scs.realtime.ref("users/user1")
 *
 * // Set data
 * ref.set(mapOf("name" to "John", "status" to "online"))
 *
 * // Listen for changes
 * ref.onValue { data ->
 *     println("User data: $data")
 * }
 *
 * // Update data
 * ref.update(mapOf("status" to "offline"))
 *
 * // Push new data (auto-generated key)
 * val newRef = scs.realtime.ref("messages").push(mapOf("text" to "Hello"))
 *
 * // Remove data
 * ref.remove()
 *
 * // Stop listening
 * ref.off()
 * ```
 */
class RealtimeService(
    private val config: ScsConfig,
    private val sessionStorage: SessionStorage
) {
    private val gson = Gson()
    private var socket: Socket? = null
    private val listeners = ConcurrentHashMap<String, MutableList<(Any?) -> Unit>>()

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)

    /**
     * Flow of the connection state
     */
    val connectionState: Flow<ConnectionState> = _connectionState.asStateFlow()

    private val _events = MutableSharedFlow<RealtimeEvent>()

    /**
     * Flow of realtime events
     */
    val events: Flow<RealtimeEvent> = _events.asSharedFlow()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Connection states for the realtime service
     */
    enum class ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        RECONNECTING
    }

    /**
     * Connect to the realtime service
     */
    fun connect() {
        if (socket?.connected() == true) return

        _connectionState.value = ConnectionState.CONNECTING

        try {
            val options = IO.Options().apply {
                transports = arrayOf("websocket")
                auth = mapOf(
                    "apiKey" to config.apiKey,
                    "token" to sessionStorage.getToken()
                )
            }

            socket = IO.socket(config.baseUrl, options).apply {
                on(Socket.EVENT_CONNECT) {
                    _connectionState.value = ConnectionState.CONNECTED
                }

                on(Socket.EVENT_DISCONNECT) {
                    _connectionState.value = ConnectionState.DISCONNECTED
                }

                on(Socket.EVENT_CONNECT_ERROR) { args ->
                    _connectionState.value = ConnectionState.DISCONNECTED
                }

                on("value") { args ->
                    handleValueEvent(args)
                }

                on("child_added") { args ->
                    handleChildEvent("child_added", args)
                }

                on("child_changed") { args ->
                    handleChildEvent("child_changed", args)
                }

                on("child_removed") { args ->
                    handleChildEvent("child_removed", args)
                }

                connect()
            }
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.DISCONNECTED
            throw ScsException(
                message = "Failed to connect to realtime service: ${e.message}",
                code = ScsException.NETWORK_ERROR
            )
        }
    }

    /**
     * Disconnect from the realtime service
     */
    fun disconnect() {
        socket?.disconnect()
        socket = null
        listeners.clear()
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    /**
     * Check if connected
     */
    fun isConnected(): Boolean = socket?.connected() == true

    /**
     * Get a reference to a path in the realtime database
     *
     * @param path The path (e.g., "users/user1" or "messages")
     * @return DatabaseReference for the path
     */
    fun ref(path: String): DatabaseReference {
        return DatabaseReference(this, path)
    }

    // Internal methods for DatabaseReference

    internal suspend fun getData(path: String): Any? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("${config.getApiUrl()}/realtime/data/$path")
            .header("X-API-Key", config.apiKey)
            .apply {
                sessionStorage.getToken()?.let {
                    header("Authorization", "Bearer $it")
                }
            }
            .get()
            .build()

        val response = httpClient.newCall(request).execute()
        val body = response.body?.string()

        if (!response.isSuccessful) {
            throw ScsException.fromApiError(response.code, body, "Failed to get data")
        }

        val jsonObject = gson.fromJson(body, JsonObject::class.java)
        jsonObject?.get("data")?.let { gson.fromJson(it, Any::class.java) }
    }

    internal suspend fun setData(path: String, data: Any?) = withContext(Dispatchers.IO) {
        val jsonBody = gson.toJson(mapOf("data" to data))
        val request = Request.Builder()
            .url("${config.getApiUrl()}/realtime/data/$path")
            .header("X-API-Key", config.apiKey)
            .header("Content-Type", "application/json")
            .apply {
                sessionStorage.getToken()?.let {
                    header("Authorization", "Bearer $it")
                }
            }
            .put(jsonBody.toRequestBody("application/json".toMediaType()))
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            throw ScsException.fromApiError(response.code, response.body?.string(), "Failed to set data")
        }
    }

    internal suspend fun updateData(path: String, data: Map<String, Any?>) = withContext(Dispatchers.IO) {
        val jsonBody = gson.toJson(data)
        val request = Request.Builder()
            .url("${config.getApiUrl()}/realtime/data/$path")
            .header("X-API-Key", config.apiKey)
            .header("Content-Type", "application/json")
            .apply {
                sessionStorage.getToken()?.let {
                    header("Authorization", "Bearer $it")
                }
            }
            .patch(jsonBody.toRequestBody("application/json".toMediaType()))
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            throw ScsException.fromApiError(response.code, response.body?.string(), "Failed to update data")
        }
    }

    internal suspend fun pushData(path: String, data: Any?): String = withContext(Dispatchers.IO) {
        val jsonBody = gson.toJson(mapOf("data" to data))
        val request = Request.Builder()
            .url("${config.getApiUrl()}/realtime/data/$path")
            .header("X-API-Key", config.apiKey)
            .header("Content-Type", "application/json")
            .apply {
                sessionStorage.getToken()?.let {
                    header("Authorization", "Bearer $it")
                }
            }
            .post(jsonBody.toRequestBody("application/json".toMediaType()))
            .build()

        val response = httpClient.newCall(request).execute()
        val body = response.body?.string()

        if (!response.isSuccessful) {
            throw ScsException.fromApiError(response.code, body, "Failed to push data")
        }

        val jsonObject = gson.fromJson(body, JsonObject::class.java)
        jsonObject?.get("key")?.asString ?: ""
    }

    internal suspend fun removeData(path: String) = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("${config.getApiUrl()}/realtime/data/$path")
            .header("X-API-Key", config.apiKey)
            .apply {
                sessionStorage.getToken()?.let {
                    header("Authorization", "Bearer $it")
                }
            }
            .delete()
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            throw ScsException.fromApiError(response.code, response.body?.string(), "Failed to remove data")
        }
    }

    internal fun subscribe(path: String, callback: (Any?) -> Unit) {
        if (!isConnected()) {
            connect()
        }

        listeners.getOrPut(path) { mutableListOf() }.add(callback)

        socket?.emit("subscribe", JSONObject().apply {
            put("path", path)
        })
    }

    internal fun unsubscribe(path: String) {
        listeners.remove(path)

        socket?.emit("unsubscribe", JSONObject().apply {
            put("path", path)
        })
    }

    private fun handleValueEvent(args: Array<Any>) {
        if (args.isEmpty()) return

        val eventData = args[0] as? JSONObject ?: return
        val path = eventData.optString("path")
        val data = eventData.opt("data")

        listeners[path]?.forEach { callback ->
            callback(data)
        }
    }

    private fun handleChildEvent(eventType: String, args: Array<Any>) {
        if (args.isEmpty()) return

        val eventData = args[0] as? JSONObject ?: return
        val path = eventData.optString("path")
        val key = eventData.optString("key")
        val data = eventData.opt("data")

        // Emit event to flow
        kotlinx.coroutines.GlobalScope.launch {
            _events.emit(RealtimeEvent(eventType, path, key, data))
        }
    }

    private fun kotlinx.coroutines.GlobalScope.launch(block: suspend () -> Unit) {
        kotlinx.coroutines.CoroutineScope(Dispatchers.Main).launch { block() }
    }
}

/**
 * Represents a realtime database event
 */
data class RealtimeEvent(
    val type: String,
    val path: String,
    val key: String?,
    val data: Any?
)

/**
 * Reference to a path in the realtime database
 */
class DatabaseReference(
    private val service: RealtimeService,
    val path: String
) {
    /**
     * Get the key (last segment of the path)
     */
    val key: String
        get() = path.split("/").lastOrNull() ?: ""

    /**
     * Get a child reference
     *
     * @param childPath Child path relative to this reference
     * @return DatabaseReference for the child
     */
    fun child(childPath: String): DatabaseReference {
        val newPath = if (path.isEmpty()) childPath else "$path/$childPath"
        return DatabaseReference(service, newPath)
    }

    /**
     * Get data at this path
     *
     * @return The data, or null if it doesn't exist
     */
    suspend fun get(): Any? {
        return service.getData(path)
    }

    /**
     * Set data at this path (overwrites existing data)
     *
     * @param data The data to set
     */
    suspend fun set(data: Any?) {
        service.setData(path, data)
    }

    /**
     * Update data at this path (merges with existing data)
     *
     * @param updates Map of updates to apply
     */
    suspend fun update(updates: Map<String, Any?>) {
        service.updateData(path, updates)
    }

    /**
     * Push new data with an auto-generated key
     *
     * @param data The data to push
     * @return Reference to the new data
     */
    suspend fun push(data: Any?): DatabaseReference {
        val key = service.pushData(path, data)
        return child(key)
    }

    /**
     * Remove data at this path
     */
    suspend fun remove() {
        service.removeData(path)
    }

    /**
     * Listen for value changes at this path
     *
     * @param callback Called when data changes
     */
    fun onValue(callback: (Any?) -> Unit) {
        service.subscribe(path, callback)
    }

    /**
     * Stop listening for changes at this path
     */
    fun off() {
        service.unsubscribe(path)
    }
}
