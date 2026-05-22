package cloud.trustpin.android.sample.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cloud.trustpin.android.sample.domain.model.DomainError
import cloud.trustpin.android.sample.domain.model.PinningCredentials
import cloud.trustpin.android.sample.domain.repository.ConfigurationRepository
import cloud.trustpin.android.sample.domain.usecase.ConfigurePinningFromAssetsUseCase
import cloud.trustpin.android.sample.domain.usecase.ConfigurePinningUseCase
import cloud.trustpin.android.sample.domain.usecase.FetchCertificateUseCase
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
    }

    fun dispatch(action: UiAction) {
        when (action) {
            is UiAction.Configure -> handleConfigure(action)
            UiAction.ConfigureFromAssets -> handleConfigureFromAssets()
            is UiAction.TestConnection -> handleTestConnection(action.url)
            is UiAction.FetchCertificate -> handleFetchCertificate(action.url)
            UiAction.ClearLog -> _state.update { it.copy(logEntries = emptyList()) }
            UiAction.ConsumeTransientMessage -> _state.update { it.copy(transientMessage = null) }
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
