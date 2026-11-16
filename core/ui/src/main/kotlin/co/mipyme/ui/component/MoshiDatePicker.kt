package co.mipyme.ui.component

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import co.mipyme.ui.Moshi
import co.mipyme.ui.MoshiPreview
import co.mipyme.ui.MoshiTheme
import co.mipyme.ui.R
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.DecimalStyle
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.withContext

@Composable
fun MoshiDatePicker(
    value: LocalDate?,
    onValueChange: (LocalDate?) -> Unit,
    modifier: Modifier = Modifier,
    label: String = stringResource(R.string.moshi_date_choose),
    enabled: Boolean = true,
    allowClear: Boolean = false,
    constraints: MoshiDateConstraints = MoshiDateConstraints(),
    presets: List<MoshiDatePreset> = emptyList(),
) {
    DatePickerTrigger(value, onValueChange, modifier, label, enabled, allowClear, constraints, presets)
}

@Composable
fun MoshiBirthDatePicker(
    value: LocalDate?,
    onValueChange: (LocalDate?) -> Unit,
    modifier: Modifier = Modifier,
    label: String = stringResource(R.string.moshi_date_birth_choose),
    enabled: Boolean = true,
    allowClear: Boolean = false,
    constraints: MoshiDateConstraints = MoshiDateConstraints(),
    today: LocalDate = LocalDate.now(),
) {
    val latest = minOf(constraints.latestDate, today)
    val birthConstraints = MoshiDateConstraints(
        earliestDate = minOf(constraints.earliestDate, latest), latestDate = latest,
        isDateDisabled = { !constraints.allows(it) },
    )
    DatePickerTrigger(value, onValueChange, modifier, label, enabled, allowClear, birthConstraints, emptyList(), today = today)
}

@Composable
fun MoshiDateInputPicker(
    value: LocalDate?,
    onValueChange: (LocalDate?) -> Unit,
    modifier: Modifier = Modifier,
    label: String = stringResource(R.string.moshi_date_choose),
    enabled: Boolean = true,
    allowClear: Boolean = false,
    constraints: MoshiDateConstraints = MoshiDateConstraints(),
) {
    DatePickerTrigger(value, onValueChange, modifier, label, enabled, allowClear, constraints, emptyList(), DateInputMode.Typed)
}

@Composable
fun MoshiNaturalDatePicker(
    value: LocalDate?,
    onValueChange: (LocalDate?) -> Unit,
    modifier: Modifier = Modifier,
    label: String = stringResource(R.string.moshi_date_choose),
    enabled: Boolean = true,
    allowClear: Boolean = false,
    constraints: MoshiDateConstraints = MoshiDateConstraints(),
    today: LocalDate = LocalDate.now(),
) {
    DatePickerTrigger(value, onValueChange, modifier, label, enabled, allowClear, constraints, emptyList(), DateInputMode.Natural, today)
}

@Composable
fun MoshiDateTimePicker(
    value: LocalDateTime?,
    onValueChange: (LocalDateTime?) -> Unit,
    modifier: Modifier = Modifier,
    label: String = stringResource(R.string.moshi_date_time_choose),
    enabled: Boolean = true,
    allowClear: Boolean = false,
    constraints: MoshiDateConstraints = MoshiDateConstraints(),
) {
    var open by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(enabled) { if (!enabled) open = false }
    val locale = pickerLocale()
    val use24Hour = DateFormat.is24HourFormat(LocalContext.current)
    PickerTrigger(
        label = label,
        value = value?.let {
            stringResource(R.string.moshi_date_time_value, formatDate(it.toLocalDate(), locale), formatTime(it.toLocalTime(), locale, use24Hour))
        },
        enabled = enabled, modifier = modifier, onClick = { open = true },
    )
    if (open) {
        DateDialog(
            value = value?.toLocalDate(), onApply = {}, onDismissRequest = { open = false },
            title = label, constraints = constraints, allowClear = allowClear,
            initialTime = value?.toLocalTime() ?: LocalTime.NOON,
            onDateTimeApply = { onValueChange(it); open = false },
        )
    }
}

@Composable
fun MoshiDateRangePicker(
    value: MoshiDateRange?,
    onValueChange: (MoshiDateRange?) -> Unit,
    modifier: Modifier = Modifier,
    label: String = stringResource(R.string.moshi_date_range_choose),
    enabled: Boolean = true,
    allowClear: Boolean = false,
    constraints: MoshiDateConstraints = MoshiDateConstraints(),
    presets: List<MoshiDateRangePreset> = emptyList(),
) {
    var open by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(enabled) { if (!enabled) open = false }
    val locale = pickerLocale()
    PickerTrigger(
        label = label, value = value?.let { rangeLabel(it, locale) },
        enabled = enabled, modifier = modifier, onClick = { open = true },
    )
    if (open) {
        MoshiDateRangePickerDialog(
            value = value, onApply = { onValueChange(it); open = false },
            onDismissRequest = { open = false }, title = label,
            constraints = constraints, allowClear = allowClear, presets = presets,
        )
    }
}

/** A standalone dialog for callers that own their trigger. Dismissal never invokes [onApply]. */
@Composable
fun MoshiDatePickerDialog(
    value: LocalDate?,
    onApply: (LocalDate?) -> Unit,
    onDismissRequest: () -> Unit,
    title: String = stringResource(R.string.moshi_date_choose),
    constraints: MoshiDateConstraints = MoshiDateConstraints(),
    allowClear: Boolean = false,
    presets: List<MoshiDatePreset> = emptyList(),
) {
    DateDialog(value, onApply, onDismissRequest, title, constraints, allowClear, presets)
}

private val DateStateSaver = Saver<MutableState<LocalDate?>, String>(
    save = { it.value?.toString().orEmpty() },
    restore = { value -> mutableStateOf(value.takeIf { it.isNotEmpty() }?.let(LocalDate::parse)) },
)
private val MonthSaver = Saver<YearMonth, String>(save = { it.toString() }, restore = { YearMonth.parse(it) })
private val TimeSaver = Saver<LocalTime, String>(save = { it.toString() }, restore = { LocalTime.parse(it) })

private enum class DateInputMode { Calendar, Typed, Natural }

@Composable
private fun DatePickerTrigger(
    value: LocalDate?, onValueChange: (LocalDate?) -> Unit, modifier: Modifier,
    label: String, enabled: Boolean, allowClear: Boolean, constraints: MoshiDateConstraints,
    presets: List<MoshiDatePreset>, inputMode: DateInputMode = DateInputMode.Calendar,
    today: LocalDate = LocalDate.now(),
) {
    var open by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(enabled) { if (!enabled) open = false }
    PickerTrigger(label, value?.let { formatDate(it, pickerLocale()) }, enabled, modifier) { open = true }
    if (open) {
        DateDialog(
            value, { onValueChange(it); open = false }, { open = false }, label,
            constraints, allowClear, presets, inputMode, today,
        )
    }
}

@Composable
private fun PickerTrigger(label: String, value: String?, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    MoshiButton(
        onClick = onClick, modifier = modifier, variant = MoshiButtonVariant.Outline, enabled = enabled,
        leadingContent = {
            Icon(painterResource(R.drawable.ic_fluent_calendar_24_regular), contentDescription = null)
        },
    ) {
        Column {
            MoshiText(label)
            if (value != null) MoshiText(value, role = MoshiTextRole.Copy14)
        }
    }
}

@Composable
private fun DateDialog(
    value: LocalDate?, onApply: (LocalDate?) -> Unit, onDismissRequest: () -> Unit, title: String,
    constraints: MoshiDateConstraints, allowClear: Boolean,
    presets: List<MoshiDatePreset> = emptyList(), inputMode: DateInputMode = DateInputMode.Calendar,
    today: LocalDate = LocalDate.now(), initialTime: LocalTime? = null,
    onDateTimeApply: ((LocalDateTime?) -> Unit)? = null,
) {
    val locale = pickerLocale()
    var draft by rememberSaveable(saver = DateStateSaver) { mutableStateOf(value) }
    var cleared by rememberSaveable { mutableStateOf(false) }
    var input by rememberSaveable { mutableStateOf(if (inputMode == DateInputMode.Typed) value?.format(dateInputFormatter(locale)).orEmpty() else "") }
    var inputEdited by rememberSaveable { mutableStateOf(false) }
    var month by rememberSaveable(stateSaver = MonthSaver) { mutableStateOf(YearMonth.from((value ?: today).coerceIn(constraints.earliestDate, constraints.latestDate))) }
    var time by rememberSaveable(stateSaver = TimeSaver) { mutableStateOf(initialTime ?: LocalTime.NOON) }
    val inputDate = when (inputMode) {
        DateInputMode.Calendar -> draft
        DateInputMode.Typed -> if (inputEdited) parseMoshiDate(input, locale) else draft
        DateInputMode.Natural -> if (inputEdited) parseMoshiNaturalDate(input, locale, today) else draft
    }
    val selected = if (cleared) null else inputDate
    val valid = selected?.let(constraints::allows) ?: (allowClear && cleared)
    val error = when {
        inputEdited && inputDate == null && !cleared -> if (inputMode == DateInputMode.Typed) {
            stringResource(R.string.moshi_date_input_error, visibleDatePattern(locale))
        } else naturalHelp(locale)
        selected != null && !constraints.allows(selected) -> stringResource(R.string.moshi_date_unavailable)
        else -> null
    }
    PickerDialogFrame(title, onDismissRequest) {
        if (inputMode != DateInputMode.Calendar) {
            MoshiTextField(
                value = input, onValueChange = {
                    input = it; inputEdited = true; cleared = false
                    val parsed = if (inputMode == DateInputMode.Typed) parseMoshiDate(it, locale) else parseMoshiNaturalDate(it, locale, today)
                    if (parsed != null) month = YearMonth.from(parsed.coerceIn(constraints.earliestDate, constraints.latestDate))
                },
                label = stringResource(if (inputMode == DateInputMode.Typed) R.string.moshi_date_input else R.string.moshi_date_natural_input),
                supportingText = error ?: if (inputMode == DateInputMode.Typed) stringResource(R.string.moshi_date_format, visibleDatePattern(locale)) else naturalHelp(locale),
                isError = error != null,
            )
            Spacer(Modifier.height(Moshi.spacing.space4))
        }
        for (preset in presets) {
            MoshiButton(
                onClick = {
                    draft = preset.date; cleared = false; inputEdited = false; month = YearMonth.from(preset.date)
                    input = if (inputMode == DateInputMode.Typed) preset.date.format(dateInputFormatter(locale)) else ""
                },
                enabled = constraints.allows(preset.date), variant = MoshiButtonVariant.Outline,
            ) { MoshiText(preset.label) }
            Spacer(Modifier.height(Moshi.spacing.space2))
        }
        MoshiText(
            selected?.let { stringResource(R.string.moshi_date_resolved, formatDate(it, locale)) }
                ?: stringResource(R.string.moshi_date_empty),
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
        Spacer(Modifier.height(Moshi.spacing.space4))
        CalendarPanel(month, { month = it }, selected, null, constraints, today, locale) {
            draft = it; cleared = false; inputEdited = false
            input = if (inputMode == DateInputMode.Typed) it.format(dateInputFormatter(locale)) else ""
        }
        if (initialTime != null) {
            Spacer(Modifier.height(Moshi.spacing.space4))
            TimeSelection(time, { time = it }, locale)
        }
        if (error != null && inputMode == DateInputMode.Calendar) {
            Spacer(Modifier.height(Moshi.spacing.space2))
            PickerError(error)
        }
        PickerActions(valid, onDismissRequest, if (allowClear) {
            { draft = null; cleared = true; input = ""; inputEdited = false }
        } else null) {
            if (onDateTimeApply != null) onDateTimeApply(selected?.atTime(time.withSecond(0).withNano(0))) else onApply(selected)
        }
    }
}

@Composable
fun MoshiDateRangePickerDialog(
    value: MoshiDateRange?, onApply: (MoshiDateRange?) -> Unit, onDismissRequest: () -> Unit,
    title: String = stringResource(R.string.moshi_date_range_choose),
    constraints: MoshiDateConstraints = MoshiDateConstraints(), allowClear: Boolean = false,
    presets: List<MoshiDateRangePreset> = emptyList(), today: LocalDate = LocalDate.now(),
) {
    var start by rememberSaveable(saver = DateStateSaver) { mutableStateOf(value?.start) }
    var end by rememberSaveable(saver = DateStateSaver) { mutableStateOf(value?.endInclusive) }
    var cleared by rememberSaveable { mutableStateOf(false) }
    var month by rememberSaveable(stateSaver = MonthSaver) { mutableStateOf(YearMonth.from((value?.start ?: today).coerceIn(constraints.earliestDate, constraints.latestDate))) }
    val range = start?.let { first -> end?.let { MoshiDateRange(first, it) } }
    var validRange by remember(range, constraints) { mutableStateOf(false) }
    LaunchedEffect(range, constraints) {
        validRange = range != null && withContext(Dispatchers.Default) {
            val context = coroutineContext
            validateDateRange(range, constraints) { context.ensureActive() }
        }
    }
    var validPresets by remember(presets, constraints) { mutableStateOf(emptyList<MoshiDateRangePreset>()) }
    LaunchedEffect(presets, constraints) {
        validPresets = withContext(Dispatchers.Default) {
            val context = coroutineContext
            presets.filter { validateDateRange(it.range, constraints) { context.ensureActive() } }
        }
    }
    val locale = pickerLocale()
    PickerDialogFrame(title, onDismissRequest, wide = true) {
        for (preset in presets) {
            MoshiButton(
                onClick = { start = preset.range.start; end = preset.range.endInclusive; cleared = false; month = YearMonth.from(preset.range.start) },
                enabled = preset in validPresets, variant = MoshiButtonVariant.Outline,
            ) { MoshiText(preset.label) }
            Spacer(Modifier.height(Moshi.spacing.space2))
        }
        MoshiText(stringResource(R.string.moshi_date_start, start?.let { formatDate(it, locale) } ?: stringResource(R.string.moshi_date_empty)))
        MoshiText(stringResource(R.string.moshi_date_end, end?.let { formatDate(it, locale) } ?: stringResource(R.string.moshi_date_empty)))
        Spacer(Modifier.height(Moshi.spacing.space4))
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val cellWidth = calendarCellWidth()
            val double = maxWidth >= cellWidth * 14 + Moshi.spacing.space4 && month < YearMonth.from(constraints.latestDate)
            Row {
                CalendarPanel(month, { month = it }, start, range, constraints, today, locale, Modifier.weight(1f)) {
                    if (start == null || end != null) { start = it; end = null } else {
                        val first = start!!; start = minOf(first, it); end = maxOf(first, it)
                    }
                    cleared = false
                }
                if (double) {
                    Spacer(Modifier.width(Moshi.spacing.space4))
                    CalendarPanel(month.plusMonths(1), { month = it.minusMonths(1).coerceIn(YearMonth.from(constraints.earliestDate), YearMonth.from(constraints.latestDate)) }, start, range, constraints, today, locale, Modifier.weight(1f)) {
                        if (start == null || end != null) { start = it; end = null } else {
                            val first = start!!; start = minOf(first, it); end = maxOf(first, it)
                        }
                        cleared = false
                    }
                }
            }
        }
        if (range != null && !validRange) {
            Spacer(Modifier.height(Moshi.spacing.space2))
            PickerError(stringResource(R.string.moshi_date_range_unavailable))
        }
        PickerActions(validRange || (allowClear && cleared), onDismissRequest, if (allowClear) {
            { start = null; end = null; cleared = true }
        } else null) { onApply(if (cleared) null else range) }
    }
}

@Composable
private fun PickerDialogFrame(title: String, onDismiss: () -> Unit, wide: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    val density = LocalDensity.current
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        CompositionLocalProvider(LocalDensity provides density) {
            Column(
                Modifier.padding(Moshi.spacing.space2)
                    .widthIn(max = if (wide) 840.dp else 480.dp).fillMaxWidth()
                    .clip(RoundedCornerShape(Moshi.spacing.space3)).background(Moshi.colors.background100)
                    .semantics { paneTitle = title }
                    .verticalScroll(rememberScrollState()).padding(Moshi.spacing.space2),
            ) {
                MoshiText(title, role = MoshiTextRole.Heading20, modifier = Modifier.semantics { heading() })
                Spacer(Modifier.height(Moshi.spacing.space4))
                content()
            }
        }
    }
}

@Composable
private fun PickerActions(valid: Boolean, onDismiss: () -> Unit, onClear: (() -> Unit)?, onApply: () -> Unit) {
    Spacer(Modifier.height(Moshi.spacing.space4))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Moshi.spacing.space2), verticalArrangement = Arrangement.spacedBy(Moshi.spacing.space2)) {
        MoshiButton(onClick = onApply, enabled = valid) { MoshiText(stringResource(R.string.moshi_date_apply)) }
        MoshiButton(onClick = onDismiss, variant = MoshiButtonVariant.Outline) { MoshiText(stringResource(R.string.moshi_date_cancel)) }
        if (onClear != null) MoshiButton(onClick = onClear, variant = MoshiButtonVariant.Text) { MoshiText(stringResource(R.string.moshi_date_clear)) }
    }
}

@Composable
private fun CalendarPanel(
    month: YearMonth, onMonthChange: (YearMonth) -> Unit, selected: LocalDate?, range: MoshiDateRange?,
    constraints: MoshiDateConstraints, today: LocalDate, locale: Locale, modifier: Modifier = Modifier,
    onSelect: (LocalDate) -> Unit,
) {
    var selector by rememberSaveable { mutableStateOf<String?>(null) }
    val firstMonth = YearMonth.from(constraints.earliestDate)
    val lastMonth = YearMonth.from(constraints.latestDate)
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Column(modifier) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            MoshiIconButton(
                if (rtl) R.drawable.ic_fluent_chevron_right_24_regular else R.drawable.ic_fluent_chevron_left_24_regular,
                stringResource(R.string.moshi_date_previous_month), { onMonthChange(month.minusMonths(1)) }, enabled = month > firstMonth,
            )
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                val monthLabel = stringResource(R.string.moshi_date_select_month)
                val yearLabel = stringResource(R.string.moshi_date_select_year)
                MoshiButton(onClick = { selector = if (selector == "month") null else "month" },
                    modifier = Modifier.semantics { contentDescription = monthLabel }, variant = MoshiButtonVariant.Text) {
                    MoshiText(month.month.getDisplayName(TextStyle.FULL, locale), modifier = Modifier)
                }
                MoshiButton(onClick = { selector = if (selector == "year") null else "year" },
                    modifier = Modifier.semantics { contentDescription = yearLabel }, variant = MoshiButtonVariant.Text) {
                    MoshiText(localizedNumber(month.year, locale))
                }
            }
            MoshiIconButton(
                if (rtl) R.drawable.ic_fluent_chevron_left_24_regular else R.drawable.ic_fluent_chevron_right_24_regular,
                stringResource(R.string.moshi_date_next_month), { onMonthChange(month.plusMonths(1)) }, enabled = month < lastMonth,
            )
        }
        when (selector) {
            "month" -> {
                MoshiText(stringResource(R.string.moshi_date_select_month), modifier = Modifier.semantics { heading() })
                FlowRow {
                    for (number in 1..12) {
                        val candidate = YearMonth.of(month.year, number)
                        MoshiButton(
                            onClick = { onMonthChange(candidate); selector = null }, enabled = candidate in firstMonth..lastMonth,
                            variant = MoshiButtonVariant.Text,
                        ) { MoshiText(candidate.month.getDisplayName(TextStyle.FULL, locale)) }
                    }
                }
            }
            "year" -> {
                MoshiText(stringResource(R.string.moshi_date_select_year), modifier = Modifier.semantics { heading() })
                val years = (firstMonth.year..lastMonth.year).toList()
                val scroll = rememberLazyListState((month.year - firstMonth.year).coerceIn(years.indices))
                LazyColumn(Modifier.height(Moshi.spacing.space12 * 6), state = scroll) {
                    items(years) { year ->
                        MoshiButton(
                            onClick = { onMonthChange(YearMonth.of(year, month.month).coerceIn(firstMonth, lastMonth)); selector = null },
                            modifier = Modifier.fillMaxWidth(), variant = MoshiButtonVariant.Text,
                        ) { MoshiText(localizedNumber(year, locale)) }
                    }
                }
            }
            else -> CalendarDays(month, selected, range, constraints, today, locale, onSelect)
        }
    }
}

@Composable
private fun calendarCellWidth(): androidx.compose.ui.unit.Dp {
    return Moshi.spacing.space12 * maxOf(1f, LocalDensity.current.fontScale / 1.4f)
}

@Composable
private fun CalendarDays(
    month: YearMonth, selected: LocalDate?, range: MoshiDateRange?, constraints: MoshiDateConstraints,
    today: LocalDate, locale: Locale, onSelect: (LocalDate) -> Unit,
) {
    val cell = calendarCellWidth()
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val narrow = maxWidth < cell * 7
        Column {
            if (narrow) MoshiText(stringResource(R.string.moshi_date_narrow_hint), role = MoshiTextRole.Copy13)
            Column(Modifier.horizontalScroll(rememberScrollState()).width(cell * 7)) {
                val firstWeekday = WeekFields.of(locale).firstDayOfWeek
                Row {
                    for (offset in 0..6) {
                        val weekday = DayOfWeek.of((firstWeekday.value - 1 + offset) % 7 + 1)
                        Box(Modifier.width(cell).heightIn(min = Moshi.spacing.space10), contentAlignment = Alignment.Center) {
                            MoshiText(weekday.getDisplayName(TextStyle.SHORT, locale), role = MoshiTextRole.Copy14,
                                modifier = Modifier.semantics { contentDescription = weekday.getDisplayName(TextStyle.FULL, locale) })
                        }
                    }
                }
                val leading = (month.atDay(1).dayOfWeek.value - firstWeekday.value + 7) % 7
                val rows = (leading + month.lengthOfMonth() + 6) / 7
                for (row in 0 until rows) {
                    Row {
                        for (column in 0..6) {
                            val number = row * 7 + column - leading + 1
                            if (number !in 1..month.lengthOfMonth()) {
                                Spacer(Modifier.size(cell))
                            } else {
                                val date = month.atDay(number)
                                val active = date == selected || date == range?.endInclusive
                                val inside = range != null && date in range.start..range.endInclusive
                                val allowed = constraints.allows(date)
                                val shape = RoundedCornerShape(Moshi.spacing.space2)
                                val states = listOfNotNull(
                                    if (active) stringResource(R.string.moshi_date_selected) else if (inside) stringResource(R.string.moshi_date_in_range) else null,
                                    if (date == today) stringResource(R.string.moshi_date_today) else null,
                                ).joinToString(", ")
                                Box(
                                    Modifier.size(cell).clip(shape)
                                        .background(if (active) Moshi.colors.gray1000 else if (inside) Moshi.colors.gray200 else Moshi.colors.background100)
                                        .then(if (date == today) Modifier.border(1.dp, Moshi.colors.blue900, shape) else Modifier)
                                        .clickable(enabled = allowed, role = Role.Button) { onSelect(date) }
                                        .semantics(mergeDescendants = true) {
                                            contentDescription = date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale))
                                            this.selected = active
                                            stateDescription = states
                                        },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    MoshiText(localizedNumber(number, locale), role = MoshiTextRole.Copy14,
                                        color = if (!allowed) Moshi.colors.gray700 else if (active) Moshi.colors.background100 else Moshi.colors.gray1000,
                                        modifier = Modifier.clearAndSetSemantics { })
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeSelection(time: LocalTime, onChange: (LocalTime) -> Unit, locale: Locale) {
    val use24Hour = DateFormat.is24HourFormat(LocalContext.current)
    MoshiText(formatTime(time, locale, use24Hour), modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Moshi.spacing.space4)) {
        TimeUnitSelection(stringResource(R.string.moshi_date_hour),
            localizedNumber(if (use24Hour) time.hour else (time.hour + 11) % 12 + 1, locale),
            stringResource(R.string.moshi_date_decrease_hour), stringResource(R.string.moshi_date_increase_hour),
            { onChange(time.minusHours(1)) }, { onChange(time.plusHours(1)) })
        TimeUnitSelection(stringResource(R.string.moshi_date_minute), localizedNumber(time.minute, locale, 2),
            stringResource(R.string.moshi_date_decrease_minute), stringResource(R.string.moshi_date_increase_minute),
            { onChange(time.withMinute((time.minute + 59) % 60)) }, { onChange(time.withMinute((time.minute + 1) % 60)) })
    }
}

@Composable
private fun TimeUnitSelection(label: String, value: String, previous: String, next: String, onPrevious: () -> Unit, onNext: () -> Unit) {
    Column {
        MoshiText(label)
        Row(verticalAlignment = Alignment.CenterVertically) {
            MoshiIconButton(R.drawable.ic_fluent_chevron_left_24_regular, previous, onPrevious)
            MoshiText(value)
            MoshiIconButton(R.drawable.ic_fluent_chevron_right_24_regular, next, onNext)
        }
    }
}

@Composable
private fun PickerError(text: String) {
    MoshiText(text, color = Moshi.colors.red900, modifier = Modifier.semantics { error(text); liveRegion = LiveRegionMode.Polite })
}

@Composable
private fun pickerLocale(): Locale {
    return LocalConfiguration.current.locales[0]
}

private fun formatDate(date: LocalDate, locale: Locale): String {
    return date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))
}

private fun formatTime(time: LocalTime, locale: Locale, use24Hour: Boolean): String {
    return time.format(DateTimeFormatter.ofPattern(if (use24Hour) "HH:mm" else "h:mm a", locale).withDecimalStyle(DecimalStyle.of(locale)))
}

private fun localizedNumber(number: Int, locale: Locale, minimumDigits: Int = 1): String {
    return java.text.NumberFormat.getIntegerInstance(locale).apply { isGroupingUsed = false; minimumIntegerDigits = minimumDigits }.format(number)
}

private fun visibleDatePattern(locale: Locale): String {
    return moshiDateInputPattern(locale).replace("uuuu", "yyyy")
}

@Composable
private fun rangeLabel(range: MoshiDateRange, locale: Locale): String {
    return stringResource(R.string.moshi_date_range_value, formatDate(range.start, locale), formatDate(range.endInclusive, locale))
}

@Composable
private fun naturalHelp(locale: Locale): String {
    return stringResource(if (locale.language in listOf("en", "es")) R.string.moshi_date_natural_help else R.string.moshi_date_natural_unsupported)
}

@MoshiPreview
@Composable
private fun MoshiDatePickerVariantsPreview() {
    MoshiTheme {
        Column(Modifier.background(Moshi.colors.background100).padding(Moshi.spacing.space4), verticalArrangement = Arrangement.spacedBy(Moshi.spacing.space2)) {
            val date = LocalDate.of(2026, 11, 16)
            MoshiDatePicker(date, {})
            MoshiDateRangePicker(MoshiDateRange(date, date.plusDays(4)), {})
            MoshiBirthDatePicker(date.minusYears(20), {})
            MoshiDateInputPicker(date, {})
            MoshiNaturalDatePicker(null, {})
            MoshiDateTimePicker(date.atTime(13, 30), {})
            MoshiDatePicker(null, {}, enabled = false)
        }
    }
}

@MoshiPreview
@Preview(name = "Large text", fontScale = 2f, widthDp = 360)
@Composable
private fun MoshiCalendarNarrowPreview() {
    CalendarPreview(false)
}

@MoshiPreview
@Preview(name = "Wide range", widthDp = 840)
@Composable
private fun MoshiCalendarWidePreview() {
    CalendarPreview(true)
}

@MoshiPreview
@Composable
private fun MoshiCalendarRtlPreview() {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) { CalendarPreview(false) }
}

@Composable
private fun CalendarPreview(wide: Boolean) {
    MoshiTheme {
        val date = LocalDate.of(2026, 11, 16)
        val range = MoshiDateRange(date, date.plusDays(4))
        val constraints = MoshiDateConstraints(isDateDisabled = { it.dayOfWeek == DayOfWeek.SUNDAY })
        Column(Modifier.background(Moshi.colors.background100).padding(Moshi.spacing.space2)) {
            Row {
                CalendarPanel(YearMonth.from(date), {}, date, range, constraints, date, Locale.US, Modifier.weight(1f)) {}
                if (wide) {
                    Spacer(Modifier.width(Moshi.spacing.space4))
                    CalendarPanel(YearMonth.from(date).plusMonths(1), {}, date, range, constraints, date, Locale.US, Modifier.weight(1f)) {}
                }
            }
            PickerError(stringResource(R.string.moshi_date_range_unavailable))
        }
    }
}
