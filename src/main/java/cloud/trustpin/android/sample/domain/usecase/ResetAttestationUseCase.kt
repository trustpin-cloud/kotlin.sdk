package cloud.trustpin.android.sample.domain.usecase

import cloud.trustpin.android.sample.domain.model.DomainError
import cloud.trustpin.android.sample.domain.repository.AttestationRepository
import cloud.trustpin.android.sample.domain.repository.Logger

/** Discard the attestation key and token; the next protected call attests a new key. */
class ResetAttestationUseCase(
    private val attestationRepository: AttestationRepository,
    private val logger: Logger,
) {

    suspend operator fun invoke() {
        try {
            attestationRepository.reset()
            logger.info("Attestation reset; the next request attests a new key")
        } catch (e: DomainError) {
            logger.error("Attestation reset failed: ${e.message}")
            throw e
        }
    }
}
