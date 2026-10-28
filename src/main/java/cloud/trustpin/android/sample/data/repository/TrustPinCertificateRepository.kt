package cloud.trustpin.android.sample.data.repository

import cloud.trustpin.android.sample.data.mapErrors
import cloud.trustpin.android.sample.domain.model.CertificateInfo
import cloud.trustpin.android.sample.domain.repository.CertificateRepository
import cloud.trustpin.kotlin.sdk.TrustPin
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.security.MessageDigest

/**
 * Calls into [TrustPin.fetchCertificate] (a main-safe suspend function) and
 * derives the SHA-256 of the PEM payload on [defaultDispatcher], since hashing is CPU work.
 */
class TrustPinCertificateRepository(
    private val defaultDispatcher: CoroutineDispatcher,
) : CertificateRepository {

    override suspend fun fetch(host: String): CertificateInfo = mapErrors("Failed to fetch certificate") {
        val pem = TrustPin.fetchCertificate(host)
        val sha256 = withContext(defaultDispatcher) {
            MessageDigest.getInstance("SHA-256")
                .digest(pem.toByteArray())
                .joinToString("") { "%02x".format(it) }
        }
        CertificateInfo(host = host, pem = pem, sha256 = sha256)
    }
}
