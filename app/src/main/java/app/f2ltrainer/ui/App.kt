package app.f2ltrainer.ui

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
                            icon = { Text(tabIcon(tab)) },
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
                Tab.PROGRESS -> ProgressScreen(vm, m)
            }
        }
        is Screen.CaseDetail -> CaseDetailScreen(vm, screen.caseId)
        Screen.Guide -> GuideScreen(vm)
        Screen.Session -> SessionScreen(vm)
    }
}

private fun tabIcon(tab: Tab) = when (tab) {
    Tab.LEARN -> "📖"
    Tab.PRACTICE -> "⏱"
    Tab.PROGRESS -> "📈"
}
