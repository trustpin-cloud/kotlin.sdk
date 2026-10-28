package cloud.trustpin.android.sample.data

import cloud.trustpin.android.sample.data.repository.toAttestationStatus
import cloud.trustpin.android.sample.domain.model.AttestationStatus
import cloud.trustpin.kotlin.attest.TrustPinAttestError
import cloud.trustpin.kotlin.attest.TrustPinAttestState
import kotlin.test.Test
import kotlin.test.assertEquals

class AttestationStatusMappingTest {

    private val now = 1_800_000_000_000L

    @Test
    fun bindingRequiredIsWaitingForASessionNotAFailure() {
        // What the SDK reports after a prefetch, before login, under a binding-required policy.
        val state = TrustPinAttestState.Failed(TrustPinAttestError.BindingRequired, Long.MAX_VALUE)
        assertEquals(AttestationStatus.WaitingForSession, state.toAttestationStatus(now))
    }

    @Test
    fun aDistantRetryIsNoAutomaticRetry() {
        val state = TrustPinAttestState.Failed(TrustPinAttestError.InvalidConfiguration, Long.MAX_VALUE)
        val status = state.toAttestationStatus(now) as AttestationStatus.Failed
        assertEquals(null, status.retryAtMillis)
    }

    @Test
    fun aBackOffKeepsItsRetryTime() {
        val state = TrustPinAttestState.Failed(TrustPinAttestError.VerifierUnavailable, now + 30_000)
        val status = state.toAttestationStatus(now) as AttestationStatus.Failed
        assertEquals(now + 30_000, status.retryAtMillis)
        assertEquals("VerifierUnavailable", status.reason)
    }

    @Test
    fun readyAndTheRest() {
        assertEquals(AttestationStatus.Ready(now + 60_000), TrustPinAttestState.Ready(now + 60_000).toAttestationStatus(now))
        assertEquals(AttestationStatus.Unavailable, TrustPinAttestState.Unavailable.toAttestationStatus(now))
        assertEquals(AttestationStatus.Attesting, TrustPinAttestState.Attesting.toAttestationStatus(now))
    }
}
