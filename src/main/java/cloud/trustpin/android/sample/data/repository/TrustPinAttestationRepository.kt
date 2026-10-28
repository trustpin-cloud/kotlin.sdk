package cloud.trustpin.android.sample.data.repository

import android.content.Context
import cloud.trustpin.android.sample.data.config.SampleApiConfig
import cloud.trustpin.android.sample.data.http.PinnedHttpClient
import cloud.trustpin.android.sample.data.http.executeAsync
import cloud.trustpin.android.sample.data.mapErrors
import cloud.trustpin.android.sample.data.remote.SampleApiResponses
import cloud.trustpin.android.sample.domain.model.AttestationStatus
import cloud.trustpin.android.sample.domain.model.DomainError
import cloud.trustpin.android.sample.domain.model.ProtectedCallOutcome
import cloud.trustpin.android.sample.domain.repository.AttestationRepository
import cloud.trustpin.kotlin.attest.TrustPinAttest
import cloud.trustpin.kotlin.attest.TrustPinAttestConfiguration
import cloud.trustpin.kotlin.attest.TrustPinAttestLogLevel
import cloud.trustpin.kotlin.attest.fromAssets
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * [AttestationRepository] over `TrustPinAttest.default` and the sample API
 * (`cloud-console-attest-sample-apps/api-mock`), mirroring the iOS sample's
 * `TrustPinAttestRepository.swift`:
 *
 * - [setUp]: set up from `assets/trustpin.json`, bind tokens to the
 *   `Authorization` header, prefetch.
 * - [callProtectedEndpoint]: `POST /login` with the demo user when there is no
 *   session (no attestation header: there is no session to bind a token to
 *   yet), then `GET /home` with the bearer token. The OkHttp adapter adds the
 *   `TrustPin-Attestation` header, bound to that `Authorization` value, and the
 *   API echoes the verified claims. A `401` drops the session.
 * - [signOut] drops the session, so the next token is bound to a new one.
 *
 * The test API's URL and demo login come from `assets/test-api.json`
 * ([SampleApiConfig]), kept apart from the SDK configuration in `trustpin.json`.
 *
 * One instance per process (the SDK's log sink and state listener are
 * process-wide). Main-safe: network calls run on OkHttp's threads, asset reads
 * on [ioDispatcher].
 */
class TrustPinAttestationRepository(
    private val applicationContext: Context,
    private val httpClient: PinnedHttpClient,
    private val ioDispatcher: CoroutineDispatcher,
) : AttestationRepository {

    private val _status = MutableStateFlow<AttestationStatus>(AttestationStatus.NotSetUp)
    override val status: StateFlow<AttestationStatus> = _status.asStateFlow()

    // The SDK calls the sink on its own threads; never block it, drop the oldest line if nobody keeps up.
    private val _sdkLog = MutableSharedFlow<String>(extraBufferCapacity = 64, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    override val sdkLog: Flow<String> = _sdkLog.asSharedFlow()

    /** Bearer token from `POST /login`; null until logged in, and after sign out or a 401. */
    @Volatile private var sessionToken: String? = null

    init {
        TrustPinAttest.default.setLogLevel(TrustPinAttestLogLevel.DEBUG)
        TrustPinAttest.setLogSink { level, instanceId, message ->
            _sdkLog.tryEmit("[attest/$instanceId] ${level.name}: $message")
        }
        TrustPinAttest.default.setStateListener { state ->
            if (_status.value != AttestationStatus.NotSetUp) _status.value = state.toAttestationStatus()
        }
    }

    override suspend fun setUp() = mapErrors("Attestation setup failed") {
        val configuration = withContext(ioDispatcher) { TrustPinAttestConfiguration.fromAssets(applicationContext) }
        TrustPinAttest.default.setup(applicationContext, configuration)
        // Tokens follow the session: bind them to the Authorization header the protected calls carry.
        TrustPinAttest.default.bindToHeader(AUTHORIZATION)
        _status.value = TrustPinAttest.default.state.toAttestationStatus()
        TrustPinAttest.default.prefetch()
    }

    override suspend fun callProtectedEndpoint(): ProtectedCallOutcome = mapErrors("Protected call failed") {
        val api = loadTestApi()
        val existing = sessionToken
        val token = existing ?: login(api).also { sessionToken = it }

        val request = Request.Builder()
            .url("${api.baseUrl}/home")
            .header(AUTHORIZATION, "Bearer $token")
            .build()
        val result = httpClient.withAttestation.newCall(request).executeAsync()
        if (result.code == 401) sessionToken = null // the session expired or was refused; log in again next time

        val home = SampleApiResponses.home(result.body)
        ProtectedCallOutcome(
            statusCode = result.code,
            loggedIn = existing == null,
            attestationHeaderSent = TrustPinAttest.HEADER_NAME in result.sentHeaderNames,
            greeting = home?.greeting,
            attestationSummary = home?.attestationSummary,
        )
    }

    override fun signOut() {
        sessionToken = null
    }

    override suspend fun reset() = mapErrors("Attestation reset failed") {
        if (_status.value != AttestationStatus.NotSetUp) TrustPinAttest.default.reset()
    }

    // --- private ----------------------------------------------------------------

    private suspend fun loadTestApi(): SampleApiConfig = withContext(ioDispatcher) {
        val text = try {
            applicationContext.assets.open(SampleApiConfig.ASSET).bufferedReader().use { it.readText() }
        } catch (_: java.io.IOException) {
            throw DomainError.Validation(SampleApiConfig.MISSING_MESSAGE)
        }
        SampleApiConfig.parse(text)
    }

    private suspend fun login(api: SampleApiConfig): String {
        val request = Request.Builder()
            .url("${api.baseUrl}/login")
            .post(SampleApiResponses.loginBody(api.username, api.password).toRequestBody(JSON))
            .build()
        val result = httpClient.withoutAttestation.newCall(request).executeAsync()
        if (result.code != 200) throw DomainError.Network("Login failed: HTTP ${result.code}")
        return SampleApiResponses.loginToken(result.body)
            ?: throw DomainError.Network("Login failed: no token in the response")
    }

    private companion object {
        const val AUTHORIZATION = "Authorization"
        val JSON = "application/json".toMediaType()
    }
}
