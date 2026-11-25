package app.threestyle.bld

import app.threestyle.cube.Alg
import app.threestyle.cube.CubeState
import kotlin.random.Random

/** Random-move scrambles (outer face turns, no redundant sequences such as "R L R'"). */
object Scrambler {
    fun random(random: Random, length: Int = 21): String {
        val faces = "UDFBRL"
        val out = ArrayList<String>()
        while (out.size < length) {
            val f = faces[random.nextInt(6)]
            if (out.isNotEmpty()) {
                val last = out.last()[0]
                if (last == f) continue
                if (Alg.axis(last) == Alg.axis(f)) {
                    // opposite faces commute: allow only one order, and never three turns on one axis
                    if (last > f) continue
                    if (out.size >= 2 && Alg.axis(out[out.size - 2][0]) == Alg.axis(f)) continue
                }
            }
            out.add(Alg.token(f, random.nextInt(1, 4))!!)
        }
        return Alg.format(out)
    }
}

/** A piece that is in place but turned: [steps] = 1 clockwise, 2 counter-clockwise (corners); 1 = flipped (edges). */
data class Misoriented(val piece: Int, val steps: Int)

data class Trace(val targets: List<Int>, val misoriented: List<Misoriented>) {
    val parity: Boolean get() = targets.size % 2 == 1
}

/** Result of checking a memo typed by the user. */
sealed interface MemoCheck {
    /** Valid memo; [misoriented] = pieces still twisted/flipped afterwards. */
    data class Ok(val targets: List<Int>, val misoriented: List<Misoriented>) : MemoCheck {
        val parity get() = targets.size % 2 == 1
    }

    /** The letter at [index] is wrong; [expected] is the only correct sticker, or null if any cycle break was allowed. */
    data class Wrong(val index: Int, val expected: Int?) : MemoCheck

    /** All letters were right but the memo stops too early; [next] is the next correct target (null: cycle break). */
    data class Incomplete(val next: Int?) : MemoCheck

    data class BadLetter(val index: Int, val letter: Char, val reason: String) : MemoCheck
}

/**
 * Tracing of a scrambled cube with the usual BLD rules: read the sticker on the buffer, shoot it
 * home; when the buffer piece itself comes home, break into a new cycle with the first unsolved
 * piece (in letter order). Pieces that are only twisted / flipped are reported separately.
 *
 * A target t means: the sticker on the buffer is moved to t (and the piece from t to the buffer).
 */
object Tracer {
    private fun swapApply(st: IntArray, b: Int, t: Int): IntArray {
        val p = Effects.swap(b, t)
        val out = IntArray(54)
        for (i in 0 until 54) out[p[i]] = st[i]
        return out
    }

    private fun inPlace(st: IntArray, f: Int) = Stickers.pieceStickers(f).all { Stickers.samePiece(st[it], it) }
    private fun solved(st: IntArray, f: Int) = Stickers.pieceStickers(f).all { st[it] == it }

    /** The default cycle break: the first sticker (letter order) of an unsolved piece that is not in place. */
    private fun cycleBreak(st: IntArray, type: PieceType, b: Int): Int? =
        Stickers.speffzOrder(type).firstOrNull { !Stickers.samePiece(it, b) && !inPlace(st, it) }

    private fun misoriented(st: IntArray, type: PieceType): List<Misoriented> =
        Stickers.pieces(type).filter { inPlace(st, it) && !solved(st, it) }.map { ref ->
            val c = Stickers.cycleFrom(ref)
            // clockwise twist: sticker k moved to k+1, so the reference position shows sticker c[n-1]
            Misoriented(ref, if (c.size == 2) 1 else if (st[ref] == c[2]) 1 else 2)
        }

    fun trace(state: CubeState, type: PieceType, buffer: Int): Trace {
        var st = state.stickers()
        val targets = ArrayList<Int>()
        repeat(64) {
            val h = st[buffer]
            val t = if (!Stickers.samePiece(h, buffer)) h else cycleBreak(st, type, buffer) ?: return Trace(targets, misoriented(st, type))
            targets.add(t)
            st = swapApply(st, buffer, t)
        }
        error("tracing did not finish")
    }

    /** Checks the user's targets step by step. */
    fun check(state: CubeState, type: PieceType, buffer: Int, targets: List<Int>): MemoCheck {
        var st = state.stickers()
        for ((i, t) in targets.withIndex()) {
            val h = st[buffer]
            if (!Stickers.samePiece(h, buffer)) {
                if (t != h) return MemoCheck.Wrong(i, h)
            } else {
                // cycle break: any sticker of a piece that is not solved (twisted pieces included)
                if (Stickers.samePiece(t, buffer) || solved(st, t)) return MemoCheck.Wrong(i, null)
            }
            st = swapApply(st, buffer, t)
        }
        val h = st[buffer]
        if (!Stickers.samePiece(h, buffer)) return MemoCheck.Incomplete(h)
        if (cycleBreak(st, type, buffer) != null) return MemoCheck.Incomplete(null)
        return MemoCheck.Ok(targets, misoriented(st, type))
    }

    /**
     * Parses letters (spaces and punctuation ignored) into stickers with [scheme].
     * @return the stickers, or a [MemoCheck.BadLetter]
     */
    fun parse(text: String, type: PieceType, buffer: Int, scheme: LetterScheme): Pair<List<Int>?, MemoCheck.BadLetter?> {
        val out = ArrayList<Int>()
        val letters = text.filter { !it.isWhitespace() && it !in ",.;-/|" }
        for ((i, c) in letters.withIndex()) {
            val s = scheme.stickerOf(type, c) ?: return null to MemoCheck.BadLetter(i, c, "not a letter of the scheme")
            if (Stickers.samePiece(s, buffer)) return null to MemoCheck.BadLetter(i, c, "is on the buffer piece")
            out.add(s)
        }
        return out to null
    }
}
