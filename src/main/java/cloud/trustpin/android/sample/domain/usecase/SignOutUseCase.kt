package cloud.trustpin.android.sample.domain.usecase

import cloud.trustpin.android.sample.domain.repository.AttestationRepository
import cloud.trustpin.android.sample.domain.repository.Logger

/** End the sample API session; the next protected call logs in and binds a token to the new session. */
class SignOutUseCase(
    private val attestationRepository: AttestationRepository,
    private val logger: Logger,
) {

    operator fun invoke() {
        attestationRepository.signOut()
        logger.info("Signed out; the next protected call logs in again and binds a token to the new session")
    }
}
