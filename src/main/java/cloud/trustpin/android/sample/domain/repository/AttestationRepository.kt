package cloud.trustpin.android.sample.domain.repository

import cloud.trustpin.android.sample.domain.model.AttestationStatus
import cloud.trustpin.android.sample.domain.model.ProtectedCallOutcome
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Boundary for app attestation (kotlin-attest) and the sample API it protects.
 * Mirrors the iOS sample's `AttestationRepository`. Every function is
 * main-safe; failures surface as [cloud.trustpin.android.sample.domain.model.DomainError].
 */
interface AttestationRepository {

    /** The SDK's state, [AttestationStatus.NotSetUp] until [setUp] completes. */
    val status: StateFlow<AttestationStatus>

    /** The attestation SDK's own log lines, for the on-screen log. */
    val sdkLog: Flow<String>

    /** Sets attestation up from `assets/trustpin.json`, binds tokens to the session, prefetches. */
    suspend fun setUp()

    /** Logs in if there is no session, then calls the protected endpoint with the session. */
    suspend fun callProtectedEndpoint(): ProtectedCallOutcome

    /** Drops the session; the next protected call logs in again. */
    fun signOut()

    /** Discards the attestation key and token; the next protected call attests a new key. */
    suspend fun reset()
}
