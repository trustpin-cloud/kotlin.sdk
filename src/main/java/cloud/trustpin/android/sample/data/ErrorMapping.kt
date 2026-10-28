package cloud.trustpin.android.sample.data

import cloud.trustpin.android.sample.domain.model.DomainError
import cloud.trustpin.kotlin.attest.TrustPinAttestError
import cloud.trustpin.kotlin.sdk.TrustPinError
import kotlinx.coroutines.CancellationException
import java.io.IOException

/**
 * Maps anything a data-layer call can throw to [DomainError], the only error
 * type the domain and presentation layers see.
 *
 * - [CancellationException] is rethrown untouched: cancellation is control
 *   flow, not a failure, and must reach the coroutine machinery.
 * - A [TrustPinError] or [TrustPinAttestError] anywhere in the cause chain
 *   wins: OkHttp reports a pin-check failure as an `SSLHandshakeException`
 *   whose cause is the SDK's error.
 */
fun Throwable.toDomainError(fallbackMessage: String): Throwable {
    if (this is CancellationException) return this
    if (this is DomainError) return this
    for (cause in generateSequence(this) { it.cause }) {
        when (cause) {
            is TrustPinError -> return DomainError.Pinning(
                typeName = cause::class.simpleName ?: "TrustPinError",
                message = cause.message ?: fallbackMessage,
            )
            is TrustPinAttestError -> return DomainError.Attestation(
                typeName = cause::class.simpleName ?: "TrustPinAttestError",
                message = cause.message ?: fallbackMessage,
            )
        }
    }
    return when (this) {
        is IOException -> DomainError.Network(message ?: this::class.simpleName ?: fallbackMessage)
        else -> DomainError.Unknown(message ?: this::class.simpleName ?: fallbackMessage)
    }
}

/** Runs [block], translating every failure except cancellation with [toDomainError]. */
inline fun <T> mapErrors(fallbackMessage: String, block: () -> T): T =
    try {
        block()
    } catch (e: Throwable) {
        throw e.toDomainError(fallbackMessage)
    }
