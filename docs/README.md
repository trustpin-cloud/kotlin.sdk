# TrustPin Kotlin SDK Documentation

[![Kotlin](https://img.shields.io/badge/Kotlin-2.3.0%2B-purple.svg)](https://kotlinlang.org)
[![Android](https://img.shields.io/badge/Android-API%2025%2B-green.svg)](https://developer.android.com)
[![JVM](https://img.shields.io/badge/JVM-11%2B-blue.svg)](https://adoptopenjdk.net)

[TrustPin](https://trustpin.cloud/) is a modern, lightweight, and secure Kotlin library that enforces **SSL Certificate Pinning** for **Android** applications. The Maven Central artifact is an Android AAR with bundled R8/ProGuard consumer rules.

The optional app attestation module ships as a second AAR, `cloud.trustpin:kotlin-attest` — see [App attestation](#-app-attestation-kotlin-attest).

> JVM/server/desktop support exists as a separately distributed hardened JAR. This documentation focuses on Android — JVM customers should request access via [support@trustpin.cloud](mailto:support@trustpin.cloud).

---

## 🚀 Key Features

- ✅ **Android AAR on Maven Central** — Ships with bundled R8/ProGuard consumer rules
- ✅ **`Context`-aware Android setup** — Bundle credentials as `assets/trustpin.json` and load them with a single call
- ✅ **Anti-rollback storage on Android** — Pinning configuration is sealed against downgrade attacks
- ✅ **Integrity check on Android** — Refuses to operate on non-production / tampered runtime environments
- ✅ **Flexible Pinning Modes** — Strict validation or permissive mode for development
- ✅ **Multiple Hash Algorithms** — SHA-256 and SHA-512 certificate validation
- ✅ **Signed Configuration** — Cryptographically signed pinning configurations
- ✅ **Built-in TrustManager & SSLSocketFactory** — Drop into OkHttp, Retrofit, Ktor, `HttpsURLConnection`
- ✅ **Multi-instance support** — Isolated pinning contexts for multi-tenant apps and libraries
- ✅ **App attestation** — Optional `kotlin-attest` AAR proves to your backend that requests come from your genuine app on a genuine Android device
- ✅ **Intelligent Caching** — 10-minute configuration cache with stale fallback
- ✅ **Comprehensive Logging** — Configurable log levels for debugging and monitoring
- ✅ **Thread-Safe** — Built with coroutines and concurrent-safe operations

---

## 📋 Platform Requirements

| Platform | Minimum Version | Notes |
|----------|----------------|-------|
| Android | API 25+ | Full feature support |
| Kotlin | 2.3.0+ | Built with Kotlin 2.3.0 |

> A hardened JVM JAR is available separately by request for server/desktop use — see [support@trustpin.cloud](mailto:support@trustpin.cloud). This documentation covers the Android distribution only.

---

## 📦 Installation

### Android Gradle (Kotlin DSL)

```kotlin
dependencies {
    implementation("cloud.trustpin:kotlin-sdk:7.0.1")
}
```

### Android Gradle (Groovy)

```groovy
dependencies {
    implementation 'cloud.trustpin:kotlin-sdk:7.0.1'
}
```

### Android Maven

```xml
<dependency>
    <groupId>cloud.trustpin</groupId>
    <artifactId>kotlin-sdk</artifactId>
    <version>7.0.1</version>
</dependency>
```

### Optional client adapters — OkHttp / Ktor

Thin, optional artifacts versioned in lockstep with the SDK. Neither pulls in the SDK or the HTTP client transitively — your app supplies both.

```kotlin
dependencies {
    implementation("cloud.trustpin:trustpin-okhttp:7.0.1")  // OkHttp
    implementation("cloud.trustpin:trustpin-ktor:7.0.1")    // Ktor (OkHttp engine); includes trustpin-okhttp
}
```

### Optional app attestation — kotlin-attest

```kotlin
dependencies {
    implementation("cloud.trustpin:kotlin-attest:7.0.1")
}
```

See [App attestation](#-app-attestation-kotlin-attest) for configuration and setup.

---

## 🔧 Quick Setup

`TrustPin.setup` accepts a `TrustPinConfiguration`. On Android there are two recommended ways to build one:

| Path | Recommended path | What you write |
|---|---|---|
| **A** — credentials bundled with the app | `TrustPinConfiguration.fromAssets(context)` | A `trustpin.json` in `src/main/assets/` |
| **B** — credentials supplied at runtime | `TrustPinConfiguration(...).withAndroidStorage(context)` | Kotlin code |

> ⚠️ **On Android, always use one of the two `Context`-aware paths.** Constructing a bare `TrustPinConfiguration` still works, but the instance runs with a weakened security profile. Treat it as a misconfiguration, not a feature.

### Path A — Bundled JSON (Android, recommended)

This is the primary Android entry point. Drop a `trustpin.json` into the app module's assets directory and load it with one call.

**`app/src/main/assets/trustpin.json`** (schema is `snake_case`):

```json
{
  "organization_id": "my-org",
  "project_id":      "my-project",
  "public_key":      "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE...",
  "mode":            "strict"
}
```

`organization_id`, `project_id`, `public_key` are required. `mode` is `"strict"` (default) or `"permissive"`. An optional `configuration_url` overrides the hosted configuration source — when present it must use HTTPS. An optional `embedded_configuration_asset` names a bundled signed configuration in `assets/` used only when no online source and no previously fetched configuration is available (see the README section "Embedded configuration" for the requirements). Unknown top-level keys are ignored for forward compatibility.

**Per-flavor / per-build-type overrides** ride on standard Android source-set asset merging — no Gradle plugin required:

```
app/src/main/assets/trustpin.json      ← default
app/src/debug/assets/trustpin.json     ← debug build type
app/src/staging/assets/trustpin.json   ← "staging" product flavor
```

**Kotlin:**

```kotlin
import cloud.trustpin.kotlin.sdk.TrustPin
import cloud.trustpin.kotlin.sdk.TrustPinConfiguration
import cloud.trustpin.kotlin.sdk.fromAssets

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            try {
                TrustPin.setup(TrustPinConfiguration.fromAssets(this@App))
            } catch (e: TrustPinError) {
                // Hard stop. Do NOT fall through to an unpinned HTTP client.
                showRetryUi(e)
            }
        }
    }
}
```

### Path B — Programmatic config (Android)

Use this when credentials come from runtime config (MDM, remote feature flag, A/B routing, …) rather than a bundled file. Always chain `.withAndroidStorage(context)`.

```kotlin
import cloud.trustpin.kotlin.sdk.TrustPin
import cloud.trustpin.kotlin.sdk.TrustPinConfiguration
import cloud.trustpin.kotlin.sdk.TrustPinMode
import cloud.trustpin.kotlin.sdk.withAndroidStorage

suspend fun initializeTrustPin(context: Context, creds: Credentials) {
    val config = TrustPinConfiguration(
        organizationId = creds.orgId,
        projectId      = creds.projectId,
        publicKey      = creds.publicKey,
        mode           = TrustPinMode.STRICT,
    ).withAndroidStorage(context)

    TrustPin.setup(config)
}
```

### ⚠️ Single-use contract for `Context`-decorated configurations

A configuration produced through `fromAssets(context)` or `.withAndroidStorage(context)` **must be passed to exactly one `TrustPin.setup` call.** Reusing the same decorated instance for a second setup silently downgrades the second instance. Build a fresh configuration for each TrustPin instance:

```kotlin
// ✅ Correct — one decorated configuration per setup.
TrustPin.instance("payments").setup(TrustPinConfiguration.fromAssets(context))
TrustPin.instance("analytics").setup(
    TrustPinConfiguration(orgId, projId, publicKey).withAndroidStorage(context)
)

// ❌ Wrong — second setup is silently downgraded.
val shared = TrustPinConfiguration(orgId, projId, publicKey).withAndroidStorage(context)
TrustPin.default.setup(shared)
TrustPin.instance("payments").setup(shared)   // degraded!
```

### Fail-closed integration

`setup` performs local validation only and starts a *background* fetch of the pinning configuration — it never blocks app launch on the network. If your app must not start networking without a validated payload, gate on `TrustPin.awaitConfiguration(timeout)` and **treat any `TrustPinError` as a hard stop** — do not continue on failure with an unpinned client:

```kotlin
try {
    TrustPin.setup(TrustPinConfiguration.fromAssets(context)) // local validation only
    TrustPin.awaitConfiguration(timeout = 10_000)             // fail-closed gate
} catch (e: TrustPinError) {
    return showRetryUi(e)   // do NOT fall through to an unpinned client
}
val client = OkHttpClient.Builder()
    .sslSocketFactory(TrustPin.makeSSLSocketFactory(), TrustPin.makeTrustManager())
    .build()
```

---

## 🛠 Usage Examples

### OkHttp

```kotlin
import cloud.trustpin.kotlin.sdk.TrustPin
import cloud.trustpin.kotlin.sdk.TrustPinConfiguration
import cloud.trustpin.kotlin.sdk.fromAssets
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class NetworkManager(context: Context) {

    suspend fun initialize() {
        TrustPin.setup(TrustPinConfiguration.fromAssets(context))
        TrustPin.awaitConfiguration()
    }

    val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .sslSocketFactory(TrustPin.makeSSLSocketFactory(), TrustPin.makeTrustManager())
            .build()
    }
}
```

### Retrofit

```kotlin
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ApiClient(context: Context) {

    suspend fun initialize() {
        TrustPin.setup(TrustPinConfiguration.fromAssets(context))
        TrustPin.awaitConfiguration()
    }

    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .sslSocketFactory(TrustPin.makeSSLSocketFactory(), TrustPin.makeTrustManager())
            .build()
    }

    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.example.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}
```

### Ktor

```kotlin
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp

class KtorNetworkClient(context: Context) {

    suspend fun initialize() {
        TrustPin.setup(TrustPinConfiguration.fromAssets(context))
        TrustPin.awaitConfiguration()
    }

    val httpClient by lazy {
        HttpClient(OkHttp) {
            engine {
                preconfigured = OkHttpClient.Builder()
                    .sslSocketFactory(TrustPin.makeSSLSocketFactory(), TrustPin.makeTrustManager())
                    .build()
            }
        }
    }
}
```

### Attestation headers (OkHttp / Ktor adapters)

The optional `trustpin-okhttp` and `trustpin-ktor` adapters install the pinned
TLS pair with one call (`OkHttpClient.Builder().trustPin()`, `engine { trustPin() }`)
and also add the headers supplied by an *attestation provider* registered on the
TrustPin instance. The TrustPin attestation module registers itself when it is
set up; you can also register your own:

```kotlin
TrustPin.setAttestationProvider(object : TrustPinAttestationProvider {
    override val headerNames = setOf("X-My-Header")
    override suspend fun attestationHeaders(request: TrustPinAttestationRequest): Map<String, String> =
        mapOf("X-My-Header" to tokenFor(request.url))
})
```

Headers are added to HTTPS requests only. A provider declares the names of the
headers it adds (`headerNames`). Through the OkHttp adapter, a header the app
already set on a request is kept, and the provider's headers are added only for
names the request lacks; on every redirect to another URL, those names and any
`TrustPin-Attestation` header are removed and the provider is asked again for
that URL, so a header meant for one host never follows a redirect to another
host or to plain `http`. Through the Ktor adapter, the provider's names belong
to it: they're replaced on every request. Headers the provider did not declare
are never added. A provider that does not answer in time, or fails, is ignored
for that request and the request is sent without its headers; a provider can
never fail a request or affect certificate pinning. With no provider
registered, requests are unchanged, except that a redirect to another URL
through the OkHttp adapter drops any `TrustPin-Attestation` header. Clients built without
the adapters can call `TrustPin.attestationHeaders(request)` (or
`attestationHeadersBlocking`) and add the result themselves; if such a client
follows redirects, turn that off or rebuild each redirect's headers with
`TrustPin.redirectedRequestHeaders(request)`.

### Manual certificate verification

For custom networking stacks or one-off certificate inspection:

```kotlin
import cloud.trustpin.kotlin.sdk.TrustPin
import cloud.trustpin.kotlin.sdk.TrustPinError
import java.security.cert.X509Certificate

suspend fun verifyCertificate(domain: String, certificate: X509Certificate) {
    try {
        TrustPin.verify(domain, certificate)
    } catch (e: TrustPinError.DomainNotRegistered) {
        // strict mode only
    } catch (e: TrustPinError.PinsMismatch) {
        // possible MITM
    } catch (e: TrustPinError) {
        // other verification failure
    }
}
```

### Multi-instance usage

Libraries or multi-tenant apps can create isolated pinning contexts:

```kotlin
val payments = TrustPin.instance("payments")
payments.setup(
    TrustPinConfiguration(orgId = "payments-org", projectId = "payments-api", publicKey = "...")
        .withAndroidStorage(context)
)

val analytics = TrustPin.instance("analytics")
analytics.setup(
    TrustPinConfiguration(orgId = "analytics-org", projectId = "analytics-api", publicKey = "...")
        .withAndroidStorage(context)
)
```

Each instance has its own state and log output tagged with the instance id. `fromAssets(context)` always applies to `TrustPin.default`; named instances use the programmatic path.

---

## 🛡 App attestation (kotlin-attest)

`kotlin-attest` proves to your backend that requests come from your genuine
app on a genuine Android device, using Android Key Attestation and Google
Play Integrity together with the verifier you run. It ships as a second AAR
next to `kotlin-sdk` (`cloud.trustpin:kotlin-attest`) and is released at the
same version, starting with 7.0.0.

```kotlin
dependencies {
    implementation("cloud.trustpin:kotlin-attest:7.0.1")
}
```

**Configuration.** Add these keys to the same `assets/trustpin.json` that
holds your pinning configuration:

| Key | Required | Type | Notes |
|---|---|---|---|
| `attest_verifier_url` | yes | string | HTTPS base URL of your attestation verifier, without the version segment (`https://api.example.com/attest`) |
| `attested_hosts` | yes | array of string | Hosts that receive the `TrustPin-Attestation` header; `*.example.com` covers one label. IP addresses are not accepted |
| `google_cloud_project_number` | no | string or number | Your Google Cloud project number, needed when your policy asks for Play Integrity |

Register the verifier's domain in your TrustPin project: calls to the
verifier are pinned by the `TrustPin` instance with the same identifier, and
in strict mode an unregistered verifier host is never reached.

**Setup.** Set up pinning first, then attestation:

```kotlin
TrustPin.setup(TrustPinConfiguration.fromAssets(context).withAndroidStorage(context))
TrustPinAttest.setup(context, TrustPinAttestConfiguration.fromAssets(context))
TrustPinAttest.bindToHeader("Authorization")   // bind tokens to the session
TrustPinAttest.prefetch()                       // so the first request does not wait
```

**Where the header comes from.** Automatically on HTTPS requests to the
configured hosts made through TrustPin's OkHttp and Ktor adapters
(`OkHttpClient.Builder().trustPin()`, `engine { trustPin() }`). For your own
client, `TrustPinAttest.attestationHeaders(url, requestHeaders)` returns the
header to add; for other clients (gRPC, WebSockets), `TrustPinAttest.getToken()`.
The header is only ever added to `https` requests. A `TrustPin-Attestation`
header your app sets itself is kept on OkHttp requests; with Ktor, the SDK
replaces it. The adapters also handle
redirects: a redirect gets the header only if it goes to an `https` URL on a
configured host. If your own client follows redirects, turn that off or
rebuild each redirect's headers with `TrustPin.redirectedRequestHeaders`, so
the token never follows a redirect to another host. The SDK never blocks or
fails your requests: when no token is available the request is sent without
the header and your backend decides how to treat it.

**Binding.** `setTokenBinding(value)` binds every token to a value of your
choice; `bindToHeader("Authorization")` binds each request's token to that
header's value on the request. The value never leaves the device. A change of
binding renews the token. `prefetch()` with `bindToHeader` attests early but
cannot bind (there is no request yet): the first bound request renews the
token once more, cheaply. If your verifier requires a binding, nothing can be
prefetched until one is known; call `setTokenBinding` with the session value
before `prefetch()`. With header binding, `getToken()` returns a token bound
to the most recent request this instance saw; use `setTokenBinding` for
clients that send no such request. `setTokenBinding(null)` removes the fixed
binding only.

**State and logging.** `TrustPinAttest.state` is `Unavailable`, `Attesting`,
`Ready(expiresAtMillis)` or `Failed(error, retryAtMillis)`;
`setStateListener` observes changes. `TrustPinAttestState` and
`TrustPinAttestError` may gain subclasses in a minor release: give `when`
expressions over them an `else` branch. `setLogLevel` and `setLogSink` control
the module's own logging, off by default; pass `null` to `setLogSink` to
restore logcat. `TrustPinAttest.reset()` (a suspending call; `resetBlocking()`
for Java) discards the attested key so the next request attests a new one.

**Emulators and development.** Emulators cannot produce device evidence. For
development, your verifier's operator can issue a debug token; pass it as
`TrustPinAttestConfiguration.debugToken` from a debug-only build field. It is
used only when the app is debuggable and ignored in every other build.

**One process.** Attestation is designed for one process per app: set it up
in the process that performs your networking. Two processes attesting at the
same time are harmless but spend evidence quota twice.

**Data safety.** For your Play Console declaration: `kotlin-attest` sends
attestation evidence and tokens only to your own verifier, sends nothing to
TrustPin, and collects no device or user identifier.

## 🎯 Pinning Modes

| Mode | Behavior | Use case |
|------|----------|----------|
| `TrustPinMode.STRICT` | Throws `TrustPinError.DomainNotRegistered` for unregistered domains | **Production** — all connections must be validated |
| `TrustPinMode.PERMISSIVE` | Allows unregistered domains to bypass pinning | **Development / testing**, or apps with dynamic endpoints |

Set it via the configuration:

```kotlin
TrustPinConfiguration(orgId, projId, publicKey, mode = TrustPinMode.STRICT)
```

Or in `trustpin.json`:

```json
{ "organization_id": "...", "project_id": "...", "public_key": "...", "mode": "strict" }
```

---

## 📊 Error Handling

All errors are subtypes of the sealed class `TrustPinError`. Catch the supertype for blanket handling, or specific cases when you can recover differently:

```kotlin
import cloud.trustpin.kotlin.sdk.TrustPinError

try {
    TrustPin.verify("api.example.com", cert)
} catch (e: TrustPinError.DomainNotRegistered) {
    // strict mode only
} catch (e: TrustPinError.PinsMismatch) {
    // possible MITM — never proceed
} catch (e: TrustPinError.AllPinsExpired) {
    // all pins for the domain expired — refresh or fail
} catch (e: TrustPinError.InvalidServerCert) {
    // certificate is not a usable X.509
} catch (e: TrustPinError) {
    // setup / network / integrity / timeout — see below
}
```

### Error reference

| Error | Raised by | Meaning |
|---|---|---|
| `InvalidProjectConfig` | `setup` | Credentials missing or malformed; on Android, `trustpin.json` missing/unreadable |
| `AlreadyInitialized` | `setup` | Instance already completed setup — reconfiguration is not supported; use a named instance |
| `ErrorFetchingPinningInfo` | `awaitConfiguration` / `verify` | Configuration could not be fetched from the CDN |
| `ConfigurationValidationFailed` | `awaitConfiguration` / `verify` | Configuration signature did not verify |
| `ConfigIntegrityError` | `awaitConfiguration` / `verify` | Configuration failed the SDK's integrity check |
| `SetupInProgress` | `setup` / `verify` / `awaitConfiguration` | Another `setup` call is in flight |
| `LockTimeout` | `setup` / `verify` | Internal lock could not be acquired |
| `NotInitialized` | `verify` / `awaitConfiguration` | `setup` has not completed successfully |
| `PinsMismatch` | `verify` | Certificate does not match any configured pin |
| `AllPinsExpired` | `verify` | All pins for the domain have expired |
| `DomainNotRegistered` | `verify` | Domain not configured (strict mode only) |
| `InvalidServerCert` | `verify` / `fetchCertificate` | Certificate is not a usable X.509 / TLS handshake failed |
| `Timeout` | `verify` / `fetchCertificate` | Operation deadline elapsed |
| `SSLContextSetupFailed` | `makeSSLSocketFactory` / `makeTrustManager` | Platform TLS stack could not be configured |
| `UnsupportedDevice` | `makeSSLSocketFactory` / `makeTrustManager` | Runtime is not a supported production Android device |

---

## 🔍 Logging and Debugging

```kotlin
import cloud.trustpin.kotlin.sdk.TrustPin
import cloud.trustpin.kotlin.sdk.TrustPinLogLevel

// Set log level before setup for complete coverage.
TrustPin.setLogLevel(TrustPinLogLevel.DEBUG)
```

| Level | Output |
|---|---|
| `NONE` | No logging |
| `ERROR` | Errors only |
| `INFO` | Errors and informational messages |
| `DEBUG` | All messages, including debug detail |

**Production guidance:** use `ERROR` or `NONE`. `DEBUG` is for development and incident response only.

Example debug output:

```
[14:30:15] [DEBUG] Starting certificate verification for domain: api.example.com
[14:30:15] [INFO]  Using cached configuration
[14:30:15] [DEBUG] Found domain configuration with 2 pins
[14:30:15] [DEBUG] Certificate hash matches sha256 pin for domain api.example.com
[14:30:15] [INFO]  Valid pin found for api.example.com
```

---

## 🏗 Best Practices

### Setup and initialization

1. Call `TrustPin.setup` **once** during app startup (typically in `Application.onCreate`).
2. On Android, **always** use `fromAssets(context)` or `.withAndroidStorage(context)`.
3. Treat any `TrustPinError` from `setup` as a hard stop — never fall through to an unpinned HTTP client.
4. Gate on `TrustPin.awaitConfiguration(timeout)` before building any pinned HTTP client your app cannot run without.
5. Set the log level **before** `setup` for full logging coverage.

### Security

1. Use `TrustPinMode.STRICT` in production.
2. Rotate pins before expiration; the SDK falls back to the most-recent valid cached configuration but cannot extend expired pins.
3. Keep `trustpin.json` in version control alongside your app source.
4. Monitor pin validation failures via your logging pipeline.

### Performance

1. Configuration caching is automatic (10 min, with stale fallback).
2. Reuse the OkHttp client — do not build one per request.
3. Use `ERROR` or `NONE` log level in production.

---

## 🧪 Testing

```kotlin
import kotlinx.coroutines.test.runTest
import org.junit.Test

class NetworkTest {

    @Test
    fun `verifies test endpoint`() = runTest {
        TrustPin.setup(
            TrustPinConfiguration(
                organizationId = "test-org",
                projectId      = "test-project",
                publicKey      = "test-key",
                mode           = TrustPinMode.PERMISSIVE,  // tests only
            )
        )

        val client = SecureNetworkClient()
        val result = client.fetchData()

        assert(result.isNotEmpty())
    }
}
```

For instrumented Android tests use `fromAssets(context)` against a dedicated `src/androidTest/assets/trustpin.json`.

---

## 🐛 Troubleshooting

### `setup` throws `InvalidProjectConfig`

- Verify `organization_id`, `project_id`, and `public_key`.
- Check for whitespace or newlines around the values.
- Ensure `public_key` is properly base64-encoded.
- On Android with `fromAssets`: confirm `trustpin.json` is in `src/main/assets/` of the **application** module (not a library module), and that it isn't being excluded by asset filters.

### `setup` throws `ConfigIntegrityError`

The downloaded configuration failed the SDK's integrity check. Re-issue credentials in the TrustPin dashboard and check for a misconfigured `configuration_url`.

### `makeSSLSocketFactory` throws `UnsupportedDevice`

Release builds of the SDK run only on supported production Android devices. For development and QA:

- Use a debug-built variant of your app — debug builds run everywhere, including emulators.
- Or use a stock production device for release-shaped testing.

### Certificate verification fails

- Confirm the domain is registered in the [TrustPin Dashboard](https://trustpin.cloud/dashboard).
- Check the certificate is a valid X.509 leaf.
- Verify pins have not expired.
- Re-test with `TrustPinMode.PERMISSIVE` to isolate whether the domain or the pin is at fault.

### OkHttp integration issues

- Initialize TrustPin **before** building the `OkHttpClient`.
- Always pass both `SSLSocketFactory` and `X509TrustManager` to `sslSocketFactory(...)`.
- Verify the OkHttp client is reused across requests, not rebuilt.

### Debug checklist

1. `TrustPin.setLogLevel(TrustPinLogLevel.DEBUG)` before `setup`.
2. Test with `TrustPinMode.PERMISSIVE` to isolate domain registration from pin validation.
3. Verify credentials in the TrustPin Dashboard.
4. Check connectivity to `cdn.trustpin.cloud`.

---

## 📖 Documentation

- **API Reference**: [Full KDoc Documentation](index.html)
- **Documentation site**: [docs.trustpin.cloud](https://docs.trustpin.cloud)
- **TrustPin Dashboard**: [trustpin.cloud/dashboard](https://trustpin.cloud/dashboard)

---

## 📝 License

**Commercial License**: For enterprise licensing or custom agreements, contact [contact@trustpin.cloud](mailto:contact@trustpin.cloud).

**Attribution Required**: When using this software, you must display "Uses TrustPin™ technology – https://trustpin.cloud" in your application.

---

## 🤝 Support & Feedback

- 📧 **Email**: [support@trustpin.cloud](mailto:support@trustpin.cloud)
- 🌐 **Website**: [https://trustpin.cloud](https://trustpin.cloud)
- 📋 **Issues**: For SDK-related issues, please contact support

---