package cloud.trustpin.android.sample.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cloud.trustpin.android.sample.domain.model.DomainError
import cloud.trustpin.android.sample.domain.model.PinningCredentials
import cloud.trustpin.android.sample.domain.repository.AttestationRepository
import cloud.trustpin.android.sample.domain.repository.ConfigurationRepository
import cloud.trustpin.android.sample.domain.usecase.CallProtectedEndpointUseCase
import cloud.trustpin.android.sample.domain.usecase.ConfigurePinningFromAssetsUseCase
import cloud.trustpin.android.sample.domain.usecase.ConfigurePinningUseCase
import cloud.trustpin.android.sample.domain.usecase.FetchCertificateUseCase
import cloud.trustpin.android.sample.domain.usecase.ResetAttestationUseCase
import cloud.trustpin.android.sample.domain.usecase.SetUpAttestationUseCase
import cloud.trustpin.android.sample.domain.usecase.SignOutUseCase
import cloud.trustpin.android.sample.domain.usecase.TestPinnedConnectionUseCase
import cloud.trustpin.kotlin.sdk.TrustPinMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Orchestrates use cases for the single-activity sample. Holds the canonical
 * [UiState] and exposes a single [dispatch] entry point — the activity has
 * no direct handle on the repositories or use cases.
 *
 * The ViewModel constructs its own [UiLogSink] so it can fold log entries
 * into [UiState.logEntries] as the use cases emit them. That keeps the log
 * feed declarative on the activity side (one render of `state.logEntries`).
 */
class MainViewModel(
    private val configurationRepository: ConfigurationRepository,
    private val configurePinning: (logger: UiLogSink) -> ConfigurePinningUseCase,
    private val configureFromAssets: (logger: UiLogSink) -> ConfigurePinningFromAssetsUseCase,
    private val testConnection: (logger: UiLogSink) -> TestPinnedConnectionUseCase,
    private val fetchCertificate: (logger: UiLogSink) -> FetchCertificateUseCase,
    private val attestationRepository: AttestationRepository,
    private val setUpAttestation: (logger: UiLogSink) -> SetUpAttestationUseCase,
    private val callProtectedEndpoint: (logger: UiLogSink) -> CallProtectedEndpointUseCase,
    private val signOut: (logger: UiLogSink) -> SignOutUseCase,
    private val resetAttestation: (logger: UiLogSink) -> ResetAttestationUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(UiState.Initial)
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val logSink = UiLogSink { entry ->
        _state.update { it.copy(logEntries = it.logEntries + entry) }
    }

    init {
        logSink.info("TrustPin Android Sample started")
        logSink.info("TrustPin configured for info-level logging")
        _state.update { it.copy(isConfigured = configurationRepository.isConfigured()) }
        // The attestation SDK's own log lines and state transitions appear on screen,
        // so device acceptance needs no logcat (spec 003 R-003-54).
        viewModelScope.launch {
            attestationRepository.status.collect { status -> _state.update { it.copy(attestationStatus = status) } }
        }
        viewModelScope.launch {
            attestationRepository.sdkLog.collect { line -> logSink.debug(line) }
        }
    }

    fun dispatch(action: UiAction) {
        when (action) {
            is UiAction.Configure -> handleConfigure(action)
            UiAction.ConfigureFromAssets -> handleConfigureFromAssets()
            is UiAction.TestConnection -> handleTestConnection(action.url)
            is UiAction.FetchCertificate -> handleFetchCertificate(action.url)
            UiAction.ClearLog -> _state.update { it.copy(logEntries = emptyList()) }
            UiAction.SetupAttestation -> handleSetupAttestation()
            UiAction.CallProtectedEndpoint -> handleCallProtected()
            UiAction.SignOut -> handleSignOut()
            UiAction.ResetAttestation -> handleResetAttestation()
            UiAction.ConsumeTransientMessage -> _state.update { it.copy(transientMessage = null) }
            is UiAction.SelectTab -> _state.update { it.copy(selectedTab = action.tab) }
        }
    }

    private fun handleConfigure(action: UiAction.Configure) {
        viewModelScope.launch {
            _state.update { it.copy(isWorking = true) }
            try {
                configurePinning(logSink).invoke(
                    PinningCredentials(
                        organizationId = action.organizationId.trim(),
                        projectId = action.projectId.trim(),
                        publicKey = action.publicKey.trim(),
                        mode = TrustPinMode.STRICT,
                    )
                )
                _state.update {
                    it.copy(
                        isConfigured = true,
                        isWorking = false,
                        status = UiState.Status.Configured,
                        transientMessage = "TrustPin configured successfully!",
                    )
                }
            } catch (e: DomainError) {
                _state.update {
                    it.copy(
                        isConfigured = configurationRepository.isConfigured(),
                        isWorking = false,
                        transientMessage = "Configuration failed: ${e.message}",
                    )
                }
            }
        }
    }

    private fun handleConfigureFromAssets() {
        viewModelScope.launch {
            _state.update { it.copy(isWorking = true) }
            try {
                configureFromAssets(logSink).invoke()
                _state.update {
                    it.copy(
                        isConfigured = true,
                        isWorking = false,
                        status = UiState.Status.Configured,
                        transientMessage = "TrustPin configured from trustpin.json",
                    )
                }
            } catch (e: DomainError) {
                _state.update {
                    it.copy(
                        isConfigured = configurationRepository.isConfigured(),
                        isWorking = false,
                        transientMessage = "Configuration failed: ${e.message}",
                    )
                }
            }
        }
    }

    private fun handleTestConnection(url: String) {
        viewModelScope.launch {
            _state.update { it.copy(isWorking = true, status = UiState.Status.Testing) }
            try {
                testConnection(logSink).invoke(url.trim())
                _state.update {
                    it.copy(
                        isWorking = false,
                        status = UiState.Status.Configured,
                        transientMessage = "Connection test successful!",
                    )
                }
            } catch (e: DomainError) {
                val message = when (e) {
                    is DomainError.Pinning -> "TrustPin validation failed: ${e.message}"
                    else -> "Connection failed: ${e.message}"
                }
                _state.update {
                    it.copy(
                        isWorking = false,
                        status = if (it.isConfigured) UiState.Status.Configured else UiState.Status.NotConfigured,
                        transientMessage = message,
                    )
                }
            }
        }
    }

    private fun handleSetupAttestation() = runWork(failurePrefix = "Attestation setup failed") {
        setUpAttestation(logSink).invoke()
        "Attestation set up"
    }

    private fun handleCallProtected() = runWork(failurePrefix = "Protected call failed") {
        val outcome = callProtectedEndpoint(logSink).invoke()
        "Protected call: ${outcome.statusCode}"
    }

    private fun handleSignOut() {
        signOut(logSink).invoke()
    }

    private fun handleResetAttestation() = runWork(failurePrefix = "Attestation reset failed") {
        resetAttestation(logSink).invoke()
        "Attestation reset"
    }

    /**
     * Runs [work] with the busy flag set and shows its result (or the failure)
     * as the transient message. Use cases log the details; a [DomainError] is
     * the only failure they throw. Cancellation (the ViewModel being cleared)
     * propagates untouched.
     */
    private fun runWork(failurePrefix: String, work: suspend () -> String) {
        viewModelScope.launch {
            _state.update { it.copy(isWorking = true) }
            try {
                val message = work()
                _state.update { it.copy(isWorking = false, transientMessage = message) }
            } catch (e: DomainError) {
                _state.update { it.copy(isWorking = false, transientMessage = "$failurePrefix: ${e.message}") }
            }
        }
    }

    private fun handleFetchCertificate(url: String) {
        viewModelScope.launch {
            _state.update { it.copy(isWorking = true, status = UiState.Status.FetchingCertificate) }
            try {
                fetchCertificate(logSink).invoke(url.trim())
                _state.update {
                    it.copy(
                        isWorking = false,
                        status = if (it.isConfigured) UiState.Status.Configured else UiState.Status.NotConfigured,
                        transientMessage = "Certificate fetched successfully!",
                    )
                }
            } catch (e: DomainError) {
                _state.update {
                    it.copy(
                        isWorking = false,
                        status = if (it.isConfigured) UiState.Status.Configured else UiState.Status.NotConfigured,
                        transientMessage = "Fetch certificate failed: ${e.message}",
                    )
                }
            }
        }
    }
}
