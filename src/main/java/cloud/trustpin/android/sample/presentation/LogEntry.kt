package cloud.trustpin.android.sample.presentation

/**
 * One line in the in-app log feed. The icon column mirrors what the original
 * activity printed inline so the visual look of the log feed is preserved.
 */
data class LogEntry(
    val timestamp: String,
    val level: Level,
    val message: String,
) {
    enum class Level(val icon: String) {
        INFO("⚙️"),
        SUCCESS("✅"),
        WARNING("⚠️"),
        ERROR("❌"),
        DEBUG("🐛"),
    }
}
