package cloud.trustpin.android.sample.data.repository

import cloud.trustpin.android.sample.domain.model.CertificateInfo
import cloud.trustpin.android.sample.domain.model.DomainError
import cloud.trustpin.android.sample.domain.repository.CertificateRepository
import cloud.trustpin.kotlin.sdk.TrustPin
import cloud.trustpin.kotlin.sdk.TrustPinError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest

/**
 * Calls into [TrustPin.fetchCertificate] and derives the SHA-256 of the PEM
 * payload. Wraps the raw exceptions into [DomainError].
 */
class TrustPinCertificateRepository : CertificateRepository {

    override suspend fun fetch(host: String): CertificateInfo = withContext(Dispatchers.IO) {
        try {
            val pem = TrustPin.fetchCertificate(host)
            val sha256 = MessageDigest.getInstance("SHA-256")
                .digest(pem.toByteArray())
                .joinToString("") { "%02x".format(it) }

            CertificateInfo(host = host, pem = pem, sha256 = sha256)
        } catch (e: TrustPinError) {
            throw DomainError.Pinning(
                typeName = e::class.simpleName ?: "TrustPinError",
                message = e.message ?: "Failed to fetch certificate",
            )
        } catch (e: Exception) {
            throw DomainError.Unknown(e.message ?: e::class.simpleName ?: "Unknown error")
        }
    }
}
