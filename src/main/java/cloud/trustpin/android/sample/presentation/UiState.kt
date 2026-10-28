package cloud.trustpin.android.sample.presentation

import cloud.trustpin.android.sample.domain.model.AttestationStatus

/**
 * Single source of truth the activity renders. The ViewModel emits one of
 * these for every state change; the activity diffs the parts it cares about
 * (button enablement, status banner text, log feed) without holding any
 * state of its own.
 */
data class UiState(
    val status: Status,
    val isConfigured: Boolean,
    val isWorking: Boolean,
    val logEntries: List<LogEntry>,
    val transientMessage: String? = null,
    val attestationStatus: AttestationStatus = AttestationStatus.NotSetUp,
    val selectedTab: Tab = Tab.Pinning,
) {
    val isAttestationSetUp: Boolean get() = attestationStatus != AttestationStatus.NotSetUp

    enum class Status { NotConfigured, Configured, Testing, FetchingCertificate }

    /** One bottom-navigation tab per SDK feature. */
    enum class Tab { Pinning, Attestation }

    companion object {
        val Initial = UiState(
            status = Status.NotConfigured,
            isConfigured = false,
            isWorking = false,
            logEntries = emptyList(),
        )
    }
}
