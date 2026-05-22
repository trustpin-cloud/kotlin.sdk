package cloud.trustpin.android.sample.data.repository

import android.content.Context
import cloud.trustpin.android.sample.domain.model.DomainError
import cloud.trustpin.android.sample.domain.model.PinningCredentials
import cloud.trustpin.android.sample.domain.repository.ConfigurationRepository
import cloud.trustpin.kotlin.sdk.TrustPin
import cloud.trustpin.kotlin.sdk.TrustPinConfiguration
import cloud.trustpin.kotlin.sdk.TrustPinError
import cloud.trustpin.kotlin.sdk.fromAssets
import cloud.trustpin.kotlin.sdk.withAndroidStorage

/**
 * Adapts [TrustPin] to the [ConfigurationRepository] contract. The mutable
 * [isConfigured] flag mirrors what the activity used to track inline.
 *
 * The [applicationContext] is captured once at construction so callers
 * (use cases, view-models) do not have to thread a [Context] through every
 * `configure(...)` call. Pass `Application` (or `Application.applicationContext`)
 * from your DI / `ServiceLocator` to avoid any Activity-leak risk.
 */
class TrustPinConfigurationRepository(
    private val applicationContext: Context,
) : ConfigurationRepository {

    @Volatile
    private var configured: Boolean = false

    override fun isConfigured(): Boolean = configured

    override suspend fun configure(credentials: PinningCredentials) {
        try {
            TrustPin.default.setup(
                TrustPinConfiguration(
                    credentials.organizationId,
                    credentials.projectId,
                    credentials.publicKey,
                    mode = credentials.mode,
                ).withAndroidStorage(applicationContext)
            )
            configured = true
        } catch (e: TrustPinError) {
            configured = false
            throw DomainError.Pinning(
                typeName = e::class.simpleName ?: "TrustPinError",
                message = e.message ?: "Unknown TrustPin error",
            )
        } catch (e: Exception) {
            configured = false
            throw DomainError.Unknown(e.message ?: e::class.simpleName ?: "Unknown error")
        }
    }

    override suspend fun configureFromAssets(): PinningCredentials {
        try {
            val configuration = TrustPinConfiguration.fromAssets(applicationContext)
            TrustPin.default.setup(configuration)
            configured = true
            return PinningCredentials(
                organizationId = configuration.organizationId,
                projectId = configuration.projectId,
                publicKey = configuration.publicKey,
                mode = configuration.mode,
            )
        } catch (e: TrustPinError) {
            configured = false
            throw DomainError.Pinning(
                typeName = e::class.simpleName ?: "TrustPinError",
                message = e.message ?: "Unknown TrustPin error",
            )
        } catch (e: Exception) {
            configured = false
            throw DomainError.Unknown(e.message ?: e::class.simpleName ?: "Unknown error")
        }
    }
}
