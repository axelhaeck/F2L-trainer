package app.f2ltrainer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.f2ltrainer.cube.Alg
import app.f2ltrainer.data.Status

@Composable
fun AlgText(alg: String, modifier: Modifier = Modifier, size: TextUnit = 18.sp, bold: Boolean = false) {
    Text(
        alg,
        modifier = modifier,
        fontFamily = FontFamily.Monospace,
        fontSize = size,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
    )
}

fun moveCountLabel(alg: String): String {
    val n = Alg.moveCount(Alg.parse(alg))
    return "$n move" + if (n == 1) "" else "s"
}

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
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { content() }
}

fun formatMs(ms: Long?): String = if (ms == null) "–" else String.format(java.util.Locale.US, "%.2fs", ms / 1000.0)

val PhaseAdjust = Color(0xFF8E8E93)
val PhasePair = Color(0xFF2F80ED)
val PhaseInsert = Color(0xFF27AE60)
