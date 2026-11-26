package app.threestyle.screens

import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.junit4.createComposeRule
import app.threestyle.ui.KeepScreenOnWhileActive
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [37])
class KeepScreenOnTest {
    @get:Rule val rule = createComposeRule()

    @Test fun onWhileActiveThenReleasedAfterOneMinute() {
        // (that a touch restarts the minute cannot be simulated: test taps fast-forward the clock)
        lateinit var view: View
        rule.setContent {
            view = LocalView.current
            KeepScreenOnWhileActive { Box(Modifier.fillMaxSize()) { Text("tap me") } }
        }
        rule.mainClock.autoAdvance = false
        rule.mainClock.advanceTimeBy(1_000)
        assertTrue("screen kept on while practising", view.keepScreenOn)
        rule.mainClock.advanceTimeBy(58_000)
        assertTrue("still on after 59 s", view.keepScreenOn)
        rule.mainClock.advanceTimeBy(2_000)
        assertFalse("released after one minute without a touch: normal dimming and auto lock", view.keepScreenOn)
    }
}
