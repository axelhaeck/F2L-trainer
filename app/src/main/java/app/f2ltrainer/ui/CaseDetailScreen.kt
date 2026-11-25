package app.f2ltrainer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import app.f2ltrainer.cube.F2l
import app.f2ltrainer.data.AlgValidator
import app.f2ltrainer.data.Cases
import app.f2ltrainer.data.PracticeItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaseDetailScreen(vm: AppViewModel, caseId: Int) {
    val case = Cases.byId(caseId)
    val std = PracticeItem(caseId, false)
    val freeItem = PracticeItem(caseId, true)
    var freeMode by rememberSaveable(caseId) { mutableStateOf(false) }
    var back by rememberSaveable(caseId) { mutableStateOf(false) }
    val item = if (freeMode) freeItem else std
    val algs = vm.algorithmsOf(item)
    var selected by rememberSaveable(caseId, freeMode) { mutableStateOf<String?>(null) }
    val stepAlg = selected?.takeIf { it in algs } ?: vm.preferredAlgOf(item)
    val stepper = rememberStepper(case.displayState, stepAlg)
    val empty = if (freeMode) setOf(F2l.Slot.BR) else emptySet()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(case.name + if (freeMode) " · free BR slot" else "") },
                navigationIcon = { TextButton(onClick = { vm.back() }) { Text("← Back") } },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(case.group.title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)

            // ---- picture + playback
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    CubeImage(
                        stepper.state, Modifier.widthIn(max = 280.dp).fillMaxWidth(0.75f),
                        back = back, emptySlots = empty, description = "${case.name} picture",
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = !back, onClick = { back = false }, label = { Text("Front view") })
                        FilterChip(selected = back, onClick = { back = true }, label = { Text("Back view") })
                    }
                    if (case.freeSlotAlgorithm != null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = !freeMode, onClick = { freeMode = false }, label = { Text("Normal") })
                            FilterChip(selected = freeMode, onClick = { freeMode = true }, label = { Text("BR slot free") })
                        }
                    }
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    AlgText(stepAlg, bold = true)
                    StepperControls(stepper, Modifier.fillMaxWidth().padding(top = 8.dp))
                    PhaseLegend()
                }
            }

            // ---- recognition
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Column(Modifier.padding(12.dp)) {
                    Text("Recognition", style = MaterialTheme.typography.titleSmall)
                    for (line in case.recognition) Text("• $line", style = MaterialTheme.typography.bodyMedium)
                    if (freeMode) {
                        Text(
                            "• The back-right slot is still unsolved (dark stickers). This shortcut may move its pieces, " +
                                "so use it only while that slot is empty.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Text(case.group.description, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp))
                }
            }

            // ---- status
            Text("Your status" + if (freeMode) " (free-slot variant)" else "", style = MaterialTheme.typography.titleSmall)
            StatusSelector(vm.progressOf(item).status, { vm.setStatus(item, it) })

            // ---- algorithms
            Text("Algorithms", style = MaterialTheme.typography.titleSmall)
            val preferred = vm.preferredAlgOf(item)
            val custom = vm.progressOf(item).customAlgs
            for (a in algs) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        AlgText(a)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                moveCountLabel(a) + (if (a == preferred) " · your algorithm" else "") +
                                    (if (a in custom) " · added by you" else ""),
                                style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f),
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (a != preferred) TextButton(onClick = { vm.setPreferred(item, a) }) { Text("Use this one") }
                            if (a != stepAlg) TextButton(onClick = { selected = a }) { Text("Show steps") }
                            if (a in custom) TextButton(onClick = { vm.removeCustom(item, a) }) { Text("Delete") }
                        }
                    }
                }
            }

            AddAlgorithm(vm, item)

            Button(onClick = { vm.startSingle(item) }, modifier = Modifier.fillMaxWidth()) {
                Text("Practise this case now")
            }
        }
    }
}

@Composable
private fun AddAlgorithm(vm: AppViewModel, item: PracticeItem) {
    var text by rememberSaveable(item.key) { mutableStateOf("") }
    var message by rememberSaveable(item.key) { mutableStateOf<String?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Add your own algorithm", style = MaterialTheme.typography.titleSmall)
        Text(
            "It is checked with the cube simulator: it must solve the pair" +
                (if (item.freeSlot) "" else " without breaking the cross or the other slots") +
                ". A missing or extra U at the start is corrected automatically.",
            style = MaterialTheme.typography.bodySmall,
        )
        OutlinedTextField(
            value = text,
            onValueChange = { text = it; message = null },
            label = { Text("e.g. U' R U R' U2 R U' R'") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, autoCorrectEnabled = false),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedButton(onClick = {
            when (val r = AlgValidator.validate(item.case, text, item.freeSlot)) {
                is AlgValidator.Result.Ok -> {
                    if (r.normalized in vm.algorithmsOf(item)) {
                        message = "Already in the list: ${r.normalized}"
                    } else {
                        vm.addCustom(item, r.normalized)
                        message = "Correct! Added: ${r.normalized}"
                        text = ""
                    }
                }
                is AlgValidator.Result.Error -> message = r.message
            }
        }) { Text("Check and add") }
        message?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.tertiary) }
    }
}
