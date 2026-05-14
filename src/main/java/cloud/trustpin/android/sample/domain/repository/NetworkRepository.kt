package cloud.trustpin.android.sample.domain.repository

import cloud.trustpin.android.sample.domain.model.ConnectionOutcome

/**
 * Boundary for executing TrustPin-pinned HTTPS requests.
 *
 * Implementations build the OkHttp client from the SDK's SSL factory + trust
 * manager and cap the returned body preview at ≤ 200 chars. Truncation is a
 * data-layer concern, not a logger concern — use cases log [ConnectionOutcome.bodyPreview]
 * verbatim.
 */
interface NetworkRepository {
    suspend fun get(url: String): ConnectionOutcome
}
