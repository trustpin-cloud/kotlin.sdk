package cloud.trustpin.android.sample.domain.usecase

import cloud.trustpin.android.sample.domain.model.CertificateInfo
import cloud.trustpin.android.sample.domain.model.DomainError
import cloud.trustpin.android.sample.domain.repository.CertificateRepository
import cloud.trustpin.android.sample.domain.repository.Logger
import java.net.URI

/**
 * Fetch the remote certificate for the host part of a user-supplied URL and
 * compute its SHA-256 digest. The raw PEM + digest also live on the returned
 * [CertificateInfo] so the activity can render them in a copy-friendly UI
 * surface alongside the log feed.
 */
class FetchCertificateUseCase(
    private val certificateRepository: CertificateRepository,
    private val logger: Logger,
) {

    suspend operator fun invoke(url: String): CertificateInfo {
        if (url.isBlank()) {
            logger.warning("Fetch certificate failed: No URL provided")
            throw DomainError.Validation("No URL provided")
        }

        val host = try {
            URI(url).host ?: throw IllegalArgumentException("missing host")
        } catch (_: Exception) {
            logger.warning("Fetch certificate failed: Invalid URL")
            throw DomainError.Validation("Invalid URL")
        }

        logger.info("Fetching certificate from: $host")

        return try {
            val info = certificateRepository.fetch(host)
            logger.success("Certificate fetched successfully!")
            logger.info("Certificate SHA256: ${info.sha256}")
            logger.debug("Certificate body:\n${info.pem}")
            info
        } catch (e: DomainError) {
            logger.error("Failed to fetch certificate: ${e.message}")
            throw e
        }
    }
}
