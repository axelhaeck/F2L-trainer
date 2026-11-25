package app.threestyle.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import app.threestyle.bld.BufferConfig
import app.threestyle.bld.LetterScheme
import app.threestyle.bld.PieceType
import app.threestyle.bld.Stickers
import app.threestyle.data.Colours

@Composable
fun SettingsScreen(vm: AppViewModel, modifier: Modifier) {
    val s = vm.settings
    val scheme = s.scheme
    val cfg = vm.bufferConfig
    val context = LocalContext.current
    var message by remember { mutableStateOf<String?>(null) }
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if (uri != null) {
            message = try {
                context.contentResolver.openOutputStream(uri)?.use { it.write(vm.exportText().toByteArray()) }
                "Backup saved."
            } catch (e: Exception) {
                "Could not save: ${e.message}"
            }
        }
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            message = try {
                val text = context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() } ?: ""
                vm.importText(text)
            } catch (e: Exception) {
                "Could not read: ${e.message}"
            }
        }
    }
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        item { Text("Settings", style = MaterialTheme.typography.headlineSmall) }
        item {
            SectionTitle("Corner buffer")
            ChipGroup {
                for (p in Stickers.pieces(PieceType.CORNER)) {
                    FilterChip(selected = p == cfg.cornerBuffer, onClick = { vm.updateSettings(s.copy(cornerBuffer = Stickers.pieceName(p))) },
                        label = { Text(pieceLabel(p, scheme)) })
                }
            }
            SectionTitle("Edge buffer")
            ChipGroup {
                for (p in Stickers.pieces(PieceType.EDGE)) {
                    FilterChip(selected = p == cfg.edgeBuffer, onClick = {
                        vm.updateSettings(s.copy(edgeBuffer = Stickers.pieceName(p),
                            parityEdge = Stickers.stickerName(BufferConfig.defaultParityEdge(p))))
                    }, label = { Text(pieceLabel(p, scheme)) })
                }
            }
            Hint("The usual buffers are UFR and UF. Progress and own algorithms of corner and edge cases are kept per buffer (twists and flips do not depend on the buffer). The algorithms for every buffer are included and checked.")
        }
        item {
            SectionTitle("Parity edge")
            Hint("Parity swaps the corner buffer with the last corner target and the edge buffer with this sticker (UF buffer: usually UR, letter ${scheme.letterOf(Stickers.byName("UR"))}).")
            ChipGroup {
                for (e in Stickers.SPEFFZ_EDGES.filter { !Stickers.samePiece(it, cfg.edgeBuffer) }) {
                    FilterChip(selected = e == cfg.parityEdge, onClick = { vm.updateSettings(s.copy(parityEdge = Stickers.stickerName(e))) },
                        label = { Text("${scheme.letterOf(e)} ${Stickers.stickerName(e)}") })
                }
            }
        }
        item { SchemeEditor(vm) }
        item {
            SectionTitle("Cube colours")
            Text("Top")
            ChipGroup {
                for (c in 0 until 6) FilterChip(selected = s.topColour == c, onClick = {
                    val front = if (CubeColours.valid(c, s.frontColour)) s.frontColour else (0 until 6).first { CubeColours.valid(c, it) }
                    vm.updateSettings(s.copy(topColour = c, frontColour = front))
                }, label = { Text(Colours.NAMES[c]) })
            }
            Text("Front")
            ChipGroup {
                for (c in 0 until 6) if (CubeColours.valid(s.topColour, c)) {
                    FilterChip(selected = s.frontColour == c, onClick = { vm.updateSettings(s.copy(frontColour = c)) }, label = { Text(Colours.NAMES[c]) })
                }
            }
            Hint("Only the pictures change: letters and algorithms always refer to positions (U = top, F = front).")
        }
        item {
            SectionTitle("Practice")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Mark a case as known when its strength reaches 5/5", modifier = Modifier.weight(1f))
                Switch(checked = s.autoKnown, onCheckedChange = { vm.updateSettings(s.copy(autoKnown = it)) })
            }
        }
        item {
            SectionTitle("Backup")
            Hint("Save or restore settings, progress, own algorithms and letter pair words as a text file. You choose the place; the app needs no storage permission.")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = { exporter.launch("3style-trainer-backup.txt") }) { Text("Export") }
                FilledTonalButton(onClick = { importer.launch(arrayOf("text/plain", "*/*")) }) { Text("Import") }
            }
            message?.let { Text(it, modifier = Modifier.padding(top = 4.dp)) }
        }
        item {
            SectionTitle("About")
            Hint("3Style Trainer works offline and has no permissions: no internet, no storage access. Every algorithm is verified with a cube simulator.")
        }
    }
}

@Composable
private fun SchemeEditor(vm: AppViewModel) {
    val s = vm.settings
    var corners by remember(s.schemeCorners) { mutableStateOf(s.schemeCorners) }
    var edges by remember(s.schemeEdges) { mutableStateOf(s.schemeEdges) }
    val valid = LetterScheme.isValid(corners) && LetterScheme.isValid(edges)
    SectionTitle("Letter scheme")
    Hint("24 different letters each, in Speffz sticker order: U face (UBL/UB first, clockwise), then L, F, R, B, D. Default: Speffz A–X.")
    OutlinedTextField(corners, { corners = it.uppercase().take(24) }, label = { Text("Corners (${corners.length}/24)") },
        singleLine = true, modifier = Modifier.fillMaxWidth(), textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters), isError = !LetterScheme.isValid(corners))
    OutlinedTextField(edges, { edges = it.uppercase().take(24) }, label = { Text("Edges (${edges.length}/24)") },
        singleLine = true, modifier = Modifier.fillMaxWidth(), textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters), isError = !LetterScheme.isValid(edges))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
        Button(onClick = { vm.updateSettings(s.copy(schemeCorners = corners, schemeEdges = edges)) },
            enabled = valid && (corners != s.schemeCorners || edges != s.schemeEdges)) { Text("Save scheme") }
        OutlinedButton(onClick = {
            corners = LetterScheme.SPEFFZ; edges = LetterScheme.SPEFFZ
            vm.updateSettings(s.copy(schemeCorners = LetterScheme.SPEFFZ, schemeEdges = LetterScheme.SPEFFZ))
        }) { Text("Speffz") }
    }
    if (!valid) Hint("Each scheme needs exactly 24 different characters.")
}
