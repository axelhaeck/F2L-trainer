package app.f2ltrainer.screens

import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import app.f2ltrainer.ui.App
import app.f2ltrainer.ui.F2lTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders the real screens on the JVM and saves PNGs to build/screenshots (for manual review).
 * Run with: ./gradlew testDebugUnitTest --tests '*ScreenshotTest*' -Pscreenshots
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [37], qualifiers = "w411dp-h891dp-xxhdpi")
class ScreenshotTest {
    @get:Rule val rule = createComposeRule()

    private fun shot(name: String) = rule.onRoot().captureRoboImage("build/screenshots/$name.png")

    @Test fun screens() {
        rule.setContent { F2lTheme { App(finish = {}) } }
        shot("01_learn")
        rule.onNode(hasScrollAction()).performScrollToNode(hasText("F2L 26"))
        rule.onNodeWithText("F2L 26").performClick()
        shot("02_case26")
        rule.onNodeWithText("Play").performClick()
        rule.mainClock.advanceTimeBy(3000)
        shot("03_case26_playing")
        rule.onNodeWithText("← Back").performClick()
        rule.onNode(hasScrollAction()).performScrollToNode(hasText("F2L 6"))
        rule.onNodeWithText("F2L 6").performClick()
        rule.onNodeWithText("BR slot free").performClick()
        shot("04_case6_free")
        rule.onNodeWithText("Back view").performClick()
        shot("05_case6_free_back")
        rule.onNodeWithText("← Back").performClick()
        rule.onNodeWithText("Guide").performClick()
        shot("06_guide")
        rule.onNodeWithText("← Back").performClick()
        rule.onNodeWithText("Practice").performClick()
        shot("07_practice_setup")
        rule.onNodeWithText("Start batch of 5").performScrollTo().performClick()
        shot("08_flashcard")
        rule.onNodeWithText("Show algorithm").performClick()
        shot("09_flashcard_revealed")
        rule.onNodeWithText("✕ End").performClick()
        rule.onNodeWithText("Progress").performClick()
        shot("10_progress")
    }
}
