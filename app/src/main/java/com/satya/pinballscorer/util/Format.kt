package com.satya.pinballscorer.util

/** Formats whole seconds as "m:ss", e.g. 754 -> "12:34". */
fun formatDuration(totalSeconds: Int): String {
    val s = totalSeconds.coerceAtLeast(0)
    val minutes = s / 60
    val seconds = s % 60
    return "%d:%02d".format(minutes, seconds)
}
