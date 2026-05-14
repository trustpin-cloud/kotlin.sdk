package cloud.trustpin.android.sample.domain.repository

/**
 * Use-case-facing log sink. Concrete implementations route lines to the UI
 * log feed, logcat, or both.
 *
 * Levels mirror the SDK's [cloud.trustpin.kotlin.sdk.TrustPinLogLevel] so the
 * same emission policy applies regardless of which side of the call (SDK
 * internal vs sample use case) produced the line.
 */
interface Logger {
    fun info(message: String)
    fun success(message: String)
    fun warning(message: String)
    fun error(message: String)
    fun debug(message: String)
}
