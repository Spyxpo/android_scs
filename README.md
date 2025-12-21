# SCS Android SDK

The official Android SDK for Spyxpo Cloud Services (SCS) - a comprehensive Backend-as-a-Service platform.

## Features

- **Authentication** - User registration, login, profile management, and role-based access
- **Database** - NoSQL document database with collections, subcollections, and advanced queries
- **Storage** - File upload, download, and management
- **Realtime Database** - Real-time data synchronization with WebSocket support
- **Cloud Messaging** - Push notifications with topics and device token management
- **Remote Configuration** - Dynamic app configuration without deployments
- **Cloud Functions** - Serverless function invocation
- **Machine Learning** - Text recognition (OCR) and image labeling
- **AI Services** - Chat, text completion, and image generation

## Requirements

- Android SDK 21+ (Android 5.0 Lollipop)
- Kotlin 1.9+
- Java 17+

## Installation

### Gradle (Kotlin DSL)

Add the JitPack repository to your project's `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

Add the dependency to your app's `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.github.user:scs-android:1.0.0")
}
```

### Gradle (Groovy)

```groovy
// settings.gradle
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}

// app/build.gradle
dependencies {
    implementation 'com.github.user:scs-android:1.0.0'
}
```

## Quick Start

### Initialize the SDK

```kotlin
import com.spyxpo.scs.Scs
import com.spyxpo.scs.ScsConfig

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize SCS
        Scs.initialize(
            context = this,
            config = ScsConfig(
                apiKey = "your-api-key",
                projectId = "your-project-id",
                baseUrl = "https://your-scs-instance.com"
            )
        )
    }
}
```

### Get the SDK Instance

```kotlin
val scs = Scs.getInstance()
```

## Authentication

### Register a New User

```kotlin
// Using coroutines
lifecycleScope.launch {
    try {
        val user = scs.auth.register(
            email = "user@example.com",
            password = "securePassword123",
            displayName = "John Doe",
            customData = mapOf("role" to "member")
        )
        Log.d("SCS", "Registered: ${user.email}")
    } catch (e: ScsException) {
        Log.e("SCS", "Registration failed: ${e.message}")
    }
}
```

### Login

```kotlin
lifecycleScope.launch {
    try {
        val user = scs.auth.login("user@example.com", "password")
        Log.d("SCS", "Logged in: ${user.displayName}")
    } catch (e: ScsException) {
        Log.e("SCS", "Login failed: ${e.message}")
    }
}
```

### Observe Auth State

```kotlin
// Using Flow
lifecycleScope.launch {
    scs.auth.currentUserFlow.collect { user ->
        if (user != null) {
            Log.d("SCS", "User logged in: ${user.email}")
        } else {
            Log.d("SCS", "User logged out")
        }
    }
}

// Using LiveData
scs.auth.currentUserLiveData.observe(this) { user ->
    // Handle user state changes
}
```

### Logout

```kotlin
scs.auth.logout()
```

## Database

NoSQL document database with collections and subcollections. SCS supports two database types:

- **eaZI**: Document-based NoSQL with Firestore-like collections, documents, and subcollections (default)
- **MongoDB**: Relational-style database with tables, columns, and rows

Configure the backend database type via `DATABASE_TYPE` environment variable (`eazi` or `mongodb`). See the main SCS documentation for details.

### Add a Document

```kotlin
lifecycleScope.launch {
    val doc = scs.database.collection("users").add(
        mapOf(
            "name" to "John Doe",
            "email" to "john@example.com",
            "age" to 30
        )
    )
    Log.d("SCS", "Document ID: ${doc.id}")
}
```

### Get Documents

```kotlin
lifecycleScope.launch {
    val users = scs.database.collection("users").get()
    users.forEach { doc ->
        Log.d("SCS", "User: ${doc.getString("name")}")
    }
}
```

### Query with Filters

```kotlin
lifecycleScope.launch {
    val adults = scs.database.collection("users")
        .where("age", ">=", 18)
        .orderBy("name", "asc")
        .limit(10)
        .get()
}
```

### Get a Specific Document

```kotlin
lifecycleScope.launch {
    val user = scs.database.collection("users").doc("userId").get()
    user?.let {
        Log.d("SCS", "Name: ${it.getString("name")}")
    }
}
```

### Update a Document

```kotlin
lifecycleScope.launch {
    scs.database.collection("users").doc("userId").update(
        mapOf("name" to "Jane Doe")
    )
}
```

### Delete a Document

```kotlin
lifecycleScope.launch {
    scs.database.collection("users").doc("userId").delete()
}
```

### Subcollections

```kotlin
lifecycleScope.launch {
    // Access a subcollection
    val posts = scs.database
        .collection("users")
        .doc("userId")
        .collection("posts")
        .get()

    // Add to subcollection
    scs.database
        .collection("users")
        .doc("userId")
        .collection("posts")
        .add(mapOf("title" to "My First Post"))
}
```

## Storage

### Upload a File

```kotlin
lifecycleScope.launch {
    val file = File("/path/to/image.jpg")
    val metadata = scs.storage.upload(file, "images")
    Log.d("SCS", "Uploaded: ${metadata.url}")
}
```

### Upload Bytes

```kotlin
lifecycleScope.launch {
    val bytes = // ... your byte array
    val metadata = scs.storage.uploadBytes(bytes, "document.pdf", "documents")
}
```

### List Files

```kotlin
lifecycleScope.launch {
    val files = scs.storage.list("images")
    files.forEach { file ->
        Log.d("SCS", "File: ${file.name} (${file.getFormattedSize()})")
    }
}
```

### Download a File

```kotlin
lifecycleScope.launch {
    val bytes = scs.storage.download(fileId)
    // Save bytes to file or use directly
}
```

### Delete a File

```kotlin
lifecycleScope.launch {
    scs.storage.delete(fileId)
}
```

## Realtime Database

### Get a Reference

```kotlin
val usersRef = scs.realtime.ref("users")
val userRef = scs.realtime.ref("users/user1")
```

### Set Data

```kotlin
lifecycleScope.launch {
    scs.realtime.ref("users/user1").set(
        mapOf(
            "name" to "John",
            "status" to "online"
        )
    )
}
```

### Listen for Changes

```kotlin
scs.realtime.ref("users/user1").onValue { data ->
    Log.d("SCS", "User data changed: $data")
}
```

### Update Data

```kotlin
lifecycleScope.launch {
    scs.realtime.ref("users/user1").update(
        mapOf("status" to "offline")
    )
}
```

### Push Data (Auto-generated Key)

```kotlin
lifecycleScope.launch {
    val newRef = scs.realtime.ref("messages").push(
        mapOf("text" to "Hello!")
    )
    Log.d("SCS", "New message key: ${newRef.key}")
}
```

### Remove Data

```kotlin
lifecycleScope.launch {
    scs.realtime.ref("users/user1").remove()
}
```

### Stop Listening

```kotlin
scs.realtime.ref("users/user1").off()
```

## Cloud Messaging

### Register Device Token

```kotlin
// Get FCM token and register it
FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
    lifecycleScope.launch {
        scs.messaging.registerToken(token, "android")
    }
}
```

### Subscribe to Topic

```kotlin
lifecycleScope.launch {
    scs.messaging.subscribeToTopic(fcmToken, "news")
}
```

### Send to Topic

```kotlin
lifecycleScope.launch {
    scs.messaging.sendToTopic(
        topic = "news",
        title = "Breaking News",
        body = "Something happened!",
        data = mapOf("url" to "https://example.com/article")
    )
}
```

### Unsubscribe from Topic

```kotlin
lifecycleScope.launch {
    scs.messaging.unsubscribeFromTopic(fcmToken, "news")
}
```

## Remote Configuration

### Fetch and Activate

```kotlin
lifecycleScope.launch {
    val updated = scs.remoteConfig.fetchAndActivate()
    if (updated) {
        Log.d("SCS", "Config updated!")
    }
}
```

### Get Values

```kotlin
val welcomeMessage = scs.remoteConfig.getString("welcome_message", "Hello!")
val maxItems = scs.remoteConfig.getInt("max_items", 10)
val featureEnabled = scs.remoteConfig.getBoolean("new_feature", false)
val settings = scs.remoteConfig.getJson("settings")
```

## Cloud Functions

### Call a Function

```kotlin
lifecycleScope.launch {
    val result = scs.functions.call(
        "processPayment",
        mapOf(
            "amount" to 100,
            "currency" to "USD"
        )
    )

    if (result.success) {
        val data = result.getDataAsMap()
        Log.d("SCS", "Payment ID: ${data?.get("paymentId")}")
    } else {
        Log.e("SCS", "Error: ${result.error}")
    }
}
```

### Using HttpsCallable

```kotlin
val processPayment = scs.functions.httpsCallable("processPayment")

lifecycleScope.launch {
    val result = processPayment.call(mapOf("amount" to 100))
}
```

## Machine Learning

### Text Recognition (OCR)

```kotlin
lifecycleScope.launch {
    val file = File("/path/to/image.jpg")
    val result = scs.ml.recognizeText(file)
    Log.d("SCS", "Recognized text: ${result.text}")
}
```

### Image Labeling

```kotlin
lifecycleScope.launch {
    val file = File("/path/to/image.jpg")
    val result = scs.ml.labelImage(file)
    result.labels.forEach { label ->
        Log.d("SCS", "${label.label}: ${label.confidence}")
    }
}
```

## AI Services

### Chat

```kotlin
lifecycleScope.launch {
    val response = scs.ai.chat(
        messages = listOf(
            ChatMessage.system("You are a helpful assistant."),
            ChatMessage.user("What is the capital of France?")
        ),
        model = "llama2"
    )
    Log.d("SCS", "AI: ${response.content}")
}
```

### Simple Ask

```kotlin
lifecycleScope.launch {
    val answer = scs.ai.ask(
        message = "What is the capital of France?",
        systemPrompt = "You are a helpful assistant."
    )
    Log.d("SCS", "AI: $answer")
}
```

### Text Completion

```kotlin
lifecycleScope.launch {
    val completion = scs.ai.complete(
        prompt = "Once upon a time",
        maxTokens = 100
    )
    Log.d("SCS", completion.content)
}
```

### Image Generation

```kotlin
lifecycleScope.launch {
    val result = scs.ai.generateImage(
        prompt = "A sunset over mountains",
        size = "512x512"
    )
    Log.d("SCS", "Image URL: ${result.imageUrl}")
}
```

### Chat Builder (Fluent API)

```kotlin
lifecycleScope.launch {
    val response = scs.ai.chat(
        scs.ai.chatBuilder()
            .system("You are a helpful assistant.")
            .user("Hello!")
            .model("llama2")
            .temperature(0.7)
            .maxTokens(100)
    )
}
```

## AI Agents

Create and manage AI agents with custom instructions and tools.

### Create an Agent

```kotlin
lifecycleScope.launch {
    val agent = scs.ai.createAgent(
        name = "Customer Support",
        instructions = "You are a helpful customer support assistant. Be polite and helpful.",
        model = "llama3.2",
        temperature = 0.7
    )
    Log.d("SCS", "Created agent: ${agent.id}")
}
```

### Run the Agent

```kotlin
lifecycleScope.launch {
    // Run the agent
    var response = scs.ai.runAgent(
        agentId = agent.id,
        input = "How do I reset my password?"
    )
    Log.d("SCS", "Agent: ${response.output}")
    Log.d("SCS", "Session: ${response.sessionId}")

    // Continue the conversation in the same session
    response = scs.ai.runAgent(
        agentId = agent.id,
        input = "Thanks! What about enabling 2FA?",
        sessionId = response.sessionId
    )
}
```

### Manage Agent Sessions

```kotlin
lifecycleScope.launch {
    // List agent sessions
    val sessions = scs.ai.listAgentSessions(agent.id)

    // Get full session history
    val session = scs.ai.getAgentSession(agent.id, response.sessionId)
    session.messages.forEach { msg ->
        Log.d("SCS", "${msg.role}: ${msg.content}")
    }

    // Delete a session
    scs.ai.deleteAgentSession(agent.id, response.sessionId)
}
```

### Manage Agents

```kotlin
lifecycleScope.launch {
    // List agents
    val agents = scs.ai.listAgents()

    // Update agent
    scs.ai.updateAgent(
        agentId = agent.id,
        instructions = "Updated instructions here",
        temperature = 0.5
    )

    // Delete agent
    scs.ai.deleteAgent(agent.id)
}
```

### Agent Tools

```kotlin
lifecycleScope.launch {
    // Define a tool for agents
    val tool = scs.ai.defineTool(
        name = "get_weather",
        description = "Get weather for a location",
        parameters = mapOf(
            "type" to "object",
            "properties" to mapOf(
                "location" to mapOf(
                    "type" to "string",
                    "description" to "City name"
                )
            )
        )
    )

    // List tools
    val tools = scs.ai.listTools()

    // Delete a tool
    scs.ai.deleteTool(tool.id)
}
```

## Error Handling

```kotlin
lifecycleScope.launch {
    try {
        val user = scs.auth.login(email, password)
    } catch (e: ScsException) {
        when (e.code) {
            ScsException.AUTH_INVALID_CREDENTIALS -> {
                // Handle invalid credentials
            }
            ScsException.AUTH_USER_NOT_FOUND -> {
                // Handle user not found
            }
            ScsException.NETWORK_ERROR -> {
                // Handle network error
            }
            else -> {
                // Handle other errors
            }
        }
        Log.e("SCS", "Error: ${e.message}, Code: ${e.code}")
    }
}
```

## ProGuard Rules

If you're using ProGuard/R8, the SDK includes consumer ProGuard rules automatically. No additional configuration is needed.

## License

MIT License - see LICENSE file for details.

## Support
