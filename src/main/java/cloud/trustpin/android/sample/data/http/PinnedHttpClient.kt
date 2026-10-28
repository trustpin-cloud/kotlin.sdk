package cloud.trustpin.android.sample.data.http

import cloud.trustpin.kotlin.sdk.okhttp.TrustPinAttestationInterceptor
import cloud.trustpin.kotlin.sdk.okhttp.trustPin
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * The process's pinned OkHttp clients. One client per process keeps a single
 * connection pool and dispatcher, as OkHttp recommends.
 *
 * Both clients are built on first use: `trustPin()` takes the TLS pair from
 * `TrustPin.default`, so the first request must come after `TrustPin.setup`.
 * The callers enforce that ordering (pinning must be configured first). A
 * failed build is retried on the next use.
 */
class PinnedHttpClient {

    /** Pinning plus the attestation interceptor: attested hosts get the `TrustPin-Attestation` header. */
    val withAttestation: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .trustPin()
            .build()
    }

    /**
     * Same pinning, connection pool and dispatcher, no attestation header. For
     * requests that must not wait for an attestation token, such as a login
     * before any session exists to bind a token to.
     */
    val withoutAttestation: OkHttpClient by lazy {
        withAttestation.newBuilder()
            .apply { networkInterceptors().removeAll { it is TrustPinAttestationInterceptor } }
            .build()
    }

    private companion object {
        const val TIMEOUT_SECONDS = 30L
    }
}
