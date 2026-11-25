package app.threestyle.bld

import app.threestyle.cube.Alg

/**
 * The commutators for all buffers are generated in advance (by [AlgAssets.generate], run from a
 * unit test) and shipped as text files in `assets/algs/`. The app only loads them and re-checks
 * every algorithm by simulation.
 *
 * File format, one case per line:
 *   `<sticker or piece> <sticker or piece> TAB <alg> TAB <alg> ...`
 * where an alg is `S|A|B` for the commutator [S: [A, B]] or `S|P` for [S: P] (moves separated by
 * spaces, S may be empty).
 */
object AlgAssets {
    const val DIR = "algs"

    fun cycleFile(type: PieceType, buffer: Int) =
        "$DIR/${if (type == PieceType.CORNER) "corners" else "edges"}-${Stickers.pieceName(buffer)}.txt"

    const val TWIST_FILE = "$DIR/twists.txt"
    const val FLIP_FILE = "$DIR/flips.txt"

    fun encode(s: Solution): String =
        if (s.primitive != null) "${Alg.format(s.setup)}|${s.primitive}"
        else "${Alg.format(s.setup)}|${Alg.format(s.partA)}|${Alg.format(s.partB)}"

    /** Decodes one algorithm; throws IllegalArgumentException if it is malformed. */
    fun decode(enc: String): Solution {
        val f = enc.split('|')
        val setup = Alg.parse(f[0])
        return when (f.size) {
            3 -> {
                val a = Alg.parse(f[1]); val b = Alg.parse(f[2])
                require(a.isNotEmpty() && b.isNotEmpty())
                val tokens = Alg.simplify(setup + a + b + Alg.invert(a) + Alg.invert(b) + Alg.invert(setup))
                Solution(Solution.format(setup, a, b), tokens, MoveCost.score(tokens), setup, a, b)
            }
            2 -> {
                val prim = f[1].trim()
                val tokens = Alg.simplify(setup + Alg.parse(prim) + Alg.invert(setup))
                val notation = if (setup.isEmpty()) prim else "[${Alg.format(setup)}: $prim]"
                Solution(notation, tokens, MoveCost.score(tokens), setup, primitive = prim)
            }
            else -> throw IllegalArgumentException("Malformed algorithm '$enc'")
        }
    }

    /** Parses a file into "name name" -> algorithms. Malformed entries are skipped. */
    fun parse(text: String): Map<String, List<Solution>> {
        val out = HashMap<String, List<Solution>>()
        for (line in text.lineSequence()) {
            if (line.isBlank() || line.startsWith("#")) continue
            val f = line.split('\t')
            out[f[0]] = f.drop(1).mapNotNull { runCatching { decode(it) }.getOrNull() }
        }
        return out
    }

    private fun line(name: String, sols: List<Solution>) =
        (listOf(name) + sols.map { encode(it) }).joinToString("\t")

    /** Generates the file for one buffer: inverse cases get the inverse commutators. */
    fun generateCycles(type: PieceType, buffer: Int, search: CycleSearch = CycleSearch(type)): String {
        val targets = Stickers.speffzOrder(type).filter { !Stickers.samePiece(it, buffer) }
        val solved = HashMap<Pair<Int, Int>, List<Solution>>()
        val sb = StringBuilder("# ${type.name.lowercase()} commutators, buffer ${Stickers.pieceName(buffer)}\n")
        for (x in targets) for (y in targets) {
            if (Stickers.samePiece(x, y)) continue
            val sols = solved[y to x]?.map { it.inverse() } ?: search.solve(buffer, x, y)
            solved[x to y] = sols
            sb.append(line("${Stickers.stickerName(x)} ${Stickers.stickerName(y)}", sols)).append('\n')
        }
        return sb.toString()
    }

    fun generateTwists(search: PrimitiveSearch = PrimitiveSearch(Primitives.TWISTS)): String {
        val pieces = Stickers.pieces(PieceType.CORNER)
        val sb = StringBuilder("# corner twists: first piece clockwise, second counter-clockwise\n")
        for (p in pieces) for (q in pieces) {
            if (p == q) continue
            sb.append(line("${Stickers.pieceName(p)} ${Stickers.pieceName(q)}", search.solve(Effects.twistFix(p, q)))).append('\n')
        }
        return sb.toString()
    }

    fun generateFlips(search: PrimitiveSearch = PrimitiveSearch(Primitives.FLIPS)): String {
        val pieces = Stickers.pieces(PieceType.EDGE)
        val sb = StringBuilder("# edge flips\n")
        for ((i, p) in pieces.withIndex()) for (q in pieces.drop(i + 1)) {
            sb.append(line("${Stickers.pieceName(p)} ${Stickers.pieceName(q)}", search.solve(Effects.flip(p, q)))).append('\n')
        }
        return sb.toString()
    }

    /** All files: name -> content. */
    fun generateAll(): Map<String, String> {
        val out = LinkedHashMap<String, String>()
        val cs = CycleSearch(PieceType.CORNER)
        val es = CycleSearch(PieceType.EDGE)
        for (b in Stickers.pieces(PieceType.CORNER)) out[cycleFile(PieceType.CORNER, b)] = generateCycles(PieceType.CORNER, b, cs)
        for (b in Stickers.pieces(PieceType.EDGE)) out[cycleFile(PieceType.EDGE, b)] = generateCycles(PieceType.EDGE, b, es)
        out[TWIST_FILE] = generateTwists()
        out[FLIP_FILE] = generateFlips()
        return out
    }
}
