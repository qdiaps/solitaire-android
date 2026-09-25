package io.github.qdiaps.solitaire.ui.game.components

/**
 * Formats elapsed play time in seconds into classic Solitaire "mm:ss" format.
 *
 * If [seconds] is 0 or negative, returns "00:00".
 * If [seconds] is 3600 or more, displays total minutes (e.g., 3661 -> "61:01").
 */
fun formatTime(seconds: Long): String {
    val totalSeconds = seconds.coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val remainingSeconds = totalSeconds % 60L

    val minStr = minutes.toString().padStart(2, '0')
    val secStr = remainingSeconds.toString().padStart(2, '0')
    return "$minStr:$secStr"
}
