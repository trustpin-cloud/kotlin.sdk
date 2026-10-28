package cloud.trustpin.android.sample.data.repository

import cloud.trustpin.android.sample.domain.model.AttestationStatus
import cloud.trustpin.kotlin.attest.TrustPinAttestError
import cloud.trustpin.kotlin.attest.TrustPinAttestState
import java.util.concurrent.TimeUnit

/**
 * Maps the SDK's state to the sample's [AttestationStatus].
 *
 * - `Failed(BindingRequired)` is the SDK waiting for a binding (the verifier
 *   requires one and the request had none), shown as
 *   [AttestationStatus.WaitingForSession], not as a failure.
 * - A distant `retryAtMillis` means "no automatic retry" (SDK contract) and
 *   becomes `null`, so it is never printed as a time.
 */
internal fun TrustPinAttestState.toAttestationStatus(nowMillis: Long = System.currentTimeMillis()): AttestationStatus =
    when (this) {
        TrustPinAttestState.Unavailable -> AttestationStatus.Unavailable
        TrustPinAttestState.Attesting -> AttestationStatus.Attesting
        is TrustPinAttestState.Ready -> AttestationStatus.Ready(expiresAtMillis)
        is TrustPinAttestState.Failed -> when (error) {
            is TrustPinAttestError.BindingRequired -> AttestationStatus.WaitingForSession
            else -> AttestationStatus.Failed(
                reason = error.message ?: error::class.simpleName.orEmpty(),
                retryAtMillis = retryAtMillis.takeIf { it - nowMillis < DISTANT_AFTER_MILLIS },
            )
        }
        // The hierarchy may gain states in a later SDK version.
        else -> AttestationStatus.Unavailable
    }

/** Anything further out than this is the SDK's "distant" value: no automatic retry. */
private val DISTANT_AFTER_MILLIS = TimeUnit.DAYS.toMillis(365)
