package cloud.trustpin.android.sample.data.repository

import android.content.Context
import cloud.trustpin.android.sample.data.mapErrors
import cloud.trustpin.android.sample.domain.model.PinningCredentials
import cloud.trustpin.android.sample.domain.repository.ConfigurationRepository
import cloud.trustpin.kotlin.sdk.EmbeddedConfiguration
import cloud.trustpin.kotlin.sdk.TrustPin
import cloud.trustpin.kotlin.sdk.TrustPinConfiguration
import cloud.trustpin.kotlin.sdk.fromAssets
import cloud.trustpin.kotlin.sdk.withAndroidStorage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * Adapts [TrustPin] to the [ConfigurationRepository] contract. The mutable
 * [isConfigured] flag mirrors what the activity used to track inline.
 *
 * The [applicationContext] is captured once at construction so callers
 * (use cases, view-models) do not have to thread a [Context] through every
 * `configure(...)` call. Pass `Application` (or `Application.applicationContext`)
 * from your DI / `ServiceLocator` to avoid any Activity-leak risk.
 *
 * Main-safe: asset reads run on [ioDispatcher]; `TrustPin.setup` is a
 * main-safe suspend function.
 */
class TrustPinConfigurationRepository(
    private val applicationContext: Context,
    private val ioDispatcher: CoroutineDispatcher,
) : ConfigurationRepository {

    @Volatile
    private var configured: Boolean = false

    override fun isConfigured(): Boolean = configured

    override suspend fun hasEmbeddedSeed(): Boolean = withContext(ioDispatcher) { embeddedSeed() != null }

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

    override suspend fun configure(credentials: PinningCredentials) = mapErrors("Unknown TrustPin error") {
        val seed = withContext(ioDispatcher) { embeddedSeed() }
        TrustPin.default.setup(
            TrustPinConfiguration(
                credentials.organizationId,
                credentials.projectId,
                credentials.publicKey,
                mode = credentials.mode,
                embeddedConfiguration = seed,
            ).withAndroidStorage(applicationContext)
        )
        configured = true
    }

    override suspend fun configureFromAssets(): PinningCredentials = mapErrors("Unknown TrustPin error") {
        val configuration = withContext(ioDispatcher) {
            val fromAssets = TrustPinConfiguration.fromAssets(applicationContext)
            // trustpin.json deliberately omits `embedded_configuration_asset`
            // (the asset is empty until a real seed is dropped in); attach it
            // programmatically when it has content. `copy` yields a new
            // instance, so it needs its own Context decoration.
            val seed = embeddedSeed()
            if (fromAssets.embeddedConfiguration == null && seed != null) {
                fromAssets.copy(embeddedConfiguration = seed).withAndroidStorage(applicationContext)
            } else {
                fromAssets
            }
        }
        TrustPin.default.setup(configuration)
        configured = true
        PinningCredentials(
            organizationId = configuration.organizationId,
            projectId = configuration.projectId,
            publicKey = configuration.publicKey,
            mode = configuration.mode,
        )
    }
}

private const val SEED_ASSET = "trustpin-seed.b64"
