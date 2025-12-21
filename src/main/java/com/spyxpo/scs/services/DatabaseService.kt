package com.spyxpo.scs.services

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.spyxpo.scs.models.QueryFilter
import com.spyxpo.scs.models.QueryOrderBy
import com.spyxpo.scs.models.ScsDocument
import com.spyxpo.scs.utils.ScsHttpClient

/**
 * Service for database operations.
 *
 * SCS supports two database backends (configured server-side via DATABASE_TYPE):
 * - **eaZI Database** (DATABASE_TYPE=eazi): File-based NoSQL, ideal for development
 * - **RelaDB** (DATABASE_TYPE=mongodb): Production-grade NoSQL with advanced features
 *
 * The SDK API remains the same regardless of backend - switching databases requires
 * no client-side code changes.
 *
 * Example usage:
 * ```kotlin
 * // Get a collection reference
 * val usersCollection = scs.database.collection("users")
 *
 * // Add a document
 * val doc = usersCollection.add(mapOf("name" to "John", "age" to 30))
 *
 * // Query documents
 * val docs = usersCollection
 *     .where("age", ">", 18)
 *     .orderBy("name", "asc")
 *     .limit(10)
 *     .get()
 *
 * // Get a specific document
 * val user = usersCollection.doc("userId").get()
 *
 * // Update a document
 * usersCollection.doc("userId").update(mapOf("name" to "Jane"))
 *
 * // Subcollections
 * val posts = usersCollection.doc("userId").collection("posts").get()
 * ```
 */
class DatabaseService(private val httpClient: ScsHttpClient) {

    private val gson = Gson()

    /**
     * Get a reference to a collection
     *
     * @param name Collection name
     * @return CollectionReference for the specified collection
     */
    fun collection(name: String): CollectionReference {
        return CollectionReference(httpClient, gson, name)
    }

    /**
     * List all root collections
     *
     * @return List of collection names
     */
    suspend fun listCollections(): List<String> {
        val response = httpClient.get("/database/collections")
        val collections = response.getAsJsonArray("collections")
        return collections?.map { it.asString } ?: emptyList()
    }

    /**
     * Create a new collection
     *
     * @param name Collection name
     */
    suspend fun createCollection(name: String) {
        val body = mapOf("name" to name)
        httpClient.post("/database/collections", body)
    }

    /**
     * Delete a collection
     *
     * @param name Collection name
     */
    suspend fun deleteCollection(name: String) {
        httpClient.delete("/database/collections/$name")
    }
}

/**
 * Reference to a collection in the database.
 */
class CollectionReference(
    private val httpClient: ScsHttpClient,
    private val gson: Gson,
    private val path: String
) {
    private val filters = mutableListOf<QueryFilter>()
    private var orderByClause: QueryOrderBy? = null
    private var limitValue: Int? = null
    private var skipValue: Int? = null

    /**
     * Get a reference to a document in this collection
     *
     * @param id Document ID
     * @return DocumentReference for the specified document
     */
    fun doc(id: String): DocumentReference {
        return DocumentReference(httpClient, gson, path, id)
    }

    /**
     * Add a filter condition to the query
     *
     * @param field Field name to filter on
     * @param operator Comparison operator (==, !=, >, >=, <, <=, in, contains)
     * @param value Value to compare against
     * @return This CollectionReference for chaining
     */
    fun where(field: String, operator: String, value: Any?): CollectionReference {
        filters.add(QueryFilter(field, operator, value))
        return this
    }

    /**
     * Add sorting to the query
     *
     * @param field Field name to sort by
     * @param direction Sort direction ("asc" or "desc")
     * @return This CollectionReference for chaining
     */
    fun orderBy(field: String, direction: String = "asc"): CollectionReference {
        orderByClause = QueryOrderBy(field, direction)
        return this
    }

    /**
     * Limit the number of results
     *
     * @param count Maximum number of documents to return
     * @return This CollectionReference for chaining
     */
    fun limit(count: Int): CollectionReference {
        limitValue = count
        return this
    }

    /**
     * Skip a number of results
     *
     * @param count Number of documents to skip
     * @return This CollectionReference for chaining
     */
    fun skip(count: Int): CollectionReference {
        skipValue = count
        return this
    }

    /**
     * Execute the query and get documents
     *
     * @return List of documents matching the query
     */
    suspend fun get(): List<ScsDocument> {
        return if (filters.isNotEmpty() || orderByClause != null) {
            // Use query endpoint
            val query = buildQueryBody()
            val response = httpClient.post("/database/collections/$path/query", query)
            parseDocuments(response)
        } else {
            // Use simple list endpoint
            val params = buildQueryParams()
            val response = httpClient.get("/database/collections/$path/documents", params)
            parseDocuments(response)
        }
    }

    /**
     * Add a new document to the collection
     *
     * @param data Document data
     * @return The created document
     */
    suspend fun add(data: Map<String, Any?>): ScsDocument {
        val response = httpClient.post("/database/collections/$path/documents", data)
        return parseDocument(response)
    }

    private fun buildQueryBody(): Map<String, Any?> {
        val query = mutableMapOf<String, Any?>()

        if (filters.isNotEmpty()) {
            query["filters"] = filters.map { it.toMap() }
        }

        orderByClause?.let {
            query["orderBy"] = it.toMap()
        }

        limitValue?.let { query["limit"] = it }
        skipValue?.let { query["skip"] = it }

        return query
    }

    private fun buildQueryParams(): Map<String, String> {
        val params = mutableMapOf<String, String>()
        limitValue?.let { params["limit"] = it.toString() }
        skipValue?.let { params["skip"] = it.toString() }
        return params
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseDocuments(response: JsonObject): List<ScsDocument> {
        val documents = response.getAsJsonArray("documents")
            ?: response.getAsJsonArray("data")
            ?: JsonArray()

        return documents.map { element ->
            val obj = element.asJsonObject
            val map = gson.fromJson<Map<String, Any?>>(obj, Map::class.java)
            ScsDocument.fromMap(map)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseDocument(response: JsonObject): ScsDocument {
        val docObj = response.getAsJsonObject("document")
            ?: response.getAsJsonObject("data")
            ?: response

        val map = gson.fromJson<Map<String, Any?>>(docObj, Map::class.java)
        return ScsDocument.fromMap(map)
    }
}

/**
 * Reference to a document in the database.
 */
class DocumentReference(
    private val httpClient: ScsHttpClient,
    private val gson: Gson,
    private val collectionPath: String,
    private val documentId: String
) {
    /**
     * The full path to this document
     */
    val path: String = "$collectionPath/$documentId"

    /**
     * The document ID
     */
    val id: String = documentId

    /**
     * Get a reference to a subcollection of this document
     *
     * @param name Subcollection name
     * @return CollectionReference for the subcollection
     */
    fun collection(name: String): CollectionReference {
        return CollectionReference(httpClient, gson, "$collectionPath/$documentId/$name")
    }

    /**
     * List subcollections of this document
     *
     * @return List of subcollection names
     */
    suspend fun listCollections(): List<String> {
        val response = httpClient.get("/database/collections/$collectionPath/documents/$documentId/collections")
        val collections = response.getAsJsonArray("collections")
        return collections?.map { it.asString } ?: emptyList()
    }

    /**
     * Get the document data
     *
     * @return The document, or null if it doesn't exist
     */
    suspend fun get(): ScsDocument? {
        return try {
            val response = httpClient.get("/database/collections/$collectionPath/documents/$documentId")
            parseDocument(response)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Set the document data (creates or overwrites)
     *
     * @param data Document data
     * @return The updated document
     */
    suspend fun set(data: Map<String, Any?>): ScsDocument {
        val response = httpClient.put("/database/collections/$collectionPath/documents/$documentId", data)
        return parseDocument(response)
    }

    /**
     * Update the document data (merges with existing)
     *
     * @param data Data to update
     * @param merge Whether to merge with existing data (default: true)
     * @return The updated document
     */
    suspend fun update(data: Map<String, Any?>, merge: Boolean = true): ScsDocument {
        val body = if (merge) {
            mapOf("data" to data, "merge" to true)
        } else {
            data
        }
        val response = httpClient.put("/database/collections/$collectionPath/documents/$documentId", body)
        return parseDocument(response)
    }

    /**
     * Delete the document
     */
    suspend fun delete() {
        httpClient.delete("/database/collections/$collectionPath/documents/$documentId")
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseDocument(response: JsonObject): ScsDocument {
        val docObj = response.getAsJsonObject("document")
            ?: response.getAsJsonObject("data")
            ?: response

        val map = gson.fromJson<Map<String, Any?>>(docObj, Map::class.java)
        return ScsDocument.fromMap(map)
    }
}
