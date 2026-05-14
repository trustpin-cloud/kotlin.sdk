package cloud.trustpin.android.sample.domain.model

import cloud.trustpin.kotlin.sdk.TrustPinMode

/**
 * Inputs needed to set TrustPin up for a project. Mirrors what the SDK's
 * [cloud.trustpin.kotlin.sdk.TrustPinConfiguration] takes, but kept as a
 * domain-owned type so the use-case layer does not depend on SDK types.
 */
data class PinningCredentials(
    val organizationId: String,
    val projectId: String,
    val publicKey: String,
    val mode: TrustPinMode,
)
