package cloud.trustpin.android.sample

import android.content.Context
import cloud.trustpin.android.sample.data.http.PinnedHttpClient
import cloud.trustpin.android.sample.data.repository.OkHttpNetworkRepository
import cloud.trustpin.android.sample.data.repository.TrustPinAttestationRepository
import cloud.trustpin.android.sample.data.repository.TrustPinCertificateRepository
import cloud.trustpin.android.sample.data.repository.TrustPinConfigurationRepository
import cloud.trustpin.android.sample.domain.repository.AttestationRepository
import cloud.trustpin.android.sample.domain.repository.CertificateRepository
import cloud.trustpin.android.sample.domain.repository.ConfigurationRepository
import cloud.trustpin.android.sample.domain.repository.Logger
import cloud.trustpin.android.sample.domain.repository.NetworkRepository
import cloud.trustpin.android.sample.domain.usecase.ConfigurePinningFromAssetsUseCase
import cloud.trustpin.android.sample.domain.usecase.ConfigurePinningUseCase
import cloud.trustpin.android.sample.domain.usecase.FetchCertificateUseCase
import cloud.trustpin.android.sample.domain.usecase.CallProtectedEndpointUseCase
import cloud.trustpin.android.sample.domain.usecase.ResetAttestationUseCase
import cloud.trustpin.android.sample.domain.usecase.SetUpAttestationUseCase
import cloud.trustpin.android.sample.domain.usecase.SignOutUseCase
import cloud.trustpin.android.sample.domain.usecase.TestPinnedConnectionUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Process-wide singletons + use-case factories. Sample-grade DI — one place
 * to wire concrete repositories, one factory per use case so each invocation
 * can take a request-scoped [Logger]. A real app would replace this with
 * Hilt/Koin/etc.
 *
 * Constructed once in [SampleApplication.onCreate] with the process's
 * [Application] context, which is held by repositories that need it (e.g.
 * `TrustPinConfigurationRepository` chains `withAndroidStorage(applicationContext)`
 * to attach the SDK's persistent integrity-check storage).
 */
class ServiceLocator(
    applicationContext: Context,
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    defaultDispatcher: CoroutineDispatcher = Dispatchers.Default,
) {

    /** One pinned OkHttp client pair per process (built on first use, after pinning is configured). */
    private val pinnedHttpClient = PinnedHttpClient()

    val configurationRepository: ConfigurationRepository =
        TrustPinConfigurationRepository(applicationContext, ioDispatcher)
    val networkRepository: NetworkRepository = OkHttpNetworkRepository(pinnedHttpClient)
    val certificateRepository: CertificateRepository = TrustPinCertificateRepository(defaultDispatcher)

    /** App attestation (kotlin-attest) over the default instance, paired with `TrustPin.default`. */
    val attestationRepository: AttestationRepository =
        TrustPinAttestationRepository(applicationContext, pinnedHttpClient, ioDispatcher)

    fun configurePinningUseCase(logger: Logger): ConfigurePinningUseCase =
        ConfigurePinningUseCase(configurationRepository, logger)

    fun configurePinningFromAssetsUseCase(logger: Logger): ConfigurePinningFromAssetsUseCase =
        ConfigurePinningFromAssetsUseCase(configurationRepository, logger)

    fun testPinnedConnectionUseCase(logger: Logger): TestPinnedConnectionUseCase =
        TestPinnedConnectionUseCase(configurationRepository, networkRepository, logger)

    fun fetchCertificateUseCase(logger: Logger): FetchCertificateUseCase =
        FetchCertificateUseCase(certificateRepository, logger)

    fun setUpAttestationUseCase(logger: Logger): SetUpAttestationUseCase =
        SetUpAttestationUseCase(configurationRepository, attestationRepository, logger)

    fun callProtectedEndpointUseCase(logger: Logger): CallProtectedEndpointUseCase =
        CallProtectedEndpointUseCase(attestationRepository, logger)

    fun signOutUseCase(logger: Logger): SignOutUseCase = SignOutUseCase(attestationRepository, logger)

    fun resetAttestationUseCase(logger: Logger): ResetAttestationUseCase =
        ResetAttestationUseCase(attestationRepository, logger)
}
