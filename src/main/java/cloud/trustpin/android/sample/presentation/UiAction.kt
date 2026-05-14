package cloud.trustpin.android.sample.presentation

/**
 * User intents the activity forwards to [MainViewModel.dispatch]. Keeping
 * these as a sealed hierarchy means new buttons add one case here and one
 * branch in the ViewModel, never a new public method on the ViewModel.
 */
sealed interface UiAction {
    data class Configure(
        val organizationId: String,
        val projectId: String,
        val publicKey: String,
    ) : UiAction

    data object ConfigureFromAssets : UiAction

    data class TestConnection(val url: String) : UiAction

    data class FetchCertificate(val url: String) : UiAction

    data object ClearLog : UiAction

    data object ConsumeTransientMessage : UiAction
}
