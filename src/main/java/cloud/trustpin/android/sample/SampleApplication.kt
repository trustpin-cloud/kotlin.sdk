package cloud.trustpin.android.sample

import android.app.Application
import cloud.trustpin.kotlin.sdk.TrustPin
import cloud.trustpin.kotlin.sdk.TrustPinLogLevel

/**
 * Application owns the [ServiceLocator] and sets the TrustPin SDK log level
 * once at process start. Activities reach the locator through
 * `(application as SampleApplication).locator`.
 */
class SampleApplication : Application() {

    lateinit var locator: ServiceLocator
        private set

    override fun onCreate() {
        super.onCreate()
        TrustPin.default.setLogLevel(TrustPinLogLevel.INFO)
        locator = ServiceLocator(applicationContext)
    }
}
