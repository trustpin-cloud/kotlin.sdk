package cloud.trustpin.android.sample.domain.usecase

import cloud.trustpin.android.sample.domain.model.DomainError
import cloud.trustpin.android.sample.domain.model.PinningCredentials
import cloud.trustpin.android.sample.domain.repository.Logger
import cloud.trustpin.android.sample.domain.repository.ConfigurationRepository

/**
 * Configure TrustPin from caller-supplied credentials, with input validation
 * up front and SDK exceptions mapped into [DomainError].
 *
 * The use case owns the log narrative for this flow (start / progress
 * milestones / outcome). Lines that would echo sensitive fields go through
 * the repository — never logged here as raw values.
 */
class ConfigurePinningUseCase(
    private val configurationRepository: ConfigurationRepository,
    private val logger: Logger,
) {

    suspend operator fun invoke(credentials: PinningCredentials) {
        if (configurationRepository.isConfigured()) {
            logger.warning("Setup attempt ignored: TrustPin already configured")
            throw DomainError.Validation("TrustPin is already configured")
        }

        if (credentials.organizationId.isBlank() ||
            credentials.projectId.isBlank() ||
            credentials.publicKey.isBlank()
        ) {
            logger.error("Configuration failed: Missing required fields")
            throw DomainError.Validation("Missing required fields")
        }

        logger.info("Configuring TrustPin...")
        logger.info("Mode: ${credentials.mode.name}")
        logger.info(
            if (configurationRepository.hasEmbeddedSeed()) {
                "Embedded configuration: assets/trustpin-seed.b64 (used only if every online source is unreachable)"
            } else {
                "Embedded configuration: none (assets/trustpin-seed.b64 is empty)"
            },
        )

        try {
            configurationRepository.configure(credentials)
            logger.success("TrustPin configuration successful")
        } catch (e: DomainError) {
            logger.error("TrustPin configuration failed: ${e.message}")
            throw e
        }
    }
}
