package app.threestyle.bld

import app.threestyle.cube.Cube

enum class PieceType { CORNER, EDGE }

/**
 * Geometry of the stickers (facelets) of corner and edge pieces.
 *
 * A sticker is a facelet index 0..53 of [Cube]. Every piece has a *reference sticker*: for corners
 * the U/D sticker, for edges the U/D sticker or, for E-slice edges, the F/B sticker. The reference
 * sticker of a buffer piece is the buffer sticker.
 */
object Stickers {
    fun typeOf(f: Int): PieceType? = when (nonZero(f)) {
        3 -> PieceType.CORNER
        2 -> PieceType.EDGE
        else -> null
    }

    private fun nonZero(f: Int): Int {
        val p = Cube.POS[f]
        return (if (p.x != 0) 1 else 0) + (if (p.y != 0) 1 else 0) + (if (p.z != 0) 1 else 0)
    }

    /**
     * Stickers in Speffz order A..X: faces U L F R B D, on each face clockwise from the top-left
     * (corners) or from the top (edges), with the usual unfolding (U with B on top, D with F on top,
     * side faces with U on top).
     */
    val SPEFFZ_CORNERS: IntArray = speffz(intArrayOf(0, 0, 0, 2, 2, 2, 2, 0))
    val SPEFFZ_EDGES: IntArray = speffz(intArrayOf(0, 1, 1, 2, 2, 1, 1, 0))

    private fun speffz(rc: IntArray): IntArray {
        val faces = intArrayOf(Cube.U, Cube.L, Cube.F, Cube.R, Cube.B, Cube.D)
        val out = IntArray(24)
        var k = 0
        for (f in faces) for (j in 0 until 4) out[k++] = f * 9 + rc[2 * j] * 3 + rc[2 * j + 1]
        return out
    }

    fun speffzOrder(type: PieceType): IntArray = if (type == PieceType.CORNER) SPEFFZ_CORNERS else SPEFFZ_EDGES

    /** Stickers of the piece that [f] belongs to. */
    fun pieceStickers(f: Int): List<Int> = Cube.faceletsOf(Cube.POS[f])

    fun samePiece(a: Int, b: Int): Boolean = Cube.POS[a] == Cube.POS[b]

    /** Name of the piece, e.g. "UFR", "UF", "FR" (faces in the order U D F B R L). */
    fun pieceName(f: Int): String = Cube.cubieName(Cube.POS[f])

    /** Name of a sticker: its face first, then the other faces of its piece, e.g. "FUR", "RU". */
    fun stickerName(f: Int): String {
        val face = Cube.FACE_NAMES[f / 9]
        return face + pieceName(f).filter { it != face }
    }

    private val byName: Map<String, Int> = (0 until 54).filter { typeOf(it) != null }.associateBy { stickerName(it) }

    fun byName(name: String): Int = byName[name] ?: throw IllegalArgumentException("Unknown sticker $name")

    fun byNameOrNull(name: String): Int? = byName[name]

    /** The reference sticker of the piece [f] belongs to. */
    fun reference(f: Int): Int {
        val first = pieceName(f)[0]
        return pieceStickers(f).first { Cube.FACE_NAMES[it / 9] == first }
    }

    fun isReference(f: Int) = reference(f) == f

    /** Reference stickers of all pieces of a type, in Speffz order of the reference stickers. */
    fun pieces(type: PieceType): List<Int> = speffzOrder(type).filter { isReference(it) }

    /**
     * The stickers of [f]'s piece starting with [f]. Corners are listed clockwise (as seen from
     * outside, looking at the corner). Moving every sticker of a piece to the next entry is a
     * clockwise twist.
     */
    fun cycleFrom(f: Int): IntArray {
        val stickers = pieceStickers(f)
        if (stickers.size == 2) return intArrayOf(f, stickers.first { it != f })
        val p = Cube.POS[f]
        val out = IntArray(3)
        out[0] = f
        for (k in 1 until 3) {
            val prev = Cube.NORMAL[out[k - 1]]
            out[k] = stickers.first { g ->
                g != out[k - 1] && cross(prev, Cube.NORMAL[g]).dot(p) < 0
            }
        }
        return out
    }

    private fun cross(a: Cube.Vec, b: Cube.Vec) =
        Cube.Vec(a.y * b.z - a.z * b.y, a.z * b.x - a.x * b.z, a.x * b.y - a.y * b.x)
}

/**
 * A letter scheme: one character per sticker for corners and edges, in Speffz sticker order.
 * The default is Speffz itself (A..X for both).
 */
data class LetterScheme(val corners: String = SPEFFZ, val edges: String = SPEFFZ) {
    init {
        require(isValid(corners) && isValid(edges)) { "A scheme needs 24 different letters" }
    }

    private fun letters(type: PieceType) = if (type == PieceType.CORNER) corners else edges

    fun letterOf(sticker: Int): Char {
        val type = Stickers.typeOf(sticker) ?: throw IllegalArgumentException("not a corner/edge sticker")
        val idx = Stickers.speffzOrder(type).indexOf(sticker)
        return letters(type)[idx]
    }

    /** The sticker with letter [c] (case insensitive), or null. */
    fun stickerOf(type: PieceType, c: Char): Int? {
        val idx = letters(type).indexOfFirst { it.equals(c, ignoreCase = true) }
        return if (idx < 0) null else Stickers.speffzOrder(type)[idx]
    }

    fun alphabet(type: PieceType): String = letters(type)

    companion object {
        const val SPEFFZ = "ABCDEFGHIJKLMNOPQRSTUVWX"

        fun isValid(s: String): Boolean =
            s.length == 24 && s.none { it.isWhitespace() } && s.uppercase().toSet().size == 24
    }
}
