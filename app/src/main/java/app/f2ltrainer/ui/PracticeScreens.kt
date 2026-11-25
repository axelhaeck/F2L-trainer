package app.f2ltrainer.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.os.SystemClock
import app.f2ltrainer.cube.F2l
import app.f2ltrainer.data.CaseGroup
import app.f2ltrainer.data.Grade
import app.f2ltrainer.data.Setup
import app.f2ltrainer.data.Status

@Composable
fun PracticeSetupScreen(vm: AppViewModel, modifier: Modifier = Modifier) {
    val c = vm.config
    val pool = vm.pool(c)
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Practice", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Practise in small batches. Every case in a batch has to be answered correctly ${c.goal}× in a row; " +
                "missed cases come back quickly. The weakest and longest-unseen cases are picked first.",
            style = MaterialTheme.typography.bodyMedium,
        )

        Text("Cases with status", style = MaterialTheme.typography.titleSmall)
        ChipGroup {
            for (s in Status.entries) {
                FilterChip(
                    selected = s in c.statuses,
                    onClick = { vm.updateConfig(c.copy(statuses = c.statuses.toggle(s))) },
                    label = { Text(s.label) },
                )
            }
        }
        Text("Groups", style = MaterialTheme.typography.titleSmall)
        ChipGroup {
            for (g in CaseGroup.entries) {
                FilterChip(
                    selected = g in c.groups,
                    onClick = { vm.updateConfig(c.copy(groups = c.groups.toggle(g))) },
                    label = { Text(g.title) },
                )
            }
        }
        SwitchRow("Include free back-slot shortcuts", c.includeFree) { vm.updateConfig(c.copy(includeFree = it)) }
        SwitchRow("Random U-layer angle (AUF)", c.randomAuf) { vm.updateConfig(c.copy(randomAuf = it)) }
        SwitchRow("Show alternative algorithms", c.showAlternatives) { vm.updateConfig(c.copy(showAlternatives = it)) }

        Text("Batch size: ${c.batchSize}", style = MaterialTheme.typography.titleSmall)
        Slider(
            value = c.batchSize.toFloat(),
            onValueChange = { vm.updateConfig(c.copy(batchSize = it.toInt())) },
            valueRange = AppViewModel.MIN_BATCH.toFloat()..AppViewModel.MAX_BATCH.toFloat(),
            steps = AppViewModel.MAX_BATCH - AppViewModel.MIN_BATCH - 1,
        )
        Text("Correct answers in a row needed: ${c.goal}", style = MaterialTheme.typography.titleSmall)
        Slider(
            value = c.goal.toFloat(),
            onValueChange = { vm.updateConfig(c.copy(goal = it.toInt())) },
            valueRange = 1f..3f,
            steps = 1,
        )

        Text("Mode", style = MaterialTheme.typography.titleSmall)
        ChipGroup {
            for (m in PracticeMode.entries) {
                FilterChip(selected = c.mode == m, onClick = { vm.updateConfig(c.copy(mode = m)) }, label = { Text(m.label) })
            }
        }
        Text(
            when (c.mode) {
                PracticeMode.FLASHCARD -> "See the case, recall the algorithm (ideally finger it), then reveal and rate yourself. " +
                    "Your recognition time is recorded."
                PracticeMode.TIMED -> "Apply the shown setup to a solved cube (yellow top, green front). " +
                    "Start the timer, solve the pair, stop the timer. Your solve time is recorded."
            },
            style = MaterialTheme.typography.bodySmall,
        )

        Text("${pool.size} case(s) match", style = MaterialTheme.typography.bodyMedium)
        Button(onClick = { vm.startBatch() }, enabled = pool.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
            Text("Start batch of ${minOf(c.batchSize, pool.size)}")
        }
        if (pool.isEmpty()) {
            Text(
                "No cases match. Select more statuses or groups, or mark cases as Learning in the Learn tab.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

private fun <T> Set<T>.toggle(v: T): Set<T> = if (v in this) this - v else this + v

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onChange(!checked) }, verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionScreen(vm: AppViewModel) {
    val s = vm.session
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (s == null || s.session.finished) "Batch finished" else "Practice") },
                navigationIcon = { TextButton(onClick = { vm.back() }) { Text("✕ End") } },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (s == null) return@Box
            @Suppress("UNUSED_VARIABLE") val v = s.version // recompose on every new exercise
            if (s.session.finished) BatchSummary(vm, s) else ExerciseView(vm, s)
        }
    }
}

@Composable
private fun ExerciseView(vm: AppViewModel, s: SessionUi) {
    val ex = s.exercise ?: return
    val item = ex.item
    val case = item.case
    val empty = if (item.freeSlot) setOf(F2l.Slot.BR) else emptySet()
    val total = s.session.items.size
    val done = total - s.session.remaining
    var back by remember(s.version) { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        LinearProgressIndicator(progress = { done.toFloat() / total }, modifier = Modifier.fillMaxWidth())
        Text("$done of $total done · streak ${s.session.streakOf(item)}/${s.session.goalStreak}", style = MaterialTheme.typography.bodySmall)
        if (item.freeSlot) {
            Text("Back-right slot is FREE – use the shortcut", color = MaterialTheme.colorScheme.tertiary,
                style = MaterialTheme.typography.titleSmall)
        }

        val timed = s.config.mode == PracticeMode.TIMED
        val showPicture = !timed || s.revealed
        if (timed && !s.revealed) {
            TimedPart(vm, s)
        }
        if (showPicture) {
            CubeImage(ex.state, Modifier.widthIn(max = 300.dp).fillMaxWidth(0.8f).clickable { back = !back },
                back = back, emptySlots = empty, description = "Case to recognise")
            Text(if (back) "Back view · tap to turn" else "Tap the cube to see the back", style = MaterialTheme.typography.labelSmall)
        }

        if (!s.revealed) {
            if (!timed) {
                Button(onClick = { vm.reveal() }, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("Show algorithm") }
            }
        } else {
            s.timeMs?.let { Text("Time: ${formatMs(it)}", style = MaterialTheme.typography.headlineSmall) }
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(case.name + if (item.freeSlot) " · free BR slot" else "", style = MaterialTheme.typography.titleMedium)
                    val preferred = vm.preferredAlgOf(item)
                    AlgText(ex.adjust(preferred), size = 22.sp, bold = true)
                    if (s.config.showAlternatives) {
                        for (a in vm.algorithmsOf(item)) if (a != preferred) AlgText(ex.adjust(a), size = 15.sp)
                    }
                }
            }
            Text("How did it go?", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                for (g in Grade.entries) {
                    val colors = when (g) {
                        Grade.AGAIN -> ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        Grade.HARD -> ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                        Grade.GOOD -> ButtonDefaults.buttonColors()
                        Grade.EASY -> ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    }
                    Button(onClick = { vm.grade(g) }, colors = colors, modifier = Modifier.weight(1f)) { Text(g.label) }
                }
            }
            Text(
                "Again = didn't know · Hard = slow or unsure · Good = knew it · Easy = instant (leaves the batch)",
                style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun TimedPart(vm: AppViewModel, s: SessionUi) {
    val ex = s.exercise ?: return
    val solving = ex.adjust(vm.preferredAlgOf(ex.item))
    val scramble = remember(s.version) { Setup.scramble(solving, s.disguise) }
    val start = s.timerStart
    if (start == null) {
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
            Column(Modifier.padding(12.dp)) {
                Text("Setup (from solved, yellow top, green front):", style = MaterialTheme.typography.labelLarge)
                Text(scramble, fontFamily = FontFamily.Monospace, fontSize = 20.sp)
            }
        }
        Button(onClick = { vm.startTimer() }, modifier = Modifier.fillMaxWidth().height(64.dp)) { Text("Start timer") }
        OutlinedButton(onClick = { vm.reveal() }) { Text("Skip timing, show solution") }
    } else {
        var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
        LaunchedEffect(start) {
            while (true) withFrameMillis { now = SystemClock.elapsedRealtime() }
        }
        Box(
            Modifier.fillMaxWidth().height(260.dp).clickable { vm.stopTimer() },
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(formatMs(now - start), style = MaterialTheme.typography.displayLarge, fontFamily = FontFamily.Monospace)
                Spacer(Modifier.height(8.dp))
                Text("Tap anywhere here to stop", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun BatchSummary(vm: AppViewModel, s: SessionUi) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Well done! ${s.session.answered} answers in this batch.", style = MaterialTheme.typography.titleMedium)
        Text(
            "Mark the cases you now know as Known: they are then left out of normal practice " +
                "(select \"Known\" in Practice to review them).",
            style = MaterialTheme.typography.bodyMedium,
        )
        for (item in s.session.items) {
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    CubeImage(item.case.displayState, Modifier.widthIn(max = 64.dp).fillMaxWidth(0.2f),
                        emptySlots = if (item.freeSlot) setOf(F2l.Slot.BR) else emptySet(), description = item.case.name)
                    Column(Modifier.weight(1f).padding(start = 8.dp)) {
                        Text(item.case.name + if (item.freeSlot) " (free slot)" else "", style = MaterialTheme.typography.titleSmall)
                        Text("Misses: ${s.session.missesOf(item)}", style = MaterialTheme.typography.bodySmall)
                        StatusSelector(vm.progressOf(item).status, { vm.setStatus(item, it) })
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = { vm.back() }) { Text("Done") }
            if (vm.pool().isNotEmpty()) {
                Button(onClick = { vm.startBatch() }) { Text("Next batch") }
            }
        }
    }
}
