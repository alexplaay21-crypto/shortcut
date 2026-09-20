package com.sunflower.shortcut.automation.logs

/**
 * Pure trimming logic, kept separate from persistence so it's testable and
 * reusable from both data.repositories.LogRepository (trim-on-insert) and
 * automation.scheduler.LogTrimWorker (periodic sweep) without depending on
 * Room from either call site.
 */
object LogRetentionPolicy {

    /** Hard cap on total stored entries, across all automations. */
    const val MAX_TOTAL_ENTRIES = 500

    /** Cap per single automation, so one noisy scenario can't push out
     *  everyone else's recent history. */
    const val MAX_ENTRIES_PER_AUTOMATION = 50

    /** Entries older than this are dropped regardless of count. */
    const val MAX_AGE_MS = 30L * 24 * 60 * 60 * 1000 // 30 days

    /**
     * Returns the entries that should be *kept*. Input does not need to be
     * pre-sorted; output is newest-first.
     */
    fun apply(entries: List<LogEntry>, nowEpochMs: Long): List<LogEntry> {
        val notExpired = entries.filter { nowEpochMs - it.startedAtEpochMs <= MAX_AGE_MS }
        val sorted = notExpired.sortedByDescending { it.startedAtEpochMs }

        val perAutomationCount = mutableMapOf<String, Int>()
        val kept = mutableListOf<LogEntry>()
        for (entry in sorted) {
            val count = perAutomationCount.getOrDefault(entry.automationId, 0)
            if (count >= MAX_ENTRIES_PER_AUTOMATION) continue
            perAutomationCount[entry.automationId] = count + 1
            kept += entry
            if (kept.size >= MAX_TOTAL_ENTRIES) break
        }
        return kept
    }
}
