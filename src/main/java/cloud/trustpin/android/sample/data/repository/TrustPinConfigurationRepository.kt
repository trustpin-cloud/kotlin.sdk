package cloud.trustpin.android.sample.data.repository

import android.content.Context
import cloud.trustpin.android.sample.domain.model.DomainError
import cloud.trustpin.android.sample.domain.model.PinningCredentials
import cloud.trustpin.android.sample.domain.repository.ConfigurationRepository
import cloud.trustpin.kotlin.sdk.EmbeddedConfiguration
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

    override fun hasEmbeddedSeed(): Boolean = embeddedSeed() != null

    /**
     * Predefined asset for the embedded (last-resort) configuration. Shipped
     * **empty** so the asset always exists; drop the signed payload downloaded
     * from the dashboard into it to exercise the cold-start path. An empty
     * file means "no seed" — the SDK would reject it at setup, so it is only
     * passed through when it has content.
     */
    private fun embeddedSeed(): EmbeddedConfiguration? {
        val size = try {
            applicationContext.assets.open(SEED_ASSET).use { it.readBytes().size }
        } catch (_: Exception) {
            return null
        }
        return if (size > 0) EmbeddedConfiguration.Asset(SEED_ASSET) else null
    }

    override suspend fun configure(credentials: PinningCredentials) {
        try {
            TrustPin.default.setup(
                TrustPinConfiguration(
                    credentials.organizationId,
                    credentials.projectId,
                    credentials.publicKey,
                    mode = credentials.mode,
                    embeddedConfiguration = embeddedSeed(),
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
            var configuration = TrustPinConfiguration.fromAssets(applicationContext)
            // trustpin.json deliberately omits `embedded_configuration_asset`
            // (the asset is empty until a real seed is dropped in); attach it
            // programmatically when it has content. `copy` yields a new
            // instance, so it needs its own Context decoration.
            val seed = embeddedSeed()
            if (configuration.embeddedConfiguration == null && seed != null) {
                configuration = configuration.copy(embeddedConfiguration = seed).withAndroidStorage(applicationContext)
            }
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

private const val SEED_ASSET = "trustpin-seed.b64"
