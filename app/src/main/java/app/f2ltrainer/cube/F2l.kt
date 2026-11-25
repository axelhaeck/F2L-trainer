package app.f2ltrainer.cube

import app.f2ltrainer.cube.Cube.D
import app.f2ltrainer.cube.Cube.F
import app.f2ltrainer.cube.Cube.R

/** F2L specific analysis. All checks use colours relative to the centres (rotation safe). */
object F2l {
    enum class Slot(val corner: String, val edge: String) {
        FR("DFR", "FR"), FL("DFL", "FL"), BR("DBR", "BR"), BL("DBL", "BL")
    }

    val CROSS = listOf("DF", "DR", "DB", "DL")

    /** The 13 cubies (excluding centres) that make up the first two layers. */
    val F2L_CUBIES: List<Cube.Vec> =
        (CROSS + Slot.entries.flatMap { listOf(it.corner, it.edge) }).map { Cube.cubie(it) }

    private fun cubieSolved(rel: IntArray, name: String): Boolean =
        Cube.faceletsOf(Cube.cubie(name)).all { rel[it] == it / 9 }

    /** Is the cross plus every slot except [free] solved? */
    fun isSolved(state: CubeState, free: Set<Slot> = emptySet()): Boolean {
        val rel = state.relativeColours()
        if (!CROSS.all { cubieSolved(rel, it) }) return false
        return Slot.entries.filter { it !in free }.all { cubieSolved(rel, it.corner) && cubieSolved(rel, it.edge) }
    }

    fun isSlotSolved(state: CubeState, slot: Slot): Boolean {
        val rel = state.relativeColours()
        return cubieSolved(rel, slot.corner) && cubieSolved(rel, slot.edge)
    }

    /** Is everything that is not part of the FR pair (cross + other three slots) solved? */
    fun isFrCase(state: CubeState, free: Set<Slot> = emptySet()): Boolean {
        val rel = state.relativeColours()
        if (!CROSS.all { cubieSolved(rel, it) }) return false
        return Slot.entries.filter { it != Slot.FR && it !in free }
            .all { cubieSolved(rel, it.corner) && cubieSolved(rel, it.edge) }
    }

    private val CUBIE_POSITIONS: List<Cube.Vec> = (0 until 54).map { Cube.POS[it] }.distinct()

    private fun findPiece(rel: IntArray, colours: Set<Int>): List<Int> {
        for (p in CUBIE_POSITIONS) {
            val fs = Cube.faceletsOf(p)
            if (fs.size == colours.size && fs.map { rel[it] }.toSet() == colours) return fs
        }
        error("piece $colours not found")
    }

    /**
     * Position of the FR pair: (facelet holding the corner's cross-colour sticker,
     * facelet holding the edge's front-colour sticker). Together they fix both pieces' position
     * and orientation.
     */
    fun pairKey(state: CubeState): Pair<Int, Int> {
        val rel = state.relativeColours()
        val c = findPiece(rel, setOf(D, F, R)).first { rel[it] == D }
        val e = findPiece(rel, setOf(F, R)).first { rel[it] == F }
        return c to e
    }

    /** True when the FR corner and FR edge form a correctly joined 1x1x2 block anywhere. */
    fun isPairConnected(state: CubeState): Boolean {
        val rel = state.relativeColours()
        val c = findPiece(rel, setOf(D, F, R))
        val e = findPiece(rel, setOf(F, R))
        fun normalOf(piece: List<Int>, colour: Int) = Cube.NORMAL[piece.first { rel[it] == colour }]
        return normalOf(c, F) == normalOf(e, F) && normalOf(c, R) == normalOf(e, R)
    }
}
