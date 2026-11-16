package co.mipyme.ui.component

import java.text.Normalizer
import java.time.DateTimeException
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.chrono.IsoChronology
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.DecimalStyle
import java.time.format.FormatStyle
import java.time.format.ResolverStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

/** Gregorian, date-only limits. [isDateDisabled] must be pure and safe to call off the main thread. */
data class MoshiDateConstraints(
    val earliestDate: LocalDate = LocalDate.of(1, 1, 1),
    val latestDate: LocalDate = LocalDate.of(9999, 12, 31),
    val isDateDisabled: (LocalDate) -> Boolean = { false },
) {
    init {
        require(earliestDate.year >= 1 && latestDate.year <= 9999)
        require(!earliestDate.isAfter(latestDate))
    }

    fun allows(date: LocalDate): Boolean {
        return !date.isBefore(earliestDate) && !date.isAfter(latestDate) && !isDateDisabled(date)
    }
}

data class MoshiDateRange(val start: LocalDate, val endInclusive: LocalDate)
data class MoshiDatePreset(val label: String, val date: LocalDate)
data class MoshiDateRangePreset(val label: String, val range: MoshiDateRange)

/** Includes every day, so a disabled interior date invalidates the entire range. */
fun MoshiDateConstraints.allows(range: MoshiDateRange): Boolean {
    return validateDateRange(range, this)
}

internal fun validateDateRange(
    range: MoshiDateRange,
    constraints: MoshiDateConstraints,
    checkCancelled: () -> Unit = {},
): Boolean {
    if (range.start.isAfter(range.endInclusive) || !constraints.allows(range.start) || !constraints.allows(range.endInclusive)) {
        return false
    }
    var day = range.start
    while (day.isBefore(range.endInclusive)) {
        day = day.plusDays(1)
        if (day.dayOfMonth == 1) checkCancelled()
        if (!constraints.allows(day)) return false
    }
    return true
}

/** Locale order and separators with an unambiguous four-digit Gregorian year. */
fun moshiDateInputPattern(locale: Locale): String {
    val localized = DateTimeFormatterBuilder.getLocalizedDateTimePattern(
        FormatStyle.SHORT, null, IsoChronology.INSTANCE, locale,
    )
    return localized.replace(Regex("y+"), "uuuu")
}

internal fun dateInputFormatter(locale: Locale): DateTimeFormatter {
    return DateTimeFormatter.ofPattern(moshiDateInputPattern(locale), locale)
        .withChronology(IsoChronology.INSTANCE)
        .withDecimalStyle(DecimalStyle.of(locale))
        .withResolverStyle(ResolverStyle.STRICT)
}

fun parseMoshiDate(text: String, locale: Locale): LocalDate? {
    return try {
        LocalDate.parse(text.trim(), dateInputFormatter(locale)).takeIf { it.year in 1..9999 }
    } catch (_: DateTimeException) {
        null
    }
}

/** Defined EN/ES grammar; [today] makes relative phrases deterministic for callers and tests. */
fun parseMoshiNaturalDate(text: String, locale: Locale, today: LocalDate): LocalDate? {
    val normalized = normalizeDatePhrase(text)
    val language = locale.language
    if (language != "en" && language != "es") {
        return parseMoshiNaturalDate(text, Locale.ENGLISH, today)
            ?: parseMoshiNaturalDate(text, Locale.forLanguageTag("es"), today)
    }
    val offset = when (normalized) {
        "today" -> if (language == "en") 0L else null
        "tomorrow" -> if (language == "en") 1L else null
        "hoy" -> if (language == "es") 0L else null
        "manana" -> if (language == "es") 1L else null
        else -> null
    }
    return try {
        val result = when {
            offset != null -> today.plusDays(offset)
            language == "en" && normalized.startsWith("next ") -> {
                val weekday = englishWeekdays[normalized.removePrefix("next ")] ?: return null
                today.with(TemporalAdjusters.next(weekday))
            }
            language == "es" && Regex("(?:el )?proximo .+").matches(normalized) -> {
                val weekday = spanishWeekdays[normalized.removePrefix("el ").removePrefix("proximo ")]
                    ?: return null
                today.with(TemporalAdjusters.next(weekday))
            }
            else -> {
                val pattern = if (language == "en") {
                    Regex("in ([0-9]+) (day|days|week|weeks)")
                } else {
                    Regex("en ([0-9]+) (dia|dias|semana|semanas)")
                }
                val match = pattern.matchEntire(normalized) ?: return null
                val count = match.groupValues[1].toLongOrNull() ?: return null
                if (match.groupValues[2].startsWith("week") || match.groupValues[2].startsWith("semana")) {
                    today.plusWeeks(count)
                } else {
                    today.plusDays(count)
                }
            }
        }
        result.takeIf { it.year in 1..9999 }
    } catch (_: DateTimeException) {
        null
    } catch (_: ArithmeticException) {
        null
    }
}

private fun normalizeDatePhrase(text: String): String {
    return Normalizer.normalize(text.lowercase(Locale.ROOT), Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "").trim().replace(Regex("\\s+"), " ")
}

private val englishWeekdays = mapOf(
    "monday" to DayOfWeek.MONDAY, "tuesday" to DayOfWeek.TUESDAY,
    "wednesday" to DayOfWeek.WEDNESDAY, "thursday" to DayOfWeek.THURSDAY,
    "friday" to DayOfWeek.FRIDAY, "saturday" to DayOfWeek.SATURDAY, "sunday" to DayOfWeek.SUNDAY,
)
private val spanishWeekdays = mapOf(
    "lunes" to DayOfWeek.MONDAY, "martes" to DayOfWeek.TUESDAY,
    "miercoles" to DayOfWeek.WEDNESDAY, "jueves" to DayOfWeek.THURSDAY,
    "viernes" to DayOfWeek.FRIDAY, "sabado" to DayOfWeek.SATURDAY, "domingo" to DayOfWeek.SUNDAY,
)
