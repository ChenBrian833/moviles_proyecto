package com.uvg.cc3087.eztunez

import android.graphics.Bitmap
import android.content.res.Configuration
import android.content.pm.ActivityInfo
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.uvg.cc3087.eztunez.ui.theme.EztunezTheme
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Before
import org.junit.Test

/** Measures real Compose content in constrained windows without changing phone settings. */
class ResponsiveScreensTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Before fun keepTestHostOrientationStable() {
        // Logical test windows supply orientation; a physical phone rotation must
        // not destroy the host Activity halfway through a layout assertion.
        compose.runOnUiThread {
            compose.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
        }
    }

    private data class Window(val name: String, val width: Int, val height: Int, val fontScale: Float)
    private val windows = listOf(
        Window("landscape", 800, 360, 1f),
        Window("small", 280, 360, 1f),
        Window("large-text", 320, 480, 2f)
    )
    private var window by mutableStateOf(windows.first())
    private var darkTheme by mutableStateOf(false)

    @Test fun customPresetCardsAndFiltersRemainReachable() {
        val custom = TuningPreset(-1, "My custom tuning", listOf(38,45,50,55,59,64),
            "Practice tuning", PresetCategory.CUSTOM)
        showWindow {
            TuningPresetsScreen(
                TuningPresetsUiState(listOf(custom), selectedPreset = custom, isLoading = false),
                {}, {}, {}, {}, {}, {}
            )
        }
        for (dark in listOf(false, true)) {
            for (size in windows) {
                compose.runOnIdle { window = size; darkTheme = dark }
                assertReadable("Tuning Presets", scroll = false)
                assertReadable("Create Custom")
                for (filter in listOf("All Presets", "Guitar", "Custom")) assertReadable(filter)
                // Anchor the lazy card at its start. Bringing a merged, taller-than-
                // viewport card into view can otherwise position its title above the window.
                compose.onNode(hasScrollToIndexAction()).performScrollToIndex(2)
                assertReadable("My custom tuning — D2 A2 D3 G3 B3 E4", scroll = false)
                assertReadable("Edit")
                assertReadable("Delete")
                assertNavigationVisible()
                screenshot("custom-presets-$dark-${size.name}")
            }
        }
    }

    @Test fun customEditorRemainsReadableWithSmallWindowsAndLargeFonts() {
        var draft by mutableStateOf(CustomPresetDraft())
        showWindow {
            CustomPresetEditor(draft, false, PresetProblem.NAME_REQUIRED, { draft = it }, {}, {})
        }
        for (dark in listOf(false, true)) {
            for (size in windows) {
                compose.runOnIdle { window = size; darkTheme = dark }
                assertReadable("Create custom tuning", scroll = false)
                assertReadable("String 6 · lowest (thickest)")
                for (note in listOf("E2", "A2", "D3", "G3", "B3", "E4")) assertReadable(note)
                assertReadable("String 1 · highest (thinnest)")
                assertReadable("Save preset")
                assertReadable("Cancel")
                screenshot("custom-editor-$dark-${size.name}")
            }
        }
    }

    @Test fun tuningModesStayVisibleAndKeepStringSelectionBehavior() {
        var mode by mutableStateOf(TuningMode.AUTO)
        var selected = 0
        showWindow {
            TuneScreen(
                mode = mode,
                uiState = TunerUiState(microphone = MicrophoneState.LISTENING),
                onSelectString = { selected = it },
                onRetry = {},
                onOpenSettings = {},
                onNavigate = {}
            )
        }
        for (dark in listOf(false, true)) {
            for (tuningMode in TuningMode.entries) {
                for (size in windows) {
                    compose.runOnIdle {
                        darkTheme = dark
                        mode = tuningMode
                        window = size
                        selected = 0
                    }
                    val automatic = tuningMode == TuningMode.AUTO
                    val title = if (automatic) "Auto Tune" else "Manual Tune"
                    val subtitle = "Acoustic · Standard"
                    assertReadable(title, scroll = false)
                    assertReadable(subtitle, scroll = false)
                    screenshot("mode-$tuningMode-$dark-${size.name}-top")
                    assertReadable(if (automatic) "Selección automática de cuerda" else "Selecciona una cuerda")
                    for (string in standardGuitarStrings) {
                        val button = compose.onAllNodesWithText(string.note).onLast()
                        button.performScrollTo().assertIsDisplayed()
                        if (automatic) {
                            button.assertIsNotEnabled()
                        } else {
                            button.assertIsEnabled().performClick()
                            compose.runOnIdle { assertEquals(string.id, selected) }
                        }
                    }
                    if (automatic) compose.runOnIdle { assertEquals(0, selected) }
                    // The mode stays visible even after scrolling down to the strings.
                    compose.onNodeWithText(title).assertIsDisplayed()
                    compose.onNodeWithText(subtitle).assertIsDisplayed()
                    assertNavigationVisible()
                    screenshot("mode-$tuningMode-$dark-${size.name}-strings")
                }
            }
        }
    }

    @Test fun tuningHeaderShowsTheSelectedPresetInBothModes() {
        var mode by mutableStateOf(TuningMode.AUTO)
        var preset by mutableStateOf(BuiltInTuningPresets.standard)
        showWindow {
            TuneScreen(
                mode = mode,
                preset = preset,
                uiState = TunerUiState(microphone = MicrophoneState.LISTENING),
                onSelectString = {},
                onRetry = {},
                onOpenSettings = {},
                onNavigate = {}
            )
        }
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        for (tuningMode in TuningMode.entries) {
            for (selectedPreset in BuiltInTuningPresets.all) {
                for (size in windows) {
                    compose.runOnIdle {
                        mode = tuningMode
                        preset = selectedPreset
                        window = size
                    }
                    val title = if (tuningMode == TuningMode.AUTO) "Auto Tune" else "Manual Tune"
                    val presetName = selectedPreset.nameRes?.let(context::getString) ?: selectedPreset.name
                    assertReadable(title, scroll = false)
                    assertReadable("Acoustic · $presetName", scroll = false)
                }
            }
        }
    }

    @Test fun homeDescriptionsAndActionsRemainReachable() {
        var started: TuningMode? = null
        showWindow {
            HomeScreen(onNavigate = {}, onStartTuning = { started = it })
        }
        for (size in windows) {
            compose.runOnIdle { window = size }
            for (description in listOf(
                "Hands-free automatic string detection",
                "Select and tune strings individually",
                "Tune to any of the 12 chromatic pitches",
                "Keep rhythm with visual and audio beats"
            )) {
                assertReadable(description)
            }
            screenshot("home-${size.name}")
            compose.onNodeWithText("Manual Tune").performScrollTo().performClick()
            compose.runOnIdle { assertEquals(TuningMode.MANUAL, started) }
            assertNavigationVisible()
        }
    }

    @Test fun tunerStringsAndMicrophoneRecoveryRemainReachable() {
        var selected = 0
        var retries = 0
        showWindow {
            TuneScreen(
                mode = TuningMode.MANUAL,
                settings = TunerSettings(notationSystem = NotationSystem.SOLFEGE),
                uiState = TunerUiState(microphone = MicrophoneState.CAPTURE_INTERRUPTED),
                onSelectString = { selected = it },
                onRetry = { retries++ },
                onOpenSettings = {},
                onNavigate = {}
            )
        }
        for ((index, size) in windows.withIndex()) {
            compose.runOnIdle { window = size }
            assertReadable("Manual Tune", scroll = false)
            assertReadable("Acoustic · Standard", scroll = false)
            compose.onNodeWithText("Retry").performScrollTo().performClick()
            for (string in standardGuitarStrings) {
                val note = formatNote(string.note, NotationSystem.SOLFEGE)
                assertReadable(note, lastMatch = string.id == 1)
                compose.onAllNodesWithText(note).onLast().performScrollTo().performClick()
                compose.runOnIdle { assertEquals(string.id, selected) }
            }
            screenshot("tune-${size.name}")
            assertNavigationVisible()
            compose.runOnIdle { assertEquals(index + 1, retries) }
        }
    }

    private fun showWindow(content: @Composable () -> Unit) {
        compose.setContent {
            val baseDensity = LocalDensity.current
            BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                // Fit the entire virtual window on the device while preserving its
                // logical dp constraints and font scale for layout measurement.
                val fit = minOf(maxWidth.value / window.width, maxHeight.value / window.height, 1f)
                val configuration = Configuration(LocalConfiguration.current).apply {
                    screenWidthDp = window.width
                    screenHeightDp = window.height
                    fontScale = window.fontScale
                    orientation = if (window.width > window.height) Configuration.ORIENTATION_LANDSCAPE
                        else Configuration.ORIENTATION_PORTRAIT
                }
                CompositionLocalProvider(
                    LocalDensity provides Density(baseDensity.density * fit, window.fontScale),
                    LocalConfiguration provides configuration
                ) {
                    key(window) {
                        Box(Modifier.requiredSize(window.width.dp, window.height.dp).testTag("responsive-window")) {
                            EztunezTheme(darkTheme = darkTheme, dynamicColor = false, content = content)
                        }
                    }
                }
            }
        }
    }

    private fun assertNavigationVisible() {
        for (destination in listOf("Home", "Tune", "Presets", "Settings")) {
            try {
                compose.onNode(
                    (hasText(destination) or hasContentDescription(destination)) and hasClickAction()
                ).assertIsDisplayed()
            } catch (failure: AssertionError) {
                throw AssertionError("Navigation $destination in ${window.name}:\n" + compose.onRoot().printToString(), failure)
            }
        }
    }

    private fun assertReadable(text: String, lastMatch: Boolean = false, scroll: Boolean = true) {
        val node = if (lastMatch) compose.onAllNodesWithText(text, useUnmergedTree = true).onLast()
        else compose.onNodeWithText(text, useUnmergedTree = true)
        // Headers are outside the scroll container; only scroll descendants need this.
        if (scroll) node.performScrollTo()
        node.assertIsDisplayed()
        val layouts = mutableListOf<TextLayoutResult>()
        node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        // Paragraphs can retain the full constraint width while short Text nodes
        // shrink to their text width. Check the actual lines, rather than that width.
        val readable = layouts.isNotEmpty() && layouts.all { layout ->
            !layout.didOverflowHeight && (0 until layout.lineCount).all { line ->
                !layout.isLineEllipsized(line) &&
                    layout.getLineLeft(line) >= -1f &&
                    layout.getLineRight(line) <= layout.size.width + 1f
            }
        }
        if (!readable) screenshot("debug-${window.name}-${text.take(8).replace(' ', '-')}")
        assertTrue("Clipped text: $text in ${window.name}: " + layouts.map {
            "size=${it.size}, paragraph=${it.multiParagraph.width}x${it.multiParagraph.height}, overflowWidth=${it.didOverflowWidth}, overflowHeight=${it.didOverflowHeight}, lines=${it.lineCount}"
        }, readable)
    }

    private fun screenshot(name: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val output = InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")
        val directory = (output?.let { File(it, "responsive-screens") }
            ?: File(context.getExternalFilesDir(null), "responsive-screens")).apply { mkdirs() }
        File(directory, "$name.png").outputStream().use {
            compose.onNodeWithTag("responsive-window").captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
