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

### OAuth and Social Sign-In

SCS provides comprehensive OAuth and social authentication support for Android. Each provider requires specific setup and credentials.

---

#### Google Sign-In

Authenticate users with their Google accounts using Google Sign-In SDK.

**Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `idToken` | `String` | Yes | Google ID token from Google Sign-In SDK |
| `accessToken` | `String?` | No | Google access token for additional API access |

**Setup Requirements:**

1. Add Google Sign-In dependency to `build.gradle.kts`:
```kotlin
implementation("com.google.android.gms:play-services-auth:21.0.0")
```

2. Configure OAuth client ID in Google Cloud Console
3. Add your app's SHA-1 fingerprint to the OAuth configuration

**Complete Implementation:**

```kotlin
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

class GoogleAuthManager(private val activity: ComponentActivity) {
    private val scs = Scs.getInstance()
    private val googleSignInClient: GoogleSignInClient

    init {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(activity.getString(R.string.google_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(activity, gso)
    }

    private val signInLauncher = activity.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        handleSignInResult(result.data)
    }

    fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        signInLauncher.launch(signInIntent)
    }

    private fun handleSignInResult(data: Intent?) {
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)

            account.idToken?.let { idToken ->
                activity.lifecycleScope.launch {
                    try {
                        val user = scs.auth.signInWithGoogle(
                            idToken = idToken,
                            accessToken = account.serverAuthCode
                        )
                        Log.d("SCS", "Signed in: ${user.email}")
                        onSignInSuccess(user)
                    } catch (e: ScsException) {
                        onSignInError(e)
                    }
                }
            }
        } catch (e: ApiException) {
            Log.e("SCS", "Google sign-in failed: ${e.statusCode}")
            onSignInError(e)
        }
    }

    fun signOut() {
        googleSignInClient.signOut()
        scs.auth.logout()
    }

    private fun onSignInSuccess(user: ScsUser) {
        // Handle successful sign-in
    }

    private fun onSignInError(error: Exception) {
        // Handle sign-in error
    }
}

// Usage in Activity/Fragment
class LoginActivity : ComponentActivity() {
    private lateinit var googleAuth: GoogleAuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        googleAuth = GoogleAuthManager(this)

        binding.googleSignInButton.setOnClickListener {
            googleAuth.signIn()
        }
    }
}
```

**Jetpack Compose Integration:**

```kotlin
@Composable
fun GoogleSignInButton(
    onSignInSuccess: (ScsUser) -> Unit,
    onSignInError: (Exception) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scs = Scs.getInstance()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            account.idToken?.let { idToken ->
                scope.launch {
                    try {
                        val user = scs.auth.signInWithGoogle(
                            idToken = idToken,
                            accessToken = account.serverAuthCode
                        )
                        onSignInSuccess(user)
                    } catch (e: ScsException) {
                        onSignInError(e)
                    }
                }
            }
        } catch (e: ApiException) {
            onSignInError(e)
        }
    }

    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.google_web_client_id))
            .requestEmail()
            .build()
    }
    val googleSignInClient = remember { GoogleSignIn.getClient(context, gso) }

    Button(onClick = { launcher.launch(googleSignInClient.signInIntent) }) {
        Icon(
            painter = painterResource(R.drawable.ic_google),
            contentDescription = null
        )
        Spacer(Modifier.width(8.dp))
        Text("Sign in with Google")
    }
}
```

---

#### Facebook Sign-In

Authenticate users with their Facebook accounts using Facebook SDK.

**Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `accessToken` | `String` | Yes | Facebook access token from Facebook SDK |

**Setup Requirements:**

1. Add Facebook SDK dependency:
```kotlin
implementation("com.facebook.android:facebook-login:16.0.0")
```

2. Configure Facebook App ID in `strings.xml`:
```xml
<string name="facebook_app_id">YOUR_APP_ID</string>
<string name="fb_login_protocol_scheme">fbYOUR_APP_ID</string>
<string name="facebook_client_token">YOUR_CLIENT_TOKEN</string>
```

3. Add to `AndroidManifest.xml`:
```xml
<meta-data
    android:name="com.facebook.sdk.ApplicationId"
    android:value="@string/facebook_app_id"/>
<meta-data
    android:name="com.facebook.sdk.ClientToken"
    android:value="@string/facebook_client_token"/>
```

**Complete Implementation:**

```kotlin
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.facebook.AccessToken

class FacebookAuthManager(private val activity: ComponentActivity) {
    private val scs = Scs.getInstance()
    private val callbackManager = CallbackManager.Factory.create()
    private val loginManager = LoginManager.getInstance()

    init {
        loginManager.registerCallback(callbackManager,
            object : FacebookCallback<LoginResult> {
                override fun onSuccess(result: LoginResult) {
                    handleFacebookToken(result.accessToken)
                }

                override fun onCancel() {
                    Log.d("SCS", "Facebook login cancelled")
                }

                override fun onError(error: FacebookException) {
                    Log.e("SCS", "Facebook login error: ${error.message}")
                    onSignInError(error)
                }
            }
        )
    }

    fun signIn() {
        loginManager.logInWithReadPermissions(
            activity,
            callbackManager,
            listOf("public_profile", "email")
        )
    }

    private fun handleFacebookToken(token: AccessToken) {
        activity.lifecycleScope.launch {
            try {
                val user = scs.auth.signInWithFacebook(
                    accessToken = token.token
                )
                Log.d("SCS", "Signed in: ${user.email}")
                onSignInSuccess(user)
            } catch (e: ScsException) {
                onSignInError(e)
            }
        }
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        callbackManager.onActivityResult(requestCode, resultCode, data)
    }

    fun signOut() {
        loginManager.logOut()
        scs.auth.logout()
    }

    private fun onSignInSuccess(user: ScsUser) {
        // Handle successful sign-in
    }

    private fun onSignInError(error: Exception) {
        // Handle sign-in error
    }
}

// Usage
class LoginActivity : ComponentActivity() {
    private lateinit var facebookAuth: FacebookAuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        facebookAuth = FacebookAuthManager(this)

        binding.facebookSignInButton.setOnClickListener {
            facebookAuth.signIn()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        facebookAuth.onActivityResult(requestCode, resultCode, data)
    }
}
```

---

#### Apple Sign-In

Authenticate users with their Apple accounts on Android using web-based OAuth.

**Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `identityToken` | `String` | Yes | Apple identity token (JWT) |
| `authorizationCode` | `String?` | No | Authorization code for server verification |
| `fullName` | `String?` | No | User's full name (only available on first sign-in) |

**Complete Implementation:**

```kotlin
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent

class AppleAuthManager(private val activity: ComponentActivity) {
    private val scs = Scs.getInstance()

    companion object {
        private const val APPLE_AUTH_URL = "https://appleid.apple.com/auth/authorize"
        private const val CLIENT_ID = "your.app.bundle.id" // Service ID from Apple Developer
        private const val REDIRECT_URI = "https://yourbackend.com/auth/apple/callback"
    }

    fun signIn() {
        val authUrl = Uri.parse(APPLE_AUTH_URL).buildUpon()
            .appendQueryParameter("client_id", CLIENT_ID)
            .appendQueryParameter("redirect_uri", REDIRECT_URI)
            .appendQueryParameter("response_type", "code id_token")
            .appendQueryParameter("response_mode", "form_post")
            .appendQueryParameter("scope", "name email")
            .build()

        val customTabsIntent = CustomTabsIntent.Builder()
            .setShowTitle(true)
            .build()

        customTabsIntent.launchUrl(activity, authUrl)
    }

    // Handle the callback from your backend (via deep link)
    suspend fun handleCallback(identityToken: String, authorizationCode: String?, fullName: String?): ScsUser {
        return scs.auth.signInWithApple(
            identityToken = identityToken,
            authorizationCode = authorizationCode,
            fullName = fullName
        )
    }
}

// In your Activity, handle the deep link callback
class AuthCallbackActivity : ComponentActivity() {
    private val appleAuth = AppleAuthManager(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        intent?.data?.let { uri ->
            if (uri.path == "/auth/apple/callback") {
                val identityToken = uri.getQueryParameter("id_token")
                val authCode = uri.getQueryParameter("code")
                val fullName = uri.getQueryParameter("name")

                identityToken?.let { token ->
                    lifecycleScope.launch {
                        try {
                            val user = appleAuth.handleCallback(token, authCode, fullName)
                            // Navigate to main screen
                        } catch (e: ScsException) {
                            // Handle error
                        }
                    }
                }
            }
        }
    }
}
```

---

#### GitHub Sign-In

Authenticate users with their GitHub accounts using OAuth.

**Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `code` | `String` | Yes | OAuth authorization code from GitHub |
| `redirectUri` | `String?` | No | Redirect URI (must match OAuth app config) |

**Complete Implementation:**

```kotlin
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent

class GitHubAuthManager(private val activity: ComponentActivity) {
    private val scs = Scs.getInstance()

    companion object {
        private const val CLIENT_ID = "your-github-client-id"
        private const val REDIRECT_URI = "yourapp://github/callback"
        private const val AUTH_URL = "https://github.com/login/oauth/authorize"
    }

    fun signIn() {
        val authUrl = Uri.parse(AUTH_URL).buildUpon()
            .appendQueryParameter("client_id", CLIENT_ID)
            .appendQueryParameter("redirect_uri", REDIRECT_URI)
            .appendQueryParameter("scope", "user:email")
            .build()

        val customTabsIntent = CustomTabsIntent.Builder()
            .setShowTitle(true)
            .build()

        customTabsIntent.launchUrl(activity, authUrl)
    }

    suspend fun handleCallback(code: String): ScsUser {
        return scs.auth.signInWithGitHub(
            code = code,
            redirectUri = REDIRECT_URI
        )
    }
}

// Handle callback in Activity with intent filter
class GitHubCallbackActivity : ComponentActivity() {
    private val githubAuth = GitHubAuthManager(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        intent?.data?.let { uri ->
            uri.getQueryParameter("code")?.let { code ->
                lifecycleScope.launch {
                    try {
                        val user = githubAuth.handleCallback(code)
                        Log.d("SCS", "Signed in: ${user.email}")
                        // Navigate to main screen
                    } catch (e: ScsException) {
                        Log.e("SCS", "GitHub sign-in failed: ${e.message}")
                    }
                }
            }
        }
    }
}

// Add to AndroidManifest.xml:
// <activity android:name=".GitHubCallbackActivity" android:exported="true">
//     <intent-filter>
//         <action android:name="android.intent.action.VIEW" />
//         <category android:name="android.intent.category.DEFAULT" />
//         <category android:name="android.intent.category.BROWSABLE" />
//         <data android:scheme="yourapp" android:host="github" android:path="/callback" />
//     </intent-filter>
// </activity>
```

---

#### Twitter/X Sign-In

Authenticate users with their Twitter/X accounts using OAuth 1.0a.

**Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `oauthToken` | `String` | Yes | Twitter OAuth token |
| `oauthTokenSecret` | `String` | Yes | Twitter OAuth token secret |

**Note:** Twitter OAuth 1.0a requires a backend server to handle token signing securely.

**Complete Implementation:**

```kotlin
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent

class TwitterAuthManager(
    private val activity: ComponentActivity,
    private val backendUrl: String
) {
    private val scs = Scs.getInstance()

    companion object {
        private const val CALLBACK_URL = "yourapp://twitter/callback"
    }

    suspend fun signIn() {
        // Step 1: Get request token from your backend
        val requestToken = getRequestToken()

        // Step 2: Open Twitter authorization
        val authUrl = Uri.parse("https://api.twitter.com/oauth/authorize")
            .buildUpon()
            .appendQueryParameter("oauth_token", requestToken.token)
            .build()

        val customTabsIntent = CustomTabsIntent.Builder().build()
        customTabsIntent.launchUrl(activity, authUrl)
    }

    private suspend fun getRequestToken(): RequestToken {
        // Call your backend to get request token
        val response = httpClient.post("$backendUrl/auth/twitter/request-token") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("callbackUrl" to CALLBACK_URL))
        }
        return response.body()
    }

    suspend fun handleCallback(oauthToken: String, oauthVerifier: String): ScsUser {
        // Step 3: Exchange for access token via your backend
        val accessToken = exchangeToken(oauthToken, oauthVerifier)

        // Step 4: Sign in with SCS
        return scs.auth.signInWithTwitter(
            oauthToken = accessToken.token,
            oauthTokenSecret = accessToken.secret
        )
    }

    private suspend fun exchangeToken(token: String, verifier: String): AccessToken {
        val response = httpClient.post("$backendUrl/auth/twitter/access-token") {
            contentType(ContentType.Application.Json)
            setBody(mapOf(
                "oauthToken" to token,
                "oauthVerifier" to verifier
            ))
        }
        return response.body()
    }
}

data class RequestToken(val token: String, val secret: String)
data class AccessToken(val token: String, val secret: String)
```

---

#### Microsoft Sign-In

Authenticate users with their Microsoft accounts using MSAL.

**Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `accessToken` | `String` | Yes | Microsoft access token from MSAL |
| `idToken` | `String?` | No | Microsoft ID token for additional claims |

**Setup Requirements:**

1. Add MSAL dependency:
```kotlin
implementation("com.microsoft.identity.client:msal:4.0.0")
```

2. Create `auth_config.json` in `res/raw/`:
```json
{
  "client_id": "your-azure-client-id",
  "authorization_user_agent": "DEFAULT",
  "redirect_uri": "msauth://com.yourapp/callback",
  "authorities": [
    {
      "type": "AAD",
      "audience": {
        "type": "AzureADMultipleOrgs"
      }
    }
  ]
}
```

**Complete Implementation:**

```kotlin
import com.microsoft.identity.client.*
import com.microsoft.identity.client.exception.MsalException

class MicrosoftAuthManager(private val activity: ComponentActivity) {
    private val scs = Scs.getInstance()
    private var msalApp: ISingleAccountPublicClientApplication? = null

    init {
        PublicClientApplication.createSingleAccountPublicClientApplication(
            activity.applicationContext,
            R.raw.auth_config,
            object : IPublicClientApplication.ISingleAccountApplicationCreatedListener {
                override fun onCreated(application: ISingleAccountPublicClientApplication) {
                    msalApp = application
                }

                override fun onError(exception: MsalException) {
                    Log.e("SCS", "MSAL init failed: ${exception.message}")
                }
            }
        )
    }

    fun signIn() {
        val app = msalApp ?: return

        val parameters = SignInParameters.builder()
            .withActivity(activity)
            .withScopes(listOf("User.Read"))
            .withCallback(object : AuthenticationCallback {
                override fun onSuccess(authenticationResult: IAuthenticationResult) {
                    handleMsalResult(authenticationResult)
                }

                override fun onError(exception: MsalException) {
                    Log.e("SCS", "Microsoft sign-in error: ${exception.message}")
                    onSignInError(exception)
                }

                override fun onCancel() {
                    Log.d("SCS", "Microsoft sign-in cancelled")
                }
            })
            .build()

        app.signIn(parameters)
    }

    private fun handleMsalResult(result: IAuthenticationResult) {
        activity.lifecycleScope.launch {
            try {
                val user = scs.auth.signInWithMicrosoft(
                    accessToken = result.accessToken,
                    idToken = result.account.idToken
                )
                Log.d("SCS", "Signed in: ${user.email}")
                onSignInSuccess(user)
            } catch (e: ScsException) {
                onSignInError(e)
            }
        }
    }

    fun signOut() {
        msalApp?.signOut(object : ISingleAccountPublicClientApplication.SignOutCallback {
            override fun onSignOut() {
                scs.auth.logout()
            }

            override fun onError(exception: MsalException) {
                Log.e("SCS", "Sign out error: ${exception.message}")
            }
        })
    }

    private fun onSignInSuccess(user: ScsUser) {
        // Handle successful sign-in
    }

    private fun onSignInError(error: Exception) {
        // Handle sign-in error
    }
}
```

---

### Anonymous Authentication

Create temporary accounts for users who want to try your app without signing up. Anonymous accounts can be upgraded to permanent accounts later.

**Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `customData` | `Map<String, Any>?` | No | Optional metadata to associate with the anonymous user |

**Use Cases:**
- Guest checkout in e-commerce apps
- Try-before-you-sign-up experiences
- Gradual onboarding flows
- Saving user data before requiring registration

**Complete Implementation:**

```kotlin
class AnonymousAuthManager {
    private val scs = Scs.getInstance()

    /**
     * Sign in anonymously with optional metadata
     */
    suspend fun signInAnonymously(referrer: String? = null): ScsUser {
        val customData = mutableMapOf<String, Any>(
            "createdAt" to System.currentTimeMillis(),
            "platform" to "Android",
            "sdkVersion" to Build.VERSION.SDK_INT
        )

        referrer?.let { customData["referrer"] = it }

        return scs.auth.signInAnonymously(customData = customData)
    }

    /**
     * Check if current user is anonymous
     */
    val isAnonymous: Boolean
        get() = scs.auth.currentUser?.isAnonymous == true

    /**
     * Upgrade anonymous account to email/password account
     */
    suspend fun upgradeWithEmailPassword(email: String, password: String): ScsUser {
        require(isAnonymous) { "Current user is not anonymous" }

        return scs.auth.linkProvider(
            provider = "password",
            credentials = mapOf(
                "email" to email,
                "password" to password
            )
        )
    }

    /**
     * Upgrade anonymous account with Google
     */
    suspend fun upgradeWithGoogle(idToken: String, accessToken: String? = null): ScsUser {
        require(isAnonymous) { "Current user is not anonymous" }

        val credentials = mutableMapOf<String, String>("idToken" to idToken)
        accessToken?.let { credentials["accessToken"] = it }

        return scs.auth.linkProvider(
            provider = "google",
            credentials = credentials
        )
    }

    /**
     * Upgrade anonymous account with Facebook
     */
    suspend fun upgradeWithFacebook(accessToken: String): ScsUser {
        require(isAnonymous) { "Current user is not anonymous" }

        return scs.auth.linkProvider(
            provider = "facebook",
            credentials = mapOf("accessToken" to accessToken)
        )
    }
}

// Jetpack Compose Usage
@Composable
fun GuestModeScreen(
    onUpgradeAccount: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val authManager = remember { AnonymousAuthManager() }
    var user by remember { mutableStateOf<ScsUser?>(null) }

    LaunchedEffect(Unit) {
        if (Scs.getInstance().auth.currentUser == null) {
            user = authManager.signInAnonymously(referrer = "onboarding")
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (authManager.isAnonymous) {
            Text(
                text = "Browsing as Guest",
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = onUpgradeAccount) {
                Text("Create Account to Save Progress")
            }
        }

        // App content...
    }
}
```

---

### Phone Number Authentication

Authenticate users with their phone numbers using SMS verification codes.

**Step 1 Parameters (sendPhoneVerificationCode):**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `phoneNumber` | `String` | Yes | Phone number in E.164 format (e.g., "+14155551234") |
| `recaptchaToken` | `String?` | No | reCAPTCHA token for abuse prevention |

**Step 2 Parameters (signInWithPhoneNumber):**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `verificationId` | `String` | Yes | Verification ID from step 1 |
| `code` | `String` | Yes | 6-digit verification code from SMS |

**Complete Implementation:**

```kotlin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PhoneAuthManager {
    private val scs = Scs.getInstance()

    private val _uiState = MutableStateFlow<PhoneAuthState>(PhoneAuthState.Idle)
    val uiState: StateFlow<PhoneAuthState> = _uiState.asStateFlow()

    private var verificationId: String? = null

    /**
     * Send verification code to phone number
     */
    suspend fun sendVerificationCode(phoneNumber: String) {
        // Validate phone number format
        require(phoneNumber.startsWith("+") && phoneNumber.length >= 10) {
            "Invalid phone number format. Use E.164 format (e.g., +14155551234)"
        }

        _uiState.value = PhoneAuthState.SendingCode

        try {
            verificationId = scs.auth.sendPhoneVerificationCode(
                phoneNumber = phoneNumber,
                recaptchaToken = null
            )
            _uiState.value = PhoneAuthState.CodeSent(phoneNumber)
        } catch (e: ScsException) {
            _uiState.value = PhoneAuthState.Error(e.message ?: "Failed to send code")
        }
    }

    /**
     * Verify code and sign in
     */
    suspend fun verifyCode(code: String): ScsUser? {
        val verId = verificationId
            ?: throw IllegalStateException("Verification ID not found. Send code first.")

        // Validate code format
        require(code.length == 6 && code.all { it.isDigit() }) {
            "Invalid verification code format"
        }

        _uiState.value = PhoneAuthState.Verifying

        return try {
            val user = scs.auth.signInWithPhoneNumber(
                verificationId = verId,
                code = code
            )
            _uiState.value = PhoneAuthState.Success(user)
            verificationId = null
            user
        } catch (e: ScsException) {
            _uiState.value = PhoneAuthState.Error(e.message ?: "Verification failed")
            null
        }
    }

    /**
     * Resend verification code
     */
    suspend fun resendCode(phoneNumber: String) {
        sendVerificationCode(phoneNumber)
    }

    fun reset() {
        verificationId = null
        _uiState.value = PhoneAuthState.Idle
    }
}

sealed class PhoneAuthState {
    object Idle : PhoneAuthState()
    object SendingCode : PhoneAuthState()
    data class CodeSent(val phoneNumber: String) : PhoneAuthState()
    object Verifying : PhoneAuthState()
    data class Success(val user: ScsUser) : PhoneAuthState()
    data class Error(val message: String) : PhoneAuthState()
}

// Jetpack Compose Phone Auth Screen
@Composable
fun PhoneAuthScreen(
    onAuthSuccess: (ScsUser) -> Unit
) {
    val authManager = remember { PhoneAuthManager() }
    val uiState by authManager.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    var phoneNumber by remember { mutableStateOf("") }
    var verificationCode by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (val state = uiState) {
            is PhoneAuthState.Idle,
            is PhoneAuthState.SendingCode,
            is PhoneAuthState.Error -> {
                // Phone number input
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("Phone Number") },
                    placeholder = { Text("+1 (555) 123-4567") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        scope.launch { authManager.sendVerificationCode(phoneNumber) }
                    },
                    enabled = phoneNumber.isNotEmpty() && state !is PhoneAuthState.SendingCode,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (state is PhoneAuthState.SendingCode) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Send Verification Code")
                    }
                }

                if (state is PhoneAuthState.Error) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            is PhoneAuthState.CodeSent,
            is PhoneAuthState.Verifying -> {
                // Verification code input
                Text(
                    text = "Enter the 6-digit code sent to ${(state as? PhoneAuthState.CodeSent)?.phoneNumber ?: phoneNumber}",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = verificationCode,
                    onValueChange = { if (it.length <= 6) verificationCode = it },
                    label = { Text("Verification Code") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        scope.launch {
                            authManager.verifyCode(verificationCode)?.let { user ->
                                onAuthSuccess(user)
                            }
                        }
                    },
                    enabled = verificationCode.length == 6 && state !is PhoneAuthState.Verifying,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (state is PhoneAuthState.Verifying) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Verify & Sign In")
                    }
                }

                TextButton(
                    onClick = { scope.launch { authManager.resendCode(phoneNumber) } }
                ) {
                    Text("Resend Code")
                }
            }

            is PhoneAuthState.Success -> {
                // Already handled via callback
            }
        }
    }
}
```

---

### Custom Token Authentication

Authenticate users with custom JWT tokens generated by your backend server. This is useful for migrating existing users or implementing custom authentication flows.

**Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `token` | `String` | Yes | Custom JWT token from your backend |

**Use Cases:**
- Migrating users from existing authentication systems
- Enterprise SSO integration
- Custom authentication flows
- Backend-initiated sessions

**Complete Implementation:**

```kotlin
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*

class CustomTokenAuthManager(private val backendUrl: String) {
    private val scs = Scs.getInstance()
    private val httpClient = HttpClient()

    /**
     * Get custom token from backend and sign in
     */
    suspend fun signInWithCustomToken(
        userId: String,
        additionalClaims: Map<String, Any>? = null
    ): ScsUser {
        // Step 1: Request custom token from your backend
        val token = requestCustomToken(userId, additionalClaims)

        // Step 2: Sign in with SCS
        return scs.auth.signInWithCustomToken(token)
    }

    private suspend fun requestCustomToken(
        userId: String,
        claims: Map<String, Any>?
    ): String {
        val response = httpClient.post("$backendUrl/auth/custom-token") {
            contentType(ContentType.Application.Json)
            setBody(buildMap {
                put("userId", userId)
                claims?.let { put("claims", it) }
            })
        }

        // Parse response and extract token
        val json = response.body<Map<String, Any>>()
        return json["token"] as? String
            ?: throw IllegalStateException("Token not found in response")
    }
}

// Enterprise SSO Integration Example
class EnterpriseSSOManager(backendUrl: String) {
    private val customTokenAuth = CustomTokenAuthManager(backendUrl)

    /**
     * Sign in with enterprise SSO credentials
     */
    suspend fun signInWithSSO(
        ssoToken: String,
        employeeId: String
    ): ScsUser {
        return customTokenAuth.signInWithCustomToken(
            userId = employeeId,
            additionalClaims = mapOf(
                "ssoToken" to ssoToken,
                "enterprise" to true,
                "loginTime" to System.currentTimeMillis()
            )
        )
    }
}

// Usage
lifecycleScope.launch {
    val ssoManager = EnterpriseSSOManager("https://api.yourcompany.com")
    try {
        val user = ssoManager.signInWithSSO(
            ssoToken = "enterprise-sso-token",
            employeeId = "EMP12345"
        )
        Log.d("SCS", "Signed in as: ${user.email}")
    } catch (e: Exception) {
        Log.e("SCS", "SSO sign-in failed: ${e.message}")
    }
}
```

---

### Account Linking

Link multiple authentication providers to a single user account, allowing users to sign in with any linked provider.

**linkProvider Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `provider` | `String` | Yes | Provider name ("google", "facebook", "apple", "github", "twitter", "microsoft", "password") |
| `credentials` | `Map<String, String>` | Yes | Provider-specific credentials |

**unlinkProvider Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `provider` | `String` | Yes | Provider name to unlink |

**Complete Implementation:**

```kotlin
class AccountLinkingManager {
    private val scs = Scs.getInstance()

    /**
     * Get currently linked providers
     */
    val linkedProviders: List<String>
        get() = scs.auth.currentUser?.providers ?: emptyList()

    /**
     * Link Google account
     */
    suspend fun linkGoogle(idToken: String, accessToken: String? = null): ScsUser {
        val credentials = mutableMapOf<String, String>("idToken" to idToken)
        accessToken?.let { credentials["accessToken"] = it }

        return scs.auth.linkProvider("google", credentials)
    }

    /**
     * Link Facebook account
     */
    suspend fun linkFacebook(accessToken: String): ScsUser {
        return scs.auth.linkProvider("facebook", mapOf("accessToken" to accessToken))
    }

    /**
     * Link email/password
     */
    suspend fun linkEmailPassword(email: String, password: String): ScsUser {
        return scs.auth.linkProvider("password", mapOf(
            "email" to email,
            "password" to password
        ))
    }

    /**
     * Unlink a provider
     */
    suspend fun unlinkProvider(provider: String): ScsUser {
        require(linkedProviders.size > 1) {
            "Cannot unlink the last provider"
        }

        return scs.auth.unlinkProvider(provider)
    }

    /**
     * Fetch sign-in methods for an email
     */
    suspend fun fetchSignInMethods(email: String): List<String> {
        return scs.auth.fetchSignInMethodsForEmail(email)
    }
}

// Jetpack Compose Linked Accounts Screen
@Composable
fun LinkedAccountsScreen() {
    val scope = rememberCoroutineScope()
    val linkingManager = remember { AccountLinkingManager() }
    var linkedProviders by remember { mutableStateOf(linkingManager.linkedProviders) }

    val allProviders = listOf(
        Triple("google", "Google", Icons.Default.AccountCircle),
        Triple("facebook", "Facebook", Icons.Default.Face),
        Triple("apple", "Apple", Icons.Default.Phone),
        Triple("github", "GitHub", Icons.Default.Code),
        Triple("microsoft", "Microsoft", Icons.Default.Email)
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        item {
            Text(
                text = "Linked Accounts",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        items(allProviders) { (providerId, name, icon) ->
            val isLinked = providerId in linkedProviders

            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(icon, contentDescription = null)

                    Spacer(modifier = Modifier.width(16.dp))

                    Text(
                        text = name,
                        modifier = Modifier.weight(1f)
                    )

                    if (isLinked) {
                        if (linkedProviders.size > 1) {
                            TextButton(
                                onClick = {
                                    scope.launch {
                                        try {
                                            linkingManager.unlinkProvider(providerId)
                                            linkedProviders = linkingManager.linkedProviders
                                        } catch (e: Exception) {
                                            // Handle error
                                        }
                                    }
                                }
                            ) {
                                Text("Unlink", color = MaterialTheme.colorScheme.error)
                            }
                        } else {
                            Text(
                                text = "Primary",
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    } else {
                        TextButton(
                            onClick = {
                                // Initiate provider linking flow
                            }
                        ) {
                            Text("Link")
                        }
                    }
                }
            }
        }
    }
}
```

---

### Password Reset & Email Verification

Handle password recovery and email verification flows.

**Complete Implementation:**

```kotlin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class PasswordResetManager {
    private val scs = Scs.getInstance()

    private val _state = MutableStateFlow<PasswordResetState>(PasswordResetState.Idle)
    val state: StateFlow<PasswordResetState> = _state

    /**
     * Send password reset email
     */
    suspend fun sendPasswordResetEmail(email: String) {
        require(isValidEmail(email)) { "Invalid email format" }

        _state.value = PasswordResetState.Sending

        try {
            scs.auth.sendPasswordResetEmail(email)
            _state.value = PasswordResetState.EmailSent(email)
        } catch (e: ScsException) {
            _state.value = PasswordResetState.Error(e.message ?: "Failed to send email")
        }
    }

    /**
     * Confirm password reset with code from email
     */
    suspend fun confirmPasswordReset(code: String, newPassword: String) {
        require(isStrongPassword(newPassword)) {
            "Password must be at least 8 characters with uppercase, lowercase, and number"
        }

        _state.value = PasswordResetState.Confirming

        try {
            scs.auth.confirmPasswordReset(
                code = code,
                newPassword = newPassword
            )
            _state.value = PasswordResetState.Success
        } catch (e: ScsException) {
            _state.value = PasswordResetState.Error(e.message ?: "Failed to reset password")
        }
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun isStrongPassword(password: String): Boolean {
        return password.length >= 8 &&
               password.any { it.isUpperCase() } &&
               password.any { it.isLowerCase() } &&
               password.any { it.isDigit() }
    }

    fun reset() {
        _state.value = PasswordResetState.Idle
    }
}

sealed class PasswordResetState {
    object Idle : PasswordResetState()
    object Sending : PasswordResetState()
    data class EmailSent(val email: String) : PasswordResetState()
    object Confirming : PasswordResetState()
    object Success : PasswordResetState()
    data class Error(val message: String) : PasswordResetState()
}

class EmailVerificationManager {
    private val scs = Scs.getInstance()

    private val _state = MutableStateFlow<EmailVerificationState>(EmailVerificationState.Idle)
    val state: StateFlow<EmailVerificationState> = _state

    /**
     * Send email verification to current user
     */
    suspend fun sendEmailVerification() {
        requireNotNull(scs.auth.currentUser) { "No user is signed in" }

        _state.value = EmailVerificationState.Sending

        try {
            scs.auth.sendEmailVerification()
            _state.value = EmailVerificationState.Sent
        } catch (e: ScsException) {
            _state.value = EmailVerificationState.Error(e.message ?: "Failed to send verification")
        }
    }

    /**
     * Verify email with code
     */
    suspend fun verifyEmail(code: String) {
        _state.value = EmailVerificationState.Verifying

        try {
            scs.auth.verifyEmail(code)
            _state.value = EmailVerificationState.Verified
        } catch (e: ScsException) {
            _state.value = EmailVerificationState.Error(e.message ?: "Verification failed")
        }
    }

    /**
     * Check if current user's email is verified
     */
    val isEmailVerified: Boolean
        get() = scs.auth.currentUser?.emailVerified == true
}

sealed class EmailVerificationState {
    object Idle : EmailVerificationState()
    object Sending : EmailVerificationState()
    object Sent : EmailVerificationState()
    object Verifying : EmailVerificationState()
    object Verified : EmailVerificationState()
    data class Error(val message: String) : EmailVerificationState()
}

// Jetpack Compose Password Reset Screen
@Composable
fun PasswordResetScreen(
    onResetSuccess: () -> Unit
) {
    val resetManager = remember { PasswordResetManager() }
    val state by resetManager.state.collectAsState()
    val scope = rememberCoroutineScope()

    var email by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (val currentState = state) {
            is PasswordResetState.Idle,
            is PasswordResetState.Sending,
            is PasswordResetState.Error -> {
                if (currentState !is PasswordResetState.EmailSent) {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { scope.launch { resetManager.sendPasswordResetEmail(email) } },
                        enabled = email.isNotEmpty() && currentState !is PasswordResetState.Sending,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Send Reset Email")
                    }

                    if (currentState is PasswordResetState.Error) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(currentState.message, color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            is PasswordResetState.EmailSent,
            is PasswordResetState.Confirming -> {
                Text("Enter the code from your email")

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("Reset Code") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("New Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        scope.launch { resetManager.confirmPasswordReset(code, newPassword) }
                    },
                    enabled = code.isNotEmpty() && newPassword.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Reset Password")
                }
            }

            is PasswordResetState.Success -> {
                LaunchedEffect(Unit) { onResetSuccess() }
            }
        }
    }
}
```

---

### Complete Authentication Service Example

Here's a comprehensive authentication service that brings together all authentication methods:

```kotlin
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Complete authentication service for Android apps
 */
class AuthService private constructor(context: Context) {

    companion object {
        @Volatile
        private var INSTANCE: AuthService? = null

        fun getInstance(context: Context): AuthService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AuthService(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val scs = Scs.getInstance()

    // Auth state
    private val _currentUser = MutableStateFlow<ScsUser?>(scs.auth.currentUser)
    val currentUser: StateFlow<ScsUser?> = _currentUser.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    val isAuthenticated: Boolean
        get() = _currentUser.value != null

    val isAnonymous: Boolean
        get() = _currentUser.value?.isAnonymous == true

    init {
        // Observe auth state changes
        scs.auth.currentUserFlow.onEach { user ->
            _currentUser.value = user
        }.launchIn(CoroutineScope(Dispatchers.Main))
    }

    // MARK: - Email/Password Auth

    suspend fun register(
        email: String,
        password: String,
        displayName: String? = null
    ): Result<ScsUser> = withLoading {
        scs.auth.register(
            email = email,
            password = password,
            displayName = displayName
        )
    }

    suspend fun login(email: String, password: String): Result<ScsUser> = withLoading {
        scs.auth.login(email, password)
    }

    // MARK: - OAuth Providers

    suspend fun signInWithGoogle(idToken: String, accessToken: String? = null): Result<ScsUser> =
        withLoading {
            scs.auth.signInWithGoogle(idToken = idToken, accessToken = accessToken)
        }

    suspend fun signInWithFacebook(accessToken: String): Result<ScsUser> = withLoading {
        scs.auth.signInWithFacebook(accessToken = accessToken)
    }

    suspend fun signInWithApple(
        identityToken: String,
        authorizationCode: String? = null,
        fullName: String? = null
    ): Result<ScsUser> = withLoading {
        scs.auth.signInWithApple(
            identityToken = identityToken,
            authorizationCode = authorizationCode,
            fullName = fullName
        )
    }

    suspend fun signInWithGitHub(code: String, redirectUri: String? = null): Result<ScsUser> =
        withLoading {
            scs.auth.signInWithGitHub(code = code, redirectUri = redirectUri)
        }

    suspend fun signInWithTwitter(oauthToken: String, oauthTokenSecret: String): Result<ScsUser> =
        withLoading {
            scs.auth.signInWithTwitter(
                oauthToken = oauthToken,
                oauthTokenSecret = oauthTokenSecret
            )
        }

    suspend fun signInWithMicrosoft(accessToken: String, idToken: String? = null): Result<ScsUser> =
        withLoading {
            scs.auth.signInWithMicrosoft(accessToken = accessToken, idToken = idToken)
        }

    // MARK: - Anonymous Auth

    suspend fun signInAnonymously(customData: Map<String, Any>? = null): Result<ScsUser> =
        withLoading {
            scs.auth.signInAnonymously(customData = customData)
        }

    // MARK: - Phone Auth

    suspend fun sendPhoneVerificationCode(
        phoneNumber: String,
        recaptchaToken: String? = null
    ): Result<String> = withLoading {
        scs.auth.sendPhoneVerificationCode(
            phoneNumber = phoneNumber,
            recaptchaToken = recaptchaToken
        )
    }

    suspend fun signInWithPhoneNumber(verificationId: String, code: String): Result<ScsUser> =
        withLoading {
            scs.auth.signInWithPhoneNumber(verificationId = verificationId, code = code)
        }

    // MARK: - Custom Token

    suspend fun signInWithCustomToken(token: String): Result<ScsUser> = withLoading {
        scs.auth.signInWithCustomToken(token)
    }

    // MARK: - Account Linking

    suspend fun linkProvider(
        provider: String,
        credentials: Map<String, String>
    ): Result<ScsUser> = withLoading {
        scs.auth.linkProvider(provider = provider, credentials = credentials)
    }

    suspend fun unlinkProvider(provider: String): Result<ScsUser> = withLoading {
        scs.auth.unlinkProvider(provider)
    }

    suspend fun fetchSignInMethodsForEmail(email: String): Result<List<String>> = withLoading {
        scs.auth.fetchSignInMethodsForEmail(email)
    }

    // MARK: - Password Reset

    suspend fun sendPasswordResetEmail(email: String): Result<Unit> = withLoading {
        scs.auth.sendPasswordResetEmail(email)
    }

    suspend fun confirmPasswordReset(code: String, newPassword: String): Result<Unit> =
        withLoading {
            scs.auth.confirmPasswordReset(code = code, newPassword = newPassword)
        }

    // MARK: - Email Verification

    suspend fun sendEmailVerification(): Result<Unit> = withLoading {
        scs.auth.sendEmailVerification()
    }

    suspend fun verifyEmail(code: String): Result<Unit> = withLoading {
        scs.auth.verifyEmail(code)
    }

    // MARK: - Sign Out

    fun signOut() {
        scs.auth.logout()
        _currentUser.value = null
    }

    // MARK: - Helper

    private suspend fun <T> withLoading(block: suspend () -> T): Result<T> {
        _isLoading.value = true
        _error.value = null

        return try {
            Result.success(block())
        } catch (e: ScsException) {
            _error.value = e.message
            Result.failure(e)
        } finally {
            _isLoading.value = false
        }
    }

    fun clearError() {
        _error.value = null
    }
}

// Usage in ViewModel
class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val authService = AuthService.getInstance(application)

    val currentUser = authService.currentUser
    val isLoading = authService.isLoading
    val error = authService.error

    fun login(email: String, password: String) {
        viewModelScope.launch {
            authService.login(email, password)
                .onSuccess { user -> Log.d("Auth", "Logged in: ${user.email}") }
                .onFailure { e -> Log.e("Auth", "Login failed", e) }
        }
    }

    fun signOut() {
        authService.signOut()
    }
}

// Usage in Compose
@Composable
fun AuthScreen(
    viewModel: AuthViewModel = viewModel()
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    when {
        currentUser != null -> MainContent(user = currentUser!!)
        else -> LoginContent(
            isLoading = isLoading,
            error = error,
            onLogin = viewModel::login
        )
    }
}
```

## Database

NoSQL document database with collections and subcollections. SCS supports two powerful database options:

### Database Types

| Type | Name | Description | Best For |
|------|------|-------------|----------|
| `eazi` | **eaZI Database** | Document-based NoSQL with Firestore-like collections, documents, and subcollections | Development, prototyping, small to medium apps |
| `reladb` | **RelaDB** | Production-grade NoSQL database with relational-style views | Production, scalability, advanced queries |

### Initialize with eaZI (Default)

```kotlin
import com.spyxpo.scs.Scs
import com.spyxpo.scs.ScsConfig

// eaZI is the default database - no special configuration needed
Scs.initialize(
    context = this,
    config = ScsConfig(
        projectId = "your-project-id",
        apiKey = "your-api-key"
        // databaseType = "eazi" is implicit
    )
)
```

### Initialize with RelaDB (Production)

```kotlin
import com.spyxpo.scs.Scs
import com.spyxpo.scs.ScsConfig

// Use RelaDB for production
Scs.initialize(
    context = this,
    config = ScsConfig(
        projectId = "your-project-id",
        apiKey = "your-api-key",
        databaseType = "reladb"  // Enable RelaDB
    )
)
```

### eaZI Database Features

- **Document-based**: Firestore-like collections and documents
- **Subcollections**: Nested data organization
- **File-based storage**: No external dependencies required
- **Zero configuration**: Works out of the box
- **Query support**: Filtering, ordering, and pagination

### RelaDB Features

- **Production-ready**: Built for reliability and performance
- **Scalable**: Horizontal scaling and replication support
- **Advanced queries**: Aggregation pipelines, complex filters
- **Indexing**: Custom indexes for optimized performance
- **Schema flexibility**: Dynamic schema with validation support
- **Relational-style views**: Table view with columns and rows in the console

### Collection Operations

```kotlin
lifecycleScope.launch {
    // Get a collection reference
    val users = scs.database.collection("users")

    // List all collections
    val collections = scs.database.listCollections()

    // Create a collection
    scs.database.createCollection("newCollection")

    // Delete a collection
    scs.database.deleteCollection("oldCollection")
}
```

### Document Operations

```kotlin
lifecycleScope.launch {
    // Add document with auto-generated ID
    val doc = scs.database.collection("users").add(
        mapOf(
            "name" to "John Doe",
            "email" to "john@example.com",
            "age" to 30,
            "tags" to listOf("developer", "kotlin"),
            "profile" to mapOf(
                "bio" to "Software developer",
                "avatar" to "https://example.com/avatar.jpg"
            )
        )
    )
    Log.d("SCS", "Document ID: ${doc.id}")

    // Set document with custom ID (creates or overwrites)
    scs.database.collection("users").doc("user-123").set(
        mapOf(
            "name" to "Jane Doe",
            "email" to "jane@example.com"
        )
    )

    // Get a single document
    val user = scs.database.collection("users").doc("user-123").get()
    user?.let {
        Log.d("SCS", "Name: ${it.getString("name")}")
    }

    // Update document (partial update)
    scs.database.collection("users").doc("user-123").update(
        mapOf(
            "age" to 31,
            "profile.bio" to "Senior developer"
        )
    )

    // Delete document
    scs.database.collection("users").doc("user-123").delete()
}
```

### Query Operations

```kotlin
lifecycleScope.launch {
    // Simple query with single filter
    val activeUsers = scs.database.collection("users")
        .where("status", "==", "active")
        .get()

    // Multiple filters
    val results = scs.database.collection("users")
        .where("age", ">=", 18)
        .where("status", "==", "active")
        .get()

    // Ordering and pagination
    val posts = scs.database.collection("posts")
        .where("published", "==", true)
        .orderBy("createdAt", "desc")
        .limit(10)
        .skip(20)
        .get()

    // Using 'in' operator
    val featured = scs.database.collection("posts")
        .where("category", "in", listOf("tech", "science", "news"))
        .get()

    // Using 'contains' for array fields
    val tagged = scs.database.collection("posts")
        .where("tags", "contains", "kotlin")
        .get()
}
```

### Query Operators

| Operator | Description | Example |
|----------|-------------|---------|
| `==` | Equal to | `.where("status", "==", "active")` |
| `!=` | Not equal to | `.where("status", "!=", "deleted")` |
| `>` | Greater than | `.where("age", ">", 18)` |
| `>=` | Greater than or equal | `.where("age", ">=", 18)` |
| `<` | Less than | `.where("price", "<", 100)` |
| `<=` | Less than or equal | `.where("price", "<=", 50)` |
| `in` | Value in array | `.where("status", "in", listOf("active", "pending"))` |
| `contains` | Array contains value | `.where("tags", "contains", "featured")` |

### Subcollections

```kotlin
lifecycleScope.launch {
    // Access a subcollection
    val postsRef = scs.database
        .collection("users")
        .doc("userId")
        .collection("posts")

    // Add to subcollection
    val post = postsRef.add(
        mapOf(
            "title" to "My First Post",
            "content" to "Hello World!",
            "createdAt" to System.currentTimeMillis()
        )
    )

    // Query subcollection
    val userPosts = postsRef
        .orderBy("createdAt", "desc")
        .limit(5)
        .get()

    // Nested subcollections (e.g., users/userId/posts/postId/comments)
    val commentsRef = scs.database
        .collection("users")
        .doc("userId")
        .collection("posts")
        .doc("postId")
        .collection("comments")

    // List subcollections of a document
    val subcollections = scs.database
        .collection("users")
        .doc("userId")
        .listCollections()
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
