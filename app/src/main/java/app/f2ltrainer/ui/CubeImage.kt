package app.f2ltrainer.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import app.f2ltrainer.cube.Cube
import app.f2ltrainer.cube.CubeState
import app.f2ltrainer.cube.F2l
import kotlin.math.min
import kotlin.math.sqrt

object CubeColours {
    // Index = face/colour: U R F D L B  (yellow top, white bottom, green front, orange right)
    val FACE = arrayOf(
        Color(0xFFFFD500), // U yellow
        Color(0xFFFF7A00), // R orange
        Color(0xFF00A651), // F green
        Color(0xFFFFFFFF), // D white
        Color(0xFFD0021B), // L red
        Color(0xFF0051BA), // B blue
    )
    val HIDDEN = Color(0xFF6A6E77)
    val EMPTY = Color(0xFF2E3036)
    val BODY = Color(0xFF15171C)
}

private val F2L_HOME_CUBIES: Set<Cube.Vec> = (F2l.F2L_CUBIES + listOf(
    Cube.NORMALS[Cube.R], Cube.NORMALS[Cube.F], Cube.NORMALS[Cube.D], Cube.NORMALS[Cube.L], Cube.NORMALS[Cube.B],
)).toSet()

/**
 * Isometric picture of the cube. Only first-two-layer pieces are coloured (wherever they
 * currently are); last-layer pieces are grey. Pieces of [emptySlots] are drawn dark ("free").
 * [back] shows the cube from behind (U, B and L faces) instead of the front (U, F and R).
 */
@Composable
fun CubeImage(
    state: CubeState,
    modifier: Modifier = Modifier,
    back: Boolean = false,
    emptySlots: Set<F2l.Slot> = emptySet(),
    description: String = "Cube picture",
) {
    val emptyCubies = remember(emptySlots) {
        emptySlots.flatMap { listOf(Cube.cubie(it.corner), Cube.cubie(it.edge)) }.toSet()
    }
    Canvas(modifier.aspectRatio(5.6f / 6.4f).semantics { contentDescription = description }) {
        val c30 = sqrt(3f) / 2f
        // projected cube spans 6*cos30 = 5.2 units horizontally and 6 units vertically
        val scale = min(size.width / 5.6f, size.height / 6.4f)
        val cx = size.width / 2f
        val cy = size.height / 2f

        fun project(x: Float, y: Float, z: Float): Offset {
            val (px, pz) = if (back) -x to -z else x to z
            return Offset(cx + (px - pz) * c30 * scale, cy + (-y + (px + pz) * 0.5f) * scale)
        }

        val faces = if (back) intArrayOf(Cube.U, Cube.B, Cube.L) else intArrayOf(Cube.U, Cube.F, Cube.R)
        // light shading for depth; kept small so white stays clearly white
        val shade = floatArrayOf(1f, 0.93f, 0.86f)
        for ((k, f) in faces.withIndex()) {
            val n = Cube.NORMALS[f]
            val (t1, t2) = tangents(n)
            // face background
            val fc = Triple(n.x * 1.5f, n.y * 1.5f, n.z * 1.5f)
            drawPath(quad(fc, t1, t2, 1.52f, ::project), CubeColours.BODY)
            for (i in 0 until 9) {
                val facelet = f * 9 + i
                val p = Cube.POS[facelet]
                val centre = Triple(p.x + n.x * 0.5f, p.y + n.y * 0.5f, p.z + n.z * 0.5f)
                val sticker = state[facelet]
                val home = Cube.POS[sticker]
                val base = when {
                    home in emptyCubies -> CubeColours.EMPTY
                    home in F2L_HOME_CUBIES -> CubeColours.FACE[sticker / 9]
                    else -> CubeColours.HIDDEN
                }
                val col = Color(base.red * shade[k], base.green * shade[k], base.blue * shade[k], 1f)
                drawPath(quad(centre, t1, t2, 0.44f, ::project), col)
            }
        }
    }
}

private fun tangents(n: Cube.Vec): Pair<Triple<Float, Float, Float>, Triple<Float, Float, Float>> = when {
    n.x != 0 -> Triple(0f, 1f, 0f) to Triple(0f, 0f, 1f)
    n.y != 0 -> Triple(1f, 0f, 0f) to Triple(0f, 0f, 1f)
    else -> Triple(1f, 0f, 0f) to Triple(0f, 1f, 0f)
}

private fun quad(
    c: Triple<Float, Float, Float>,
    t1: Triple<Float, Float, Float>,
    t2: Triple<Float, Float, Float>,
    h: Float,
    project: (Float, Float, Float) -> Offset,
): Path {
    val path = Path()
    val signs = listOf(1f to 1f, 1f to -1f, -1f to -1f, -1f to 1f)
    for ((idx, s) in signs.withIndex()) {
        val x = c.first + (t1.first * s.first + t2.first * s.second) * h
        val y = c.second + (t1.second * s.first + t2.second * s.second) * h
        val z = c.third + (t1.third * s.first + t2.third * s.second) * h
        val o = project(x, y, z)
        if (idx == 0) path.moveTo(o.x, o.y) else path.lineTo(o.x, o.y)
    }
    path.close()
    return path
}
