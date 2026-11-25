package app.f2ltrainer.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import app.f2ltrainer.data.Leitner
import app.f2ltrainer.data.PracticeItem
import app.f2ltrainer.data.Status

@Composable
fun ProgressScreen(vm: AppViewModel, modifier: Modifier = Modifier) {
    var confirmReset by rememberSaveable { mutableStateOf(false) }
    val items = Cases.ALL.map { PracticeItem(it.id, false) }
    val counts = Status.entries.associateWith { s -> items.count { vm.progressOf(it).status == s } }
    // weakest first: lowest memory strength, then slowest recognition
    val sorted = items.sortedWith(compareBy({ vm.progressOf(it).box }, { -(vm.progressOf(it).avgRecog ?: 0L) }))

    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Text("Progress", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Known: ${counts[Status.KNOWN]} · Learning: ${counts[Status.LEARNING]} · New: ${counts[Status.NEW]}",
                style = MaterialTheme.typography.titleMedium,
            )
            LinearProgressIndicator(
                progress = { (counts[Status.KNOWN] ?: 0) / items.size.toFloat() },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            )
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                for (g in CaseGroup.entries) {
                    val inGroup = items.filter { it.case.group == g }
                    val known = inGroup.count { vm.progressOf(it).status == Status.KNOWN }
                    Text("${g.title}: $known / ${inGroup.size} known", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        item {
            Text("All cases (weakest first)", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
            Text(
                "Strength 0–${Leitner.MAX} grows with correct answers. Times are averages of your last attempts.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        items(sorted, key = { it.key }) { item ->
            val p = vm.progressOf(item)
            Card(Modifier.fillMaxWidth().clickable { vm.open(Screen.CaseDetail(item.caseId)) }) {
                Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    CubeImage(item.case.displayState, Modifier.width(52.dp), description = item.case.name)
                    Column(Modifier.weight(1f).padding(start = 8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(item.case.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                            StatusBadge(p.status)
                        }
                        Text(
                            "Strength ${p.box}/${Leitner.MAX} · ${p.attempts} answers, ${p.misses} missed",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(
                            "Recognition ${formatMs(p.avgRecog)} · Solve ${formatMs(p.avgSolve)}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
        item {
            OutlinedButton(onClick = { confirmReset = true }, modifier = Modifier.padding(top = 16.dp)) {
                Text("Reset all progress")
            }
        }
    }
    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Reset all progress?") },
            text = { Text("All statuses, statistics, chosen and custom algorithms are deleted. This cannot be undone.") },
            confirmButton = { TextButton(onClick = { vm.resetProgress(); confirmReset = false }) { Text("Reset") } },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel") } },
        )
    }
}
