package cloud.trustpin.android.sample.presentation

import cloud.trustpin.android.sample.domain.model.AttestationStatus
import cloud.trustpin.android.sample.domain.model.DomainError
import cloud.trustpin.android.sample.domain.usecase.CallProtectedEndpointUseCase
import cloud.trustpin.android.sample.domain.usecase.ResetAttestationUseCase
import cloud.trustpin.android.sample.domain.usecase.SetUpAttestationUseCase
import cloud.trustpin.android.sample.domain.usecase.SignOutUseCase
import cloud.trustpin.android.sample.fakes.FakeAttestationRepository
import cloud.trustpin.android.sample.fakes.FakeConfigurationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelAttestationTest {

    private val configuration = FakeConfigurationRepository(configured = true)
    private val attestation = FakeAttestationRepository()
    private lateinit var viewModel: MainViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        viewModel = MainViewModel(
            configurationRepository = configuration,
            configurePinning = { error("not used") },
            configureFromAssets = { error("not used") },
            testConnection = { error("not used") },
            fetchCertificate = { error("not used") },
            attestationRepository = attestation,
            setUpAttestation = { logger -> SetUpAttestationUseCase(configuration, attestation, logger) },
            callProtectedEndpoint = { logger -> CallProtectedEndpointUseCase(attestation, logger) },
            signOut = { logger -> SignOutUseCase(attestation, logger) },
            resetAttestation = { logger -> ResetAttestationUseCase(attestation, logger) },
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val state get() = viewModel.state.value
    private fun logged(text: String) = state.logEntries.any { it.message.contains(text) }

    @Test
    fun statusFlowDrivesTheState() = runTest {
        assertFalse(state.isAttestationSetUp)
        attestation.statusFlow.value = AttestationStatus.Ready(expiresAtMillis = 42L)
        assertEquals(AttestationStatus.Ready(42L), state.attestationStatus)
        assertTrue(state.isAttestationSetUp)
    }

    @Test
    fun sdkLogLinesReachTheOnScreenLog() = runTest {
        attestation.sdkLog.emit("[attest/default] INFO: Attesting a new key")
        assertTrue(logged("Attesting a new key"))
    }

    @Test
    fun setUpThenCallProtectedEndpoint() = runTest {
        viewModel.dispatch(UiAction.SetupAttestation)
        assertTrue(state.isAttestationSetUp)
        assertEquals("Attestation set up", state.transientMessage)
        assertFalse(state.isWorking)

        viewModel.dispatch(UiAction.CallProtectedEndpoint)
        assertEquals("Protected call: 200", state.transientMessage)
        assertTrue(logged("GET /home answered 200"))
    }

    @Test
    fun failuresBecomeAMessageNotACrash() = runTest {
        attestation.statusFlow.value = AttestationStatus.Unavailable
        attestation.callError = DomainError.Network("Login failed: HTTP 401")
        viewModel.dispatch(UiAction.CallProtectedEndpoint)
        assertEquals("Protected call failed: Login failed: HTTP 401", state.transientMessage)
        assertFalse(state.isWorking)
    }

    @Test
    fun signOutAndReset() = runTest {
        attestation.statusFlow.value = AttestationStatus.Ready(1L)
        viewModel.dispatch(UiAction.SignOut)
        viewModel.dispatch(UiAction.ResetAttestation)
        assertEquals(1, attestation.signOutCalls)
        assertEquals(1, attestation.resetCalls)
        assertEquals(AttestationStatus.Unavailable, state.attestationStatus)
    }
}
