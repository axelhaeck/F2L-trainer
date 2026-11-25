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
import androidx.compose.ui.test.performTextInput
import app.threestyle.ui.App
import app.threestyle.ui.AppTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Clicks through all screens of the real app on the JVM (a UI smoke test).
 * With -Pscreenshots it also saves PNGs to build/screenshots for manual review:
 *   ./gradlew testDebugUnitTest --tests '*ScreenshotTest*' -Pscreenshots
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [37], qualifiers = "w411dp-h891dp-xxhdpi")
class ScreenshotTest {
    @get:Rule val rule = createComposeRule()

    private fun shot(name: String) = rule.onRoot().captureRoboImage("build/screenshots/$name.png")

    private fun waitFor(text: String) = rule.waitUntil(20_000) {
        rule.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()
    }

    private fun scrollTo(text: String) {
        rule.onNode(hasScrollAction()).performScrollToNode(hasText(text, substring = true))
    }

    @Test fun screens() {
        rule.setContent { AppTheme { App(finish = {}) } }
        waitFor("Pure commutators")
        shot("01_learn_corners")
        rule.onAllNodesWithText("U interchange", substring = true).onFirst().performClick()
        rule.waitForIdle()
        shot("02_set")
        rule.onAllNodesWithText("Practise this set").onFirst().assertExists()
        // open the first case of the set
        rule.onAllNodesWithText("New").onFirst().performClick()
        rule.waitForIdle()
        shot("03_case")
        scrollTo("Step 0 of")
        rule.onNodeWithText("Play").performClick()
        rule.mainClock.advanceTimeBy(3000)
        rule.waitUntil(10_000) { rule.onAllNodesWithText("Step 0 of", substring = true).fetchSemanticsNodes().isEmpty() }
        scrollTo("Step ")
        shot("04_case_step")
        scrollTo("Add your own algorithm")
        shot("05_case_algs")
        rule.onNodeWithText("← Back").performClick()
        rule.onNodeWithText("← Back").performClick()
        rule.onNodeWithText("Edges (440)").performClick()
        waitFor("4-movers")
        shot("06_learn_edges")
        rule.onNodeWithText("Corner twists (56)").performClick()
        rule.waitForIdle()
        shot("07_learn_twists")
        rule.onAllNodesWithText("With buffer").onFirst().assertExists()
        rule.onNodeWithText("Parity (21)").performClick()
        rule.waitForIdle()
        rule.onAllNodesWithText("Parity (1/", substring = true).onFirst().performClick()
        rule.waitForIdle()
        rule.onAllNodesWithText("New").onFirst().performClick()
        rule.waitForIdle()
        shot("08_parity_case")
        rule.onNodeWithText("← Back").performClick()
        rule.onNodeWithText("← Back").performClick()

        rule.onNodeWithText("Practice").performClick()
        rule.waitForIdle()
        shot("09_practice_setup")
        scrollTo("Start batch")
        rule.onAllNodesWithText("Start batch", substring = true).onFirst().performClick()
        rule.waitForIdle()
        shot("10_flashcard")
        rule.onNodeWithText("Show commutator").performClick()
        rule.waitForIdle()
        shot("11_flashcard_revealed")
        scrollTo("Good")
        rule.onNodeWithText("Good").performClick()
        rule.onNodeWithText("✕ End").performClick()

        rule.onNodeWithText("Letters").performClick()
        rule.waitForIdle()
        shot("12_letters")
        scrollTo("Sticker → letter")
        rule.onAllNodesWithText("Corners").onFirst().assertExists()
        rule.onAllNodesWithText("Both")[0].performClick()
        rule.waitForIdle()
        shot("13_quiz_to_letter")
        // answer: one of A/B is wrong (two different stickers), the feedback is shown
        rule.onNodeWithText("A").performClick()
        rule.waitForIdle()
        if (rule.onAllNodesWithText("Next").fetchSemanticsNodes().isEmpty()) {
            rule.mainClock.advanceTimeBy(1000)
            rule.onNodeWithText("B").performClick()
            rule.waitForIdle()
        }
        shot("13b_quiz_answered")
        rule.onNodeWithText("← Back").performClick()
        rule.onAllNodesWithText("Both")[1].performClick()
        rule.waitForIdle()
        shot("13c_quiz_to_sticker")
        rule.onNodeWithText("← Back").performClick()
        scrollTo("Trace a scramble")
        rule.onNodeWithText("Trace a scramble").performClick()
        rule.waitForIdle()
        shot("14_memo")
        rule.onNodeWithText("Edge memo (letters)").performTextInput("AB")
        rule.onNodeWithText("Check").performClick()
        rule.onNodeWithText("Show solution").performClick()
        rule.waitForIdle()
        scrollTo("One correct memo")
        shot("15_memo_checked")
        rule.onNodeWithText("← Back").performClick()
        scrollTo("Edit pair words")
        rule.onNodeWithText("Edit pair words").performClick()
        rule.waitForIdle()
        shot("16_pair_words")
        rule.onNodeWithText("← Back").performClick()

        rule.onNodeWithText("Progress").performClick()
        rule.waitForIdle()
        shot("17_progress")
        rule.onNodeWithText("Settings").performClick()
        rule.waitForIdle()
        shot("18_settings")
        scrollTo("Cube colours")
        shot("19_settings_scheme")
        // switch the edge buffer: the library is rebuilt
        rule.onNode(hasScrollAction()).performScrollToNode(hasText("DF (U)"))
        rule.onNodeWithText("DF (U)").performClick()
        rule.onNodeWithText("Learn").performClick()
        waitFor("Edges (440)")
        rule.onNodeWithText("Edges (440)").performClick()
        waitFor("edges DF")
        rule.waitForIdle()
        shot("20_learn_edges_DF")
        rule.onNodeWithText("Guide").performClick()
        rule.waitForIdle()
        shot("21_guide")
    }
}
