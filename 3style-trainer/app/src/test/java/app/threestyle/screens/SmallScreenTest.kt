package app.threestyle.screens

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.assertIsDisplayed
import app.threestyle.ui.App
import app.threestyle.ui.AppTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The letter quiz must fit on a small portrait phone without scrolling. */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [37], qualifiers = "w360dp-h640dp-xhdpi")
class SmallScreenTest {
    @get:Rule val rule = createComposeRule()

    @Test fun quizFitsWithoutScrolling() {
        rule.setContent { AppTheme { App(finish = {}) } }
        rule.onNodeWithText("Letters").performClick()
        rule.onNode(hasScrollAction()).performScrollToNode(hasText("Sticker → letter"))
        rule.onAllNodesWithText("Both")[0].performClick()
        rule.waitForIdle()
        // first and last key of the 6 x 4 grid are visible
        rule.onNodeWithText("A").assertIsDisplayed()
        rule.onNodeWithText("X").assertIsDisplayed()
        rule.onRoot().captureRoboImage("build/screenshots/30_quiz_small_phone.png")
    }
}
