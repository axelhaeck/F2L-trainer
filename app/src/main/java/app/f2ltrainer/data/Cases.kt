package app.f2ltrainer.data

import app.f2ltrainer.cube.Alg
import app.f2ltrainer.cube.Cube
import app.f2ltrainer.cube.CubeState
import app.f2ltrainer.cube.F2l

enum class CaseGroup(val title: String, val description: String) {
    BASIC(
        "Basic inserts",
        "The corner and edge are placed so that one short trigger pairs and inserts them in one go. " +
            "Every other case is solved by turning it into one of these patterns, so learn to spot them instantly.",
    ),
    SPLIT(
        "Corner on top, cross colour to the side",
        "Both pieces are in the top layer and the corner's cross colour points to the side. The pieces are " +
            "either apart or joined the wrong way. A setup trigger creates a connected pair or a basic insert, then the pair goes in.",
    ),
    CROSSUP(
        "Corner on top, cross colour up",
        "Both pieces are in the top layer and the corner's cross colour faces up. The corner has to be turned so " +
            "that the cross colour points to the side, so these cases take a few more moves.",
    ),
    EDGESLOT(
        "Edge in slot, corner on top",
        "The edge is already in the slot (correct or flipped) and the corner is in the top layer. The edge is " +
            "brought out together with the corner, or the corner is placed so that one insert solves both.",
    ),
    CORNERSLOT(
        "Corner in slot, edge on top",
        "The corner is in the slot (solved or twisted) and the edge is in the top layer. Usually the corner is " +
            "taken out while the edge is lined up, then the pair is inserted.",
    ),
    BOTHSLOT(
        "Corner and edge in slot",
        "Both pieces are in the slot but not solved. They are taken out (together or separately) and re-inserted. " +
            "These are the rarest cases; learn them last.",
    ),
}

internal data class RawCase(val id: Int, val group: CaseGroup, val algs: List<String>, val freeBr: String?)

/** One of the 41 F2L cases for the front-right slot. */
class F2lCase internal constructor(raw: RawCase) {
    val id: Int = raw.id
    val group: CaseGroup = raw.group
    /** Built-in algorithms; the first one is the recommended main algorithm. */
    val algorithms: List<String> = raw.algs
    /** Shorter algorithm that may destroy the back-right slot (only useful while BR is unsolved). */
    val freeSlotAlgorithm: String? = raw.freeBr

    val name: String get() = "F2L $id"

    /**
     * The case as shown in the pictures: cross and the three other slots solved, FR pair placed,
     * centres in standard orientation. Built from the main algorithm with rotations removed.
     */
    val displayState: CubeState by lazy { stateSolvedBy(algorithms[0]) }

    /** Human readable description of where the two pieces are, derived from [displayState]. */
    val recognition: List<String> by lazy { describe(displayState) }

    companion object {
        /** The state that [alg] solves (inverse of its face moves applied to a solved cube). */
        fun stateSolvedBy(alg: String): CubeState {
            val (faces, _) = Alg.toFaceMoves(Alg.parse(alg))
            return CubeState.SOLVED.apply(Alg.invert(faces))
        }

        private val COLOUR_NAMES = arrayOf("yellow", "orange", "green", "white", "red", "blue")

        private fun posName(p: Cube.Vec): String = when (Cube.cubieName(p)) {
            "UF" -> "top layer, front"
            "UR" -> "top layer, right"
            "UB" -> "top layer, back"
            "UL" -> "top layer, left"
            "UFR" -> "top layer, front-right"
            "UFL" -> "top layer, front-left"
            "UBR" -> "top layer, back-right"
            "UBL" -> "top layer, back-left"
            "FR", "DFR" -> "in the slot"
            else -> Cube.cubieName(p)
        }

        private fun dirName(n: Cube.Vec): String = when (n) {
            Cube.NORMALS[Cube.U] -> "up"
            Cube.NORMALS[Cube.D] -> "down"
            Cube.NORMALS[Cube.F] -> "to the front"
            Cube.NORMALS[Cube.B] -> "to the back"
            Cube.NORMALS[Cube.R] -> "to the right"
            else -> "to the left"
        }

        internal fun describe(s: CubeState): List<String> {
            val (cf, ef) = F2l.pairKey(s)
            val cornerPos = Cube.POS[cf]
            val edgePos = Cube.POS[ef]
            val cross = COLOUR_NAMES[Cube.D]
            val front = COLOUR_NAMES[Cube.F]
            val right = COLOUR_NAMES[Cube.R]
            val corner = if (Cube.cubieName(cornerPos) == "DFR") {
                if (cf / 9 == Cube.D) "Corner: in the slot, solved"
                else "Corner: in the slot, twisted ($cross faces ${dirName(Cube.NORMAL[cf])})"
            } else {
                "Corner: ${posName(cornerPos)}, $cross faces ${dirName(Cube.NORMAL[cf])}"
            }
            val edge = if (Cube.cubieName(edgePos) == "FR") {
                if (ef / 9 == Cube.F) "Edge: in the slot, correctly oriented" else "Edge: in the slot, flipped"
            } else {
                val top = if (ef / 9 == Cube.U) front else right
                "Edge: ${posName(edgePos)}, $top on top"
            }
            return listOf(corner, edge, "Target slot: front-right ($front/$right)")
        }
    }
}

object Cases {
    val ALL: List<F2lCase> = CaseData.CASES.map { F2lCase(it) }
    private val byId = ALL.associateBy { it.id }
    fun byId(id: Int): F2lCase = byId.getValue(id)
}
