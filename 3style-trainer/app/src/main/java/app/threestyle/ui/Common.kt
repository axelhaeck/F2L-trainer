package app.threestyle.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.threestyle.bld.BldCase
import app.threestyle.bld.CaseType
import app.threestyle.bld.LetterScheme
import app.threestyle.bld.Stickers
import app.threestyle.cube.Cube
import app.threestyle.cube.CubeState
import app.threestyle.data.Status

@Composable
fun AlgText(alg: String, modifier: Modifier = Modifier, size: TextUnit = 18.sp, bold: Boolean = false, color: Color = Color.Unspecified) {
    Text(
        alg,
        modifier = modifier,
        fontFamily = FontFamily.Monospace,
        fontSize = size,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
        color = color,
    )
}

fun moveCountLabel(n: Int): String = "$n move" + if (n == 1) "" else "s"

@Composable
fun StatusBadge(status: Status, modifier: Modifier = Modifier) {
    val (bg, fg) = when (status) {
        Status.NEW -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        Status.LEARNING -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        Status.KNOWN -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
    }
    Surface(modifier, color = bg, shape = MaterialTheme.shapes.small) {
        Text(status.label, color = fg, style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
    }
}

@Composable
fun StatusSelector(status: Status, onChange: (Status) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for (s in Status.entries) {
            FilterChip(selected = s == status, onClick = { onChange(s) }, label = { Text(s.label) })
        }
    }
}

@Composable
fun ChipGroup(content: @Composable () -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) { content() }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
        modifier = modifier.padding(top = 12.dp, bottom = 4.dp))
}

@Composable
fun Hint(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = modifier)
}

fun formatMs(ms: Long?): String = if (ms == null) "–" else String.format(java.util.Locale.US, "%.2fs", ms / 1000.0)

/** A full screen with a title bar and a back button; the content is a lazy list. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubScreen(
    title: String,
    onBack: () -> Unit,
    actions: @Composable () -> Unit = {},
    content: LazyListScope.() -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { TextButton(onClick = onBack) { Text("← Back") } },
                actions = { actions() },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) { content() }
    }
}

/** A full screen with a title bar and a back button; the content does not scroll (fixed portrait layout). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FixedScreen(title: String, onBack: () -> Unit, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { TextButton(onClick = onBack) { Text("← Back") } },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content,
        )
    }
}

/**
 * Letters in rows of [columns] (default 4: A B C D / E F G H / ...), i.e. one row per face in Speffz
 * order. [cell] draws one letter; its modifier gives every cell the same width.
 */
@Composable
fun LetterGrid(
    letters: List<Char>,
    modifier: Modifier = Modifier,
    columns: Int = 4,
    cell: @Composable (Char, Modifier) -> Unit,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        for (row in letters.chunked(columns)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (c in row) cell(c, Modifier.weight(1f))
                // keep the cell width in an incomplete last row
                repeat(columns - row.size) { Box(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
fun Loading(modifier: Modifier = Modifier, text: String = "Loading commutators…") {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Text(text, modifier = Modifier.padding(top = 12.dp))
        }
    }
}

/** What to draw for a case: the pieces in colour, the labels and the arrows. */
data class CaseVisual(val pieces: Set<Cube.Vec>, val markers: List<Marker>, val arrows: List<Arrow>)

fun caseVisual(case: BldCase, scheme: LetterScheme): CaseVisual {
    val s = case.stickers
    fun letter(f: Int) = scheme.letterOf(f).toString()
    return when (case.type) {
        CaseType.CORNER, CaseType.EDGE -> CaseVisual(
            setOf(Cube.POS[case.buffer], Cube.POS[s[0]], Cube.POS[s[1]]),
            listOf(Marker(case.buffer, "", ring = true), Marker(s[0], letter(s[0])), Marker(s[1], letter(s[1]))),
            listOf(Arrow(case.buffer, s[0]), Arrow(s[0], s[1]), Arrow(s[1], case.buffer, dashed = true)),
        )
        CaseType.TWIST -> CaseVisual(
            setOf(Cube.POS[s[0]], Cube.POS[s[1]]),
            listOf(Marker(s[0], "↻"), Marker(s[1], "↺")),
            emptyList(),
        )
        CaseType.FLIP -> CaseVisual(
            setOf(Cube.POS[s[0]], Cube.POS[s[1]]),
            listOf(Marker(s[0], "⇅"), Marker(s[1], "⇅")),
            emptyList(),
        )
        CaseType.PARITY -> CaseVisual(
            setOf(Cube.POS[case.buffer], Cube.POS[s[0]], Cube.POS[s[1]], Cube.POS[s[2]]),
            listOf(Marker(case.buffer, "", ring = true), Marker(s[2], "", ring = true),
                Marker(s[0], letter(s[0])), Marker(s[1], letter(s[1]))),
            listOf(Arrow(case.buffer, s[0]), Arrow(s[0], case.buffer), Arrow(s[2], s[1]), Arrow(s[1], s[2])),
        )
    }
}

/** Positions that currently hold a sticker of one of the [homes] pieces (follows them while they move). */
fun focusOf(state: CubeState, homes: Set<Cube.Vec>): Set<Cube.Vec> =
    (0 until 54).filter { Cube.POS[state[it]] in homes }.map { Cube.POS[it] }.toSet()

@Composable
fun CaseNet(
    case: BldCase,
    vm: AppViewModel,
    modifier: Modifier = Modifier,
    state: CubeState = case.state,
    decorate: Boolean = true,
) {
    val s = vm.settings
    val visual = caseVisual(case, s.scheme)
    CubeNet(
        state = state,
        faceColours = CubeColours.faceColours(s.topColour, s.frontColour),
        modifier = modifier.fillMaxWidth(),
        focus = focusOf(state, visual.pieces),
        markers = if (decorate) visual.markers else emptyList(),
        arrows = if (decorate) visual.arrows else emptyList(),
        description = "${case.type.short} ${case.label(s.scheme)}: ${case.describe(s.scheme)}",
    )
}

/** Short explanation of a commutator class, shown above its sets. */
fun groupExplanation(group: String): String = when (group) {
    "4-movers" -> "Two single moves: [A, B] = A B A' B'. A slice move (e.g. M') and a half turn " +
        "(e.g. U2) that each touch two of the three pieces. The shortest cases."
    "Pure commutators" -> "No setup. A is an insertion (3 moves, e.g. R' D R) that brings one target " +
        "into the interchange layer; B turns that layer (the interchange) so that another target takes its place."
    "Cancelling setup (A9 type)" -> "One setup move that cancels into the commutator, so the algorithm " +
        "is shorter than setup + commutator + undo (corners: often 9 moves)."
    "1-move setup" -> "One setup move S turns the case into a pure commutator: [S: [A, B]] = S, commutator, S'."
    "2-move setup" -> "Two setup moves before a pure commutator. Learn the setup as one chunk."
    "3-move setup" -> "Three setup moves before a pure commutator."
    "With buffer" -> "One of the two pieces is the buffer."
    "Without buffer" -> "Neither piece is the buffer."
    "Parity" -> "Odd number of targets: the last corner target is solved together with the edge swap."
    else -> ""
}

fun pieceLabel(f: Int, scheme: LetterScheme): String = "${Stickers.pieceName(f)} (${scheme.letterOf(f)})"

/** Phase of every move of an algorithm, for colouring: setup, A, B, A', B', undo setup. */
enum class Phase(val label: String, val colour: Color) {
    SETUP("Setup", Color(0xFF8E8E93)),
    A("A", Color(0xFF2F80ED)),
    B("B", Color(0xFFEB5757)),
    A_INV("A'", Color(0xFF56CCF2)),
    B_INV("B'", Color(0xFFF2994A)),
    UNDO("Undo setup", Color(0xFF8E8E93)),
    MOVES("Moves", Color(0xFF27AE60)),
}

