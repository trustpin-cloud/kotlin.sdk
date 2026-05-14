package cloud.trustpin.android.sample.domain.usecase

import cloud.trustpin.android.sample.domain.model.ConnectionOutcome
import cloud.trustpin.android.sample.domain.model.DomainError
import cloud.trustpin.android.sample.domain.repository.ConfigurationRepository
import cloud.trustpin.android.sample.domain.repository.Logger
import cloud.trustpin.android.sample.domain.repository.NetworkRepository

/**
 * Perform a TrustPin-pinned HTTPS GET against [url].
 *
 * Logs the high-level milestones (start, validating, success/failure) at
 * `info`/`success`/`error` and the request details (method, target URL,
 * status code, body preview) at `debug`. The body preview is whatever the
 * [NetworkRepository] returns — truncation lives at the data boundary.
 */
class TestPinnedConnectionUseCase(
    private val configurationRepository: ConfigurationRepository,
    private val networkRepository: NetworkRepository,
    private val logger: Logger,
) {

    suspend operator fun invoke(url: String): ConnectionOutcome {
        if (!configurationRepository.isConfigured()) {
            logger.warning("Test connection failed: TrustPin not configured")
            throw DomainError.Validation("TrustPin is not configured")
        }

        if (url.isBlank()) {
            logger.warning("Test connection failed: No URL provided")
            throw DomainError.Validation("No URL provided")
        }

        logger.info("Testing connection to: $url")
        logger.info("Using TrustPin SSL certificate validation")
        logger.debug("Method: GET")
        logger.debug("URL: $url")
        logger.debug("User-Agent: TrustPin-Android-Sample/1.0.0")

        return try {
            val outcome = networkRepository.get(url)
            logger.success("Connection test successful!")
            logger.debug("Status: ${outcome.statusCode} ${outcome.message}")
            logger.debug("Response preview: ${outcome.bodyPreview}")
            outcome
        } catch (e: DomainError) {
            logger.error("Connection failed: ${e.message}")
            throw e
        }
    }
}
