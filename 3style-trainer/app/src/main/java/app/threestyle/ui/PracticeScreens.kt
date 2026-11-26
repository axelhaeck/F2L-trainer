package app.threestyle.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.threestyle.bld.CaseType
import app.threestyle.cube.Alg
import app.threestyle.data.Grade
import app.threestyle.data.PracticeMode
import app.threestyle.data.Status
import kotlin.math.roundToInt

@Composable
fun PracticeSetupScreen(vm: AppViewModel, modifier: Modifier) {
    val lib = vm.library ?: return Loading(modifier)
    val c = vm.practiceConfig
    var showSets by remember { mutableStateOf(c.sets.isNotEmpty()) }
    val pool = vm.pool(c)
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        item { Text("Practice", style = MaterialTheme.typography.headlineSmall) }
        item {
            SectionTitle("Cases")
            ChipGroup {
                for (t in CaseType.entries) {
                    FilterChip(selected = t in c.types, onClick = {
                        val types = if (t in c.types) c.types - t else c.types + t
                        if (types.isNotEmpty()) vm.updatePracticeConfig(c.copy(types = types))
                    }, label = { Text(t.label) })
                }
            }
        }
        item {
            SectionTitle("Status")
            ChipGroup {
                for (s in Status.entries) {
                    FilterChip(selected = s in c.statuses, onClick = {
                        val st = if (s in c.statuses) c.statuses - s else c.statuses + s
                        if (st.isNotEmpty()) vm.updatePracticeConfig(c.copy(statuses = st))
                    }, label = { Text(s.label) })
                }
            }
            Hint("Cases you know are left out unless \"Known\" is selected (review). A case becomes known when its strength reaches 5/5 (can be turned off in Settings) or when you mark it; forgetting a known case in practice makes it \"Learning\" again.")
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (c.sets.isEmpty()) "All sets" else "${c.sets.size} selected set(s)", modifier = Modifier.weight(1f))
                if (c.sets.isNotEmpty()) TextButton(onClick = { vm.updatePracticeConfig(c.copy(sets = emptySet())) }) { Text("All sets") }
                TextButton(onClick = { showSets = !showSets }) { Text(if (showSets) "Hide sets" else "Choose sets") }
            }
        }
        if (showSets) {
            items(lib.sets.filter { it.type in c.types }, key = { it.id }) { set ->
                Row(Modifier.fillMaxWidth().clickable {
                    vm.updatePracticeConfig(c.copy(sets = if (set.id in c.sets) c.sets - set.id else c.sets + set.id))
                }, verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = set.id in c.sets, onCheckedChange = null)
                    Column(Modifier.padding(start = 8.dp)) {
                        Text("${set.type.short}: ${set.title}", style = MaterialTheme.typography.bodyMedium)
                        Hint(set.keys.mapNotNull { lib.case(it)?.label(vm.settings.scheme) }.joinToString(" "))
                    }
                }
            }
        }
        item {
            SectionTitle("Batch")
            Text("${c.batchSize} cases per batch")
            Slider(
                value = c.batchSize.toFloat(),
                onValueChange = { vm.updatePracticeConfig(c.copy(batchSize = it.roundToInt())) },
                valueRange = AppViewModel.MIN_BATCH.toFloat()..AppViewModel.MAX_BATCH.toFloat(),
                steps = AppViewModel.MAX_BATCH - AppViewModel.MIN_BATCH - 1,
            )
            Text("Correct in a row to finish a case")
            ChipGroup {
                for (g in 1..3) FilterChip(selected = c.goal == g, onClick = { vm.updatePracticeConfig(c.copy(goal = g)) }, label = { Text("$g×") })
            }
            Hint("The weakest and longest unseen cases are picked first, a case together with its inverse.")
        }
        item {
            SectionTitle("Mode")
            ChipGroup {
                for (m in PracticeMode.entries) FilterChip(selected = c.mode == m, onClick = { vm.updatePracticeConfig(c.copy(mode = m)) }, label = { Text(m.label) })
            }
            Hint(c.mode.hint + " The time from showing the letters to revealing is recorded.")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Show the cube picture before revealing", modifier = Modifier.weight(1f))
                Switch(checked = c.showPicture, onCheckedChange = { vm.updatePracticeConfig(c.copy(showPicture = it)) })
            }
        }
        item {
            Text("${pool.size} cases match.", modifier = Modifier.padding(top = 8.dp))
            Button(onClick = { vm.startBatch() }, enabled = pool.isNotEmpty(), modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                Text("Start batch of ${minOf(c.batchSize, pool.size)}")
            }
            if (pool.isEmpty()) Hint("Nothing to practise: select more case types, statuses or sets.")
        }
    }
}

@Composable
fun SessionScreen(vm: AppViewModel) {
    val s = vm.session ?: return Loading(text = "")
    val lib = vm.library ?: return Loading()
    val scheme = vm.settings.scheme
    val endButton: @Composable () -> Unit = { TextButton(onClick = { vm.back() }) { Text("✕ End") } }
    val key = s.current
    if (key == null) SubScreen(s.title, { vm.back() }, actions = endButton) {
        run {
            item {
                Text("Batch done!", style = MaterialTheme.typography.headlineSmall)
                Hint("${s.session.answered} answers.")
            }
            items(s.session.items) { k ->
                val case = lib.case(k) ?: return@items
                val p = vm.progressOf(k)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(case.label(scheme), fontFamily = FontFamily.Monospace, fontSize = 18.sp, modifier = Modifier.weight(1f))
                    Text("${s.session.missesOf(k)} misses · strength ${p.box}/5  ", style = MaterialTheme.typography.bodySmall)
                    StatusBadge(p.status)
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 12.dp)) {
                    Button(onClick = { vm.back(); vm.startBatch() }) { Text("Next batch") }
                    OutlinedButton(onClick = { vm.back() }) { Text("Done") }
                }
            }
        }
    } else FlashcardScreen(vm, s, key, endButton)
}

@Composable
private fun FlashcardScreen(vm: AppViewModel, s: SessionUi, key: String, endButton: @Composable () -> Unit) {
    val lib = vm.library ?: return Loading()
    val scheme = vm.settings.scheme
    val case = lib.case(key) ?: return Loading(text = "")
    val alg = vm.preferredAlgOf(case)
    SplitSubScreen(s.title, { vm.back() }, actions = endButton, side = {
        Hint("${s.session.remaining} left in this batch · ${s.session.answered} answered · this case ${s.session.streakOf(key)}/${s.session.goalStreak} in a row")
            Card(Modifier.fillMaxWidth().heightIn(min = 120.dp).clickable(enabled = !s.revealed) { vm.reveal() }) {
                Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(case.type.label, style = MaterialTheme.typography.labelLarge)
                    Text(case.label(scheme), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold,
                        fontSize = if (case.type == CaseType.TWIST || case.type == CaseType.FLIP) 30.sp else 64.sp, textAlign = TextAlign.Center)
                    if (case.type == CaseType.PARITY) Hint("Parity, last corner target")
                    if (!s.revealed) Hint(if (s.config.mode == PracticeMode.RECALL) "Recall the commutator, then tap" else "Execute it, then tap")
                }
            }
        if (s.config.showPicture || s.revealed) CaseNet(case, vm)
    }) {
        if (!s.revealed) {
            item {
                Button(onClick = { vm.reveal() }, modifier = Modifier.fillMaxWidth()) { Text("Show commutator") }
            }
        } else {
            item {
                Text(case.describe(scheme), style = MaterialTheme.typography.bodyMedium)
                Text("Time: ${formatMs(s.timeMs)}", style = MaterialTheme.typography.bodyMedium)
                if (alg != null) {
                    AlgText(alg, size = 22.sp, bold = true, modifier = Modifier.padding(top = 4.dp))
                    val moves = Alg.simplify(Alg.parse(alg))
                    AlgText(Alg.format(moves), size = 14.sp)
                    Hint(moveCountLabel(Alg.moveCount(moves)))
                }
                TextButton(onClick = { vm.open(Screen.CaseDetail(key)) }) { Text("Open case details") }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (g in Grade.entries) {
                        val colours = when (g) {
                            Grade.AGAIN -> ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            Grade.HARD -> ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                            else -> ButtonDefaults.buttonColors()
                        }
                        Button(onClick = { vm.grade(g) }, colors = colours, modifier = Modifier.weight(1f), contentPadding = PaddingValues(4.dp)) {
                            Text(g.label)
                        }
                    }
                }
                Hint("Again = did not know it · Hard = slow or unsure · Good = knew it · Easy = instant (done for this batch)")
            }
        }
    }
}
