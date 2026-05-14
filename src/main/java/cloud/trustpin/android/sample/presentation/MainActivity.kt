package cloud.trustpin.android.sample.presentation

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import cloud.trustpin.android.sample.R
import cloud.trustpin.android.sample.SampleApplication
import cloud.trustpin.android.sample.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

/**
 * Thin view layer. Forwards every user intent through [MainViewModel.dispatch]
 * and re-renders from the single [UiState] the ViewModel emits. No domain
 * types, no SDK calls, no coroutines that touch anything but the state Flow.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val viewModel by viewModels<MainViewModel> {
        MainViewModelFactory(application, (application as SampleApplication).locator)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        wireClickListeners()
        observeState()
    }

    private fun wireClickListeners() {
        binding.btnSetup.setOnClickListener {
            viewModel.dispatch(
                UiAction.Configure(
                    organizationId = binding.etOrganizationId.text.toString(),
                    projectId = binding.etProjectId.text.toString(),
                    publicKey = binding.etPublicKey.text.toString(),
                )
            )
        }
        binding.btnSetupFromAssets.setOnClickListener {
            viewModel.dispatch(UiAction.ConfigureFromAssets)
        }
        binding.btnTestConnection.setOnClickListener {
            viewModel.dispatch(UiAction.TestConnection(binding.etTestUrl.text.toString()))
        }
        binding.btnFetchCertificate.setOnClickListener {
            viewModel.dispatch(UiAction.FetchCertificate(binding.etTestUrl.text.toString()))
        }
        binding.btnClearLog.setOnClickListener {
            viewModel.dispatch(UiAction.ClearLog)
        }
        binding.tvDashboardLink.setOnClickListener { openDashboardLink() }
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { render(it) }
            }
        }
    }

    private fun render(state: UiState) {
        binding.tvStatus.text = when (state.status) {
            UiState.Status.NotConfigured -> getString(R.string.status_not_configured)
            UiState.Status.Configured -> getString(R.string.status_configured)
            UiState.Status.Testing -> getString(R.string.status_testing)
            UiState.Status.FetchingCertificate -> getString(R.string.status_fetching_cert)
        }

        val canEditCredentials = !state.isConfigured && !state.isWorking
        binding.etOrganizationId.isEnabled = canEditCredentials
        binding.etProjectId.isEnabled = canEditCredentials
        binding.etPublicKey.isEnabled = canEditCredentials

        binding.btnSetup.isEnabled = canEditCredentials
        binding.btnSetup.text = if (state.isConfigured) {
            getString(R.string.setup_trustpin_configured)
        } else {
            getString(R.string.setup_trustpin)
        }
        binding.btnSetupFromAssets.isEnabled = canEditCredentials

        val canRunNetwork = state.isConfigured && !state.isWorking
        binding.btnTestConnection.isEnabled = canRunNetwork
        binding.btnFetchCertificate.isEnabled = !state.isWorking

        binding.tvLogOutput.text = renderLog(state)
        binding.tvLogOutput.post {
            (binding.tvLogOutput.parent as? android.widget.ScrollView)
                ?.fullScroll(android.widget.ScrollView.FOCUS_DOWN)
        }

        state.transientMessage?.let { message ->
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            viewModel.dispatch(UiAction.ConsumeTransientMessage)
        }
    }

    private fun renderLog(state: UiState): CharSequence {
        if (state.logEntries.isEmpty()) {
            return "Welcome to TrustPin Android Sample\nConfigure TrustPin and test connections...\n"
        }
        return buildString {
            for (entry in state.logEntries) {
                append('[').append(entry.timestamp).append("] ")
                append(entry.level.icon).append(' ')
                append(entry.message)
                append('\n')
            }
        }
    }

    private fun openDashboardLink() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.banner_link_url)))
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Log.w(TAG, "No activity available to open dashboard URL", e)
            Toast.makeText(this, "No browser available to open the dashboard link", Toast.LENGTH_LONG).show()
        }
    }

    private companion object {
        const val TAG = "TrustPinSample"
    }
}
