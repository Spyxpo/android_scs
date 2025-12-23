package com.spyxpo.scs.services

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.spyxpo.scs.ScsConfig
import com.spyxpo.scs.models.*
import com.spyxpo.scs.utils.ScsHttpClient
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Call service for voice/video calls, group calls, and live streaming.
 * Provides real-time communication capabilities using WebRTC and Socket.IO signaling.
 */
class CallService(
    private val httpClient: ScsHttpClient,
    private val config: ScsConfig
) {
    private val gson = Gson()
    private var socket: Socket? = null
    private var _connected = false
    private var _currentCallId: String? = null
    private var _participantId: String? = null

    // Event flows
    private val _participantJoined = MutableSharedFlow<Participant>()
    private val _participantLeft = MutableSharedFlow<Participant>()
    private val _newProducer = MutableSharedFlow<Map<String, Any>>()
    private val _mediaStateChanged = MutableSharedFlow<Map<String, Any>>()
    private val _chatMessage = MutableSharedFlow<Map<String, Any>>()
    private val _callEnded = MutableSharedFlow<Map<String, Any>>()
    private val _transcriptionSegment = MutableSharedFlow<Map<String, Any>>()

    /**
     * Whether connected to a call
     */
    val isConnected: Boolean get() = _connected

    /**
     * Current call ID
     */
    val currentCallId: String? get() = _currentCallId

    /**
     * Current participant ID
     */
    val participantId: String? get() = _participantId

    // Event streams
    val onParticipantJoined: Flow<Participant> = _participantJoined.asSharedFlow()
    val onParticipantLeft: Flow<Participant> = _participantLeft.asSharedFlow()
    val onNewProducer: Flow<Map<String, Any>> = _newProducer.asSharedFlow()
    val onMediaStateChanged: Flow<Map<String, Any>> = _mediaStateChanged.asSharedFlow()
    val onChatMessage: Flow<Map<String, Any>> = _chatMessage.asSharedFlow()
    val onCallEnded: Flow<Map<String, Any>> = _callEnded.asSharedFlow()
    val onTranscriptionSegment: Flow<Map<String, Any>> = _transcriptionSegment.asSharedFlow()

    /**
     * Get call service statistics
     */
    suspend fun getStats(): CallStats {
        val response = httpClient.get("calls/stats")
        val stats = response.getAsJsonObject("stats")
        return CallStats(
            totalCalls = stats?.get("totalCalls")?.asInt ?: 0,
            activeCalls = stats?.get("activeCalls")?.asInt ?: 0,
            totalMinutes = stats?.get("totalMinutes")?.asInt ?: 0,
            recordings = stats?.get("recordings")?.asInt ?: 0
        )
    }

    /**
     * Create a new call
     */
    suspend fun createCall(options: CreateCallOptions = CreateCallOptions()): Call {
        val body = mapOf(
            "type" to options.type.name.lowercase(),
            "mode" to options.mode.name.lowercase(),
            "displayName" to options.displayName,
            "maxParticipants" to options.maxParticipants,
            "settings" to (options.settings ?: emptyMap<String, Any>()),
            "metadata" to (options.metadata ?: emptyMap<String, Any>())
        )
        val response = httpClient.post("calls/create", body)
        return parseCall(response.getAsJsonObject("call"))
    }

    /**
     * List calls
     */
    suspend fun listCalls(
        status: String? = null,
        type: String? = null,
        limit: Int = 50
    ): List<Call> {
        val params = mutableMapOf<String, String>()
        status?.let { params["status"] = it }
        type?.let { params["type"] = it }
        params["limit"] = limit.toString()

        val response = httpClient.get("calls", params)
        val callsArray = response.getAsJsonArray("calls")
        return callsArray?.map { parseCall(it.asJsonObject) } ?: emptyList()
    }

    /**
     * Get call details
     */
    suspend fun getCall(callId: String): Call {
        val response = httpClient.get("calls/$callId")
        return parseCall(response.getAsJsonObject("call"))
    }

    /**
     * Update call settings
     */
    suspend fun updateCall(callId: String, updates: Map<String, Any>): Call {
        val response = httpClient.put("calls/$callId", updates)
        return parseCall(response.getAsJsonObject("call"))
    }

    /**
     * End a call
     */
    suspend fun endCall(callId: String): Call {
        val response = httpClient.delete("calls/$callId")
        return parseCall(response.getAsJsonObject("call"))
    }

    /**
     * Generate a join token
     */
    suspend fun generateToken(callId: String, options: GenerateTokenOptions): CallToken {
        val body = mutableMapOf<String, Any?>(
            "userId" to options.userId,
            "displayName" to options.displayName,
            "role" to roleToString(options.role),
            "permissions" to options.permissions,
            "expiresIn" to options.expiresIn
        ).filterValues { it != null }

        val response = httpClient.post("calls/$callId/tokens", body)
        return CallToken(
            token = response.get("token")?.asString ?: "",
            tokenId = response.get("tokenId")?.asString ?: "",
            role = parseRole(response.get("role")?.asString),
            permissions = response.getAsJsonArray("permissions")?.map { it.asString } ?: emptyList(),
            expiresAt = response.get("expiresAt")?.asString ?: ""
        )
    }

    /**
     * Validate a call token
     */
    suspend fun validateToken(callId: String, token: String): JsonObject {
        return httpClient.post("calls/$callId/tokens/validate", mapOf("token" to token))
    }

    /**
     * Join a call using Socket.IO
     */
    suspend fun joinCall(callId: String, token: String): Map<String, Any> =
        suspendCancellableCoroutine { continuation ->
            if (_connected) {
                continuation.resumeWithException(Exception("Already connected to a call"))
                return@suspendCancellableCoroutine
            }

            try {
                val options = IO.Options().apply {
                    transports = arrayOf("websocket", "polling")
                    auth = mapOf("token" to token)
                }

                socket = IO.socket("${config.baseUrl}/calls", options)

                socket?.on(Socket.EVENT_CONNECT) {
                    // Connected to call signaling server
                }

                socket?.on("call:joined") { args ->
                    val data = args[0] as JSONObject
                    _connected = true
                    _currentCallId = callId
                    _participantId = data.optString("participantId")
                    setupEventHandlers()
                    continuation.resume(jsonObjectToMap(data))
                }

                socket?.on("call:error") { args ->
                    val data = args[0] as JSONObject
                    continuation.resumeWithException(
                        Exception(data.optString("message", "Connection error"))
                    )
                }

                socket?.on(Socket.EVENT_DISCONNECT) {
                    _connected = false
                    _currentCallId = null
                    _participantId = null
                }

                socket?.connect()

            } catch (e: Exception) {
                continuation.resumeWithException(e)
            }

            continuation.invokeOnCancellation {
                socket?.disconnect()
            }
        }

    private fun setupEventHandlers() {
        socket?.on("call:participant:joined") { args ->
            val data = args[0] as JSONObject
            val participant = parseParticipantFromJson(data)
            _participantJoined.tryEmit(participant)
        }

        socket?.on("call:participant:left") { args ->
            val data = args[0] as JSONObject
            val participant = parseParticipantFromJson(data)
            _participantLeft.tryEmit(participant)
        }

        socket?.on("call:producer:new") { args ->
            val data = args[0] as JSONObject
            _newProducer.tryEmit(jsonObjectToMap(data))
        }

        socket?.on("call:media-state:changed") { args ->
            val data = args[0] as JSONObject
            _mediaStateChanged.tryEmit(jsonObjectToMap(data))
        }

        socket?.on("call:chat:message") { args ->
            val data = args[0] as JSONObject
            _chatMessage.tryEmit(jsonObjectToMap(data))
        }

        socket?.on("call:ended") { args ->
            val data = args[0] as JSONObject
            _callEnded.tryEmit(jsonObjectToMap(data))
            leaveCall()
        }

        socket?.on("call:transcription:segment") { args ->
            val data = args[0] as JSONObject
            _transcriptionSegment.tryEmit(jsonObjectToMap(data))
        }
    }

    /**
     * Leave the current call
     */
    fun leaveCall() {
        socket?.emit("call:leave")
        socket?.disconnect()
        socket = null
        _connected = false
        _currentCallId = null
        _participantId = null
    }

    /**
     * Update media state
     */
    fun updateMediaState(video: Boolean? = null, audio: Boolean? = null) {
        val data = JSONObject().apply {
            video?.let { put("video", it) }
            audio?.let { put("audio", it) }
        }
        socket?.emit("call:media-state", data)
    }

    /**
     * Send chat message
     */
    fun sendChatMessage(message: String) {
        val data = JSONObject().apply {
            put("message", message)
        }
        socket?.emit("call:chat:message", data)
    }

    /**
     * Send reaction
     */
    fun sendReaction(emoji: String) {
        val data = JSONObject().apply {
            put("emoji", emoji)
        }
        socket?.emit("call:reaction", data)
    }

    /**
     * Raise/lower hand
     */
    fun raiseHand(raised: Boolean) {
        val data = JSONObject().apply {
            put("raised", raised)
        }
        socket?.emit("call:raise-hand", data)
    }

    /**
     * Get participants in a call
     */
    suspend fun getParticipants(callId: String): List<Participant> {
        val response = httpClient.get("calls/$callId/participants")
        val participantsArray = response.getAsJsonArray("participants")
        return participantsArray?.map { parseParticipant(it.asJsonObject) } ?: emptyList()
    }

    /**
     * Kick a participant
     */
    suspend fun kickParticipant(callId: String, participantId: String) {
        httpClient.post("calls/$callId/participants/$participantId/kick", emptyMap<String, Any>())
    }

    /**
     * Mute a participant
     */
    suspend fun muteParticipant(callId: String, participantId: String, mediaType: String = "audio") {
        httpClient.post("calls/$callId/participants/$participantId/mute", mapOf("mediaType" to mediaType))
    }

    /**
     * Start recording
     */
    suspend fun startRecording(callId: String, options: StartRecordingOptions = StartRecordingOptions()): JsonObject {
        return httpClient.post("calls/$callId/recordings/start", mapOf(
            "type" to options.type,
            "format" to options.format
        ))
    }

    /**
     * Stop recording
     */
    suspend fun stopRecording(callId: String): JsonObject {
        return httpClient.post("calls/$callId/recordings/stop", emptyMap<String, Any>())
    }

    /**
     * List recordings
     */
    suspend fun listRecordings(callId: String): List<CallRecording> {
        val response = httpClient.get("calls/$callId/recordings")
        val recordingsArray = response.getAsJsonArray("recordings")
        return recordingsArray?.map { json ->
            val obj = json.asJsonObject
            CallRecording(
                recordingId = obj.get("recordingId")?.asString ?: "",
                callId = obj.get("callId")?.asString ?: "",
                type = obj.get("type")?.asString ?: "",
                format = obj.get("format")?.asString ?: "",
                status = obj.get("status")?.asString ?: "",
                duration = obj.get("duration")?.asInt,
                size = obj.get("size")?.asLong,
                url = obj.get("url")?.asString,
                startedAt = obj.get("startedAt")?.asString ?: "",
                stoppedAt = obj.get("stoppedAt")?.asString
            )
        } ?: emptyList()
    }

    /**
     * Start transcription
     */
    suspend fun startTranscription(callId: String): JsonObject {
        return httpClient.post("calls/$callId/transcription/start", emptyMap<String, Any>())
    }

    /**
     * Stop transcription
     */
    suspend fun stopTranscription(callId: String): JsonObject {
        return httpClient.post("calls/$callId/transcription/stop", emptyMap<String, Any>())
    }

    /**
     * Get transcription
     */
    suspend fun getTranscription(callId: String): CallTranscription? {
        val response = httpClient.get("calls/$callId/transcription")
        val transcription = response.getAsJsonObject("transcription") ?: return null

        val segments = transcription.getAsJsonArray("segments")?.map { seg ->
            val s = seg.asJsonObject
            TranscriptionSegment(
                participantId = s.get("participantId")?.asString ?: "",
                displayName = s.get("displayName")?.asString ?: "",
                text = s.get("text")?.asString ?: "",
                timestamp = s.get("timestamp")?.asString ?: "",
                confidence = s.get("confidence")?.asFloat
            )
        } ?: emptyList()

        val analysis = transcription.getAsJsonObject("analysis")?.let { a ->
            TranscriptionAnalysis(
                summary = a.get("summary")?.asString,
                topics = a.getAsJsonArray("topics")?.map { it.asString },
                sentiment = a.get("sentiment")?.asString,
                actionItems = a.getAsJsonArray("actionItems")?.map { it.asString }
            )
        }

        return CallTranscription(
            transcriptionId = transcription.get("transcriptionId")?.asString ?: "",
            callId = transcription.get("callId")?.asString ?: "",
            status = transcription.get("status")?.asString ?: "",
            segments = segments,
            fullText = transcription.get("fullText")?.asString,
            analysis = analysis
        )
    }

    /**
     * Get TURN servers
     */
    suspend fun getTurnServers(): List<TurnServer> {
        val response = httpClient.get("calls/turn-servers")
        val serversArray = response.getAsJsonArray("servers")
        return serversArray?.map { json ->
            val obj = json.asJsonObject
            TurnServer(
                urls = obj.getAsJsonArray("urls")?.map { it.asString } ?: emptyList(),
                username = obj.get("username")?.asString,
                credential = obj.get("credential")?.asString
            )
        } ?: emptyList()
    }

    // Helper functions
    private fun parseCall(json: JsonObject?): Call {
        if (json == null) throw Exception("Invalid call data")
        return Call(
            callId = json.get("callId")?.asString ?: "",
            roomId = json.get("roomId")?.asString ?: "",
            projectId = json.get("projectId")?.asString ?: "",
            type = parseCallType(json.get("type")?.asString),
            mode = parseCallMode(json.get("mode")?.asString),
            status = json.get("status")?.asString ?: "",
            hostId = json.get("hostId")?.asString,
            hostDisplayName = json.get("hostDisplayName")?.asString,
            maxParticipants = json.get("maxParticipants")?.asInt ?: 50,
            settings = json.getAsJsonObject("settings")?.entrySet()?.associate {
                it.key to (it.value?.asString ?: "")
            } ?: emptyMap(),
            startedAt = json.get("startedAt")?.asString,
            endedAt = json.get("endedAt")?.asString,
            duration = json.get("duration")?.asInt ?: 0,
            createdAt = json.get("createdAt")?.asString ?: ""
        )
    }

    private fun parseParticipant(json: JsonObject): Participant {
        return Participant(
            participantId = json.get("participantId")?.asString ?: "",
            userId = json.get("userId")?.asString,
            displayName = json.get("displayName")?.asString ?: "",
            role = parseRole(json.get("role")?.asString),
            status = json.get("status")?.asString ?: "",
            mediaState = json.getAsJsonObject("mediaState")?.entrySet()?.associate {
                it.key to (it.value?.asBoolean ?: false)
            } ?: emptyMap(),
            joinedAt = json.get("joinedAt")?.asString ?: "",
            leftAt = json.get("leftAt")?.asString,
            duration = json.get("duration")?.asInt ?: 0
        )
    }

    private fun parseParticipantFromJson(json: JSONObject): Participant {
        return Participant(
            participantId = json.optString("participantId", ""),
            userId = json.optString("userId", null),
            displayName = json.optString("displayName", ""),
            role = parseRole(json.optString("role", null)),
            status = json.optString("status", ""),
            mediaState = json.optJSONObject("mediaState")?.let { ms ->
                ms.keys().asSequence().associate { key ->
                    key to ms.optBoolean(key, false)
                }
            } ?: emptyMap(),
            joinedAt = json.optString("joinedAt", ""),
            leftAt = json.optString("leftAt", null),
            duration = json.optInt("duration", 0)
        )
    }

    private fun parseCallType(type: String?): CallType {
        return when (type?.lowercase()) {
            "voice" -> CallType.VOICE
            "livestream" -> CallType.LIVESTREAM
            else -> CallType.VIDEO
        }
    }

    private fun parseCallMode(mode: String?): CallMode {
        return when (mode?.lowercase()) {
            "p2p" -> CallMode.P2P
            "broadcast" -> CallMode.BROADCAST
            else -> CallMode.GROUP
        }
    }

    private fun parseRole(role: String?): ParticipantRole {
        return when (role?.lowercase()) {
            "host" -> ParticipantRole.HOST
            "co-host" -> ParticipantRole.CO_HOST
            "viewer" -> ParticipantRole.VIEWER
            else -> ParticipantRole.PARTICIPANT
        }
    }

    private fun roleToString(role: ParticipantRole): String {
        return when (role) {
            ParticipantRole.HOST -> "host"
            ParticipantRole.CO_HOST -> "co-host"
            ParticipantRole.VIEWER -> "viewer"
            ParticipantRole.PARTICIPANT -> "participant"
        }
    }

    private fun jsonObjectToMap(json: JSONObject): Map<String, Any> {
        val map = mutableMapOf<String, Any>()
        json.keys().forEach { key ->
            val value = json.get(key)
            map[key] = when (value) {
                is JSONObject -> jsonObjectToMap(value)
                else -> value
            }
        }
        return map
    }
}
