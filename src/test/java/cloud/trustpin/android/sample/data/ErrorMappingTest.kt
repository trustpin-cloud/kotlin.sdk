package cloud.trustpin.android.sample.data

import cloud.trustpin.android.sample.domain.model.DomainError
import kotlinx.coroutines.CancellationException
import java.io.IOException
import javax.net.ssl.SSLHandshakeException
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertSame

class ErrorMappingTest {

    @Test
    fun cancellationIsNeverWrapped() {
        val cancellation = CancellationException("cleared")
        assertSame(cancellation, cancellation.toDomainError("x"))
    }

    @Test
    fun domainErrorsPassThrough() {
        val error = DomainError.Validation("bad")
        assertSame(error, error.toDomainError("x"))
    }

    @Test
    fun ioFailuresAreNetworkErrors() {
        assertIs<DomainError.Network>(IOException("No route to host").toDomainError("x"))
        assertIs<DomainError.Network>(SSLHandshakeException("handshake").toDomainError("x"))
    }

    @Test
    fun anythingElseIsUnknown() {
        assertIs<DomainError.Unknown>(IllegalStateException("boom").toDomainError("x"))
    }
}
