package cloud.trustpin.android.sample.domain.usecase

import android.content.Context
import cloud.trustpin.android.sample.domain.model.DomainError
import cloud.trustpin.android.sample.domain.model.PinningCredentials
import cloud.trustpin.android.sample.domain.repository.Logger
import cloud.trustpin.android.sample.domain.repository.ConfigurationRepository

/**
 * Configure TrustPin by loading `assets/trustpin.json` — the recommended
 * setup path for SDK 4.3.0+. Logs the non-sensitive fields (org/project ids
 * are echoed in their full form here because the assets file is shipped
 * inside the APK; for the redaction path see [ConfigurePinningUseCase]).
 */
class ConfigurePinningFromAssetsUseCase(
    private val configurationRepository: ConfigurationRepository,
    private val logger: Logger,
) {

    suspend operator fun invoke(context: Context): PinningCredentials {
        if (configurationRepository.isConfigured()) {
            logger.warning("Setup attempt ignored: TrustPin already configured")
            throw DomainError.Validation("TrustPin is already configured")
        }

        logger.info("Loading TrustPin configuration from assets/trustpin.json...")

        return try {
            val credentials = configurationRepository.configureFromAssets(context)
            logger.success("TrustPin configured from trustpin.json")
            credentials
        } catch (e: DomainError) {
            logger.error("Failed to configure from trustpin.json: ${e.message}")
            throw e
        }
    }
}
