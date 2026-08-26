package cloud.trustpin.android.sample.domain.repository

import cloud.trustpin.android.sample.domain.model.PinningCredentials

/**
 * Boundary between the use-case layer and the TrustPin SDK's configuration
 * surface. Implementations are responsible for translating SDK exceptions
 * into [cloud.trustpin.android.sample.domain.model.DomainError].
 *
 * The Android implementation holds the application `Context` as a
 * constructor dependency (see `TrustPinConfigurationRepository`), so use
 * cases and view-models do not need to pass it through every call.
 */
interface ConfigurationRepository {

    /** Whether [configure] (or [configureFromAssets]) has completed for this process. */
    fun isConfigured(): Boolean

    /**
     * Whether the APK ships a non-empty `assets/trustpin-seed.b64`. Both
     * configuration paths hand it to the SDK as the embedded configuration so
     * the cold-start fallback can be exercised (airplane mode + first launch).
     */
    fun hasEmbeddedSeed(): Boolean

    /** Configure the SDK from caller-supplied credentials. Suspends until done. */
    suspend fun configure(credentials: PinningCredentials)

    /**
     * Load credentials from `assets/trustpin.json` and configure. Returns the
     * loaded [PinningCredentials] so the caller can echo non-sensitive fields
     * (org id, project id, mode) into its log feed.
     */
    suspend fun configureFromAssets(): PinningCredentials
}
