package cloud.trustpin.android.sample.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cloud.trustpin.android.sample.ServiceLocator

/**
 * Manual factory pulling collaborators from [ServiceLocator]. A real app
 * would use a DI framework — this is the simplest equivalent that keeps the
 * activity ignorant of construction.
 */
class MainViewModelFactory(
    private val locator: ServiceLocator,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(MainViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        return MainViewModel(
            configurationRepository = locator.configurationRepository,
            configurePinning = { logger -> locator.configurePinningUseCase(logger) },
            configureFromAssets = { logger -> locator.configurePinningFromAssetsUseCase(logger) },
            testConnection = { logger -> locator.testPinnedConnectionUseCase(logger) },
            fetchCertificate = { logger -> locator.fetchCertificateUseCase(logger) },
        ) as T
    }
}
