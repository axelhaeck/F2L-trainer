package app.threestyle.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import app.threestyle.cube.Cube
import app.threestyle.cube.CubeState
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/** Display colours of the six faces for a chosen top / front colour. */
object CubeColours {
    /** Colour index (= face of the standard scheme: U white, R red, F green, D yellow, L orange, B blue). */
    val PALETTE = arrayOf(
        Color(0xFFFFFFFF), // white
        Color(0xFFD0021B), // red
        Color(0xFF00A651), // green
        Color(0xFFFFD500), // yellow
        Color(0xFFFF7A00), // orange
        Color(0xFF0051BA), // blue
    )

    private fun cross(a: Cube.Vec, b: Cube.Vec) =
        Cube.Vec(a.y * b.z - a.z * b.y, a.z * b.x - a.x * b.z, a.x * b.y - a.y * b.x)

    fun valid(top: Int, front: Int): Boolean = top in 0..5 && front in 0..5 && Cube.NORMALS[top].dot(Cube.NORMALS[front]) == 0

    /** colour index shown on each face (U R F D L B) when [top] is up and [front] in front. */
    fun faceColours(top: Int, front: Int): IntArray {
        val (t, f) = if (valid(top, front)) top to front else 0 to 2
        val up = Cube.NORMALS[t]
        val fr = Cube.NORMALS[f]
        val right = cross(up, fr)
        return IntArray(6) { face ->
            val n = Cube.NORMALS[face]
            val v = right * n.x + up * n.y + fr * n.z
            Cube.NORMALS.indexOf(v)
        }
    }
}

/** A label on a sticker. [ring] draws a strong outline (used for the buffer). */
data class Marker(val facelet: Int, val text: String, val ring: Boolean = false)

/** An arrow from one sticker to another. */
data class Arrow(val from: Int, val to: Int, val dashed: Boolean = false)

private val FACE_OFFSET = arrayOf(
    1 to 0, // U
    2 to 1, // R
    1 to 1, // F
    1 to 2, // D
    0 to 1, // L
    3 to 1, // B
)

/** Cell (column, row) of a facelet in the 12 x 9 grid of the unfolded cube. */
private fun cellOf(f: Int): Pair<Int, Int> {
    val (fx, fy) = FACE_OFFSET[f / 9]
    val r = (f % 9) / 3
    val c = f % 3
    return fx * 3 + c to fy * 3 + r
}

private fun faceletAt(col: Int, row: Int): Int? {
    for (face in 0 until 6) {
        val (fx, fy) = FACE_OFFSET[face]
        val c = col - fx * 3
        val r = row - fy * 3
        if (c in 0..2 && r in 0..2) return face * 9 + r * 3 + c
    }
    return null
}

/**
 * The cube as an unfolded net (U on top, L F R B in the middle row, D below), so every sticker is
 * visible. Pieces outside [focus] (cubie positions) are drawn faded.
 */
@Composable
fun CubeNet(
    state: CubeState,
    faceColours: IntArray,
    modifier: Modifier = Modifier,
    focus: Set<Cube.Vec>? = null,
    markers: List<Marker> = emptyList(),
    arrows: List<Arrow> = emptyList(),
    highlight: Int? = null,
    description: String = "Cube net",
    /** false: fill the whole area given by [modifier] (the net is drawn centred and as large as fits). */
    keepAspectRatio: Boolean = true,
    onTap: ((Int) -> Unit)? = null,
) {
    val measurer = rememberTextMeasurer()
    val background = MaterialTheme.colorScheme.surface
    val outline = MaterialTheme.colorScheme.onSurface
    val accent = Color(0xFFE91E63)
    var m = (if (keepAspectRatio) modifier.aspectRatio(12f / 9f) else modifier).semantics { contentDescription = description }
    if (onTap != null) {
        m = m.pointerInput(onTap) {
            detectTapGestures { pos ->
                val cell = min(size.width / 12f, size.height / 9f)
                val ox = (size.width - cell * 12f) / 2f
                val oy = (size.height - cell * 9f) / 2f
                val col = ((pos.x - ox) / cell).toInt()
                val row = ((pos.y - oy) / cell).toInt()
                if (pos.x >= ox && pos.y >= oy) faceletAt(col, row)?.let { onTap(it) }
            }
        }
    }
    val markerByFacelet = remember(markers) { markers.associateBy { it.facelet } }
    Canvas(m) {
        val cell = min(size.width / 12f, size.height / 9f)
        val ox = (size.width - cell * 12f) / 2f
        val oy = (size.height - cell * 9f) / 2f
        fun centre(f: Int): Offset {
            val (c, r) = cellOf(f)
            return Offset(ox + (c + 0.5f) * cell, oy + (r + 0.5f) * cell)
        }
        val inset = cell * 0.06f
        for (f in 0 until 54) {
            val (c, r) = cellOf(f)
            val base = CubeColours.PALETTE[faceColours[state[f] / 9]]
            val faded = focus != null && f % 9 != 4 && Cube.POS[f] !in focus
            val col = if (faded) lerp(base, background, 0.6f) else base
            drawRoundRect(
                color = col,
                topLeft = Offset(ox + c * cell + inset, oy + r * cell + inset),
                size = Size(cell - 2 * inset, cell - 2 * inset),
                cornerRadius = CornerRadius(cell * 0.12f),
            )
            drawRoundRect(
                color = outline.copy(alpha = if (faded) 0.35f else 0.5f),
                topLeft = Offset(ox + c * cell + inset, oy + r * cell + inset),
                size = Size(cell - 2 * inset, cell - 2 * inset),
                cornerRadius = CornerRadius(cell * 0.12f),
                style = Stroke(width = cell * 0.025f),
            )
        }
        for (a in arrows) drawArrow(centre(a.from), centre(a.to), cell, accent, a.dashed)
        if (highlight != null) {
            val (c, r) = cellOf(highlight)
            drawRoundRect(
                color = accent,
                topLeft = Offset(ox + c * cell + inset * 0.5f, oy + r * cell + inset * 0.5f),
                size = Size(cell - inset, cell - inset),
                cornerRadius = CornerRadius(cell * 0.14f),
                style = Stroke(width = cell * 0.12f),
            )
            drawCircle(Color.Black, radius = cell * 0.16f, center = centre(highlight))
            drawCircle(accent, radius = cell * 0.11f, center = centre(highlight))
        }
        for ((f, mk) in markerByFacelet) {
            val p = centre(f)
            if (mk.ring) {
                drawCircle(Color.Black, radius = cell * 0.40f, center = p, style = Stroke(width = cell * 0.10f))
                drawCircle(Color.White, radius = cell * 0.40f, center = p, style = Stroke(width = cell * 0.04f))
            }
            if (mk.text.isNotEmpty()) {
                val bg = CubeColours.PALETTE[faceColours[state[f] / 9]]
                val fg = if (bg.luminance() > 0.4f) Color.Black else Color.White
                val style = TextStyle(color = fg, fontSize = (cell * 0.42f / density).sp, fontWeight = FontWeight.Bold)
                val layout = measurer.measure(mk.text, style)
                drawText(layout, topLeft = Offset(p.x - layout.size.width / 2f, p.y - layout.size.height / 2f))
            }
        }
    }
}

private fun DrawScope.drawArrow(from: Offset, to: Offset, cell: Float, colour: Color, dashed: Boolean) {
    val dx = to.x - from.x
    val dy = to.y - from.y
    val len = sqrt(dx * dx + dy * dy)
    if (len < 1f) return
    val ux = dx / len
    val uy = dy / len
    // slight curve so that arrows in both directions between two stickers do not overlap
    val bend = min(cell * 0.35f, len * 0.25f)
    val mid = Offset((from.x + to.x) / 2f - uy * bend, (from.y + to.y) / 2f + ux * bend)
    val gap = min(cell * 0.42f, len * 0.3f)
    val start = Offset(from.x + ux * gap, from.y + uy * gap)
    // the arrow head points along the tangent at the end of the curve
    val tx = to.x - mid.x
    val ty = to.y - mid.y
    val tl = sqrt(tx * tx + ty * ty)
    val end = Offset(to.x - tx / tl * gap, to.y - ty / tl * gap)
    val path = Path().apply {
        moveTo(start.x, start.y)
        quadraticTo(mid.x, mid.y, end.x, end.y)
    }
    val effect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(cell * 0.2f, cell * 0.14f)) else null
    drawPath(path, Color.Black, style = Stroke(width = cell * 0.16f, cap = StrokeCap.Round, pathEffect = effect))
    drawPath(path, colour, style = Stroke(width = cell * 0.09f, cap = StrokeCap.Round, pathEffect = effect))
    val angle = atan2(ty, tx)
    val head = cell * 0.30f
    val wing = 0.5f
    val p1 = Offset(end.x - head * cos(angle - wing), end.y - head * sin(angle - wing))
    val p2 = Offset(end.x - head * cos(angle + wing), end.y - head * sin(angle + wing))
    val tip = Offset(end.x + cos(angle) * cell * 0.08f, end.y + sin(angle) * cell * 0.08f)
    val headPath = Path().apply { moveTo(tip.x, tip.y); lineTo(p1.x, p1.y); lineTo(p2.x, p2.y); close() }
    drawPath(headPath, Color.Black, style = Stroke(width = cell * 0.08f))
    drawPath(headPath, colour)
}
