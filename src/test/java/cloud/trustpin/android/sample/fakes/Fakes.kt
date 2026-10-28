package cloud.trustpin.android.sample.fakes

import cloud.trustpin.android.sample.domain.model.AttestationStatus
import cloud.trustpin.android.sample.domain.model.DomainError
import cloud.trustpin.android.sample.domain.model.PinningCredentials
import cloud.trustpin.android.sample.domain.model.ProtectedCallOutcome
import cloud.trustpin.android.sample.domain.repository.AttestationRepository
import cloud.trustpin.android.sample.domain.repository.ConfigurationRepository
import cloud.trustpin.android.sample.domain.repository.Logger
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class RecordingLogger : Logger {
    val lines = mutableListOf<Pair<String, String>>()
    override fun info(message: String) { lines += "info" to message }
    override fun success(message: String) { lines += "success" to message }
    override fun warning(message: String) { lines += "warning" to message }
    override fun error(message: String) { lines += "error" to message }
    override fun debug(message: String) { lines += "debug" to message }
}

class FakeConfigurationRepository(var configured: Boolean = true) : ConfigurationRepository {
    override fun isConfigured(): Boolean = configured
    override suspend fun hasEmbeddedSeed(): Boolean = false
    override suspend fun configure(credentials: PinningCredentials) { configured = true }
    override suspend fun configureFromAssets(): PinningCredentials = error("not used")
}

class FakeAttestationRepository : AttestationRepository {
    val statusFlow = MutableStateFlow<AttestationStatus>(AttestationStatus.NotSetUp)
    override val status: StateFlow<AttestationStatus> = statusFlow
    override val sdkLog = MutableSharedFlow<String>(extraBufferCapacity = 16)

    var setUpError: DomainError? = null
    var outcome = ProtectedCallOutcome(200, loggedIn = true, attestationHeaderSent = true, greeting = "Welcome back, Demo!", attestationSummary = "android/production pass, bound")
    var callError: DomainError? = null
    var setUpCalls = 0
    var signOutCalls = 0
    var resetCalls = 0

    override suspend fun setUp() {
        setUpCalls++
        setUpError?.let { throw it }
        statusFlow.value = AttestationStatus.Unavailable
    }

    override suspend fun callProtectedEndpoint(): ProtectedCallOutcome {
        callError?.let { throw it }
        return outcome
    }

    override fun signOut() { signOutCalls++ }

    override suspend fun reset() {
        resetCalls++
        statusFlow.value = AttestationStatus.Unavailable
    }
}
