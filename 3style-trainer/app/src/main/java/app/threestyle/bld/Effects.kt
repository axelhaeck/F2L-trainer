package app.threestyle.bld

import app.threestyle.cube.CubeState
import app.threestyle.cube.Perm

/**
 * Constructs, directly from the sticker geometry, the facelet permutation that an algorithm for a
 * case must perform. Algorithms are checked against these, so the check does not depend on the
 * search that found them.
 */
object Effects {
    /** The piece of [from] moves rigidly to the place of [to], with sticker [from] landing on [to]. */
    private fun rigid(from: Int, to: Int, p: IntArray) {
        val a = Stickers.cycleFrom(from)
        val b = Stickers.cycleFrom(to)
        require(a.size == b.size) { "different piece types" }
        for (k in a.indices) p[a[k]] = b[k]
    }

    /**
     * 3-cycle for the letter pair (x, y) with buffer sticker b: the sticker at the buffer goes to
     * x, the sticker at x goes to y, the sticker at y goes to the buffer.
     */
    fun cycle(b: Int, x: Int, y: Int): IntArray {
        require(!Stickers.samePiece(b, x) && !Stickers.samePiece(b, y) && !Stickers.samePiece(x, y))
        val p = Perm.IDENTITY.copyOf()
        rigid(b, x, p); rigid(x, y, p); rigid(y, b, p)
        return p
    }

    /** Swap of two pieces: the sticker at a goes to b and the sticker at b goes to a. */
    fun swap(a: Int, b: Int, into: IntArray = Perm.IDENTITY.copyOf()): IntArray {
        require(!Stickers.samePiece(a, b))
        rigid(a, b, into); rigid(b, a, into)
        return into
    }

    /** Parity: swap corner buffer with corner target x and edge buffer with the parity edge sticker. */
    fun parity(cornerBuffer: Int, x: Int, edgeBuffer: Int, edgeTarget: Int): IntArray =
        swap(edgeBuffer, edgeTarget, swap(cornerBuffer, x))

    /** Rotates the stickers of [f]'s piece: +1 = clockwise twist (corners), any odd value flips an edge. */
    private fun turnPiece(f: Int, steps: Int, p: IntArray) {
        val c = Stickers.cycleFrom(f)
        val n = c.size
        for (k in 0 until n) p[c[k]] = c[((k + steps) % n + n) % n]
    }

    /**
     * Twist case "P clockwise, Q counter-clockwise": the algorithm turns P counter-clockwise and Q
     * clockwise (it undoes the twists).
     */
    fun twistFix(p: Int, q: Int): IntArray {
        require(!Stickers.samePiece(p, q))
        val r = Perm.IDENTITY.copyOf()
        turnPiece(p, -1, r); turnPiece(q, 1, r)
        return r
    }

    /** Flip of two edges. */
    fun flip(p: Int, q: Int): IntArray {
        require(!Stickers.samePiece(p, q))
        val r = Perm.IDENTITY.copyOf()
        turnPiece(p, 1, r); turnPiece(q, 1, r)
        return r
    }

    /** The cube before the algorithm: applying an algorithm with this [effect] solves it. */
    fun caseState(effect: IntArray): CubeState = CubeState.SOLVED.applyPerm(Perm.inverse(effect))

    /** Does [alg] perform exactly [effect] (whole-cube rotations at the end are allowed)? */
    fun solves(effect: IntArray, alg: String): Boolean = try {
        caseState(effect).apply(alg).isSolvedUpToRotation()
    } catch (_: IllegalArgumentException) {
        false
    }
}
