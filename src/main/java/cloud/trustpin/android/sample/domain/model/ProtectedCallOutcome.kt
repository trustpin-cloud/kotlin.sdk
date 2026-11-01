package cloud.trustpin.android.sample.domain.model

/**
 * Result of calling the sample API's protected endpoint or its sensitive
 * operation. A non-2xx status is
 * an outcome, not an error: a `401` with an attestation rejection is exactly
 * what the sample wants to show.
 */
data class ProtectedCallOutcome(
    val statusCode: Int,
    /** Whether this call had to log in first (no session, or the previous one expired). */
    val loggedIn: Boolean,
    /** Whether the request carried the `TrustPin-Attestation` header. */
    val attestationHeaderSent: Boolean,
    /** Whether the request carried the `TrustPin-Assertion` header (sensitive operations only). */
    val assertionHeaderSent: Boolean = false,
    val greeting: String?,
    /** The API's echo of the verified attestation claims, or why it rejected the header. */
    val attestationSummary: String?,
    /** Names of the headers the request carried on the network, for the log. */
    val sentHeaderNames: Set<String> = emptySet(),
    /** The start of the response body, for the log when the call did not succeed. */
    val responseExcerpt: String? = null,
) {
    val isSuccess: Boolean get() = statusCode in 200..299

    companion object {
        const val EXCERPT_LENGTH = 400

        /** The first [EXCERPT_LENGTH] characters of [body] on one line, or null when empty. */
        fun excerpt(body: String): String? = body.trim().replace(Regex("\\s+"), " ").take(EXCERPT_LENGTH).ifEmpty { null }
    }
}
