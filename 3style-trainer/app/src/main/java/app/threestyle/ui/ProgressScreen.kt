package app.threestyle.ui

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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.threestyle.bld.CaseType
import app.threestyle.data.Status

@Composable
fun ProgressScreen(vm: AppViewModel, modifier: Modifier) {
    val lib = vm.library ?: return Loading(modifier)
    val scheme = vm.settings.scheme
    var confirmReset by remember { mutableStateOf(false) }
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Text("Progress", style = MaterialTheme.typography.headlineSmall)
            Hint("For the buffers ${pieceLabel(lib.config.cornerBuffer, scheme)} and ${pieceLabel(lib.config.edgeBuffer, scheme)}. Progress is kept separately for every buffer.")
        }
        items(CaseType.entries) { type ->
            val cases = lib.cases(type)
            val ps = cases.map { vm.progressOf(it.key) }
            val known = ps.count { it.status == Status.KNOWN }
            val learning = ps.count { it.status == Status.LEARNING }
            val recall = ps.mapNotNull { it.avgRecall }
            val exec = ps.mapNotNull { it.avgExec }
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Row {
                        Text(type.label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Text("$known / ${cases.size} known")
                    }
                    LinearProgressIndicator(progress = { if (cases.isEmpty()) 0f else known / cases.size.toFloat() },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp))
                    Hint("$learning learning · ${cases.size - known - learning} new" +
                        (if (recall.isNotEmpty()) " · recall ${formatMs(recall.sum() / recall.size)}" else "") +
                        (if (exec.isNotEmpty()) " · execution ${formatMs(exec.sum() / exec.size)}" else ""))
                }
            }
        }
        item { SectionTitle("Weakest cases") ; Hint("Most misses relative to answers, then lowest strength.") }
        val weakest = lib.all.map { it to vm.progressOf(it.key) }.filter { it.second.attempts > 0 }
            .sortedWith(compareByDescending<Pair<app.threestyle.bld.BldCase, app.threestyle.data.ItemProgress>> { it.second.misses.toFloat() / it.second.attempts }
                .thenBy { it.second.box }).take(15)
        if (weakest.isEmpty()) item { Hint("Nothing practised yet.") }
        items(weakest, key = { it.first.key }) { (case, p) ->
            Row(Modifier.fillMaxWidth().clickable { vm.open(Screen.CaseDetail(case.key)) }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(case.label(scheme), fontFamily = FontFamily.Monospace, fontSize = 18.sp, modifier = Modifier.width(150.dp))
                Text("${case.type.short} · ${p.misses}/${p.attempts} missed · strength ${p.box}/5", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                StatusBadge(p.status)
            }
        }
        item {
            OutlinedButton(onClick = { confirmReset = true }, modifier = Modifier.padding(top = 16.dp)) { Text("Reset learning progress") }
            Hint("Your own algorithms and algorithm choices are kept.")
        }
    }
    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Reset progress?") },
            text = { Text("All cases (all buffers) become \"New\" and their statistics are deleted.") },
            confirmButton = { TextButton(onClick = { vm.resetProgress(); confirmReset = false }) { Text("Reset") } },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel") } },
        )
    }
}
