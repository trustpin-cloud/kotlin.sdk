package cloud.trustpin.android.sample.data.repository

import cloud.trustpin.android.sample.domain.model.ConnectionOutcome
import cloud.trustpin.android.sample.domain.model.DomainError
import cloud.trustpin.android.sample.domain.repository.NetworkRepository
import cloud.trustpin.kotlin.sdk.TrustPinError
import cloud.trustpin.kotlin.sdk.okhttp.trustPin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * OkHttp-backed [NetworkRepository] that pins via the `:trustpin-okhttp`
 * adapter (`OkHttpClient.Builder.trustPin()`).
 *
 * Body preview is capped to [BODY_PREVIEW_LIMIT] characters here — the
 * data-layer truncation contract from the iOS spec — so the use-case layer
 * does not need to re-truncate (and cannot accidentally undo the cap).
 */
class OkHttpNetworkRepository : NetworkRepository {

    override suspend fun get(url: String): ConnectionOutcome = withContext(Dispatchers.IO) {
        try {
            val client = buildClient()
            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", USER_AGENT)
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                val preview = if (body.length > BODY_PREVIEW_LIMIT) {
                    body.substring(0, BODY_PREVIEW_LIMIT) + "…"
                } else {
                    body
                }

                ConnectionOutcome(
                    statusCode = response.code,
                    message = response.message,
                    headerCount = response.headers.size,
                    bodyPreview = preview,
                )
            }
        } catch (e: TrustPinError) {
            throw DomainError.Pinning(
                typeName = e::class.simpleName ?: "TrustPinError",
                message = e.message ?: "TrustPin validation failed",
            )
        } catch (e: Exception) {
            throw DomainError.Network(e.message ?: e::class.simpleName ?: "Network error")
        }
    }

    private fun buildClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .trustPin()
            .build()
    }

    companion object {
        private const val USER_AGENT = "TrustPin-Android-Sample/1.0.0"
        private const val REQUEST_TIMEOUT_SECONDS = 30L
        private const val BODY_PREVIEW_LIMIT = 200
    }
}
