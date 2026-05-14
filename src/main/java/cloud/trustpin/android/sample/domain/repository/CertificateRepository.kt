package cloud.trustpin.android.sample.domain.repository

import cloud.trustpin.android.sample.domain.model.CertificateInfo

/**
 * Boundary for fetching a remote certificate via the TrustPin SDK and
 * deriving its SHA-256 digest. Hosts the raw PEM + digest as fields so
 * copy-friendly UI surfaces can render them without going through a log feed.
 */
interface CertificateRepository {
    suspend fun fetch(host: String): CertificateInfo
}
