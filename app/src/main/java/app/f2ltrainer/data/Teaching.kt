package app.f2ltrainer.data

import app.f2ltrainer.cube.Alg
import app.f2ltrainer.cube.CubeState
import app.f2ltrainer.cube.F2l

/** Splits an algorithm into "adjust", "pair up" and "insert" phases by simulating it. */
object Teaching {
    enum class Kind(val label: String) { ADJUST("Adjust"), PAIR("Pair up"), INSERT("Insert") }

    /** [from, to) are token indices. */
    data class Phase(val kind: Kind, val from: Int, val to: Int)

    fun phases(start: CubeState, tokens: List<String>): List<Phase> {
        val n = tokens.size
        if (n == 0) return emptyList()
        val connected = BooleanArray(n + 1)
        var s = start
        connected[0] = F2l.isPairConnected(s)
        for (i in 0 until n) {
            s = s.applyMove(tokens[i])
            connected[i + 1] = F2l.isPairConnected(s)
        }
        // first index from which the pair stays connected until the end
        var m = n
        while (m > 0 && connected[m - 1]) m--
        val result = ArrayList<Phase>()
        // leading U turns / y rotations before any pairing work
        val limit = if (m == 0) n else m
        var adj = 0
        while (adj < limit && (tokens[adj][0] == 'U' || tokens[adj][0] == 'y')) adj++
        if (adj == limit) adj = 0 // the U turn itself creates/inserts the pair
        if (adj > 0) result.add(Phase(Kind.ADJUST, 0, adj))
        // If the pieces only join with the very last move (e.g. D-move tricks), that move both
        // joins and inserts them: it is shown as the insert.
        val pairEnd = if (m == n) n - 1 else m
        if (pairEnd > adj) result.add(Phase(Kind.PAIR, adj, pairEnd))
        val insertFrom = maxOf(pairEnd, adj)
        if (n > insertFrom) result.add(Phase(Kind.INSERT, insertFrom, n))
        return result
    }

    fun phases(start: CubeState, alg: String) = phases(start, Alg.parse(alg))
}
