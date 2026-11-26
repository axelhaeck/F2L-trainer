package app.threestyle.screens

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import app.threestyle.ui.App
import app.threestyle.ui.AppTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The screens with a cube picture in landscape and in portrait with a large font (screenshots for review). */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [37])
class LayoutTest {
    @get:Rule val rule = createComposeRule()

    private fun shot(name: String) = rule.onRoot().captureRoboImage("build/screenshots/$name.png")
    private fun waitFor(text: String) = rule.waitUntil(20_000) {
        rule.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()
    }

    private fun walk(prefix: String) {
        rule.setContent { AppTheme { App(finish = {}) } }
        waitFor("Pure commutators")
        rule.onAllNodesWithText("U interchange", substring = true).onFirst().performClick()
        rule.onAllNodesWithText("New").onFirst().performClick()
        rule.waitForIdle()
        shot("${prefix}_case")
        rule.onNodeWithText("← Back").performClick()
        rule.onAllNodesWithText("Practise this set").onFirst().performClick()
        rule.waitForIdle()
        shot("${prefix}_flashcard")
        rule.onNodeWithText("Show commutator").performClick()
        rule.waitForIdle()
        shot("${prefix}_flashcard_revealed")
        rule.onNodeWithText("✕ End").performClick()
        rule.onNodeWithText("← Back").performClick()
        rule.onNodeWithText("Letters").performClick()
        rule.waitForIdle()
        shot("${prefix}_letters")
        rule.onNode(hasScrollAction()).performScrollToNode(hasText("Trace a scramble"))
        rule.onNodeWithText("Trace a scramble").performClick()
        rule.waitForIdle()
        shot("${prefix}_memo")
    }

    @Config(qualifiers = "w891dp-h411dp-land-xxhdpi")
    @Test fun landscape() = walk("40_land")

    @Config(qualifiers = "w392dp-h850dp-xxhdpi")
    @Test fun portraitLargeFont() {
        RuntimeEnvironment.setFontScale(1.3f)
        walk("41_port_bigfont")
    }
}
