package co.mipyme.ui.util

import java.util.Locale

/** Returns uppercase initials from the first two whitespace-separated words, or empty for blank text. */
fun String.initials(): String {
    return trim()
        .splitToSequence(Regex("[\\s\\p{Z}]+"))
        .filter { it.isNotEmpty() }
        .take(2)
        .joinToString("") { word ->
            word.substring(0, word.offsetByCodePoints(0, 1)).uppercase(Locale.ROOT)
        }
}
