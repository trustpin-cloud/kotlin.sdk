package cloud.trustpin.android.sample.domain.model

/**
 * Certificate snapshot returned by [cloud.trustpin.android.sample.domain.repository.CertificateRepository].
 *
 * The raw [pem] and [sha256] are deliberately exposed as typed fields rather
 * than logged. UI surfaces that need them (a copy-friendly screen) can render
 * the values directly; use-case log lines must go through the redaction
 * helpers instead.
 */
data class CertificateInfo(
    val host: String,
    val pem: String,
    val sha256: String,
)
