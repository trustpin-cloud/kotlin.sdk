package cloud.trustpin.android.sample.domain.repository

import android.content.Context
import cloud.trustpin.android.sample.domain.model.PinningCredentials

/**
 * Boundary between the use-case layer and the TrustPin SDK's configuration
 * surface. Implementations are responsible for translating SDK exceptions
 * into [cloud.trustpin.android.sample.domain.model.DomainError].
 */
interface ConfigurationRepository {

    /** Whether [configure] (or [configureFromAssets]) has completed for this process. */
    fun isConfigured(): Boolean

    /** Configure the SDK from caller-supplied credentials. Suspends until done. */
    suspend fun configure(credentials: PinningCredentials)

    /**
     * Load credentials from `assets/trustpin.json` and configure. Returns the
     * loaded [PinningCredentials] so the caller can echo non-sensitive fields
     * (org id, project id, mode) into its log feed.
     */
    suspend fun configureFromAssets(context: Context): PinningCredentials
}
