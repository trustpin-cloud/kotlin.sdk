package cloud.trustpin.android.sample

import android.content.Context
import cloud.trustpin.android.sample.data.repository.OkHttpNetworkRepository
import cloud.trustpin.android.sample.data.repository.TrustPinCertificateRepository
import cloud.trustpin.android.sample.data.repository.TrustPinConfigurationRepository
import cloud.trustpin.android.sample.domain.repository.CertificateRepository
import cloud.trustpin.android.sample.domain.repository.ConfigurationRepository
import cloud.trustpin.android.sample.domain.repository.Logger
import cloud.trustpin.android.sample.domain.repository.NetworkRepository
import cloud.trustpin.android.sample.domain.usecase.ConfigurePinningFromAssetsUseCase
import cloud.trustpin.android.sample.domain.usecase.ConfigurePinningUseCase
import cloud.trustpin.android.sample.domain.usecase.FetchCertificateUseCase
import cloud.trustpin.android.sample.domain.usecase.TestPinnedConnectionUseCase

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
class ServiceLocator(applicationContext: Context) {

    val configurationRepository: ConfigurationRepository =
        TrustPinConfigurationRepository(applicationContext)
    val networkRepository: NetworkRepository = OkHttpNetworkRepository()
    val certificateRepository: CertificateRepository = TrustPinCertificateRepository()

    fun configurePinningUseCase(logger: Logger): ConfigurePinningUseCase =
        ConfigurePinningUseCase(configurationRepository, logger)

    fun configurePinningFromAssetsUseCase(logger: Logger): ConfigurePinningFromAssetsUseCase =
        ConfigurePinningFromAssetsUseCase(configurationRepository, logger)

    fun testPinnedConnectionUseCase(logger: Logger): TestPinnedConnectionUseCase =
        TestPinnedConnectionUseCase(configurationRepository, networkRepository, logger)

    fun fetchCertificateUseCase(logger: Logger): FetchCertificateUseCase =
        FetchCertificateUseCase(certificateRepository, logger)
}
