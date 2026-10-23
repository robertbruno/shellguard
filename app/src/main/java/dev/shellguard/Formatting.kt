package dev.shellguard

import java.util.Locale
import java.util.concurrent.TimeUnit

/** Formats a byte count using binary units. */
fun formatBytes(bytes: Long): String {
    if (bytes < 0) return "-"
    if (bytes < 1024) return "$bytes B"

    val units = listOf("KB", "MB", "GB", "TB")
    var value = bytes.toDouble() / 1024
    var unitIndex = 0

    while (value >= 1024 && unitIndex < units.lastIndex) {
        value /= 1024
        unitIndex++
    }

    return if (value >= 100) {
        String.format(Locale.getDefault(), "%.0f %s", value, units[unitIndex])
    } else {
        String.format(Locale.getDefault(), "%.1f %s", value, units[unitIndex])
    }
}

/**
 * Days remaining until [expiresAt], floored at zero. Returns null when the
 * expiry is unknown so callers can omit the hint rather than show a wrong one.
 */
fun daysUntil(expiresAt: Long?, now: Long): Int? {
    if (expiresAt == null) return null
    val remaining = expiresAt - now
    if (remaining <= 0) return 0
    return TimeUnit.MILLISECONDS.toDays(remaining).toInt()
}
