# TrustPin Kotlin SDK

[![Kotlin](https://img.shields.io/badge/Kotlin-2.3.0%2B-purple.svg)](https://kotlinlang.org)
[![Android](https://img.shields.io/badge/Android-API%2025%2B-green.svg)](https://developer.android.com)
[![JVM](https://img.shields.io/badge/JVM-11%2B-blue.svg)](https://adoptopenjdk.net)
[![License](https://img.shields.io/badge/License-TrustPin-green.svg)](LICENSE)

TrustPin is a modern, lightweight, and secure Kotlin library for **SSL Certificate Pinning** in Android applications, with a hardened JVM JAR available by request for server and desktop customers. Built with Kotlin Coroutines and following OWASP recommendations, it prevents man-in-the-middle (MITM) attacks by validating server authenticity at the TLS level.

Available on Maven Central for Android as an AAR: [`cloud.trustpin:kotlin-sdk`](https://central.sonatype.com/artifact/cloud.trustpin/kotlin-sdk).

JVM customers should request access to the hardened JVM JAR via email at [support@trustpin.cloud](mailto:support@trustpin.cloud).

## 🚀 Key Features

- ✅ **Android AAR on Maven Central** with bundled R8/ProGuard consumer rules
- ✅ **Hardened JVM JAR** available by request for desktop / server customers
- ✅ **Strict or permissive pinning** — enforce in production, relax during development
- ✅ **Drop-in integration** — `TrustManager` and `SSLSocketFactory` for OkHttp / `HttpsURLConnection`
- ✅ **Managed configuration** delivered from TrustPin and cached locally
- ✅ **Coroutine- and Java-friendly APIs**
- ✅ **Minimal dependencies** — Kotlin stdlib + coroutines only

---

## 📑 Table of Contents

- [Platform Requirements](#-platform-requirements)
- [Installation](#-installation)
- [Quick Setup](#-quick-setup)
  - [Android — `fromAssets(context)`](#1a-via-trustpinjson-shipped-in-assets-recommended)
  - [Android — `withAndroidStorage(context)`](#1b-programmatic-via-withandroidstoragecontext)
  - [JVM](#2-configure-on-jvm)
- [`trustpin.json` reference (Android)](#-trustpinjson-reference-android)
- [Usage Examples](#-usage-examples)
  - [OkHttp integration](#okhttp-integration)
  - [Retrofit](#retrofit)
  - [Ktor](#ktor)
  - [Custom configuration URL](#custom-configuration-url)
  - [Manual certificate verification](#manual-certificate-verification)
  - [HttpsURLConnection](#httpsurlconnection)
- [Pinning Modes](#-pinning-modes)
- [Error Handling](#-error-handling)
- [Logging](#-logging)
- [Best Practices](#-best-practices)
- [API Reference](#-api-reference)
- [Suspend vs Blocking API](#-suspend-vs-blocking-api)
- [Testing](#-testing)
- [Troubleshooting](#-troubleshooting)
- [Documentation, License & Support](#-documentation)

---

## 📋 Platform Requirements

| Platform | Minimum Version | Notes |
|----------|-----------------|-------|
| Android  | API 25+         | Full feature support |
| JVM      | Java 11+        | Hardened JAR available by request |
| Kotlin   | 2.3.0+          | Built with Kotlin 2.3.0 |

---

## 📦 Installation

### Android — Gradle (Kotlin DSL)

```kotlin
dependencies {
    implementation("cloud.trustpin:kotlin-sdk:6.0.0")
}
```

### Android — Gradle (Groovy)

```groovy
dependencies {
    implementation 'cloud.trustpin:kotlin-sdk:6.0.0'
}
```

### Android — Maven

```xml
<dependency>
    <groupId>cloud.trustpin</groupId>
    <artifactId>kotlin-sdk</artifactId>
    <version>1.2.0</version>
</dependency>
```

### JVM

The Maven Central artifact is an Android AAR. For JVM / server / desktop use, request the hardened JVM JAR at [support@trustpin.cloud](mailto:support@trustpin.cloud).

### R8 / ProGuard

The Android AAR ships consumer rules automatically. Apps should not need to add TrustPin-specific keep rules. The SDK keeps the documented public API stable while obfuscating internal and private implementation details.

---

## 🔧 Quick Setup

### Configure on Android (recommended)

On Android, setup **must go through a `Context`-aware entry point** so the SDK can persist its integrity-check state under `Context.getNoBackupFilesDir()`. There are two equivalent recommended paths.

> ⚠️ **Do not use the contextless `TrustPinConfiguration(orgId, projId, publicKey)` constructor on Android.** It works, but the SDK has no `Context` and falls back to an in-memory integrity-check backend that does **not survive a process restart**. A one-shot `info` log is emitted so the regression is operator-visible.

> ⚠️ **Decorated configurations are single-use on Android.** A `TrustPinConfiguration` produced by `fromAssets(context)` or `.withAndroidStorage(context)` MUST be passed to **exactly one** `TrustPin.setup` call. The Android storage backend is consumed on first use; reusing the same configuration object silently falls back to in-memory storage on the second instance. Build a fresh decorated configuration for each TrustPin instance:
>
> ```kotlin
> // ✓ Correct
> TrustPin.instance("payments").setup(TrustPinConfiguration.fromAssets(context))
> TrustPin.instance("analytics").setup(TrustPinConfiguration.fromAssets(context))
>
> // ✗ Wrong — second setup silently loses Android persistence
> val shared = TrustPinConfiguration.fromAssets(context)
> TrustPin.default.setup(shared)
> TrustPin.instance("payments").setup(shared)
> ```
>
> The restriction does not apply on JVM.

#### 1a. Via `trustpin.json` shipped in `assets/` (recommended)

Ship a `trustpin.json` in your app's `assets/` directory; the loader reads it and **automatically attaches the Android-persistent storage backend** for you.

```kotlin
import android.content.Context
import cloud.trustpin.kotlin.sdk.TrustPin
import cloud.trustpin.kotlin.sdk.TrustPinConfiguration
import cloud.trustpin.kotlin.sdk.fromAssets

suspend fun initializeTrustPin(context: Context) {
    // fromAssets(context) reads assets/trustpin.json AND calls
    // .withAndroidStorage(context) internally.
    TrustPin.setup(TrustPinConfiguration.fromAssets(context))
}
```

From Java:

```java
import cloud.trustpin.kotlin.sdk.TrustPin;
import cloud.trustpin.kotlin.sdk.TrustPinConfiguration;
import cloud.trustpin.kotlin.sdk.TrustPinConfigurationAssets;

void initializeTrustPin(Context context) {
    TrustPinConfiguration config = TrustPinConfigurationAssets.fromAssets(context);
    TrustPin.getDefault().setupBlocking(config);
}
```

See [`trustpin.json` reference](#-trustpinjson-reference-android) for the file format and per-flavor / per-build-type overrides.

#### 1b. Programmatic, via `withAndroidStorage(context)`

When you can't ship a bundled JSON (credentials come from runtime config, A/B-test routing, MDM channel, etc.), build the configuration programmatically and **chain `.withAndroidStorage(context)`** to attach the persistent storage backend.

```kotlin
import android.content.Context
import cloud.trustpin.kotlin.sdk.TrustPin
import cloud.trustpin.kotlin.sdk.TrustPinConfiguration
import cloud.trustpin.kotlin.sdk.TrustPinMode
import cloud.trustpin.kotlin.sdk.withAndroidStorage

suspend fun initializeTrustPin(context: Context) {
    val config = TrustPinConfiguration(
        organizationId = "your-org-id",
        projectId      = "your-project-id",
        publicKey      = "your-base64-public-key",
        mode           = TrustPinMode.STRICT,
    ).withAndroidStorage(context)          // ← required on Android
    TrustPin.setup(config)
}
```

`withAndroidStorage(context)` captures `context.applicationContext` (no Activity leak), registers the storage backend in an SDK-internal binding, and returns the same configuration so the call chains cleanly.

### 2. Configure on JVM

JVM customers (server-side, desktop, embedded) construct the configuration directly — there is no `Context`, and the SDK keeps integrity-check state in-memory for the lifetime of the JVM process.

```kotlin
import cloud.trustpin.kotlin.sdk.TrustPin
import cloud.trustpin.kotlin.sdk.TrustPinConfiguration
import cloud.trustpin.kotlin.sdk.TrustPinMode

suspend fun initializeTrustPin() {
    TrustPin.setup(
        TrustPinConfiguration(
            organizationId = "your-org-id",
            projectId      = "your-project-id",
            publicKey      = "your-base64-public-key",
            mode           = TrustPinMode.STRICT,
        )
    )
}
```

> 💡 Find your credentials in the [TrustPin Dashboard](https://app.trustpin.cloud).
>
> ⚙️ TrustPin provides both **suspend** functions (recommended for Kotlin coroutines) and **blocking** functions (for Java interop and non-coroutine contexts). See [Suspend vs Blocking API](#-suspend-vs-blocking-api).

---

## 📄 `trustpin.json` reference (Android)

The `default` TrustPin instance can be configured from a JSON file in your application's `assets/` directory — same pattern as `google-services.json`. The loader reads the file and automatically chains `.withAndroidStorage(context)` so the returned configuration is production-ready.

### 1. Add the file

`app/src/main/assets/trustpin.json`:

```json
{
  "organization_id": "your-org-id",
  "project_id": "your-project-id",
  "public_key": "MFkwEwYH...",
  "mode": "strict",
  "configuration_url": "https://custom.example.com/config/signed.b64"
}
```

| Field | Required | Notes |
|---|---|---|
| `organization_id` | ✅ | Organization identifier |
| `project_id` | ✅ | Project identifier |
| `public_key` | ✅ | Base64-encoded public key |
| `mode` | ❌ | `"strict"` (default) or `"permissive"` |
| `configuration_url` | ❌ | Optional custom source (HTTPS only) |

Unknown top-level keys are ignored for forward compatibility.

### 2. Load it at setup time

See [§1a above](#1a-via-trustpinjson-shipped-in-assets-recommended). A custom filename can be supplied:

```kotlin
TrustPinConfiguration.fromAssets(context, fileName = "my-config.json")
```

### 3. Per-flavor / per-build-type configuration

Per-variant overrides use Android's standard asset-merging — no Gradle plugin required:

```
app/src/main/assets/trustpin.json    ← default
app/src/debug/assets/trustpin.json   ← overrides for the `debug` build type
app/src/ff/assets/trustpin.json      ← overrides for the `ff` product flavor
```

### Notes

- **Default instance only.** File-based setup configures `TrustPin.default`. Named instances (`TrustPin.instance(id)`) continue to use programmatic `TrustPinConfiguration(...)`.
- **Android only.** The hardened JVM JAR does not include `fromAssets`.
- **The file is bundled into the APK** — `public_key` is the verification key for signed pin payloads, not key material.
- **Failure modes** (file missing, malformed JSON, invalid `mode`, non-HTTPS `configuration_url`, etc.) all surface as `TrustPinError.InvalidProjectConfig`. A descriptive reason is written to logcat under the `[TrustPin]` tag.
- **No log-level field** — log verbosity stays under programmatic control via `TrustPin.setLogLevel(...)`.

---

## 🛠 Usage Examples

### OkHttp integration

The recommended integration pattern for OkHttp applications:

```kotlin
import cloud.trustpin.kotlin.sdk.TrustPin
import cloud.trustpin.kotlin.sdk.ssl.TrustPinSSLSocketFactory
import okhttp3.OkHttpClient
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

    suspend fun fetchData(url: String): String =
        httpClient.newCall(Request.Builder().url(url).build()).execute().use {
            it.body?.string() ?: ""
        }
}
```

### Retrofit

```kotlin
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

val sslSocketFactory = TrustPinSSLSocketFactory.create()
val okHttpClient = OkHttpClient.Builder()
    .sslSocketFactory(sslSocketFactory, sslSocketFactory.trustManager())
    .build()

val retrofit = Retrofit.Builder()
    .baseUrl("https://api.example.com/")
    .client(okHttpClient)
    .addConverterFactory(GsonConverterFactory.create())
    .build()
```

### Ktor

```kotlin
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*

val sslSocketFactory = TrustPinSSLSocketFactory.create()
val client = HttpClient(OkHttp) {
    engine {
        preconfigured = OkHttpClient.Builder()
            .sslSocketFactory(sslSocketFactory, sslSocketFactory.trustManager())
            .build()
    }
}
```

### Custom configuration URL

For custom deployment scenarios or alternative configuration endpoints:

```kotlin
import java.net.URI

TrustPin.setup(
    organizationId   = "your-org-id",
    projectId        = "your-project-id",
    publicKey        = "your-public-key",
    configurationURL = URI.create("https://custom.example.com/config/signed-payload.b64").toURL(),
    mode             = TrustPinMode.STRICT
)
```

### Manual certificate verification

For custom networking stacks or certificate inspection:

```kotlin
import cloud.trustpin.kotlin.sdk.TrustPin
import cloud.trustpin.kotlin.sdk.TrustPinError
import java.security.cert.X509Certificate

suspend fun verifyCertificate(domain: String, certificate: X509Certificate) {
    try {
        TrustPin.verify(domain, certificate)
    } catch (e: TrustPinError.DomainNotRegistered) {
        // Strict mode only: domain is not in your pinning configuration.
    } catch (e: TrustPinError.PinsMismatch) {
        // Certificate doesn't match any configured pin — potential MITM.
    }
}
```

### HttpsURLConnection

```kotlin
import javax.net.ssl.HttpsURLConnection

val sslSocketFactory = TrustPinSSLSocketFactory.create()
HttpsURLConnection.setDefaultSSLSocketFactory(sslSocketFactory)

val connection = URL("https://api.example.com").openConnection() as HttpsURLConnection
val body = connection.inputStream.bufferedReader().readText()
```

---

## 🎯 Pinning Modes

| Mode | Behaviour |
|------|-----------|
| **`TrustPinMode.STRICT`** | Throws `TrustPinError.DomainNotRegistered` for unregistered domains. Recommended for production. |
| **`TrustPinMode.PERMISSIVE`** | Allows unregistered domains to bypass pinning. For development / dynamic endpoints. |

---

## 📊 Error Handling

```kotlin
import cloud.trustpin.kotlin.sdk.TrustPinError

try {
    TrustPin.verify(domain = "api.example.com", certificate = cert)
} catch (e: TrustPinError.DomainNotRegistered) {
    // Strict mode only: domain is not in your pinning configuration.
} catch (e: TrustPinError.PinsMismatch) {
    // Certificate doesn't match configured pins — potential MITM.
} catch (e: TrustPinError.AllPinsExpired) {
    // All pins for the domain have expired.
} catch (e: TrustPinError.InvalidServerCert) {
    // Certificate format is invalid.
} catch (e: TrustPinError.InvalidProjectConfig) {
    // Setup parameters are invalid.
} catch (e: TrustPinError.ErrorFetchingPinningInfo) {
    // Transient network / configuration fetch problem.
} catch (e: TrustPinError.ConfigurationValidationFailed) {
    // Configuration signature validation failed.
} catch (e: TrustPinError.ConfigIntegrityError) {
    // Configuration failed an integrity check — hard stop.
}
```

---

## 🔍 Logging

```kotlin
TrustPin.setLogLevel(TrustPinLogLevel.DEBUG)
// Levels: NONE, ERROR, INFO, DEBUG
```

Set the log level before `setup` for complete logging coverage. Use `ERROR` or `NONE` in production.

---

## 🏗 Best Practices

### Security
- Always use `TrustPinMode.STRICT` in production.
- Rotate pins before expiration; monitor pin-validation failures.
- Use HTTPS for all pinned domains.

### Setup
- Call `TrustPin.setup()` exactly once at app launch.
- Treat setup errors as hard stops — don't continue to construct an unpinned `OkHttpClient`.
- Gate on `awaitConfiguration(timeout)` before constructing pinned clients when your app must not run without a validated payload.

### Development workflow
- Start in `TrustPinMode.PERMISSIVE` during development; switch to `STRICT` for production.
- Use `DEBUG` log level when troubleshooting; revert to `ERROR` or `NONE` for release builds.
- On Android, release builds run only on stock production devices; use debug-built variants for development, CI, and emulators. If you need a release-shaped APK on an emulator for QA, declare a non-`release` buildType with `isDebuggable = true` (e.g. a `releaseStaging` variant). Never upload a debuggable APK to Play.
- To support end users on community or custom OS builds — where release builds would otherwise fail with `TrustPinError.UnsupportedDevice` — declare the following inside `<application>` in your manifest:

  ```xml
  <meta-data
      android:name="cloud.trustpin.android.allowNonOemImages"
      android:value="true" />
  ```

  This is a deliberate availability trade-off: it relaxes the SDK's default device policy for production installs so such devices are supported. Emulators remain unsupported for release builds regardless of this opt-out.

---

## 📚 API Reference

### Core API

#### `TrustPin`

Main SDK interface with **dual API design**.

```kotlin
class TrustPin {
    companion object {
        val default: TrustPin
        // id must match [a-zA-Z0-9._-]+ — reverse-DNS ("com.example.app")
        // and the bare "default" reserved name are the two notable cases.
        fun instance(id: String): TrustPin
    }

    // ── Suspend API (recommended for Kotlin) ──────────────────────────────

    suspend fun setup(configuration: TrustPinConfiguration)

    // Fail-closed gate: waits for the configuration to be fetched, signature-
    // verified, and accepted by the SDK's integrity check.
    suspend fun awaitConfiguration(timeout: Long = 30_000)
    fun awaitConfigurationBlocking(timeout: Long = 30_000)

    // Synchronous payload-state read — never triggers a fetch.
    val isConfigurationLoaded: Boolean

    suspend fun verify(domain: String,
                       certificate: X509Certificate,
                       timeout: Long = 30_000)
    suspend fun verify(domain: String,
                       certificate: String,
                       timeout: Long = 30_000)
    suspend fun verify(domain: String,
                       chain: List<X509Certificate>,
                       timeout: Long = 30_000)

    suspend fun fetchCertificate(host: String,
                                 port: Int = 443,
                                 timeout: Long = 30_000): String

    // ── Blocking API (for Java interop) ───────────────────────────────────

    fun setupBlocking(configuration: TrustPinConfiguration)
    fun verifyBlocking(domain: String, certificate: X509Certificate, timeout: Long = 30_000)
    fun verifyBlocking(domain: String, certificate: String, timeout: Long = 30_000)
    fun verifyBlocking(domain: String, chain: List<X509Certificate>, timeout: Long = 30_000)
    fun fetchCertificateBlocking(host: String, port: Int = 443, timeout: Long = 30_000): String

    // ── SSL integration ───────────────────────────────────────────────────

    fun makeSSLSocketFactory(): SSLSocketFactory
    fun makeTrustManager(): X509TrustManager

    // ── Logging ───────────────────────────────────────────────────────────

    fun setLogLevel(level: TrustPinLogLevel)
}
```

`timeout` is in milliseconds, default 30 000, clamped to `[10 000, 120 000]`.

#### `TrustPinConfiguration`

```kotlin
data class TrustPinConfiguration(
    val organizationId: String,
    val projectId: String,
    val publicKey: String,
    val mode: TrustPinMode = TrustPinMode.STRICT,
    val configurationURL: URL? = null
)

// Android-only extensions
fun TrustPinConfiguration.withAndroidStorage(context: Context): TrustPinConfiguration
fun TrustPinConfiguration.Companion.fromAssets(
    context: Context,
    fileName: String = "trustpin.json"
): TrustPinConfiguration
```

#### `TrustPinMode`

```kotlin
enum class TrustPinMode {
    STRICT,      // Throws for unregistered domains (production)
    PERMISSIVE   // Allows unregistered domains to bypass pinning (development)
}
```

#### `TrustPinLogLevel`

```kotlin
enum class TrustPinLogLevel(val value: Int) {
    NONE(0), ERROR(1), INFO(2), DEBUG(3)
}
```

#### `TrustPinError`

```kotlin
sealed class TrustPinError : Exception() {
    object InvalidProjectConfig          : TrustPinError()
    object SetupInProgress               : TrustPinError()
    object LockTimeout                   : TrustPinError()
    object NotInitialized                : TrustPinError()
    object ErrorFetchingPinningInfo      : TrustPinError()
    object InvalidServerCert             : TrustPinError()
    object PinsMismatch                  : TrustPinError()
    object AllPinsExpired                : TrustPinError()
    object ConfigurationValidationFailed : TrustPinError()
    object DomainNotRegistered           : TrustPinError()
    object ConfigIntegrityError          : TrustPinError()
}
```

#### `TrustPinSSLSocketFactory`

Context-aware `SSLSocketFactory` with built-in TrustPin certificate validation.

```kotlin
class TrustPinSSLSocketFactory : SSLSocketFactory() {
    companion object {
        fun create(): TrustPinSSLSocketFactory
    }

    fun trustManager(): X509TrustManager
}
```

```kotlin
val sslSocketFactory = TrustPinSSLSocketFactory.create()
val client = OkHttpClient.Builder()
    .sslSocketFactory(sslSocketFactory, sslSocketFactory.trustManager())
    .build()
```

---

## 🔄 Suspend vs Blocking API

TrustPin provides two API styles:

| Feature              | Suspend API                 | Blocking API                  |
|----------------------|-----------------------------|-------------------------------|
| Best for             | Kotlin coroutines           | Java interop, legacy code     |
| Performance          | Non-blocking                | Blocks the calling thread     |
| Cancellation         | Supports coroutine cancellation | None                      |
| Usage context        | `suspend` fns / coroutine builders | Any function context   |

```kotlin
// Suspend (recommended for Kotlin)
suspend fun initialize() {
    TrustPin.setup(configuration)
}

// Blocking (Java interop)
fun initializeFromJava() {
    TrustPin.getDefault().setupBlocking(configuration)
}
```

Blocking APIs must not be called from the Android main thread; they throw `IllegalStateException` if you try.

---

## 🧪 Testing

### Unit test (permissive mode for isolation)

```kotlin
import kotlinx.coroutines.test.runTest
import org.junit.Test

class NetworkTest {
    @Test
    fun `secure network request succeeds`() = runTest {
        TrustPin.setup(
            TrustPinConfiguration(
                organizationId = "test-org",
                projectId      = "test-project",
                publicKey      = "test-key",
                mode           = TrustPinMode.PERMISSIVE
            )
        )
        val result = SecureNetworkClient().fetchData()
        assert(result.isNotEmpty())
    }
}
```

### Integration test with `MockWebServer`

```kotlin
import okhttp3.mockwebserver.MockWebServer

class TrustPinIntegrationTest {
    private lateinit var mockServer: MockWebServer

    @Before fun setUp()    { mockServer = MockWebServer().also { it.start() } }
    @After  fun tearDown() { mockServer.shutdown() }

    @Test fun `pinning with mock server`() = runTest {
        TrustPin.setup(
            TrustPinConfiguration(
                organizationId = "test-org",
                projectId      = "test-project",
                publicKey      = "test-key",
                mode           = TrustPinMode.PERMISSIVE  // allow mock server
            )
        )
        // Test networking code against mockServer.url("/")
    }
}
```

---

## 🐛 Troubleshooting

### Setup fails with `InvalidProjectConfig`
- Verify organization ID, project ID, and public key against the dashboard.
- Check for stray whitespace or newlines in credentials.
- Ensure the public key is properly base64-encoded.

### Certificate verification fails
- Confirm the domain is registered in the TrustPin dashboard.
- Check the certificate format (must be valid X.509).
- Verify pins haven't expired.
- Test with `TrustPinMode.PERMISSIVE` first.

### OkHttp integration issues
- Ensure TrustPin is initialized before creating `OkHttpClient`.
- Use `TrustPinSSLSocketFactory.create()` with OkHttp's `sslSocketFactory(factory, trustManager)`.
- Always pass both the `SSLSocketFactory` and the `TrustManager` to OkHttp.

### Debug steps
1. Enable debug logging: `TrustPin.setLogLevel(TrustPinLogLevel.DEBUG)`
2. Test in `TrustPinMode.PERMISSIVE` first
3. Re-verify credentials in the dashboard
4. Check network connectivity to `cdn.trustpin.cloud`

---

## 📖 Documentation

- **API reference**: [docs.trustpin.cloud/sdk/kotlin](https://docs.trustpin.cloud/sdk/kotlin)
- **Dashboard**: [app.trustpin.cloud](https://app.trustpin.cloud)
- **Support**: [trustpin.cloud](https://trustpin.cloud/)

## 📝 License

This project is licensed under the TrustPin Binary License Agreement — see [LICENSE](LICENSE).

**Commercial License**: for enterprise licensing or custom agreements, contact [contact@trustpin.cloud](mailto:contact@trustpin.cloud).

**Attribution required**: applications using this software must display *"Uses TrustPin™ technology – https://trustpin.cloud"*.

## 🤝 Support

- 📧 [support@trustpin.cloud](mailto:support@trustpin.cloud)
- 🌐 [https://trustpin.cloud](https://trustpin.cloud)

---

*Built with ❤️ by the TrustPin team*
