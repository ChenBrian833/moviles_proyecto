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
            assertReadable("Standard · Manual Tune")
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
                            EztunezTheme(dynamicColor = false, content = content)
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

    private fun assertReadable(text: String, lastMatch: Boolean = false) {
        val node = if (lastMatch) compose.onAllNodesWithText(text, useUnmergedTree = true).onLast()
        else compose.onNodeWithText(text, useUnmergedTree = true)
        // Headers are outside the scroll container; only scroll descendants need this.
        if (text != "Standard · Manual Tune") node.performScrollTo()
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
