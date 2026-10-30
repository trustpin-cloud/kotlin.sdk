package cloud.trustpin.android.sample.data.http

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** A fully read HTTP response: safe to inspect on any thread, nothing left to close. */
class HttpResult(
    val code: Int,
    val message: String,
    val headerCount: Int,
    val body: String,
    /** Names of the headers the request actually carried, interceptors included. */
    val sentHeaderNames: Set<String>,
)

/**
 * Executes the call on OkHttp's own dispatcher threads and reads at most
 * [maxBodyBytes] of the body there, so the caller's thread never touches the
 * network. Main-safe: call it from any coroutine, including `Dispatchers.Main`.
 *
 * Why not `withContext(Dispatchers.IO) { execute() }`: OkHttp interceptors run
 * on the calling thread, and TrustPin's attestation interceptor blocks while it
 * looks up a token. Blocking a coroutine worker thread risks starving the pool
 * (the SDK logs a warning when it happens); OkHttp's threads are made for it.
 *
 * Cancelling the coroutine cancels the call.
 */
suspend fun Call.executeAsync(maxBodyBytes: Long = DEFAULT_MAX_BODY_BYTES): HttpResult =
    suspendCancellableCoroutine { continuation ->
        continuation.invokeOnCancellation { cancel() }
        enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val result = try {
                    response.use {
                        HttpResult(
                            code = it.code,
                            message = it.message,
                            headerCount = it.headers.size,
                            body = it.peekBody(maxBodyBytes).string(),
                            // The request sent on the network carries what the
                            // network interceptors (TrustPin's attestation one
                            // included) added; `it.request` is the app's.
                            sentHeaderNames = (it.networkResponse?.request ?: it.request).headers.names(),
                        )
                    }
                } catch (e: IOException) {
                    continuation.resumeWithException(e)
                    return
                }
                continuation.resume(result)
            }

            override fun onFailure(call: Call, e: IOException) {
                continuation.resumeWithException(e)
            }
        })
    }

private const val DEFAULT_MAX_BODY_BYTES = 64L * 1024
