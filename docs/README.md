# TrustPin Kotlin SDK Documentation

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.25%2B-purple.svg)](https://kotlinlang.org)
[![Android](https://img.shields.io/badge/Android-API%2021%2B-green.svg)](https://developer.android.com)
[![JVM](https://img.shields.io/badge/JVM-11%2B-blue.svg)](https://adoptopenjdk.net)

[TrustPin](https://trustpin.cloud/) is a modern, lightweight, and secure Kotlin library designed to enforce **SSL Certificate Pinning** for Android and JVM applications. Built with Kotlin Coroutines and following OWASP security recommendations, TrustPin prevents man-in-the-middle (MITM) attacks by ensuring server authenticity at the TLS level.

---

## 🚀 Key Features

- ✅ **Kotlin Multiplatform** - Shared codebase for Android and JVM platforms
- ✅ **Flexible Pinning Modes** - Strict validation or permissive mode for development
- ✅ **Multiple Hash Algorithms** - SHA-256 and SHA-512 certificate validation
- ✅ **Signed Configuration** - Cryptographically signed pinning configurations
- ✅ **Android/JVM Integrations** - Built-in TrustManager and SSLSocketFactory support
- ✅ **Intelligent Caching** - 10-minute configuration cache with stale fallback
- ✅ **Comprehensive Logging** - Configurable log levels for debugging and monitoring
- ✅ **Thread-Safe** - Built with coroutines and concurrent-safe operations
- ✅ **Enhanced Security** - Advanced signature verification with multiple authentication methods

---

## 📋 Platform Requirements

| Platform | Minimum Version | Notes |
|----------|----------------|-------|
| Android | API 25 (7.1+) | Full feature support |
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
    <version>4.1.0</version>
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

> 💡 **Find your credentials** in the [TrustPin Dashboard](https://trustpin.cloud/dashboard)
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

### Example Debug Output

```
[14:30:15] [DEBUG] Starting certificate verification for domain: api.example.com
[14:30:15] [DEBUG] Sanitized domain: api.example.com
[14:30:15] [INFO] Using cached configuration
[14:30:15] [DEBUG] Found domain configuration with 2 pins
[14:30:15] [DEBUG] Certificate hash matches sha256 pin for domain api.example.com
[14:30:15] [INFO] Valid pin found for api.example.com
```

---

## 🏗 Best Practices

### Setup and Initialization

1. **Call `TrustPin.setup()` once** during app initialization (typically in `Application.onCreate()`)
2. **Handle setup errors gracefully** - don't block app launch if TrustPin fails
3. **Set log level before setup** for complete logging coverage
4. **Use coroutines** for setup in Android lifecycle-aware components

### Security Recommendations

1. **Always use `TrustPinMode.STRICT` in production**
2. **Rotate pins before expiration**
3. **Monitor pin validation failures**
4. **Use HTTPS for all pinned domains**
5. **Keep public keys secure and version-controlled**

### Performance Optimization

1. **Cache TrustPin configuration** (handled automatically)
2. **Reuse OkHttpClient instances** with TrustPin SSLSocketFactory
3. **Use appropriate log levels** (`ERROR` or `NONE` in production)
4. **Initialize early** to avoid setup delays during first network requests

### Development Workflow

1. **Start with `TrustPinMode.PERMISSIVE`** during development
2. **Test all endpoints** with pinning enabled
3. **Validate pin configurations** in staging
4. **Switch to `TrustPinMode.STRICT`** for production releases
5. **Use debug logging** to troubleshoot pinning issues

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
```

---

## 🧪 Testing

### Unit Testing with TrustPin

```kotlin
import kotlinx.coroutines.test.runTest
import org.junit.Test

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

- **API Reference**: [Full KDoc Documentation](index.html)
- **Documentation site**: [Documentation site](https://docs.trustpin.cloud)
- **TrustPin Dashboard**: [Configure domains and pins](https://trustpin.cloud/dashboard)
---

## 📝 License

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
