# TrustPin Kotlin SDK

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.25%2B-purple.svg)](https://kotlinlang.org)
[![Android](https://img.shields.io/badge/Android-API%2021%2B-green.svg)](https://developer.android.com)
[![JVM](https://img.shields.io/badge/JVM-8%2B-blue.svg)](https://adoptopenjdk.net)
[![License](https://img.shields.io/badge/License-TrustPin-green.svg)](LICENSE)

TrustPin is a modern, lightweight, and secure Kotlin Multiplatform library for **SSL Certificate Pinning** in Android and JVM applications. Built with Kotlin Coroutines and following OWASP security recommendations, TrustPin prevents man-in-the-middle (MITM) attacks by ensuring server authenticity at the TLS level.

Available on Maven Central: [`cloud.trustpin:kotlin-sdk`](https://central.sonatype.com/artifact/cloud.trustpin/kotlin-sdk)

## 🚀 Key Features

- ✅ **Android & JVM support** with a single artifact
- ✅ **Strict or permissive pinning** — enforce in production, relax during development
- ✅ **Drop-in integration** — `TrustManager` and `SSLSocketFactory` for OkHttp / `HttpsURLConnection`
- ✅ **Managed configuration** delivered from TrustPin and cached locally
- ✅ **Configurable logging** for diagnostics
- ✅ **Coroutine- and Java-friendly APIs**
## 🏗️ Architecture

TrustPin SDK provides comprehensive certificate pinning functionality with minimal dependencies:

- ✅ **Minimal dependencies** (Kotlin stdlib + coroutines only)
- ✅ **Works with any HTTP client** including OkHttp, HttpURLConnection, etc.
- ✅ **Built-in TrustManager and SSLSocketFactory** for easy integration
- ✅ **Manual certificate verification** for custom implementations

---

## 📋 Platform Requirements

| Platform | Minimum Version | Notes |
|----------|----------------|-------|
| Android | API 21+ (Recommended: API 25+) | Full feature support |
| JVM | Java 11+ | Desktop/Server applications |
| Kotlin | 2.3.0+ | Built with Kotlin 2.3.0 |

---

## 📦 Installation

### Gradle (Kotlin DSL)

Add to your `build.gradle.kts`:

```kotlin
dependencies {
    implementation("cloud.trustpin:kotlin-sdk:4.1.0")
}
```

### Gradle (Groovy)

Add to your `build.gradle`:

```groovy
dependencies {
    implementation 'cloud.trustpin:kotlin-sdk:4.1.0'
}
```

### Maven

Add to your `pom.xml`:

```xml
<dependency>
    <groupId>cloud.trustpin</groupId>
    <artifactId>kotlin-sdk</artifactId>
    <version>1.2.0</version>
</dependency>
```

---

## 🔧 Quick Setup

### 1. Create and Configure

```kotlin
import cloud.trustpin.kotlin.sdk.TrustPin
import cloud.trustpin.kotlin.sdk.TrustPinMode

// Configure with your project credentials (suspend function)
suspend fun initializeTrustPin() {
    TrustPin.setup(
        organizationId = "your-org-id",
        projectId = "your-project-id", 
        publicKey = "your-base64-public-key",
        mode = TrustPinMode.STRICT  // Recommended
    )
}
```

> 💡 **Find your credentials** in the [TrustPin Dashboard](https://app.trustpin.cloud)
> 
> ⚙️ **Dual API Design**: TrustPin provides both **suspend functions** (recommended for Kotlin coroutines) and **blocking functions** (for Java interop and non-coroutine contexts).

### 2. Choose Your Pinning Mode

TrustPin offers two validation modes:

#### Strict Mode (Recommended for Production)
```kotlin
suspend fun setupProduction() {
    TrustPin.setup(
        // ... your credentials
        mode = TrustPinMode.STRICT  // Throws error for unregistered domains
    )
}
```

#### Permissive Mode (Development & Testing)
```kotlin
suspend fun setupDevelopment() {
    TrustPin.setup(
        // ... your credentials  
        mode = TrustPinMode.PERMISSIVE  // Allows unregistered domains to bypass pinning
    )
}
```

---

## 🛠 Usage Examples

### Dual API Design

TrustPin provides both **suspend** and **blocking** APIs to support different use cases:

```kotlin
// Suspend API - for Kotlin coroutines (recommended)
suspend fun setupAndVerify() {
    // Setup TrustPin configuration
    TrustPin.setup(
        organizationId = "your-org-id",
        projectId = "your-project-id", 
        publicKey = "your-public-key",
        mode = TrustPinMode.STRICT
    )
    
    // Verify certificates
    val certificate: X509Certificate = // ... 
    TrustPin.verify("api.example.com", certificate)
}

// Blocking API - for Java interop and non-coroutine contexts
fun setupBlocking() {
    // Setup TrustPin configuration
    TrustPin.setupBlocking(
        organizationId = "your-org-id",
        projectId = "your-project-id", 
        publicKey = "your-public-key",
        mode = TrustPinMode.STRICT
    )
    
    // Verify certificates
    val certificate: X509Certificate = // ...
    TrustPin.verifyBlocking("api.example.com", certificate)
}
```

### OkHttp Integration with SSLSocketFactory

The recommended integration pattern for OkHttp applications:

```kotlin
import cloud.trustpin.kotlin.sdk.TrustPin
import cloud.trustpin.kotlin.sdk.TrustPinMode
import cloud.trustpin.kotlin.sdk.ssl.TrustPinSSLSocketFactory
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class NetworkManager {
    
    private val httpClient by lazy {
        val sslSocketFactory = TrustPinSSLSocketFactory.create()
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .sslSocketFactory(sslSocketFactory, sslSocketFactory.trustManager())
            .build()
    }
    
    suspend fun initialize() {
        TrustPin.setup(
            organizationId = "your-org-id",
            projectId = "your-project-id",
            publicKey = "your-public-key",
            mode = TrustPinMode.STRICT
        )
    }
    
    suspend fun fetchData(): String {
        val request = Request.Builder()
            .url("https://api.example.com/data")
            .build()
            
        return httpClient.newCall(request).execute().use { response ->
            response.body?.string() ?: ""
        }
    }
}
```

### Custom Configuration URL Setup

For custom deployment scenarios or alternative configuration endpoints:

```kotlin
import cloud.trustpin.kotlin.sdk.TrustPin
import cloud.trustpin.kotlin.sdk.TrustPinMode
import java.net.URI

// Setup with custom configuration URL
suspend fun setupWithCustomURL() {
    TrustPin.setup(
        organizationId = "your-org-id",
        projectId = "your-project-id", 
        publicKey = "your-public-key",
        configurationURL = URI.create("https://custom.example.com/config/signed-payload.b64").toURL(),
        mode = TrustPinMode.STRICT
    )
}

// For blocking contexts, wrap with runBlocking
fun setupWithCustomURLBlocking() {
    runBlocking {
        TrustPin.setup(
            organizationId = "your-org-id",
            projectId = "your-project-id", 
            publicKey = "your-public-key",
            configurationURL = URI.create("https://custom.example.com/config/signed-payload.b64").toURL(),
            mode = TrustPinMode.STRICT
        )
    }
}
```

### Manual Certificate Verification

For custom networking stacks or certificate inspection:

```kotlin
import cloud.trustpin.kotlin.sdk.TrustPin
import cloud.trustpin.kotlin.sdk.TrustPinError
import java.security.cert.X509Certificate

// Verify an X.509 certificate for a specific domain
suspend fun verifyCertificate() {
    val domain = "api.example.com"
    val certificate: X509Certificate = // ... obtained from connection

    try {
        TrustPin.verify(domain = domain, certificate = certificate)
        println("✅ Certificate is valid and matches configured pins")
    } catch (e: TrustPinError.DomainNotRegistered) {
        println("⚠️ Domain not configured for pinning")
    } catch (e: TrustPinError.PinsMismatch) {
        println("❌ Certificate doesn't match any configured pins")
    } catch (e: TrustPinError) {
        println("💥 Verification failed: ${e.message}")
    }
}
```

### Advanced Integration with HttpsURLConnection

```kotlin
import cloud.trustpin.kotlin.sdk.TrustPin
import cloud.trustpin.kotlin.sdk.TrustPinLogLevel
import cloud.trustpin.kotlin.sdk.ssl.TrustPinSSLSocketFactory
import javax.net.ssl.HttpsURLConnection
import java.net.URL

class SecureNetworkClient {
    
    suspend fun initialize() {
        // Enable debug logging
        TrustPin.setLogLevel(TrustPinLogLevel.DEBUG)
        
        // Setup with permissive mode for staging
        TrustPin.setup(
            organizationId = "staging-org-id",
            projectId = "staging-project-id",
            publicKey = "staging-public-key",
            mode = TrustPinMode.PERMISSIVE
        )
    }
    
    fun configureGlobalSSL() {
        // Configure global SSL for HttpsURLConnection
        val sslSocketFactory = TrustPinSSLSocketFactory.create()
        HttpsURLConnection.setDefaultSSLSocketFactory(sslSocketFactory)
    }
    
    suspend fun makeSecureRequest(url: String): String {
        val connection = URL(url).openConnection() as HttpsURLConnection
        return connection.inputStream.bufferedReader().readText()
    }
}
```

---

## 🔧 Android Integration Examples

### Retrofit with OkHttp

```kotlin
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import cloud.trustpin.kotlin.sdk.ssl.TrustPinSSLSocketFactory
import java.util.concurrent.TimeUnit

class ApiClient {
    
    private val okHttpClient by lazy {
        val sslSocketFactory = TrustPinSSLSocketFactory.create()
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .sslSocketFactory(sslSocketFactory, sslSocketFactory.trustManager())
            .build()
    }
    
    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.example.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    suspend fun initialize() {
        TrustPin.setup(
            organizationId = "prod-org-id",
            projectId = "prod-project-id",
            publicKey = "prod-public-key",
            mode = TrustPinMode.STRICT
        )
    }
    
    fun <T> createService(serviceClass: Class<T>): T {
        return retrofit.create(serviceClass)
    }
}
```

### Ktor Client Integration

```kotlin
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.request.*
import cloud.trustpin.kotlin.sdk.ssl.TrustPinSSLSocketFactory

class KtorNetworkClient {
    
    private val httpClient by lazy {
        val sslSocketFactory = TrustPinSSLSocketFactory.create()
        HttpClient(OkHttp) {
            engine {
                preconfigured = OkHttpClient.Builder()
                    .sslSocketFactory(sslSocketFactory, sslSocketFactory.trustManager())
                    .build()
            }
        }
    }
    
    suspend fun initialize() {
        TrustPin.setup(
            organizationId = "your-org-id",
            projectId = "your-project-id",
            publicKey = "your-public-key",
            mode = TrustPinMode.STRICT
        )
    }
    
    suspend fun fetchData(): String {
        return httpClient.get("https://api.example.com/data")
    }
}
```

---

## 🎯 Pinning Modes Explained

| Mode | Behavior | Use Case |
|------|----------|----------|
| **`TrustPinMode.STRICT`** | ❌ Throws `TrustPinError.DomainNotRegistered` for unregistered domains | **Production environments** where all connections should be validated |
| **`TrustPinMode.PERMISSIVE`** | ✅ Allows unregistered domains to bypass pinning | **Development/Testing** or apps connecting to dynamic domains |

### When to Use Each Mode

#### Strict Mode (`TrustPinMode.STRICT`)
- ✅ **Production applications**
- ✅ **High-security environments**  
- ✅ **Known, fixed set of API endpoints**
- ✅ **Compliance requirements**

#### Permissive Mode (`TrustPinMode.PERMISSIVE`)
- ✅ **Development and staging**
- ✅ **Applications with dynamic/unknown endpoints**
- ✅ **Gradual migration to certificate pinning**
- ✅ **Third-party SDK integrations**

---

## 📊 Error Handling

TrustPin provides detailed error types for proper handling:

```kotlin
import cloud.trustpin.kotlin.sdk.TrustPinError

try {
    TrustPin.verify(domain = "api.example.com", certificate = cert)
} catch (e: TrustPinError.DomainNotRegistered) {
    // Domain not configured in TrustPin (only in strict mode)
    handleUnregisteredDomain()
} catch (e: TrustPinError.PinsMismatch) {
    // Certificate doesn't match configured pins - possible MITM
    handleSecurityThreat()
} catch (e: TrustPinError.AllPinsExpired) {
    // All pins for domain have expired
    handleExpiredPins()
} catch (e: TrustPinError.InvalidServerCert) {
    // Certificate format is invalid
    handleInvalidCertificate()
} catch (e: TrustPinError.InvalidProjectConfig) {
    // Setup parameters are invalid
    handleConfigurationError()
} catch (e: TrustPinError.ErrorFetchingPinningInfo) {
    // Network error fetching configuration
    handleNetworkError()
} catch (e: TrustPinError.ConfigurationValidationFailed) {
    // Configuration signature validation failed
    handleSignatureError()
}
```

---

## 🔍 Logging and Debugging

TrustPin provides comprehensive logging for debugging and monitoring:

```kotlin
import cloud.trustpin.kotlin.sdk.TrustPinLogLevel

// Set log level before setup
TrustPin.setLogLevel(TrustPinLogLevel.DEBUG)

// Available log levels:
// TrustPinLogLevel.NONE   - No logging
// TrustPinLogLevel.ERROR  - Errors only  
// TrustPinLogLevel.INFO   - Errors and informational messages
// TrustPinLogLevel.DEBUG  - All messages including debug information
```

---

## 🏗 Best Practices

### Security Recommendations

1. **Always use `TrustPinMode.STRICT` in production**
2. **Rotate pins before expiration**
3. **Monitor pin validation failures**
4. **Use HTTPS for all pinned domains**
5. **Keep public keys secure and version-controlled**

### Performance Optimization

1. **Reuse `OkHttpClient` instances** — don't rebuild the factory per request
2. **Use `ERROR` or `NONE` log levels** in production

### Development Workflow

1. **Start with `TrustPinMode.PERMISSIVE`** during development
2. **Test all endpoints** with pinning enabled
3. **Validate pin configurations** in staging
4. **Switch to `TrustPinMode.STRICT`** for production releases

---

## 🔧 Advanced Configuration

### Custom OkHttp Configuration

```kotlin
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import cloud.trustpin.kotlin.sdk.ssl.TrustPinSSLSocketFactory
import java.util.concurrent.TimeUnit

val sslSocketFactory = TrustPinSSLSocketFactory.create()
val okHttpClient = OkHttpClient.Builder()
    .connectTimeout(30, TimeUnit.SECONDS)
    .readTimeout(30, TimeUnit.SECONDS)
    .writeTimeout(30, TimeUnit.SECONDS)
    .sslSocketFactory(sslSocketFactory, sslSocketFactory.trustManager())
    .connectionPool(ConnectionPool(10, 5, TimeUnit.MINUTES))
    .dispatcher(Dispatcher().apply { maxRequests = 64 })
    .build()
```

### Error Recovery Strategies

```kotlin
import kotlinx.coroutines.delay

suspend fun performNetworkRequest(): String? {
    return try {
        secureNetworkRequest()
    } catch (e: TrustPinError.DomainNotRegistered) {
        // Log security event but continue in permissive mode
        logger.warning("Unregistered domain accessed")
        fallbackNetworkRequest()
    } catch (e: TrustPinError.PinsMismatch) {
        // This is a serious security issue - do not retry
        logger.critical("Certificate pinning failed - possible MITM attack")
        throw SecurityException("Potential MITM attack detected")
    } catch (e: TrustPinError.ErrorFetchingPinningInfo) {
        // Retry with exponential backoff
        delay(1000)
        retryNetworkRequest()
    }
}
```

### Coroutine Integration

```kotlin
import kotlinx.coroutines.*

class NetworkService(private val scope: CoroutineScope) {
    
    fun initialize() {
        scope.launch {
            try {
                TrustPin.setup(
                    organizationId = "org-id",
                    projectId = "project-id",
                    publicKey = "public-key",
                    mode = TrustPinMode.STRICT
                )
                logger.info("TrustPin initialized successfully")
            } catch (e: Exception) {
                logger.error("TrustPin initialization failed", e)
            }
        }
    }
    
    fun validateCertificateAsync(domain: String, certificate: X509Certificate) {
        scope.launch {
            try {
                TrustPin.verify(domain, certificate)
                onValidationSuccess(domain)
            } catch (e: TrustPinError) {
                onValidationFailure(domain, e)
            }
        }
    }
}
```

---

## 📚 API Reference

### Core API

#### `TrustPin` Object
Main SDK interface for certificate pinning operations with **dual API design**.

```kotlin
object TrustPin {
    // Suspend API (recommended for Kotlin coroutines)
    
    // Initialize SDK with project credentials
    suspend fun setup(
        organizationId: String,
        projectId: String, 
        publicKey: String,
        mode: TrustPinMode = TrustPinMode.STRICT
    )
    
    // Initialize SDK with custom configuration URL
    suspend fun setup(
        organizationId: String,
        projectId: String, 
        publicKey: String,
        configurationURL: URL,
        mode: TrustPinMode = TrustPinMode.STRICT
    )
    
    // Verify certificate against configured pins
    suspend fun verify(domain: String, certificate: X509Certificate)

    // Verify a PEM-encoded certificate against configured pins
    suspend fun verify(domain: String, certificate: String)

    // Verify the leaf of a server-presented chain. Empty chains and chains exceeding
    // the SDK's internal length limit are rejected before any pin comparison.
    suspend fun verify(domain: String, chain: List<X509Certificate>)

    // Blocking API (for Java interop and non-coroutine contexts)
    
    // Initialize SDK with project credentials (blocking)
    fun setupBlocking(
        organizationId: String,
        projectId: String, 
        publicKey: String,
        mode: TrustPinMode = TrustPinMode.STRICT
    )
    
    // Initialize SDK with custom configuration URL (blocking)
    fun setupBlocking(
        organizationId: String,
        projectId: String, 
        publicKey: String,
        configurationURL: URL,
        mode: TrustPinMode = TrustPinMode.STRICT
    )
    
    // Verify certificate against configured pins (blocking)
    fun verifyBlocking(domain: String, certificate: X509Certificate)

    // Verify a PEM-encoded certificate against configured pins (blocking)
    fun verifyBlocking(domain: String, certificate: String)

    // Verify the leaf of a server-presented chain (blocking)
    fun verifyBlocking(domain: String, chain: List<X509Certificate>)

    // Configure logging verbosity (non-suspend)
    fun setLogLevel(level: TrustPinLogLevel)
}
```

#### `TrustPinMode` Enum
Controls validation behavior for unregistered domains.

```kotlin
enum class TrustPinMode {
    STRICT,      // Throws error for unregistered domains (production)
    PERMISSIVE   // Allows unregistered domains to bypass pinning (development)
}
```

#### `TrustPinLogLevel` Enum
Configures SDK logging verbosity.

```kotlin
enum class TrustPinLogLevel(val value: Int) {
    NONE(0),     // No logging
    ERROR(1),    // Errors only
    INFO(2),     // Errors and info
    DEBUG(3)     // All messages including debug
}
```

#### `TrustPinError` Sealed Class
Detailed error types for different failure scenarios.

```kotlin
sealed class TrustPinError : Exception() {
    object InvalidProjectConfig : TrustPinError()          // Invalid setup parameters
    object SetupInProgress : TrustPinError()               // Another setup() call is in flight; retry later
    object LockTimeout : TrustPinError()                   // Internal state lock could not be acquired (pathology)
    object NotInitialized : TrustPinError()                // verify() called before setup() completed
    object ErrorFetchingPinningInfo : TrustPinError()      // Configuration fetch failed
    object InvalidServerCert : TrustPinError()             // Invalid certificate format
    object PinsMismatch : TrustPinError()                  // Certificate doesn't match pins
    object AllPinsExpired : TrustPinError()                // All pins have expired
    object ConfigurationValidationFailed : TrustPinError() // Configuration validation failed
    object DomainNotRegistered : TrustPinError()           // Domain not configured (strict mode)
}
```

### Certificate Pinning Integration API

#### `TrustPinSSLSocketFactory` Class
Context-aware SSLSocketFactory with built-in TrustPin certificate validation.

```kotlin
class TrustPinSSLSocketFactory : SSLSocketFactory() {
    companion object {
        fun create(): TrustPinSSLSocketFactory
    }
    
    fun trustManager(): X509TrustManager  // Get the associated TrustManager
    
    // Standard SSLSocketFactory methods
    override fun createSocket(): Socket
    override fun createSocket(host: String, port: Int): Socket
    override fun createSocket(host: String, port: Int, localHost: InetAddress, localPort: Int): Socket
    override fun createSocket(host: InetAddress, port: Int): Socket
    override fun createSocket(address: InetAddress, port: Int, localAddress: InetAddress, localPort: Int): Socket
    override fun createSocket(s: Socket, host: String, port: Int, autoClose: Boolean): Socket
    override fun getDefaultCipherSuites(): Array<String>
    override fun getSupportedCipherSuites(): Array<String>
}
```

**Usage Example:**
```kotlin
val sslSocketFactory = TrustPinSSLSocketFactory.create()
val client = OkHttpClient.Builder()
    .sslSocketFactory(sslSocketFactory, sslSocketFactory.trustManager())
    .build()
```

The TrustPinSSLSocketFactory automatically handles trust management internally and provides context-aware certificate validation without requiring manual hostname management.

---

## 🔄 Choosing the Right API

TrustPin provides two API styles to fit different development contexts:

### API Selection Guide

**Suspend API (Recommended for Kotlin):**
```kotlin
// ✅ For Kotlin coroutine contexts
suspend fun initialize() {
    TrustPin.setup(
        organizationId = "your-org-id",
        projectId = "your-project-id",
        publicKey = "your-public-key",
        mode = TrustPinMode.STRICT
    )
}

suspend fun verifyCert(domain: String, cert: X509Certificate) {
    TrustPin.verify(domain, cert)
}
```

**Blocking API (For Java and Non-Coroutine Contexts):**
```kotlin
// ✅ For Java interop and blocking Kotlin contexts
fun initialize() {
    TrustPin.setupBlocking(
        organizationId = "your-org-id",
        projectId = "your-project-id",
        publicKey = "your-public-key",
        mode = TrustPinMode.STRICT
    )
}

fun verifyCert(domain: String, cert: X509Certificate) {
    TrustPin.verifyBlocking(domain, cert)
}
```

### When to Use Which API

#### 1. Use Suspend API When:
- **Kotlin coroutine contexts**: `lifecycleScope`, `viewModelScope`, `coroutineScope`
- **Async operations**: Network calls, file I/O, or other async work
- **Modern Kotlin applications**: Leveraging structured concurrency

```kotlin
class NetworkService {
    // ✅ Coroutine-based applications
    suspend fun initializeInCoroutine() {
        TrustPin.setup(/* ... */) // Suspend API
    }
    
    // ✅ Android lifecycle-aware
    fun initializeInAndroid() {
        lifecycleScope.launch {
            TrustPin.setup(/* ... */) // Suspend API in coroutine
        }
    }
}
```

#### 2. Use Blocking API When:
- **Java interoperability**: Called from Java code
- **Legacy code**: Non-coroutine contexts that can't be easily migrated
- **Synchronous contexts**: When you need synchronous behavior

```kotlin
class LegacyService {
    // ✅ Java interop or legacy blocking contexts
    fun initializeBlocking() {
        TrustPin.setupBlocking(/* ... */) // Blocking API
    }
    
    // ✅ When called from Java
    @JvmStatic
    fun initializeFromJava() {
        TrustPin.setupBlocking(/* ... */) // Java-friendly
    }
}
```

### API Comparison

| Feature | Suspend API | Blocking API |
|---------|-------------|--------------|
| **Best for** | Kotlin coroutines | Java interop, legacy code |
| **Performance** | Non-blocking, efficient | Blocks calling thread |
| **Cancellation** | Supports coroutine cancellation | No built-in cancellation |
| **Thread Safety** | Coroutine-safe | Thread-safe via runBlocking |
| **Usage Context** | `suspend` functions, coroutine builders | Any function context |

### Benefits of Dual API Design

- **Flexibility**: Choose the right API for your context
- **Java Interoperability**: Full Java support with blocking API  
- **Modern Kotlin**: Optimal coroutine support with suspend API
- **Migration Friendly**: Both APIs available without deprecation
- **Performance Options**: Non-blocking or blocking as needed

---

## 🧪 Testing

### Unit Testing with TrustPin

```kotlin
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.Before

class NetworkTest {
    
    @Test
    fun `test secure network request`() = runTest {
        // Use permissive mode for testing
        TrustPin.setup(
            organizationId = "test-org",
            projectId = "test-project",
            publicKey = "test-key",
            mode = TrustPinMode.PERMISSIVE
        )
        
        val networkClient = SecureNetworkClient()
        val result = networkClient.fetchData()
        
        assert(result.isNotEmpty())
    }
}
```

### Integration Testing

```kotlin
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test

class TrustPinIntegrationTest {
    private lateinit var mockServer: MockWebServer
    
    @Before
    fun setUp() {
        mockServer = MockWebServer()
        mockServer.start()
    }
    
    @After
    fun tearDown() {
        mockServer.shutdown()
    }
    
    @Test
    fun `test certificate pinning with mock server`() = runTest {
        TrustPin.setup(
            organizationId = "test-org",
            projectId = "test-project",
            publicKey = "test-key",
            mode = TrustPinMode.PERMISSIVE // Allow mock server
        )
        
        // Test your networking code with mockServer.url("/")
    }
}
```

---

## 🐛 Troubleshooting

### Common Issues

#### **Setup Fails with `InvalidProjectConfig`**
- ✅ Verify organization ID, project ID, and public key are correct
- ✅ Check for extra whitespace or newlines in credentials
- ✅ Ensure public key is properly base64-encoded

#### **Certificate Verification Fails**
- ✅ Confirm domain is registered in TrustPin dashboard
- ✅ Check certificate format (must be valid X.509)
- ✅ Verify pins haven't expired
- ✅ Test with `TrustPinMode.PERMISSIVE` first

#### **OkHttp Integration Issues**
- ✅ Ensure TrustPin is initialized before creating OkHttpClient
- ✅ Use `TrustPinSSLSocketFactory.create()` with OkHttp's `sslSocketFactory()` method
- ✅ Always pass both SSLSocketFactory and TrustManager to OkHttp
- ✅ Verify coroutine context for suspend functions

### Debug Steps

1. **Enable debug logging**: `TrustPin.setLogLevel(TrustPinLogLevel.DEBUG)`
2. **Test with permissive mode** first
3. **Verify credentials** in TrustPin dashboard
4. **Check network connectivity** to `cdn.trustpin.cloud`
5. **Inspect certificate details** with openssl or browser tools

---

## 📖 Documentation

- **API Reference**: Complete API documentation above
- **Documentation**: [Full API reference with examples](https://docs.trustpin.cloud/sdk/kotlin)
- **TrustPin Dashboard**: [Configure domains and pins](https://app.trustpin.cloud)

---

## 📝 License

This project is licensed under the TrustPin Binary License Agreement - see the [LICENSE](LICENSE) file for details.

**Commercial License**: For enterprise licensing or custom agreements, contact [contact@trustpin.cloud](mailto:contact@trustpin.cloud)

**Attribution Required**: When using this software, you must display "Uses TrustPin™ technology – https://trustpin.cloud" in your application.

---

## 🤝 Support & Feedback

For questions and support:

- 📧 **Email**: [support@trustpin.cloud](mailto:support@trustpin.cloud)
- 🌐 **Website**: [https://trustpin.cloud](https://trustpin.cloud)
- 📋 **Issues**: For SDK-related issues, please contact support

---

*Built with ❤️ by the TrustPin team*