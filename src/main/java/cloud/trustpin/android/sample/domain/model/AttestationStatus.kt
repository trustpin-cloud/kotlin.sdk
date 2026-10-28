package cloud.trustpin.android.sample.domain.model

/**
 * The attestation SDK's state as the sample shows it. Domain-owned, so the
 * presentation layer never depends on SDK types.
 */
sealed interface AttestationStatus {
    /** `setUp` has not completed yet. */
    data object NotSetUp : AttestationStatus

    /** Set up; no token yet (none requested, or reset). */
    data object Unavailable : AttestationStatus

    /** A token is being obtained. */
    data object Attesting : AttestationStatus

    /**
     * The verifier binds tokens to the session and no request has carried one
     * yet (for example the prefetch right after setup, before login). Not a
     * failure: the next request with an `Authorization` header gets its token.
     */
    data object WaitingForSession : AttestationStatus

    /** A token is held until [expiresAtMillis] (epoch milliseconds). */
    data class Ready(val expiresAtMillis: Long) : AttestationStatus

    /**
     * The last attempt failed with [reason]. The next attempt is allowed from
     * [retryAtMillis] (epoch milliseconds); `null` means no automatic retry
     * (the SDK reports a distant time).
     */
    data class Failed(val reason: String, val retryAtMillis: Long?) : AttestationStatus
}
