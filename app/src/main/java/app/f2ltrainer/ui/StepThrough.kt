package app.f2ltrainer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
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
import app.f2ltrainer.cube.Alg
import app.f2ltrainer.cube.CubeState
import app.f2ltrainer.cube.F2l
import app.f2ltrainer.data.Teaching
import kotlinx.coroutines.delay

/** Holds the playback position of an algorithm on a start state. */
class StepperState(val start: CubeState, val alg: String) {
    val tokens: List<String> = Alg.parse(alg)
    val states: List<CubeState> = buildList {
        var s = start
        add(s)
        for (t in tokens) { s = s.applyMove(t); add(s) }
    }
    val phases: List<Teaching.Phase> = Teaching.phases(start, tokens)
    var index by mutableIntStateOf(0)
    var playing by mutableStateOf(false)
    val state: CubeState get() = states[index]

    fun phaseOf(tokenIndex: Int): Teaching.Kind? = phases.firstOrNull { tokenIndex >= it.from && tokenIndex < it.to }?.kind
}

@Composable
fun rememberStepper(start: CubeState, alg: String) = remember(start, alg) { StepperState(start, alg) }

fun phaseColour(kind: Teaching.Kind?): Color = when (kind) {
    Teaching.Kind.ADJUST -> PhaseAdjust
    Teaching.Kind.PAIR -> PhasePair
    Teaching.Kind.INSERT -> PhaseInsert
    null -> Color.Gray
}

/** Move chips + playback controls. The cube picture itself is drawn by the caller from [StepperState.state]. */
@Composable
fun StepperControls(st: StepperState, modifier: Modifier = Modifier) {
    LaunchedEffect(st, st.playing) {
        if (st.playing) {
            if (st.index >= st.tokens.size) st.index = 0
            while (st.playing && st.index < st.tokens.size) {
                delay(750)
                st.index++
            }
            st.playing = false
        }
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            for ((i, t) in st.tokens.withIndex()) {
                val col = phaseColour(st.phaseOf(i))
                val done = i < st.index
                val next = i == st.index
                Box(
                    Modifier
                        .background(if (done) col else col.copy(alpha = 0.18f), RoundedCornerShape(6.dp))
                        .then(if (next) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(6.dp)) else Modifier)
                        .clickable { st.playing = false; st.index = i + 1 }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(
                        t,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = if (done) Color.White else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = { st.playing = false; st.index = 0 }) { Text("⏮") }
            OutlinedButton(onClick = { st.playing = false; if (st.index > 0) st.index-- }) { Text("◀") }
            FilledTonalButton(onClick = { st.playing = !st.playing }) { Text(if (st.playing) "Pause" else "Play") }
            OutlinedButton(onClick = { st.playing = false; if (st.index < st.tokens.size) st.index++ }) { Text("▶") }
        }
        Text(
            "Move ${st.index} of ${st.tokens.size}" + when {
                st.index == st.tokens.size && F2l.isSolved(st.state) -> " · solved"
                st.index == st.tokens.size -> " · pair solved"
                else -> ""
            },
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
fun PhaseLegend() {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        for (k in Teaching.Kind.entries) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(12.dp).background(phaseColour(k), RoundedCornerShape(3.dp)))
                Text(" " + k.label, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

