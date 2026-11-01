package cloud.trustpin.android.sample.domain.usecase

import cloud.trustpin.android.sample.domain.model.AttestationStatus
import cloud.trustpin.android.sample.domain.model.DomainError
import cloud.trustpin.android.sample.domain.model.ProtectedCallOutcome
import cloud.trustpin.android.sample.domain.repository.AttestationRepository
import cloud.trustpin.android.sample.domain.repository.Logger

/**
 * Call the sample API's sensitive operation with the attestation token and an
 * assertion over the request body, and log what the API made of both.
 */
class CallSensitiveOperationUseCase(
    private val attestationRepository: AttestationRepository,
    private val logger: Logger,
) {

    suspend operator fun invoke(): ProtectedCallOutcome {
        if (attestationRepository.status.value == AttestationStatus.NotSetUp) {
            logger.warning("Sensitive call refused: set attestation up first")
            throw DomainError.Validation("Set attestation up first")
        }

        logger.info("Calling the sensitive operation (token + assertion)...")
        val outcome = try {
            attestationRepository.callSensitiveEndpoint()
        } catch (e: DomainError) {
            logger.error("Sensitive call failed: ${e.message}")
            throw e
        }

        if (outcome.loggedIn) logger.info("Logged in to the sample API (new session)")
        val line = "POST /transfer answered ${outcome.statusCode} " +
            "(attestation header sent: ${outcome.attestationHeaderSent}, assertion header sent: ${outcome.assertionHeaderSent})"
        if (outcome.isSuccess) logger.success(line) else logger.warning(line)
        logger.debug("Sent headers: ${outcome.sentHeaderNames.sorted().joinToString()}")
        outcome.greeting?.let { logger.info("API: $it") }
        outcome.attestationSummary?.let { logger.info("API attestation: $it") }
        if (!outcome.isSuccess) outcome.responseExcerpt?.let { logger.debug("API response: $it") }
        return outcome
    }
}
