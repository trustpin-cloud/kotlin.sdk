package cloud.trustpin.android.sample.presentation

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
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
import cloud.trustpin.android.sample.databinding.CardLogBinding
import cloud.trustpin.android.sample.domain.model.AttestationStatus
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.launch

/**
 * Thin view layer. Forwards every user intent through [MainViewModel.dispatch]
 * and re-renders from the single [UiState] the ViewModel emits. No domain
 * types, no SDK calls, no coroutines that touch anything but the state Flow.
 *
 * Two tabs behind a bottom navigation bar: Pinning (kotlin-sdk) and
 * Attestation (kotlin-attest). The selected tab is part of [UiState], so it
 * survives configuration changes like everything else the screen shows.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val viewModel by viewModels<MainViewModel> {
        MainViewModelFactory((application as SampleApplication).locator)
    }

    private val pinning get() = binding.tabPinning
    private val attestation get() = binding.tabAttestation
    private val logCards: List<CardLogBinding> get() = listOf(pinning.logCard, attestation.logCard)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // The bottom navigation bar pads itself for the navigation-bar inset, so the
        // root takes only the top and side insets.
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        wireNavigation()
        wireClickListeners()
        observeState()
    }

    private fun wireNavigation() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            val tab = when (item.itemId) {
                R.id.nav_attestation -> UiState.Tab.Attestation
                else -> UiState.Tab.Pinning
            }
            viewModel.dispatch(UiAction.SelectTab(tab))
            true
        }
        // Re-selecting the current tab scrolls it back to the top.
        binding.bottomNav.setOnItemReselectedListener { item ->
            val scroller = if (item.itemId == R.id.nav_attestation) attestation.root else pinning.root
            scroller.smoothScrollTo(0, 0)
        }
    }

    private fun wireClickListeners() {
        // Pinning tab
        pinning.btnSetup.setOnClickListener {
            viewModel.dispatch(
                UiAction.Configure(
                    organizationId = pinning.etOrganizationId.text.toString(),
                    projectId = pinning.etProjectId.text.toString(),
                    publicKey = pinning.etPublicKey.text.toString(),
                )
            )
        }
        pinning.btnSetupFromAssets.setOnClickListener {
            viewModel.dispatch(UiAction.ConfigureFromAssets)
        }
        pinning.btnTestConnection.setOnClickListener {
            viewModel.dispatch(UiAction.TestConnection(pinning.etTestUrl.text.toString()))
        }
        pinning.btnFetchCertificate.setOnClickListener {
            viewModel.dispatch(UiAction.FetchCertificate(pinning.etTestUrl.text.toString()))
        }
        pinning.tvDashboardLink.setOnClickListener { openDashboardLink() }

        // Attestation tab
        attestation.btnGoToPinning.setOnClickListener {
            viewModel.dispatch(UiAction.SelectTab(UiState.Tab.Pinning))
        }
        attestation.btnSetupAttestation.setOnClickListener { viewModel.dispatch(UiAction.SetupAttestation) }
        attestation.btnCallProtected.setOnClickListener { viewModel.dispatch(UiAction.CallProtectedEndpoint) }
        attestation.btnSignOut.setOnClickListener { viewModel.dispatch(UiAction.SignOut) }
        attestation.btnResetAttestation.setOnClickListener { viewModel.dispatch(UiAction.ResetAttestation) }

        // Both tabs
        logCards.forEach { card ->
            card.btnClearLog.setOnClickListener { viewModel.dispatch(UiAction.ClearLog) }
        }
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { render(it) }
            }
        }
    }

    private fun render(state: UiState) {
        renderTabs(state)
        renderPinning(state)
        renderAttestation(state)
        renderLog(state)

        state.transientMessage?.let { message ->
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            viewModel.dispatch(UiAction.ConsumeTransientMessage)
        }
    }

    private fun renderTabs(state: UiState) {
        val showAttestation = state.selectedTab == UiState.Tab.Attestation
        pinning.root.visibility = if (showAttestation) View.GONE else View.VISIBLE
        attestation.root.visibility = if (showAttestation) View.VISIBLE else View.GONE

        val itemId = if (showAttestation) R.id.nav_attestation else R.id.nav_pinning
        // Guarded: setting the selection calls the listener, which dispatches the same tab.
        if (binding.bottomNav.selectedItemId != itemId) binding.bottomNav.selectedItemId = itemId
    }

    private fun renderPinning(state: UiState) {
        pinning.tvStatus.text = when (state.status) {
            UiState.Status.NotConfigured -> getString(R.string.status_not_configured)
            UiState.Status.Configured -> getString(R.string.status_configured)
            UiState.Status.Testing -> getString(R.string.status_testing)
            UiState.Status.FetchingCertificate -> getString(R.string.status_fetching_cert)
        }

        val canEditCredentials = !state.isConfigured && !state.isWorking
        pinning.etOrganizationId.isEnabled = canEditCredentials
        pinning.etProjectId.isEnabled = canEditCredentials
        pinning.etPublicKey.isEnabled = canEditCredentials

        pinning.btnSetup.isEnabled = canEditCredentials
        pinning.btnSetup.text = if (state.isConfigured) {
            getString(R.string.setup_trustpin_configured)
        } else {
            getString(R.string.setup_trustpin)
        }
        pinning.btnSetupFromAssets.isEnabled = canEditCredentials

        pinning.btnTestConnection.isEnabled = state.isConfigured && !state.isWorking
        pinning.btnFetchCertificate.isEnabled = !state.isWorking
    }

    private fun renderAttestation(state: UiState) {
        attestation.pinningRequiredBanner.visibility = if (state.isConfigured) View.GONE else View.VISIBLE
        attestation.btnSetupAttestation.isEnabled = state.isConfigured && !state.isAttestationSetUp && !state.isWorking
        attestation.btnCallProtected.isEnabled = state.isAttestationSetUp && !state.isWorking
        attestation.btnSignOut.isEnabled = state.isAttestationSetUp && !state.isWorking
        attestation.btnResetAttestation.isEnabled = state.isAttestationSetUp && !state.isWorking
        attestation.tvAttestationState.text = getString(R.string.attestation_state, describe(state.attestationStatus))
    }

    private fun describe(status: AttestationStatus): String = when (status) {
        AttestationStatus.NotSetUp -> getString(R.string.attestation_status_not_set_up)
        AttestationStatus.Unavailable -> getString(R.string.attestation_status_unavailable)
        AttestationStatus.Attesting -> getString(R.string.attestation_status_attesting)
        AttestationStatus.WaitingForSession -> getString(R.string.attestation_status_waiting_for_session)
        is AttestationStatus.Ready -> getString(R.string.attestation_status_ready, formatTime(status.expiresAtMillis))
        is AttestationStatus.Failed -> status.retryAtMillis
            ?.let { getString(R.string.attestation_status_failed, status.reason, formatTime(it)) }
            ?: getString(R.string.attestation_status_failed_no_retry, status.reason)
    }

    private fun formatTime(epochMillis: Long): String =
        DateFormat.getTimeInstance(DateFormat.MEDIUM).format(Date(epochMillis))

    private fun renderLog(state: UiState) {
        val text = logText(state)
        logCards.forEach { card ->
            card.tvLogOutput.text = text
            card.logScroll.post { card.logScroll.fullScroll(View.FOCUS_DOWN) }
        }
    }

    private fun logText(state: UiState): CharSequence {
        if (state.logEntries.isEmpty()) return getString(R.string.log_welcome)
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
