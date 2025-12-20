package com.spyxpo.scs

/**
 * Exception class for SCS SDK errors.
 *
 * @property message The error message
 * @property code The error code (e.g., "auth/invalid-credentials")
 * @property statusCode The HTTP status code
 * @property details Additional error details
 */
class ScsException(
    override val message: String,
    val code: String? = null,
    val statusCode: Int? = null,
    val details: Map<String, Any>? = null
) : Exception(message) {

    companion object {
        // Authentication errors
        const val AUTH_INVALID_CREDENTIALS = "auth/invalid-credentials"
        const val AUTH_USER_NOT_FOUND = "auth/user-not-found"
        const val AUTH_EMAIL_ALREADY_IN_USE = "auth/email-already-in-use"
        const val AUTH_WEAK_PASSWORD = "auth/weak-password"
        const val AUTH_UNAUTHORIZED = "auth/unauthorized"
        const val AUTH_TOKEN_EXPIRED = "auth/token-expired"

        // Database errors
        const val DATABASE_NOT_FOUND = "database/not-found"
        const val DATABASE_PERMISSION_DENIED = "database/permission-denied"
        const val DATABASE_INVALID_QUERY = "database/invalid-query"

        // Storage errors
        const val STORAGE_FILE_NOT_FOUND = "storage/file-not-found"
        const val STORAGE_PERMISSION_DENIED = "storage/permission-denied"
        const val STORAGE_UPLOAD_FAILED = "storage/upload-failed"

        // Network errors
        const val NETWORK_ERROR = "network/error"
        const val NETWORK_TIMEOUT = "network/timeout"

        // General errors
        const val UNKNOWN_ERROR = "unknown/error"
        const val INVALID_ARGUMENT = "invalid/argument"

        /**
         * Create an exception from an API error response
         */
        fun fromApiError(
            statusCode: Int,
            errorBody: String?,
            defaultMessage: String = "An error occurred"
        ): ScsException {
            return try {
                val gson = com.google.gson.Gson()
                val errorResponse = gson.fromJson(errorBody, ApiErrorResponse::class.java)
                ScsException(
                    message = errorResponse?.error ?: errorResponse?.message ?: defaultMessage,
                    code = mapStatusCodeToErrorCode(statusCode),
                    statusCode = statusCode
                )
            } catch (e: Exception) {
                ScsException(
                    message = errorBody ?: defaultMessage,
                    code = mapStatusCodeToErrorCode(statusCode),
                    statusCode = statusCode
                )
            }
        }

        private fun mapStatusCodeToErrorCode(statusCode: Int): String {
            return when (statusCode) {
                400 -> INVALID_ARGUMENT
                401 -> AUTH_UNAUTHORIZED
                403 -> DATABASE_PERMISSION_DENIED
                404 -> DATABASE_NOT_FOUND
                409 -> AUTH_EMAIL_ALREADY_IN_USE
                else -> UNKNOWN_ERROR
            }
        }
    }

    override fun toString(): String {
        return "ScsException(code=$code, statusCode=$statusCode, message=$message)"
    }
}

/**
 * Internal class for parsing API error responses
 */
internal data class ApiErrorResponse(
    val error: String?,
    val message: String?,
    val details: Map<String, Any>?
)
