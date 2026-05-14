package cloud.trustpin.android.sample.presentation

import cloud.trustpin.android.sample.domain.repository.Logger
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * [Logger] implementation that pushes formatted entries into a callback for
 * the ViewModel to fold into [UiState.logEntries].
 *
 * The sink stays in the presentation layer because the timestamp format and
 * the UI-bound side effect are presentation concerns; the use cases only
 * know about the [Logger] interface.
 */
class UiLogSink(
    private val onEntry: (LogEntry) -> Unit,
) : Logger {

    private val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    override fun info(message: String) = emit(LogEntry.Level.INFO, message)
    override fun success(message: String) = emit(LogEntry.Level.SUCCESS, message)
    override fun warning(message: String) = emit(LogEntry.Level.WARNING, message)
    override fun error(message: String) = emit(LogEntry.Level.ERROR, message)
    override fun debug(message: String) = emit(LogEntry.Level.DEBUG, message)

    private fun emit(level: LogEntry.Level, message: String) {
        onEntry(LogEntry(timestamp = dateFormat.format(Date()), level = level, message = message))
    }
}
