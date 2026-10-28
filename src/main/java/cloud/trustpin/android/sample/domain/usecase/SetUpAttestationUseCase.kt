package cloud.trustpin.android.sample.domain.usecase

import cloud.trustpin.android.sample.domain.model.AttestationStatus
import cloud.trustpin.android.sample.domain.model.DomainError
import cloud.trustpin.android.sample.domain.repository.AttestationRepository
import cloud.trustpin.android.sample.domain.repository.ConfigurationRepository
import cloud.trustpin.android.sample.domain.repository.Logger

/**
 * Set attestation up. Attestation pairs with `TrustPin.default` and reaches its
 * verifier through it, so pinning must be configured first.
 */
class SetUpAttestationUseCase(
    private val configurationRepository: ConfigurationRepository,
    private val attestationRepository: AttestationRepository,
    private val logger: Logger,
) {

    suspend operator fun invoke() {
        if (!configurationRepository.isConfigured()) {
            logger.warning("Attestation setup refused: configure pinning first")
            throw DomainError.Validation("Configure pinning first")
        }
        if (attestationRepository.status.value != AttestationStatus.NotSetUp) {
            logger.warning("Attestation setup ignored: already set up")
            throw DomainError.Validation("Attestation is already set up")
        }

        logger.info("Setting up attestation from assets/trustpin.json...")
        try {
            attestationRepository.setUp()
            logger.success("Attestation set up; tokens are bound to the Authorization header")
        } catch (e: DomainError) {
            logger.error("Attestation setup failed: ${e.message}")
            throw e
        }
    }
}
