package app.threestyle.screens

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import app.threestyle.ui.App
import app.threestyle.ui.AppTheme
import app.threestyle.ui.quizPauseMs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [37], qualifiers = "w411dp-h891dp-xxhdpi")
class QuizTimingTest {
    @get:Rule val rule = createComposeRule()

    private fun has(t: String) = rule.onAllNodesWithText(t, substring = true).fetchSemanticsNodes().isNotEmpty()

    @Test fun pauses() {
        assertEquals(190L, quizPauseMs(correct = true, slow = false))
        assertEquals(900L, quizPauseMs(correct = true, slow = true))
        assertEquals(1500L, quizPauseMs(correct = false, slow = false))
    }

    /** After a mistake there is no Next button: the quiz asks the next sticker by itself. */
    @Test fun movesOnAutomatically() {
        rule.setContent { AppTheme { App(finish = {}) } }
        rule.onNodeWithText("Letters").performClick()
        rule.onNode(hasScrollAction()).performScrollToNode(hasText("Both"))
        rule.onAllNodesWithText("Both")[0].performClick()
        rule.waitForIdle()
        var answered = 0
        for (letter in listOf("A", "B", "A", "B", "A", "B")) {
            rule.onNodeWithText(letter).performClick()
            answered++
            assertFalse("no Next button", has("Next"))
            // the next question comes by itself (the answer counter shows it was counted)
            rule.waitUntil(5_000) { has("Which letter is the highlighted sticker?") && has("/ $answered correct") }
        }
        assertTrue(has("/ 6 correct"))
    }
}
