package app.threestyle

import app.threestyle.bld.BufferConfig
import app.threestyle.bld.CaseType
import app.threestyle.bld.Effects
import app.threestyle.bld.LetterScheme
import app.threestyle.bld.Library
import app.threestyle.bld.PieceType
import app.threestyle.bld.Primitives
import app.threestyle.bld.Stickers
import app.threestyle.cube.Alg
import app.threestyle.cube.Cube
import app.threestyle.cube.CubeState
import app.threestyle.cube.Perm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BldEngineTest {
    private val scheme = LetterScheme()

    @Test fun speffzLetters() {
        val corners = listOf(
            "UBL", "UBR", "UFR", "UFL", "LUB", "LUF", "LDF", "LDB", "FUL", "FUR", "FDR", "FDL",
            "RUF", "RUB", "RDB", "RDF", "BUR", "BUL", "BDL", "BDR", "DFL", "DFR", "DBR", "DBL",
        )
        val edges = listOf(
            "UB", "UR", "UF", "UL", "LU", "LF", "LD", "LB", "FU", "FR", "FD", "FL",
            "RU", "RB", "RD", "RF", "BU", "BL", "BD", "BR", "DF", "DR", "DB", "DL",
        )
        for (i in 0 until 24) {
            val c = Stickers.SPEFFZ_CORNERS[i]
            val e = Stickers.SPEFFZ_EDGES[i]
            assertEquals(corners[i], Stickers.stickerName(c))
            assertEquals(edges[i], Stickers.stickerName(e))
            assertEquals('A' + i, scheme.letterOf(c))
            assertEquals('A' + i, scheme.letterOf(e))
            assertEquals(c, scheme.stickerOf(PieceType.CORNER, 'a' + i))
            assertEquals(c, Stickers.byName(corners[i]))
        }
        // sticker geometry: the named face is the sticker's face, the other letters its piece
        for (i in 0 until 24) {
            val c = Stickers.SPEFFZ_CORNERS[i]
            assertEquals(corners[i][0], Cube.FACE_NAMES[c / 9])
            assertEquals(corners[i].toSet(), Stickers.pieceName(c).toSet())
        }
    }

    @Test fun clockwiseOrder() {
        // UFR: U -> R -> F is clockwise; DFR: D -> F -> R
        assertEquals(listOf("UFR", "RUF", "FUR"), Stickers.cycleFrom(Stickers.byName("UFR")).map { Stickers.stickerName(it) })
        assertEquals(listOf("DFR", "FDR", "RDF"), Stickers.cycleFrom(Stickers.byName("DFR")).map { Stickers.stickerName(it) })
        assertEquals(listOf("UBL", "LUB", "BUL"), Stickers.cycleFrom(Stickers.byName("UBL")).map { Stickers.stickerName(it) })
        // The beginner corner twist R' D' R D (x2) turns UFR: its U sticker ends on F or R.
        val s = CubeState.SOLVED.apply("R' D' R D R' D' R D")
        val ufrU = Stickers.byName("UFR")
        // after the sequence the sticker at UFR's U position is one of UFR's own stickers
        assertTrue(Stickers.samePiece(s[ufrU], ufrU))
        assertTrue(s[ufrU] != ufrU)
    }

    @Test fun effectsAreLegalAndCorrect() {
        val b = Stickers.byName("UFR")
        val a = Stickers.byName("UBL")
        val g = Stickers.byName("LDF")
        val p = Effects.cycle(b, a, g)
        // the sticker at the buffer goes to A, the one at A to G, the one at G to the buffer
        assertEquals(a, p[b]); assertEquals(g, p[a]); assertEquals(b, p[g])
        assertEquals(9, Perm.moved(p).size)
    }

    @Test fun knownCommutators() {
        // Classic UF buffer edge commutators: [U2, M'] solves UF -> UB -> DF, i.e. pair A U
        val uf = Stickers.byName("UF")
        fun edge(x: String, y: String) = Effects.cycle(uf, Stickers.byName(x), Stickers.byName(y))
        assertTrue(Effects.solves(edge("UB", "DF"), "[U2, M']"))
        assertFalse(Effects.solves(edge("DF", "UB"), "[U2, M']"))
        assertTrue(Effects.solves(edge("DF", "UB"), "[M', U2]"))
        assertTrue(Effects.solves(edge("UB", "DB"), "[M, U2]"))
        // Corner: [R' D' R, U] - UFR buffer; check by direct simulation of letters
        val ufr = Stickers.byName("UFR")
        val state = CubeState.SOLVED.apply(Alg.invert(Alg.parse("[R' D' R, U]")))
        // the sticker now on the buffer belongs at the first target; find it
        val first = state[ufr]
        val c = Effects.cycle(ufr, first, state[first])
        assertTrue(Effects.solves(c, "[R' D' R, U]"))
    }

    @Test fun primitivesAreValid() {
        assertTrue(Primitives.TWISTS.size > 50)
        for (t in Primitives.TWISTS) assertTrue(t, Primitives.isTwoTwist(Perm.of(t)))
        for (f in Primitives.FLIPS) assertTrue(f, Primitives.isTwoFlip(Perm.of(f)))
        for (p in Primitives.PARITY) {
            val aufs = listOf("", "U", "U2", "U'")
            assertTrue(p, aufs.any { a -> aufs.any { b -> Primitives.isParitySwap(Perm.of("$a $p $b")) } })
        }
        for (p in Primitives.PARITY_VARIANTS) assertTrue(p, Primitives.isParitySwap(Perm.of(p)))
    }

    @Test fun defaultLibraryIsCompleteAndCorrect() {
        val t0 = System.currentTimeMillis()
        val lib = Library(BufferConfig(), TestAssets.reader)
        println("default library built in ${System.currentTimeMillis() - t0} ms")
        assertEquals(378, lib.corners.size)
        assertEquals(440, lib.edges.size)
        assertEquals(56, lib.twists.size)
        assertEquals(66, lib.flips.size)
        assertEquals(21, lib.parity.size)
        for (c in lib.all) {
            assertTrue("no algorithm for ${c.key}", c.solutions.isNotEmpty())
            for (s in c.solutions) {
                assertTrue("${c.key}: ${s.notation}", c.isSolvedBy(s.notation))
                assertTrue("${c.key}: ${s.moves}", c.isSolvedBy(Alg.format(s.moves)))
                assertEquals(Alg.simplify(Alg.parse(s.notation)), s.moves)
            }
        }
        // inverse cases have inverse algorithms
        for (c in lib.corners + lib.edges) {
            val inv = lib.case(c.inverseKey!!)!!
            assertEquals(c.solutions[0].moveCount, inv.solutions[0].moveCount)
            assertEquals(c.solutions[0].setup, inv.solutions[0].setup)
            assertEquals(c.solutions[0].partA, inv.solutions[0].partB)
            assertEquals(c.solutions[0].partB, inv.solutions[0].partA)
        }
        // every case is in exactly one set, sets are small, inverse pairs share a set
        val inSets = lib.sets.flatMap { it.keys }
        assertEquals(lib.all.size, inSets.size)
        assertEquals(lib.all.map { it.key }.toSet(), inSets.toSet())
        for (s in lib.sets) assertTrue(s.title, s.keys.size in 1..8)
        for (c in lib.corners + lib.edges + lib.twists) {
            assertEquals(lib.setOf(c.key), lib.setOf(c.inverseKey!!))
        }
        for (type in CaseType.entries) println("$type groups: ${lib.groups(type)}")
        val u = lib.edges.first { it.label(scheme) == "AU" }
        println("AU: ${u.solutions.map { it.notation }}")
        assertEquals("[U2, M']", u.solutions[0].notation)
    }

    @Test fun twistsAndFlipsDirections() {
        val lib = Library(BufferConfig(), TestAssets.reader)
        val tw = lib.twists.first { it.key == "T:UFR:UBL" }
        // case state: UFR twisted clockwise -> its U sticker sits on the R position? No: clockwise
        // means U -> R -> F; so the U-colour sticker of UFR is now at the R facelet.
        val st = tw.state
        assertEquals(Stickers.byName("UFR"), st[Stickers.byName("RUF")])
        assertEquals(Stickers.byName("UBL"), st[Stickers.byName("BUL")])
        assertNotNull(lib.flips.firstOrNull { it.key == "F:UR:UF" })
    }
}
