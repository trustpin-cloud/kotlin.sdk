package cloud.trustpin.android.sample

import android.app.Application
import android.util.Log
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
        // Route SDK log output into the app's own logging pipeline. Without a
        // sink the SDK already writes to logcat under its own tag; this
        // demonstrates redirecting it — a real app would forward to Timber,
        // Crashlytics breadcrumbs, etc. The sink runs on SDK-internal threads,
        // so keep it non-blocking.
        TrustPin.setLogSink { level, instanceId, message ->
            val priority = when (level) {
                TrustPinLogLevel.ERROR -> Log.ERROR
                TrustPinLogLevel.INFO -> Log.INFO
                else -> Log.DEBUG
            }
            Log.println(priority, LOG_TAG, "[$instanceId] $message")
        }
        locator = ServiceLocator(applicationContext)
    }

    private companion object {
        const val LOG_TAG = "TrustPinSample"
    }
}
