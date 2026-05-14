package cloud.trustpin.android.sample.domain.model

/**
 * Result of a TrustPin-pinned HTTPS request, as surfaced to the use-case
 * layer. The body has already been truncated by the data layer — the spec
 * caps preview at ≤ 200 chars at the network repository boundary so the use
 * case logs whatever it gets without re-truncating.
 */
data class ConnectionOutcome(
    val statusCode: Int,
    val message: String,
    val headerCount: Int,
    val bodyPreview: String,
)
