package cloud.trustpin.android.sample.domain

import cloud.trustpin.android.sample.domain.model.AttestationStatus
import cloud.trustpin.android.sample.domain.model.DomainError
import cloud.trustpin.android.sample.domain.usecase.CallProtectedEndpointUseCase
import cloud.trustpin.android.sample.domain.usecase.SetUpAttestationUseCase
import cloud.trustpin.android.sample.fakes.FakeAttestationRepository
import cloud.trustpin.android.sample.fakes.FakeConfigurationRepository
import cloud.trustpin.android.sample.fakes.RecordingLogger
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class AttestationUseCasesTest {

    private val logger = RecordingLogger()
    private val attestation = FakeAttestationRepository()

    @Test
    fun setUpRequiresPinningFirst() = runTest {
        val useCase = SetUpAttestationUseCase(FakeConfigurationRepository(configured = false), attestation, logger)
        assertFailsWith<DomainError.Validation> { useCase() }
        assertEquals(0, attestation.setUpCalls)
    }

    @Test
    fun setUpOnlyOnce() = runTest {
        val useCase = SetUpAttestationUseCase(FakeConfigurationRepository(), attestation, logger)
        useCase()
        assertEquals(AttestationStatus.Unavailable, attestation.status.value)
        assertFailsWith<DomainError.Validation> { useCase() }
        assertEquals(1, attestation.setUpCalls)
    }

    @Test
    fun setUpFailureIsLoggedAndRethrown() = runTest {
        attestation.setUpError = DomainError.Attestation("InvalidConfiguration", "InvalidConfiguration")
        val useCase = SetUpAttestationUseCase(FakeConfigurationRepository(), attestation, logger)
        assertFailsWith<DomainError.Attestation> { useCase() }
        assertTrue(logger.lines.any { it.first == "error" && it.second.contains("InvalidConfiguration") })
    }

    @Test
    fun protectedCallRequiresSetUp() = runTest {
        assertFailsWith<DomainError.Validation> { CallProtectedEndpointUseCase(attestation, logger)() }
    }

    @Test
    fun protectedCallLogsTheApiVerdict() = runTest {
        attestation.statusFlow.value = AttestationStatus.Unavailable
        val outcome = CallProtectedEndpointUseCase(attestation, logger)()
        assertEquals(200, outcome.statusCode)
        assertTrue(logger.lines.any { it.first == "success" && it.second.startsWith("GET /home answered 200") })
        assertTrue(logger.lines.any { it.second == "API attestation: android/production pass, bound" })
    }

    @Test
    fun aRejectedCallIsAWarningNotAnError() = runTest {
        attestation.statusFlow.value = AttestationStatus.Unavailable
        attestation.outcome = attestation.outcome.copy(statusCode = 401, attestationHeaderSent = false, attestationSummary = "rejected: no TrustPin-Attestation header")
        val outcome = CallProtectedEndpointUseCase(attestation, logger)()
        assertEquals(401, outcome.statusCode)
        assertTrue(logger.lines.any { it.first == "warning" && it.second.startsWith("GET /home answered 401") })
    }
}
