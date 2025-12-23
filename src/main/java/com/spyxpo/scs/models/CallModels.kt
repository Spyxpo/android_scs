package com.spyxpo.scs.models

import com.google.gson.annotations.SerializedName
import java.util.Date

/**
 * Call types
 */
enum class CallType {
    @SerializedName("voice") VOICE,
    @SerializedName("video") VIDEO,
    @SerializedName("livestream") LIVESTREAM
}

/**
 * Call modes
 */
enum class CallMode {
    @SerializedName("p2p") P2P,
    @SerializedName("group") GROUP,
    @SerializedName("broadcast") BROADCAST
}

/**
 * Participant roles
 */
enum class ParticipantRole {
    @SerializedName("host") HOST,
    @SerializedName("co-host") CO_HOST,
    @SerializedName("participant") PARTICIPANT,
    @SerializedName("viewer") VIEWER
}

/**
 * Call data model
 */
data class Call(
    val callId: String,
    val roomId: String,
    val projectId: String,
    val type: CallType,
    val mode: CallMode,
    val status: String,
    val hostId: String?,
    val hostDisplayName: String?,
    val maxParticipants: Int,
    val settings: Map<String, Any>,
    val startedAt: String?,
    val endedAt: String?,
    val duration: Int,
    val createdAt: String
)

/**
 * Participant data model
 */
data class Participant(
    val participantId: String,
    val userId: String?,
    val displayName: String,
    val role: ParticipantRole,
    val status: String,
    val mediaState: Map<String, Boolean>,
    val joinedAt: String,
    val leftAt: String?,
    val duration: Int
)

/**
 * Call token data
 */
data class CallToken(
    val token: String,
    val tokenId: String,
    val role: ParticipantRole,
    val permissions: List<String>,
    val expiresAt: String
)

/**
 * Call service statistics
 */
data class CallStats(
    val totalCalls: Int,
    val activeCalls: Int,
    val totalMinutes: Int,
    val recordings: Int
)

/**
 * Recording data
 */
data class CallRecording(
    val recordingId: String,
    val callId: String,
    val type: String,
    val format: String,
    val status: String,
    val duration: Int?,
    val size: Long?,
    val url: String?,
    val startedAt: String,
    val stoppedAt: String?
)

/**
 * Transcription data
 */
data class CallTranscription(
    val transcriptionId: String,
    val callId: String,
    val status: String,
    val segments: List<TranscriptionSegment>,
    val fullText: String?,
    val analysis: TranscriptionAnalysis?
)

/**
 * Transcription segment
 */
data class TranscriptionSegment(
    val participantId: String,
    val displayName: String,
    val text: String,
    val timestamp: String,
    val confidence: Float?
)

/**
 * AI analysis of transcription
 */
data class TranscriptionAnalysis(
    val summary: String?,
    val topics: List<String>?,
    val sentiment: String?,
    val actionItems: List<String>?
)

/**
 * Options for creating a call
 */
data class CreateCallOptions(
    val type: CallType = CallType.VIDEO,
    val mode: CallMode = CallMode.GROUP,
    val displayName: String = "Host",
    val maxParticipants: Int = 50,
    val settings: Map<String, Any>? = null,
    val metadata: Map<String, Any>? = null
)

/**
 * Options for generating a call token
 */
data class GenerateTokenOptions(
    val userId: String? = null,
    val displayName: String,
    val role: ParticipantRole = ParticipantRole.PARTICIPANT,
    val permissions: List<String>? = null,
    val expiresIn: Int? = null
)

/**
 * Options for starting recording
 */
data class StartRecordingOptions(
    val type: String = "composite",
    val format: String = "mp4"
)

/**
 * TURN server configuration
 */
data class TurnServer(
    val urls: List<String>,
    val username: String?,
    val credential: String?
)
