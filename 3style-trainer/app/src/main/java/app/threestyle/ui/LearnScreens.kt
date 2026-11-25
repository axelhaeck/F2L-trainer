package app.threestyle.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.threestyle.bld.BldCase
import app.threestyle.bld.CaseType
import app.threestyle.bld.Solution
import app.threestyle.bld.Stickers
import app.threestyle.cube.Alg
import app.threestyle.data.Status
import kotlinx.coroutines.delay

@Composable
fun LearnScreen(vm: AppViewModel, modifier: Modifier) {
    val lib = vm.library ?: return Loading(modifier)
    val scheme = vm.settings.scheme
    val type = vm.learnType
    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Learn", style = MaterialTheme.typography.headlineSmall)
                    Hint("Buffers: corners ${pieceLabel(lib.config.cornerBuffer, scheme)}, edges ${pieceLabel(lib.config.edgeBuffer, scheme)}")
                }
                TextButton(onClick = { vm.open(Screen.Guide) }) { Text("Guide") }
            }
        }
        item {
            ChipGroup {
                for (t in CaseType.entries) {
                    FilterChip(selected = t == type, onClick = { vm.learnType = t },
                        label = { Text("${t.label} (${lib.cases(t).size})") })
                }
            }
        }
        item {
            val cases = lib.cases(type)
            val known = cases.count { vm.progressOf(it.key).status == Status.KNOWN }
            val learning = cases.count { vm.progressOf(it.key).status == Status.LEARNING }
            Hint("$known known · $learning learning · ${cases.size - known - learning} new. " +
                "The cases are grouped by commutator class and the layer of the interchange; a case and its inverse are always in the same set.")
        }
        for (group in lib.groups(type)) {
            item(key = "g/$group") {
                SectionTitle(group)
                Hint(groupExplanation(group))
            }
            items(lib.sets.filter { it.type == type && it.group == group }, key = { it.id }) { set ->
                val known = set.keys.count { vm.progressOf(it).status == Status.KNOWN }
                val learning = set.keys.count { vm.progressOf(it).status == Status.LEARNING }
                Card(Modifier.fillMaxWidth().clickable { vm.open(Screen.SetDetail(set.id)) }) {
                    Column(Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(set.title.removePrefix("$group · "),
                                style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                            Text("$known/${set.keys.size} known", style = MaterialTheme.typography.labelMedium,
                                color = if (known == set.keys.size) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        val labels = set.keys.mapNotNull { lib.case(it)?.label(scheme) }
                        Text(labels.joinToString("  "), fontFamily = FontFamily.Monospace, fontSize = 15.sp,
                            modifier = Modifier.padding(top = 4.dp))
                        if (learning > 0) Hint("$learning learning")
                    }
                }
            }
        }
    }
}

@Composable
fun SetDetailScreen(vm: AppViewModel, setId: String) {
    val lib = vm.library ?: return Loading()
    val set = lib.sets.firstOrNull { it.id == setId } ?: return SubScreen("Set", { vm.back() }) {
        item { Text("This set does not exist for the current buffers.") }
    }
    val scheme = vm.settings.scheme
    SubScreen(set.title, { vm.back() }) {
        item { Hint(groupExplanation(set.group)) }
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { vm.startSet(set.id) }) { Text("Practise this set") }
                OutlinedButton(onClick = { vm.setStatus(set.keys, Status.KNOWN) }) { Text("All known") }
                OutlinedButton(onClick = { vm.setStatus(set.keys, Status.NEW) }) { Text("All new") }
            }
        }
        items(set.keys, key = { it }) { key ->
            val case = lib.case(key) ?: return@items
            val alg = vm.preferredAlgOf(case)
            val p = vm.progressOf(key)
            Card(Modifier.fillMaxWidth().clickable { vm.open(Screen.CaseDetail(key)) }) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(case.label(scheme), fontFamily = FontFamily.Monospace, fontSize = 22.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(if (case.type == CaseType.CORNER || case.type == CaseType.EDGE || case.type == CaseType.PARITY) 52.dp else 150.dp))
                    Column(Modifier.weight(1f)) {
                        if (alg != null) {
                            AlgText(alg, size = 15.sp)
                            Hint(moveCountLabel(Alg.moveCount(Alg.simplify(Alg.parse(alg)))))
                        } else Text("No algorithm", color = MaterialTheme.colorScheme.error)
                    }
                    StatusBadge(p.status)
                }
            }
        }
    }
}

/** Moves of an algorithm with their phase, in written order (before cancellations). */
fun phasedMoves(sol: Solution?, alg: String): List<Pair<String, Phase>> {
    if (sol == null) return Alg.parse(alg).map { it to Phase.MOVES }
    val out = ArrayList<Pair<String, Phase>>()
    sol.setup.forEach { out.add(it to Phase.SETUP) }
    if (sol.primitive != null) {
        Alg.parse(sol.primitive).forEach { out.add(it to Phase.MOVES) }
    } else {
        sol.partA.forEach { out.add(it to Phase.A) }
        sol.partB.forEach { out.add(it to Phase.B) }
        Alg.invert(sol.partA).forEach { out.add(it to Phase.A_INV) }
        Alg.invert(sol.partB).forEach { out.add(it to Phase.B_INV) }
    }
    Alg.invert(sol.setup).forEach { out.add(it to Phase.UNDO) }
    return out
}

@Composable
fun CaseDetailScreen(vm: AppViewModel, key: String) {
    val lib = vm.library ?: return Loading()
    val case = lib.case(key) ?: return SubScreen("Case", { vm.back() }) {
        item { Text("This case does not exist for the current buffers.") }
    }
    val scheme = vm.settings.scheme
    val p = vm.progressOf(key)
    val alg = vm.preferredAlgOf(case)
    val sol = case.solutions.firstOrNull { it.notation == alg }
    SubScreen("${case.type.short} ${case.label(scheme)}", { vm.back() }) {
        item {
            Text(case.describe(scheme), style = MaterialTheme.typography.bodyLarge)
            lib.setOf(key)?.let { Hint("Set: ${it.title}") }
        }
        item {
            CaseNet(case, vm)
            Hint(when (case.type) {
                CaseType.CORNER, CaseType.EDGE -> "The cube before the algorithm. Ring = buffer; the arrows show where the stickers go: buffer → ${scheme.letterOf(case.stickers[0])} → ${scheme.letterOf(case.stickers[1])} → buffer."
                CaseType.TWIST -> "↻ = twisted clockwise, ↺ = twisted counter-clockwise. The algorithm twists them back."
                CaseType.FLIP -> "The two marked edges are flipped."
                CaseType.PARITY -> "Rings = buffers. The algorithm swaps the corner buffer with ${scheme.letterOf(case.stickers[0])} and the edge buffer with ${scheme.letterOf(case.stickers[1])}."
            })
        }
        if (alg == null) {
            item { Text("No algorithm found for this case. Add your own below.", color = MaterialTheme.colorScheme.error) }
        } else {
            item {
                SectionTitle("Algorithm")
                AlgText(alg, size = 22.sp, bold = true)
                val moves = Alg.simplify(Alg.parse(alg))
                Text("${Alg.format(moves)}  (${moveCountLabel(Alg.moveCount(moves))})",
                    fontFamily = FontFamily.Monospace, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (sol != null && (sol.primitive == null || sol.setup.isNotEmpty())) Structure(sol)
            }
            item { StepThrough(vm, case, sol, alg) }
        }
        item {
            SectionTitle("Status")
            StatusSelector(p.status, { vm.setStatus(key, it) })
            Hint("Strength ${p.box}/5 · ${p.attempts} answers, ${p.misses} misses · recall ${formatMs(p.avgRecall)} · execution ${formatMs(p.avgExec)}")
            FilledTonalButton(onClick = { vm.startSingle(key) }, modifier = Modifier.padding(top = 8.dp)) { Text("Practise this case") }
        }
        item { SectionTitle("Algorithms") ; Hint("Tap one to use it in practice. Own algorithms are checked by simulation before they are added.") }
        items(vm.algorithmsOf(case)) { a ->
            val isCustom = a in p.customAlgs && case.solutions.none { it.notation == a }
            val selected = a == alg
            Row(
                Modifier.fillMaxWidth()
                    .background(if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent, RoundedCornerShape(8.dp))
                    .clickable { vm.setPreferred(key, a) }
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    AlgText(a, size = 16.sp, bold = selected)
                    Hint(moveCountLabel(Alg.moveCount(Alg.simplify(Alg.parse(a)))) + if (isCustom) " · your algorithm" else "")
                }
                if (isCustom) TextButton(onClick = { vm.removeCustom(key, a) }) { Text("Remove") }
            }
        }
        item { AddAlgorithm(vm, case) }
    }
}

@Composable
private fun Structure(sol: Solution) {
    Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        @Composable
        fun row(label: String, moves: List<String>, colour: Color) {
            if (moves.isEmpty()) return
            Row {
                Text(label, color = colour, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(110.dp))
                AlgText(Alg.format(moves), size = 15.sp)
            }
        }
        row("Setup", sol.setup, Phase.SETUP.colour)
        if (sol.primitive != null) {
            row("Algorithm", Alg.parse(sol.primitive), Phase.MOVES.colour)
        } else {
            val aIsInterchange = sol.interchange === sol.partA
            val bIsInterchange = sol.interchange === sol.partB
            row(if (aIsInterchange) "A interchange" else "A insertion", sol.partA, Phase.A.colour)
            row(if (bIsInterchange) "B interchange" else "B insertion", sol.partB, Phase.B.colour)
            if (sol.cancels) Hint("Some moves cancel when written out.")
        }
    }
}

@Composable
private fun StepThrough(vm: AppViewModel, case: BldCase, sol: Solution?, alg: String) {
    val moves = remember(alg) { phasedMoves(sol, alg) }
    var step by remember(alg) { mutableIntStateOf(0) }
    var playing by remember(alg) { mutableStateOf(false) }
    val state = remember(alg, step) { case.state.apply(moves.take(step).map { it.first }) }
    LaunchedEffect(playing, alg) {
        while (playing) {
            delay(700)
            if (step >= moves.size) { playing = false } else step++
        }
    }
    Column {
        SectionTitle("Step through")
        CaseNet(case, vm, state = state, decorate = step == 0)
        FlowRow(Modifier.padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            moves.forEachIndexed { i, (m, phase) ->
                val done = i < step
                Box(
                    Modifier.background(if (done) phase.colour else phase.colour.copy(alpha = 0.18f), RoundedCornerShape(6.dp))
                        .clickable { playing = false; step = i + 1 }
                        .padding(horizontal = 7.dp, vertical = 3.dp),
                ) {
                    Text(m, fontFamily = FontFamily.Monospace, fontSize = 16.sp,
                        color = if (done) Color.White else MaterialTheme.colorScheme.onSurface)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedButton(onClick = { playing = false; step = 0 }) { Text("⏮") }
            OutlinedButton(onClick = { playing = false; if (step > 0) step-- }) { Text("◀") }
            Button(onClick = { if (step >= moves.size) step = 0; playing = !playing }) { Text(if (playing) "Pause" else "Play") }
            OutlinedButton(onClick = { playing = false; if (step < moves.size) step++ }) { Text("▶") }
            OutlinedButton(onClick = { playing = false; step = moves.size }) { Text("⏭") }
        }
        val legend = moves.map { it.second }.distinct()
        Hint("Step $step of ${moves.size}" + (if (step == moves.size && case.state.apply(moves.map { it.first }).isSolvedUpToRotation()) " · solved ✓" else "") +
            "   " + legend.joinToString("  ") { it.label })
        Spacer(Modifier.padding(2.dp))
    }
}

@Composable
private fun AddAlgorithm(vm: AppViewModel, case: BldCase) {
    var text by remember(case.key) { mutableStateOf("") }
    var message by remember(case.key) { mutableStateOf<String?>(null) }
    Column {
        SectionTitle("Add your own algorithm")
        OutlinedTextField(
            value = text,
            onValueChange = { text = it; message = null },
            label = { Text("e.g. [R' D: [R U R', D2]]") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
            Button(onClick = {
                val err = vm.addCustom(case, text)
                message = err ?: "Added and selected."
                if (err == null) text = ""
            }) { Text("Check and add") }
            message?.let {
                Text(it, modifier = Modifier.padding(start = 12.dp),
                    color = if (it.startsWith("Added")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
            }
        }
        Hint("Notation: U D F B R L, M E S, u d f b r l (or Rw), x y z; [A, B] = A B A' B', [A: B] = A B A'.")
        Hint("Pieces: ${case.stickers.joinToString { Stickers.stickerName(it) }}")
    }
}
