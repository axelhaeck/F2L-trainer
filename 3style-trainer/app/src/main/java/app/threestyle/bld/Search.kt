package app.threestyle.bld

import app.threestyle.cube.Alg
import app.threestyle.cube.Cube
import app.threestyle.cube.Perm

/**
 * One algorithm for a case.
 *
 * For commutators [setup] is S, [partA]/[partB] are A and B of `[S: [A, B]]`; for algorithms built
 * from a fixed building block ([primitive]) the notation is `[S: primitive]`.
 */
data class Solution(
    val notation: String,
    val moves: List<String>,
    val score: Double,
    val setup: List<String>,
    val partA: List<String> = emptyList(),
    val partB: List<String> = emptyList(),
    val primitive: String? = null,
) {
    val moveCount: Int get() = Alg.moveCount(moves)

    /** The setup merges with or cancels into the commutator (fewer moves than written). */
    val cancels: Boolean
        get() {
            val written = if (primitive != null) Alg.parse(primitive).size else 2 * (partA.size + partB.size)
            return moves.size < 2 * setup.size + written
        }

    /** The single-move part of a commutator (the "interchange"), if there is exactly one or both. */
    val interchange: List<String>?
        get() = when {
            primitive != null -> null
            partA.size == 1 && partB.size == 1 -> if (Alg.isSlice(partA[0]) && !Alg.isSlice(partB[0])) partB else partA
            partB.size == 1 -> partB
            partA.size == 1 -> partA
            else -> null
        }

    /** [S: [B, A]], the inverse algorithm. Only for commutators. */
    fun inverse(): Solution {
        require(primitive == null)
        val tokens = Alg.simplify(setup + partB + partA + Alg.invert(partB) + Alg.invert(partA) + Alg.invert(setup))
        return Solution(format(setup, partB, partA), tokens, MoveCost.score(tokens), setup, partB, partA)
    }

    companion object {
        fun format(s: List<String>, a: List<String>, b: List<String>): String {
            val c = "[${Alg.format(a)}, ${Alg.format(b)}]"
            return if (s.isEmpty()) c else "[${Alg.format(s)}: $c]"
        }
    }
}

/**
 * Estimated execution cost of moves: <R, U> is cheapest, then D, M; F and L need small regrips,
 * B, E, S and rotations are slow. Half turns are slightly slower than quarter turns.
 */
object MoveCost {
    fun of(t: String): Double {
        val base = when (t[0]) {
            'R', 'U' -> 1.0
            'M' -> 1.1
            'D' -> 1.2
            'F' -> 1.6
            'L' -> 1.7
            'r' -> 1.3
            'E', 'S' -> 1.9
            'u', 'd' -> 1.6
            'l' -> 1.8
            'B' -> 2.8
            'f', 'b' -> 2.8
            'x', 'y', 'z' -> 1.2
            else -> 3.0
        }
        return base + if (t.endsWith("2")) 0.3 else 0.0
    }

    fun score(tokens: List<String>): Double = tokens.sumOf { of(it) }
}

private fun moveTokens(letters: String): List<String> =
    letters.flatMap { c -> listOf("$c", "${c}2", "$c'") }

/**
 * Canonical move sequences (setups) of exactly [depth] moves: no two consecutive moves of the same
 * layer, and moves about the same axis that commute only in one order.
 */
internal fun setupSequences(moves: List<String>, depth: Int): List<List<String>> {
    var level = listOf(emptyList<String>())
    repeat(depth) {
        val next = ArrayList<List<String>>()
        for (s in level) for (m in moves) {
            if (s.isNotEmpty()) {
                val last = s.last()[0]
                if (last == m[0]) continue
                if (Alg.axis(last) == Alg.axis(m[0]) && last > m[0]) continue
            }
            next.add(s + m)
        }
        level = next
    }
    return level
}

private class Candidate(val solution: Solution, val notationLength: Int)

private val CANDIDATE_ORDER = compareBy<Candidate>({ it.solution.score }, { it.notationLength }, { it.solution.notation })

/** Keeps the best candidate per distinct move sequence, returns the [n] best. */
private class Best {
    private val byMoves = HashMap<List<String>, Candidate>()
    fun offer(c: Candidate) {
        val old = byMoves[c.solution.moves]
        if (old == null || CANDIDATE_ORDER.compare(c, old) < 0) byMoves[c.solution.moves] = c
    }
    val isEmpty get() = byMoves.isEmpty()
    fun top(n: Int): List<Solution> = byMoves.values.sortedWith(CANDIDATE_ORDER).take(n).map { it.solution }
}

/**
 * Finds commutators `[S: [A, B]]` for 3-cycles of corners or edges.
 *
 * Pure commutators [A, B] are enumerated once, with A and B each a single move or an insertion
 * X Y X' (corners: face turns; edges: face and slice turns), and indexed by the 3-cycle they
 * perform. A case is then solved by trying all setups S up to two moves (three if needed) and
 * looking up the cycle that S turns the case into.
 */
class CycleSearch(val type: PieceType) {
    private val moves = moveTokens(if (type == PieceType.CORNER) "UDFBRL" else "UDFBRLMES")
    private val parts: List<List<String>>
    private val table = HashMap<Int, MutableList<Int>>()

    // integer representation of moves for the fast inner loop
    private val letters = moves.map { it[0] }.distinct()
    private val mLetter = IntArray(moves.size) { letters.indexOf(moves[it][0]) }
    private val mPow = IntArray(moves.size) { Alg.power(moves[it]) }
    private val mAxis = IntArray(moves.size) { Alg.axis(moves[it][0]) }
    private val mCost = DoubleArray(moves.size) { MoveCost.of(moves[it]) }
    private val byLetterPow = IntArray(letters.size * 4) { -1 }.also { t ->
        for (m in moves.indices) t[mLetter[m] * 4 + mPow[m]] = m
    }
    private fun ints(tokens: List<String>) = IntArray(tokens.size) { moves.indexOf(tokens[it]) }

    private val partInts: Array<IntArray>
    private val partInvInts: Array<IntArray>

    init {
        val p = ArrayList<List<String>>()
        for (m in moves) p.add(listOf(m))
        for (x in moves) for (y in moves) {
            if (Alg.axis(x[0]) != Alg.axis(y[0])) p.add(listOf(x, y, Alg.invertToken(x)))
        }
        parts = p
        partInts = Array(p.size) { ints(p[it]) }
        partInvInts = Array(p.size) { ints(Alg.invert(p[it])) }
        val perm = parts.map { Perm.of(it) }
        val inv = perm.map { Perm.inverse(it) }
        val want = if (type == PieceType.CORNER) 3 else 2
        val c = IntArray(54)
        val n = parts.size
        for (i in 0 until n) {
            val pa = perm[i]; val ai = inv[i]
            for (j in 0 until n) {
                val pb = perm[j]; val bi = inv[j]
                var moved = 0
                var ok = true
                for (f in 0 until 54) {
                    val d = bi[ai[pb[pa[f]]]]
                    c[f] = d
                    if (d != f) {
                        moved++
                        if (KIND[f] != want) { ok = false; break }
                    }
                }
                if (!ok || moved != 3 * want) continue
                for (f in 0 until 54) {
                    if (c[f] == f) continue
                    if (c[c[c[f]]] != f || Cube.POS[c[f]] == Cube.POS[f]) { ok = false; break }
                }
                if (!ok) continue
                val body = i * n + j
                for (f in 0 until 54) {
                    if (c[f] == f) continue
                    val k = key(f, c[f], c[c[f]])
                    val list = table.getOrPut(k) { ArrayList(4) }
                    if (list.lastOrNull() != body) list.add(body)
                }
            }
        }
    }

    private class Setup(val tokens: List<String>, val perm: IntArray, val ints: IntArray, val inv: IntArray)

    private fun setupsOf(depths: IntRange) = depths.flatMap { setupSequences(moves, it) }.map {
        Setup(it, Perm.of(it), ints(it), ints(Alg.invert(it)))
    }

    private val setups: List<Setup> = setupsOf(0..2)
    private val longSetups: List<Setup> by lazy { setupsOf(3..3) }

    /**
     * Best commutators (at most [n]) for buffer sticker [b] and targets [x], [y]: the sticker at b
     * goes to x, the sticker at x to y and the sticker at y to b.
     */
    @Synchronized
    fun solve(b: Int, x: Int, y: Int, n: Int = 3): List<Solution> {
        val top = TopN(n)
        collect(setups, b, x, y, top)
        if (top.isEmpty) collect(longSetups, b, x, y, top)
        return top.entries.map { e ->
            val s = if (e.long) longSetups[e.setup] else setups[e.setup]
            val np = parts.size
            val a = parts[e.body / np]
            val bb = parts[e.body % np]
            val tokens = e.moves.map { moves[it] }
            Solution(Solution.format(s.tokens, a, bb), tokens, MoveCost.score(tokens), s.tokens, a, bb)
        }
    }

    private class Entry(val score: Double, val nLen: Int, val setup: Int, val body: Int, val long: Boolean, val moves: IntArray) {
        val key: String = String(CharArray(moves.size) { (moves[it] + 48).toChar() })
    }

    /** The best n candidates with different move sequences; ties: shorter notation, then search order. */
    private class TopN(val n: Int) {
        val entries = ArrayList<Entry>()
        val isEmpty get() = entries.isEmpty()
        fun threshold(): Double = if (entries.size < n) Double.MAX_VALUE else entries.last().score

        fun offer(e: Entry) {
            val same = entries.indexOfFirst { it.key == e.key }
            if (same >= 0) {
                if (better(e, entries[same])) { entries.removeAt(same); insert(e) }
                return
            }
            insert(e)
            if (entries.size > n) entries.removeAt(entries.size - 1)
        }

        private fun insert(e: Entry) {
            var i = 0
            while (i < entries.size && !better(e, entries[i])) i++
            entries.add(i, e)
        }

        private fun better(a: Entry, b: Entry) = a.score < b.score || (a.score == b.score && a.nLen < b.nLen)
    }

    private val buf = IntArray(128)

    /** Simplifies the concatenation of the arrays into [buf]; returns the length. */
    private fun simplifyInto(vararg seqs: IntArray): Int {
        var len = 0
        for (seq in seqs) for (t in seq) {
            var j = len - 1
            var merged = false
            while (j >= 0 && mAxis[buf[j]] == mAxis[t]) {
                if (mLetter[buf[j]] == mLetter[t]) {
                    val pw = (mPow[buf[j]] + mPow[t]) % 4
                    if (pw == 0) {
                        System.arraycopy(buf, j + 1, buf, j, len - j - 1)
                        len--
                    } else buf[j] = byLetterPow[mLetter[t] * 4 + pw]
                    merged = true
                    break
                }
                j--
            }
            if (!merged) buf[len++] = t
        }
        return len
    }

    private fun collect(list: List<Setup>, b: Int, x: Int, y: Int, top: TopN) {
        val np = parts.size
        val long = list !== setups
        for ((si, s) in list.withIndex()) {
            val bodies = table[key(s.perm[b], s.perm[x], s.perm[y])] ?: continue
            for (body in bodies) {
                val ia = body / np
                val ib = body % np
                val len = simplifyInto(s.ints, partInts[ia], partInts[ib], partInvInts[ia], partInvInts[ib], s.inv)
                var score = 0.0
                for (k in 0 until len) score += mCost[buf[k]]
                if (score > top.threshold()) continue
                val nLen = 2 * s.ints.size + partInts[ia].size + partInts[ib].size
                top.offer(Entry(score, nLen, si, body, long, buf.copyOf(len)))
            }
        }
    }

    private companion object {
        /** 3 for corner facelets, 2 for edge facelets, 1 for centres. */
        val KIND = IntArray(54) { f ->
            val p = Cube.POS[f]
            (if (p.x != 0) 1 else 0) + (if (p.y != 0) 1 else 0) + (if (p.z != 0) 1 else 0)
        }

        /** Key of the cycle u -> v -> w -> u, independent of the starting point. */
        fun key(u: Int, v: Int, w: Int): Int = when {
            u < v && u < w -> (u * 54 + v) * 54 + w
            v < w -> (v * 54 + w) * 54 + u
            else -> (w * 54 + u) * 54 + v
        }
    }
}

/**
 * Finds algorithms `[S: P]` where P is one of a list of fixed building blocks (twist commutators,
 * edge flip algorithms, parity swaps) and S is a setup of up to two moves (three if needed),
 * including slice moves and whole-cube rotations.
 */
class PrimitiveSearch(primitives: List<String>) {
    private class Key(val p: IntArray) {
        override fun equals(other: Any?) = other is Key && other.p.contentEquals(p)
        override fun hashCode() = p.contentHashCode()
    }

    private val table = HashMap<Key, MutableList<String>>()

    init {
        for (alg in primitives) table.getOrPut(Key(Perm.of(alg))) { ArrayList() }.add(alg)
    }

    private val moves = moveTokens("UDFBRLMESxyz")
    private val setups by lazy { (0..2).flatMap { setupSequences(moves, it) }.map { Triple(it, Perm.of(it), Perm.inverse(Perm.of(it))) } }
    private val longSetups by lazy { setupSequences(moves, 3).map { Triple(it, Perm.of(it), Perm.inverse(Perm.of(it))) } }

    fun solve(effect: IntArray, n: Int = 3): List<Solution> {
        val best = Best()
        collect(setups, effect, best)
        if (best.isEmpty) collect(longSetups, effect, best)
        return best.top(n)
    }

    private fun collect(list: List<Triple<List<String>, IntArray, IntArray>>, effect: IntArray, best: Best) {
        for ((s, ps, psInv) in list) {
            // the building block must perform S * effect * S^-1
            val c = IntArray(54) { j -> ps[effect[psInv[j]]] }
            val prims = table[Key(c)] ?: continue
            for (prim in prims) {
                val tokens = Alg.simplify(s + Alg.parse(prim) + Alg.invert(s))
                val notation = if (s.isEmpty()) prim else "[${Alg.format(s)}: $prim]"
                val sol = Solution(notation, tokens, MoveCost.score(tokens), s, primitive = prim)
                best.offer(Candidate(sol, s.size))
            }
        }
    }
}

/** The fixed building blocks. Each one is checked by the unit tests. */
object Primitives {
    /**
     * Corner twists: [A, B] where A = X Y X' Y' X Y X' twists one corner in place without
     * disturbing the layer of B (the classic R' D' R D R' D' R), and B turns that layer.
     */
    val TWISTS: List<String> by lazy {
        val quarter = "UDFBRL".flatMap { listOf("$it", "$it'") }
        val faces = moveTokens("UDFBRL")
        val out = ArrayList<String>()
        for (x in quarter) for (y in quarter) {
            if (Alg.axis(x[0]) == Alg.axis(y[0])) continue
            val xi = Alg.invertToken(x); val yi = Alg.invertToken(y)
            val a = "$x $y $xi $yi $x $y $xi"
            for (b in faces) {
                for (alg in listOf("[$a, $b]", "[$b, $a]")) {
                    if (isTwoTwist(Perm.of(alg))) out.add(alg)
                }
            }
        }
        out
    }

    /** Pure 2-flips in <M, U> (found by an exhaustive search; 12-13 moves). */
    val FLIPS = listOf(
        "M U M U M U2 M' U M' U M' U2",
        "M U M U2 M' U M' U M' U2 M U",
        "M U' M U2 M' U' M' U' M' U2 M U'",
        "M' U M' U2 M U M U M U2 M' U",
        "M' U' M' U2 M U' M U' M U2 M' U'",
        "M U M' U M' U2 M U M U M U2 M2",
        "M U M' U2 M U M U M U2 M' U M2",
        "M U2 M U M U M U2 M' U M' U M2",
        "M U' M' U2 M U' M U' M U2 M' U' M2",
        "M2 U M' U M' U2 M U M U M U2 M",
        "M2 U M' U2 M U M U M U2 M' U M",
        "M2 U' M' U2 M U' M U' M U2 M' U' M",
        "U M U M U M U2 M' U M' U M' U",
        "U M U' M U2 M' U' M' U' M' U2 M U2",
        "U M' U' M' U2 M U' M U' M U2 M' U2",
    )

    /** PLL algorithms that (with the right AUF) swap two corners and two edges (for parity). */
    val PARITY = listOf(
        "R U R' U' R' F R2 U' R' U' R U R' F'",          // T: UFR-UBR, UL-UR
        "R U R' F' R U R' U' R' F R2 U' R' U'",          // Jb: UFR-UBR, UF-UR
        "R' U L' U2 R U' R' U2 R L U'",                  // Ja
        "F R U' R' U' R U R' F' R U R' U' R' F R F'",    // Y: UFR-UBL, UL-UB
        "R U' R' U' R U R D R' U' R D' R' U2 R' U'",     // Ra
        "R2 F R U R U' R' F' R U2 R' U2 R",              // Rb
        "R' U' F' R U R' U' R' F R2 U' R' U' R U R' U R", // F
    )

    /** The parity algorithms with all pre- and post-AUFs (each one is a different swap pattern). */
    val PARITY_VARIANTS: List<String> by lazy {
        val aufs = listOf("", "U", "U2", "U'")
        PARITY.flatMap { p -> aufs.flatMap { a -> aufs.map { b -> Alg.format(Alg.simplify(Alg.parse("$a $p $b"))) } } }
            .distinct().filter { isParitySwap(Perm.of(it)) }
    }

    fun isTwoTwist(p: IntArray): Boolean {
        val moved = Perm.moved(p)
        if (moved.size != 6) return false
        if (moved.any { Stickers.typeOf(it) != PieceType.CORNER || !Stickers.samePiece(it, p[it]) }) return false
        return moved.map { Cube.POS[it] }.toSet().size == 2
    }

    fun isTwoFlip(p: IntArray): Boolean {
        val moved = Perm.moved(p)
        if (moved.size != 4) return false
        return moved.all { Stickers.typeOf(it) == PieceType.EDGE && Stickers.samePiece(it, p[it]) && p[p[it]] == it }
    }

    /** Exactly two corners swapped and two edges swapped (orientation arbitrary), nothing else. */
    fun isParitySwap(p: IntArray): Boolean {
        val moved = Perm.moved(p)
        val corners = moved.filter { Stickers.typeOf(it) == PieceType.CORNER }.map { Cube.POS[it] }.toSet()
        val edges = moved.filter { Stickers.typeOf(it) == PieceType.EDGE }.map { Cube.POS[it] }.toSet()
        if (corners.size != 2 || edges.size != 2 || moved.size != 10) return false
        return moved.all { Cube.POS[p[it]] != Cube.POS[it] && Cube.POS[p[p[it]]] == Cube.POS[it] }
    }
}
