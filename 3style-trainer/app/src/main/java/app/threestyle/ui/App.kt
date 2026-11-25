package app.threestyle.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun App(finish: () -> Unit, vm: AppViewModel = viewModel()) {
    // Back goes up the screen stack, then to the Learn tab; from there the system closes the app.
    val top = vm.current
    BackHandler(enabled = vm.stack.size > 1 || (top is Screen.Home && top.tab != Tab.LEARN)) {
        if (!vm.back()) {
            if (top is Screen.Home && top.tab != Tab.LEARN) vm.selectTab(Tab.LEARN) else finish()
        }
    }
    when (val screen = vm.current) {
        is Screen.Home -> Scaffold(
            bottomBar = {
                NavigationBar {
                    for (tab in Tab.entries) {
                        NavigationBarItem(
                            selected = screen.tab == tab,
                            onClick = { vm.selectTab(tab) },
                            icon = { Text(tab.icon) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            },
        ) { padding ->
            val m = Modifier.padding(padding)
            when (screen.tab) {
                Tab.LEARN -> LearnScreen(vm, m)
                Tab.PRACTICE -> PracticeSetupScreen(vm, m)
                Tab.LETTERS -> LettersScreen(vm, m)
                Tab.PROGRESS -> ProgressScreen(vm, m)
                Tab.SETTINGS -> SettingsScreen(vm, m)
            }
        }
        is Screen.SetDetail -> SetDetailScreen(vm, screen.setId)
        is Screen.CaseDetail -> CaseDetailScreen(vm, screen.key)
        Screen.Session -> SessionScreen(vm)
        is Screen.LetterQuiz -> LetterQuizScreen(vm, screen.toLetter, screen.types)
        Screen.PairWords -> PairWordsScreen(vm)
        Screen.PairDrill -> PairDrillScreen(vm)
        Screen.Memo -> MemoScreen(vm)
        Screen.Guide -> GuideScreen(vm)
    }
}
