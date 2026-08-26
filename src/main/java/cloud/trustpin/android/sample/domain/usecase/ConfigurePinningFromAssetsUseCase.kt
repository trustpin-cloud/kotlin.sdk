package cloud.trustpin.android.sample.domain.usecase

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

    suspend operator fun invoke(): PinningCredentials {
        if (configurationRepository.isConfigured()) {
            logger.warning("Setup attempt ignored: TrustPin already configured")
            throw DomainError.Validation("TrustPin is already configured")
        }

        logger.info("Loading TrustPin configuration from assets/trustpin.json...")
        logger.info(
            if (configurationRepository.hasEmbeddedSeed()) {
                "Embedded configuration: assets/trustpin-seed.b64 (used only if every online source is unreachable)"
            } else {
                "Embedded configuration: none (assets/trustpin-seed.b64 is empty)"
            },
        )

        return try {
            val credentials = configurationRepository.configureFromAssets()
            logger.success("TrustPin configured from trustpin.json")
            credentials
        } catch (e: DomainError) {
            logger.error("Failed to configure from trustpin.json: ${e.message}")
            throw e
        }
    }
}
