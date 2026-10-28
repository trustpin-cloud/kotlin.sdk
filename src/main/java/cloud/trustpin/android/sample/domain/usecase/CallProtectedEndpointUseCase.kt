package cloud.trustpin.android.sample.domain.usecase

import cloud.trustpin.android.sample.domain.model.AttestationStatus
import cloud.trustpin.android.sample.domain.model.DomainError
import cloud.trustpin.android.sample.domain.model.ProtectedCallOutcome
import cloud.trustpin.android.sample.domain.repository.AttestationRepository
import cloud.trustpin.android.sample.domain.repository.Logger

/** Call the sample API's protected endpoint and log what the API made of the attestation. */
class CallProtectedEndpointUseCase(
    private val attestationRepository: AttestationRepository,
    private val logger: Logger,
) {

    suspend operator fun invoke(): ProtectedCallOutcome {
        if (attestationRepository.status.value == AttestationStatus.NotSetUp) {
            logger.warning("Protected call refused: set attestation up first")
            throw DomainError.Validation("Set attestation up first")
        }

        logger.info("Calling the protected endpoint...")
        val outcome = try {
            attestationRepository.callProtectedEndpoint()
        } catch (e: DomainError) {
            logger.error("Protected call failed: ${e.message}")
            throw e
        }

        if (outcome.loggedIn) logger.info("Logged in to the sample API (new session)")
        val line = "GET /home answered ${outcome.statusCode} (attestation header sent: ${outcome.attestationHeaderSent})"
        if (outcome.isSuccess) logger.success(line) else logger.warning(line)
        outcome.greeting?.let { logger.info("API: $it") }
        outcome.attestationSummary?.let { logger.info("API attestation: $it") }
        return outcome
    }
}
