package app.f2ltrainer.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.f2ltrainer.data.CaseGroup
import app.f2ltrainer.data.Cases
import app.f2ltrainer.data.PracticeItem
import app.f2ltrainer.data.Status

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearnScreen(vm: AppViewModel, modifier: Modifier = Modifier) {
    var filter by rememberSaveable { mutableStateOf<Status?>(null) }
    val shown = Cases.ALL.filter { filter == null || vm.progressOf(PracticeItem(it.id, false)).status == filter }
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("F2L cases", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
                TextButton(onClick = { vm.open(Screen.Guide) }) { Text("Guide") }
            }
            Text(
                "Tap a case to study it: picture, recognition, algorithms with step-by-step playback, " +
                    "and the free back-slot shortcut. Mark cases as Learning or Known to control what you practise.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.padding(4.dp))
            ChipGroup {
                FilterChip(selected = filter == null, onClick = { filter = null }, label = { Text("All") })
                for (s in Status.entries) {
                    val n = Cases.ALL.count { vm.progressOf(PracticeItem(it.id, false)).status == s }
                    FilterChip(selected = filter == s, onClick = { filter = s }, label = { Text("${s.label} ($n)") })
                }
            }
        }
        for (group in CaseGroup.entries) {
            val inGroup = shown.filter { it.group == group }
            if (inGroup.isEmpty()) continue
            item(key = "h-$group") {
                Column(Modifier.padding(top = 12.dp)) {
                    Text(group.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
            items(inGroup, key = { it.id }) { case ->
                val item = PracticeItem(case.id, false)
                Card(Modifier.fillMaxWidth().clickable { vm.open(Screen.CaseDetail(case.id)) }) {
                    Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        CubeImage(case.displayState, Modifier.width(76.dp), description = case.name)
                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(case.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                                StatusBadge(vm.progressOf(item).status)
                            }
                            AlgText(vm.preferredAlgOf(item), size = MaterialTheme.typography.bodyLarge.fontSize)
                            if (case.freeSlotAlgorithm != null) {
                                Text("+ free-slot shortcut", style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                    }
                }
            }
        }
    }
}
