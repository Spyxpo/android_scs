package com.spyxpo.scs.services

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.spyxpo.scs.models.CloudFunction
import com.spyxpo.scs.models.FunctionLog
import com.spyxpo.scs.models.FunctionResult
import com.spyxpo.scs.utils.ScsHttpClient

/**
 * Service for cloud functions.
 *
 * Example usage:
 * ```kotlin
 * // Call a function
 * val result = scs.functions.call("processData", mapOf(
 *     "value" to 100,
 *     "type" to "analytics"
 * ))
 *
 * if (result.success) {
 *     val data = result.getDataAsMap()
 *     println("Result: ${data?.get("result")}")
 * }
 *
 * // Get a callable reference
 * val processData = scs.functions.httpsCallable("processData")
 * val result = processData.call(mapOf("value" to 100))
 *
 * // List functions
 * val functions = scs.functions.list()
 * ```
 */
class FunctionsService(
    private val httpClient: ScsHttpClient,
    private val projectId: String
) {
    private val gson = Gson()

    /**
     * Call a cloud function
     *
     * @param functionName Name of the function to call
     * @param data Optional data to pass to the function
     * @return Function execution result
     */
    suspend fun call(functionName: String, data: Map<String, Any?>? = null): FunctionResult {
        val body = data ?: emptyMap<String, Any?>()
        val response = httpClient.post("/functions/invoke/$projectId/$functionName", body)

        val success = response.get("success")?.asBoolean ?: true
        val resultData = response.get("data")?.let { gson.fromJson(it, Any::class.java) }
        val error = response.get("error")?.asString
        val executionTime = response.get("executionTime")?.asLong

        return FunctionResult(
            success = success,
            data = resultData,
            error = error,
            executionTime = executionTime
        )
    }

    /**
     * Get a callable function reference
     *
     * @param functionName Name of the function
     * @return HttpsCallable that can be used to call the function
     */
    fun httpsCallable(functionName: String): HttpsCallable {
        return HttpsCallable(this, functionName)
    }

    /**
     * List all cloud functions
     *
     * @return List of cloud functions
     */
    suspend fun list(): List<CloudFunction> {
        val response = httpClient.get("/functions/")
        val functions = response.getAsJsonArray("functions") ?: JsonArray()

        return functions.map { element ->
            gson.fromJson(element, CloudFunction::class.java)
        }
    }

    /**
     * Get a specific function's details
     *
     * @param functionId Function ID
     * @return Function details
     */
    suspend fun get(functionId: String): CloudFunction {
        val response = httpClient.get("/functions/$functionId")
        return gson.fromJson(response.getAsJsonObject("function"), CloudFunction::class.java)
    }

    /**
     * Get function logs
     *
     * @param functionId Function ID
     * @param limit Maximum number of logs to return
     * @return List of log entries
     */
    suspend fun getLogs(functionId: String, limit: Int? = null): List<FunctionLog> {
        val params = limit?.let { mapOf("limit" to it.toString()) }
        val response = httpClient.get("/functions/$functionId/logs", params)
        val logs = response.getAsJsonArray("logs") ?: JsonArray()

        return logs.map { element ->
            gson.fromJson(element, FunctionLog::class.java)
        }
    }

    // ==================== Admin Methods ====================

    /**
     * Create a new cloud function (admin)
     *
     * @param name Function name
     * @param code Function code
     * @param runtime Runtime environment
     * @param handler Handler function name
     * @param description Optional description
     * @param timeout Optional timeout in seconds
     * @param memory Optional memory limit in MB
     * @return The created function
     */
    suspend fun create(
        name: String,
        code: String,
        runtime: String = "nodejs18",
        handler: String = "handler",
        description: String? = null,
        timeout: Int? = null,
        memory: Int? = null
    ): CloudFunction {
        val body = mutableMapOf<String, Any?>(
            "name" to name,
            "code" to code,
            "runtime" to runtime,
            "handler" to handler
        )
        description?.let { body["description"] = it }
        timeout?.let { body["timeout"] = it }
        memory?.let { body["memory"] = it }

        val response = httpClient.post("/functions/", body)
        return gson.fromJson(response.getAsJsonObject("function"), CloudFunction::class.java)
    }

    /**
     * Update a cloud function (admin)
     *
     * @param functionId Function ID
     * @param code New function code
     * @param description New description
     * @param timeout New timeout
     * @param memory New memory limit
     * @return The updated function
     */
    suspend fun update(
        functionId: String,
        code: String? = null,
        description: String? = null,
        timeout: Int? = null,
        memory: Int? = null
    ): CloudFunction {
        val body = mutableMapOf<String, Any?>()
        code?.let { body["code"] = it }
        description?.let { body["description"] = it }
        timeout?.let { body["timeout"] = it }
        memory?.let { body["memory"] = it }

        val response = httpClient.put("/functions/$functionId", body)
        return gson.fromJson(response.getAsJsonObject("function"), CloudFunction::class.java)
    }

    /**
     * Delete a cloud function (admin)
     *
     * @param functionId Function ID
     */
    suspend fun delete(functionId: String) {
        httpClient.delete("/functions/$functionId")
    }

    /**
     * Test a cloud function (admin)
     *
     * @param functionId Function ID
     * @param testData Optional test data
     * @return Test result
     */
    suspend fun test(functionId: String, testData: Map<String, Any?>? = null): FunctionResult {
        val response = httpClient.post("/functions/$functionId/test", testData ?: emptyMap<String, Any?>())

        val success = response.get("success")?.asBoolean ?: true
        val resultData = response.get("data")?.let { gson.fromJson(it, Any::class.java) }
        val error = response.get("error")?.asString
        val executionTime = response.get("executionTime")?.asLong

        return FunctionResult(
            success = success,
            data = resultData,
            error = error,
            executionTime = executionTime
        )
    }
}

/**
 * Callable wrapper for a cloud function
 */
class HttpsCallable(
    private val service: FunctionsService,
    private val functionName: String
) {
    /**
     * Call the function
     *
     * @param data Optional data to pass to the function
     * @return Function execution result
     */
    suspend fun call(data: Map<String, Any?>? = null): FunctionResult {
        return service.call(functionName, data)
    }
}
