package cloud.trustpin.android.sample.domain.model

/**
 * Sealed error surface returned by use cases.
 *
 * The data layer maps raw exceptions (TrustPinError subclasses, IOException,
 * etc.) into one of these so the presentation layer can render outcomes
 * without depending on SDK or transport types.
 */
sealed class DomainError(message: String) : Exception(message) {

    /** Caller-supplied input failed validation before any I/O. */
    class Validation(message: String) : DomainError(message)

    /** TrustPin SDK rejected the configuration or a pin check. */
    class Pinning(val typeName: String, message: String) : DomainError(message)

    /** Transport-level failure during a network request. */
    class Network(message: String) : DomainError(message)

    /** Anything the data layer could not classify. */
    class Unknown(message: String) : DomainError(message)
}
