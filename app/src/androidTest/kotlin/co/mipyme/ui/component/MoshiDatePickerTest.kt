package co.mipyme.ui.component

import android.content.res.Configuration
import android.graphics.Bitmap
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import co.mipyme.ui.MoshiTheme
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class MoshiDatePickerTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    private val date = LocalDate.of(2026, 11, 16)

    @Composable
    private fun EnglishContent(content: @Composable () -> Unit) {
        val configuration = Configuration(LocalConfiguration.current).apply { setLocale(Locale.US) }
        CompositionLocalProvider(LocalConfiguration provides configuration) { MoshiTheme { content() } }
    }

    private fun day(value: LocalDate): SemanticsNodeInteraction {
        return rule.onNodeWithContentDescription(value.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(Locale.US)))
    }

    @Test
    fun cancelBackAndClearNeverEmitUntilApply() {
        val value = mutableStateOf<LocalDate?>(date)
        var emissions = 0
        rule.setContent { EnglishContent { MoshiDatePicker(value.value, { value.value = it; emissions++ }, allowClear = true) } }
        rule.onNodeWithText("Choose date").performClick()
        day(date.plusDays(1)).performScrollTo().assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp).performClick()
        rule.runOnIdle { assertEquals(date, value.value); assertEquals(0, emissions) }
        rule.onNodeWithText("Cancel").performScrollTo().performClick()
        rule.onNodeWithText("Choose date").performClick()
        day(date).assertIsSelected()
        rule.onNodeWithText("Clear").performScrollTo().performClick()
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        rule.waitForIdle()
        rule.runOnIdle { assertEquals(date, value.value); assertEquals(0, emissions) }
        rule.onNodeWithText("Choose date").performClick()
        rule.onNodeWithText("Clear").performScrollTo().performClick()
        rule.onNodeWithText("Apply").performScrollTo().assertIsEnabled().performClick()
        rule.runOnIdle { assertNull(value.value); assertEquals(1, emissions) }
    }

    @Test
    fun typedDraftIsStrictAndRestrictionsApply() {
        var value: LocalDate? = date
        rule.setContent { EnglishContent { MoshiDateInputPicker(date, { value = it }, constraints = MoshiDateConstraints(isDateDisabled = { it == date.plusDays(2) })) } }
        rule.onNodeWithText("Choose date").performClick()
        rule.onNode(hasSetTextAction()).performTextReplacement("2/29/2025")
        rule.onNodeWithText("Apply").performScrollTo().assertIsNotEnabled()
        rule.onNode(hasSetTextAction()).performScrollTo().performTextReplacement("11/18/2026")
        rule.onNodeWithText("Apply").performScrollTo().assertIsNotEnabled()
        rule.onNode(hasSetTextAction()).performScrollTo().performTextReplacement("11/17/2026")
        rule.onNodeWithText("Selected date: Nov 17, 2026").assertExists()
        rule.runOnIdle { assertEquals(date, value) }
        rule.onNodeWithText("Apply").performScrollTo().assertIsEnabled().performClick()
        rule.runOnIdle { assertEquals(date.plusDays(1), value) }
    }

    @Test
    fun naturalDraftResolvesBeforeConfirmation() {
        var value: LocalDate? = null
        rule.setContent { EnglishContent { MoshiNaturalDatePicker(null, { value = it }, today = date) } }
        rule.onNodeWithText("Choose date").performClick()
        rule.onNode(hasSetTextAction()).performTextInput("next monday")
        rule.onNodeWithText("Selected date: Nov 23, 2026").assertExists()
        rule.runOnIdle { assertNull(value) }
        rule.onNodeWithText("Apply").performScrollTo().performClick()
        rule.runOnIdle { assertEquals(date.plusDays(7), value) }
    }

    @Test
    fun rangesRejectInteriorDisabledDatesAndInvalidPresets() {
        var value: MoshiDateRange? = null
        val constraints = MoshiDateConstraints(date, date.plusDays(20)) { it == date.plusDays(2) }
        rule.setContent { EnglishContent {
            MoshiDateRangePicker(null, { value = it }, constraints = constraints,
                presets = listOf(MoshiDateRangePreset("Closed interval", MoshiDateRange(date, date.plusDays(4)))))
        } }
        rule.onNodeWithText("Choose date range").performClick()
        rule.onNodeWithText("Closed interval").assertIsNotEnabled()
        day(date).performScrollTo().performClick()
        day(date.plusDays(4)).performScrollTo().performClick()
        rule.onNodeWithText("Apply").performScrollTo().assertIsNotEnabled()
        day(date).performScrollTo().performClick()
        day(date.plusDays(1)).performScrollTo().performClick()
        rule.waitUntil(5000) {
            rule.onAllNodes(hasText("Apply") and isEnabled()).fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("Apply").performScrollTo().performClick()
        rule.runOnIdle { assertEquals(MoshiDateRange(date, date.plusDays(1)), value) }
    }

    @Test
    fun birthDateDisablesFutureAndProvidesMonthYearSelection() {
        rule.setContent { EnglishContent { MoshiBirthDatePicker(date, {}, today = date) } }
        rule.onNodeWithText("Choose birth date").performClick()
        day(date.plusDays(1)).assertIsNotEnabled()
        rule.onNodeWithContentDescription("Choose year").performClick()
        rule.onNodeWithText("2025").performScrollTo().performClick()
        rule.onNodeWithContentDescription("Choose month").performClick()
        rule.onNodeWithText("February").performScrollTo().performClick()
        day(LocalDate.of(2025, 2, 1)).assertExists()
    }

    @Test
    fun dateAndMinuteAreCommittedTogether() {
        var result: LocalDateTime? = null
        rule.setContent { EnglishContent { MoshiDateTimePicker(date.atTime(13, 30, 15), { result = it }) } }
        rule.onNodeWithText("Choose date and time").performClick()
        rule.onNodeWithContentDescription("Next minute").performScrollTo().performClick()
        rule.runOnIdle { assertNull(result) }
        rule.onNodeWithText("Apply").performScrollTo().performClick()
        rule.runOnIdle { assertEquals(date.atTime(13, 31), result) }
    }

    @Test
    fun externalChangesAndDisablingPreserveCommittedValue() {
        val enabled = mutableStateOf(true)
        val committed = mutableStateOf<LocalDate?>(date)
        var emissions = 0
        rule.setContent { EnglishContent { MoshiDatePicker(committed.value, { emissions++ }, enabled = enabled.value) } }
        rule.onNodeWithText("Choose date").performClick()
        day(date.plusDays(1)).performScrollTo().performClick()
        rule.runOnIdle { committed.value = date.plusDays(5); enabled.value = false }
        rule.onNodeWithText("Apply").assertDoesNotExist()
        rule.onNodeWithText("Choose date").assertIsNotEnabled()
        rule.runOnIdle { enabled.value = true }
        rule.onNodeWithText("Choose date").performClick()
        day(date.plusDays(5)).assertIsSelected()
        rule.runOnIdle { assertEquals(0, emissions) }
    }

    @Test
    fun savesOpenDialogAndUnconfirmedTypedDraftAcrossRecreation() {
        var emissions = 0
        val restoration = StateRestorationTester(rule)
        restoration.setContent { EnglishContent { MoshiDateInputPicker(date, { emissions++ }) } }
        rule.onNodeWithText("Choose date").performClick()
        rule.onNode(hasSetTextAction()).performTextReplacement("11/17/2026")
        restoration.emulateSavedInstanceStateRestore()
        rule.onNodeWithText("Selected date: Nov 17, 2026").assertExists()
        rule.onNodeWithText("Apply").performScrollTo().assertIsEnabled()
        rule.runOnIdle { assertEquals(0, emissions) }
        rule.onNodeWithText("Cancel").performScrollTo().performClick()
        rule.onNodeWithText("Choose date").performClick()
        day(date).assertIsSelected()
    }

    @Test
    fun restoresIncompleteRangeWithoutRestoringOldEnd() {
        var result: MoshiDateRange? = null
        val restoration = StateRestorationTester(rule)
        restoration.setContent { EnglishContent {
            MoshiDateRangePickerDialog(MoshiDateRange(date, date.plusDays(1)), { result = it }, {}, today = date)
        } }
        day(date.plusDays(4)).performScrollTo().performClick()
        restoration.emulateSavedInstanceStateRestore()
        rule.onNodeWithText("Apply").performScrollTo().assertIsNotEnabled()
        day(date.plusDays(5)).performScrollTo().performClick()
        rule.waitUntil(5000) { rule.onAllNodes(hasText("Apply") and isEnabled()).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("Apply").performScrollTo().performClick()
        rule.runOnIdle { assertEquals(MoshiDateRange(date.plusDays(4), date.plusDays(5)), result) }
    }

    @Test
    fun capturesRangeAndKeepsTwoMonthNavigationWithinBounds() {
        val first = LocalDate.of(2026, 1, 1)
        rule.setContent { EnglishContent {
            MoshiDateRangePickerDialog(MoshiDateRange(first.plusDays(4), first.plusDays(8)), {}, {},
                constraints = MoshiDateConstraints(first, LocalDate.of(2026, 3, 31)), today = first)
        } }
        if (rule.onAllNodesWithContentDescription("Previous month").fetchSemanticsNodes().size == 2) {
            rule.onAllNodesWithContentDescription("Previous month")[1].performClick()
            day(first).assertExists()
            day(first.minusDays(1)).assertDoesNotExist()
            // Changing the right selector to the earliest month must also preserve the base bound.
            rule.onAllNodesWithContentDescription("Choose month")[1].performClick()
            rule.onAllNodesWithText("January").onLast().performScrollTo().performClick()
            day(first).assertExists()
            day(first.minusDays(1)).assertDoesNotExist()
        }
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val directory = File(instrumentation.targetContext.getExternalFilesDir(null), "date-pickers").apply { mkdirs() }
        rule.waitForIdle()
        val wide = rule.onAllNodesWithContentDescription("Previous month").fetchSemanticsNodes().size == 2
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        File(directory, if (wide) "range-wide.png" else "range-narrow.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }

    @Test
    fun capturesDialogInBothThemesAndLargeRtlText() {
        val dark = mutableStateOf(false)
        val largeRtl = mutableStateOf(false)
        rule.setContent {
            val config = Configuration(LocalConfiguration.current).apply { setLocale(Locale.US) }
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalConfiguration provides config,
                LocalDensity provides Density(density.density, if (largeRtl.value) 2f else 1f),
                LocalLayoutDirection provides if (largeRtl.value) LayoutDirection.Rtl else LayoutDirection.Ltr,
            ) {
                MoshiTheme(darkTheme = dark.value) {
                    MoshiDatePickerDialog(date, {}, {}, constraints = MoshiDateConstraints(isDateDisabled = { it == date.plusDays(2) }))
                }
            }
        }
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val directory = File(instrumentation.targetContext.getExternalFilesDir(null), "date-pickers").apply { mkdirs() }
        listOf("single-light", "single-dark", "single-large-rtl").forEachIndexed { index, name ->
            rule.runOnIdle { dark.value = index == 1; largeRtl.value = index == 2 }
            rule.waitForIdle()
            rule.onNodeWithText("Sun").assertExists()
            day(date).assertWidthIsAtLeast(if (largeRtl.value) 68.dp else 48.dp).assertHeightIsAtLeast(48.dp)
            val bitmap = instrumentation.uiAutomation.takeScreenshot()
            File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
    }
}
