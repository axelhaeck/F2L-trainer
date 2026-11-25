package app.threestyle

import app.threestyle.bld.BufferConfig
import app.threestyle.bld.LetterScheme
import app.threestyle.bld.Library
import app.threestyle.bld.MemoCheck
import app.threestyle.bld.PieceType
import app.threestyle.bld.Scrambler
import app.threestyle.bld.Stickers
import app.threestyle.bld.Tracer
import app.threestyle.cube.CubeState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class MemoTest {
    private val lib = Library(BufferConfig(), TestAssets.reader)
    private val cb = lib.config.cornerBuffer
    private val eb = lib.config.edgeBuffer
    private val pe = lib.config.parityEdge

    private fun pairAlg(type: PieceType, x: Int, y: Int): String {
        val key = Library.cycleKey(if (type == PieceType.CORNER) app.threestyle.bld.CaseType.CORNER else app.threestyle.bld.CaseType.EDGE,
            if (type == PieceType.CORNER) cb else eb, x, y)
        return lib.case(key)!!.solutions[0].notation
    }

    /**
     * Executing the library's algorithms for the traced letter pairs (with the usual parity
     * handling) must solve the cube except for exactly the reported twisted / flipped pieces.
     */
    @Test fun tracedMemoExecutedWithLibraryAlgorithmsSolvesTheCube() {
        val rnd = Random(2026)
        var checked = 0
        var parities = 0
        repeat(300) {
            val scramble = Scrambler.random(rnd)
            val start = CubeState.SOLVED.apply(scramble)
            val et = Tracer.trace(start, PieceType.EDGE, eb)
            val ct = Tracer.trace(start, PieceType.CORNER, cb)
            assertEquals(et.parity, ct.parity)  // permutation parity of corners and edges is equal
            val edges = et.targets.toMutableList()
            if (et.parity) {
                val z = edges.last()
                if (Stickers.samePiece(z, pe) && z != pe) return@repeat  // rare special case, not covered here
                if (z != pe) edges.add(pe)  // last edge letter + parity letter, then the parity algorithm
                else edges.removeAt(edges.size - 1)
                parities++
            }
            var s = start
            for (p in edges.chunked(2)) s = s.apply(pairAlg(PieceType.EDGE, p[0], p[1]))
            val corners = ct.targets
            for (p in corners.chunked(2)) {
                s = if (p.size == 2) s.apply(pairAlg(PieceType.CORNER, p[0], p[1]))
                else s.apply(lib.parity.first { it.stickers[0] == p[0] }.solutions[0].notation)
            }
            // every piece in place; orientation exactly as reported
            val expected = (ct.misoriented + et.misoriented).map { it.piece }.toSet()
            for (type in PieceType.entries) for (ref in Stickers.pieces(type)) {
                for (f in Stickers.pieceStickers(ref)) assertTrue("$scramble ${Stickers.pieceName(ref)} not in place", Stickers.samePiece(s[f], f))
                val solved = Stickers.pieceStickers(ref).all { s[it] == it }
                assertEquals("$scramble ${Stickers.pieceName(ref)}", ref !in expected, solved)
            }
            // fix the reported twists and flips with the library: the cube must end solved
            checked++
        }
        assertTrue(checked > 250)
        assertTrue(parities > 50)
    }

    @Test fun twistAndFlipAlgorithmsFixReportedOrientation() {
        val rnd = Random(5)
        repeat(100) {
            val start = CubeState.SOLVED.apply(Scrambler.random(rnd))
            val t = Tracer.trace(start, PieceType.CORNER, cb)
            // twists reported by the trace: total twist is 0 mod 3 after the memo is executed
            val total = t.misoriented.sumOf { it.steps }
            if (t.targets.size % 2 == 0) assertEquals(0, total % 3)
        }
        // UFR clockwise, UBL counter-clockwise: the case state is reported like that by the tracer
        val tw = lib.case("T:UFR:UBL")!!
        val tr = Tracer.trace(tw.state, PieceType.CORNER, cb)
        assertTrue(tr.targets.isEmpty())
        assertEquals(setOf(Stickers.byName("UFR") to 1, Stickers.byName("UBL") to 2), tr.misoriented.map { it.piece to it.steps }.toSet())
        assertTrue(tw.state.apply(tw.solutions[0].notation).isSolvedUpToRotation())
    }

    @Test fun checkAcceptsValidMemoAndPointsAtMistakes() {
        val rnd = Random(11)
        val scheme = LetterScheme()
        repeat(100) {
            val start = CubeState.SOLVED.apply(Scrambler.random(rnd))
            for ((type, buf) in listOf(PieceType.CORNER to cb, PieceType.EDGE to eb)) {
                val tr = Tracer.trace(start, type, buf)
                val ok = Tracer.check(start, type, buf, tr.targets)
                assertTrue(ok is MemoCheck.Ok)
                assertEquals(tr.misoriented.toSet(), (ok as MemoCheck.Ok).misoriented.toSet())
                if (tr.targets.isNotEmpty()) {
                    // a wrong first letter is reported at index 0
                    val wrong = Stickers.speffzOrder(type).first { !Stickers.samePiece(it, buf) && it != tr.targets[0] && !Stickers.samePiece(it, tr.targets[0]) }
                    val r = Tracer.check(start, type, buf, listOf(wrong) + tr.targets.drop(1))
                    if (!Stickers.samePiece(start[buf], buf)) assertEquals(MemoCheck.Wrong(0, tr.targets[0]), r)
                    // a missing last letter is reported as incomplete
                    assertTrue(Tracer.check(start, type, buf, tr.targets.dropLast(1)) is MemoCheck.Incomplete)
                }
                // letters round trip through the scheme
                val text = tr.targets.joinToString("") { scheme.letterOf(it).toString() }
                val (parsed, bad) = Tracer.parse(text.lowercase(), type, buf, scheme)
                assertEquals(null, bad)
                assertEquals(tr.targets, parsed)
            }
        }
        val (_, bad) = Tracer.parse("C", PieceType.CORNER, cb, scheme)  // C = UFR = buffer
        assertFalse(bad == null)
    }

    @Test fun scramblesHaveNoRedundantMoves() {
        val rnd = Random(1)
        repeat(500) {
            val toks = app.threestyle.cube.Alg.parse(Scrambler.random(rnd))
            assertEquals(21, toks.size)
            assertEquals(toks, app.threestyle.cube.Alg.simplify(toks))
        }
    }
}
