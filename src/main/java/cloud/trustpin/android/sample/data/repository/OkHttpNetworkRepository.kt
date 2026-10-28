package cloud.trustpin.android.sample.data.repository

import cloud.trustpin.android.sample.data.http.PinnedHttpClient
import cloud.trustpin.android.sample.data.http.executeAsync
import cloud.trustpin.android.sample.data.mapErrors
import cloud.trustpin.android.sample.domain.model.ConnectionOutcome
import cloud.trustpin.android.sample.domain.repository.NetworkRepository
import okhttp3.Request

/**
 * [NetworkRepository] over the process's pinned OkHttp client (the
 * `:trustpin-okhttp` adapter's `OkHttpClient.Builder.trustPin()`).
 *
 * Body preview is capped to [BODY_PREVIEW_LIMIT] characters here, the
 * data-layer truncation contract shared with the iOS sample, so the use-case
 * layer never needs to re-truncate. Main-safe: the call runs on OkHttp's threads.
 */
class OkHttpNetworkRepository(
    private val httpClient: PinnedHttpClient,
) : NetworkRepository {

    override suspend fun get(url: String): ConnectionOutcome = mapErrors("Network error") {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .build()
        val result = httpClient.withAttestation.newCall(request).executeAsync()
        ConnectionOutcome(
            statusCode = result.code,
            message = result.message,
            headerCount = result.headerCount,
            bodyPreview = if (result.body.length > BODY_PREVIEW_LIMIT) result.body.take(BODY_PREVIEW_LIMIT) + "…" else result.body,
        )
    }

    companion object {
        const val USER_AGENT = "TrustPin-Android-Sample/1.0.0"
        private const val BODY_PREVIEW_LIMIT = 200
    }
}
