package app.threestyle.screens

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import app.threestyle.ui.App
import app.threestyle.ui.AppTheme
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
class PlayerTest {
    @get:Rule val rule = createComposeRule()

    private fun has(t: String) = rule.onAllNodesWithText(t, substring = true).fetchSemanticsNodes().isNotEmpty()

    @Test fun stepAndPlay() {
        rule.setContent { AppTheme { App(finish = {}) } }
        rule.waitUntil(20_000) { has("Pure commutators") }
        rule.onAllNodesWithText("U interchange", substring = true).onFirst().performClick()
        rule.onAllNodesWithText("New").onFirst().performClick()
        rule.onNode(hasScrollAction()).performScrollToNode(hasText("Step 0 of", substring = true))
        org.junit.Assert.assertTrue(has("Step 0 of 8"))
        rule.onNodeWithText("▶").performClick()
        rule.waitForIdle()
        org.junit.Assert.assertTrue(has("Step 1 of 8"))
        rule.onNodeWithText("◀").performClick()
        rule.waitForIdle()
        org.junit.Assert.assertTrue(has("Step 0 of 8"))
        rule.onNodeWithText("Play").performClick()
        org.junit.Assert.assertTrue(has("Pause"))
        rule.mainClock.advanceTimeBy(3000)
        rule.waitUntil(10_000) { !has("Step 0 of") }
        rule.onNodeWithText("⏭").performClick()
        rule.waitForIdle()
        // the full algorithm solves the case
        org.junit.Assert.assertTrue(has("Step 8 of 8 · solved ✓"))
    }
}
