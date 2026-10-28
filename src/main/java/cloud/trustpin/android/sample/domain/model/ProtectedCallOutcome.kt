package cloud.trustpin.android.sample.domain.model

/**
 * Result of calling the sample API's protected endpoint. A non-2xx status is
 * an outcome, not an error: a `401` with an attestation rejection is exactly
 * what the sample wants to show.
 */
data class ProtectedCallOutcome(
    val statusCode: Int,
    /** Whether this call had to log in first (no session, or the previous one expired). */
    val loggedIn: Boolean,
    /** Whether the request carried the `TrustPin-Attestation` header. */
    val attestationHeaderSent: Boolean,
    val greeting: String?,
    /** The API's echo of the verified attestation claims, or why it rejected the header. */
    val attestationSummary: String?,
) {
    val isSuccess: Boolean get() = statusCode in 200..299
}
