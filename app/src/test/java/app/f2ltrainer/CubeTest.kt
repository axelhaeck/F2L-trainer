package app.f2ltrainer

import app.f2ltrainer.cube.Alg
import app.f2ltrainer.cube.Cube
import app.f2ltrainer.cube.CubeState
import app.f2ltrainer.cube.F2l
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test
import kotlin.random.Random

class CubeTest {
    private val solved = CubeState.SOLVED
    private val allTokens = Cube.MOVE_LETTERS.flatMap { c -> listOf("$c", "${c}2", "$c'") }

    @Test fun faceletLayoutIsConsistent() {
        // every facelet has a unique (position, normal) and centres are at index f*9+4
        for (f in 0 until 6) assertEquals(Cube.NORMALS[f], Cube.POS[f * 9 + 4])
        val pairs = (0 until 54).map { Cube.POS[it] to Cube.NORMAL[it] }.toSet()
        assertEquals(54, pairs.size)
        // U face top row borders B, F face top row borders U
        assertEquals("UBL", Cube.cubieName(Cube.POS[0]))
        assertEquals("UFR", Cube.cubieName(Cube.POS[8]))
        assertEquals("UFL", Cube.cubieName(Cube.POS[18]))
        assertEquals("DFR", Cube.cubieName(Cube.POS[3 * 9 + 2]))
    }

    @Test fun everyMoveIsAPermutationOfOrderFour() {
        for (t in allTokens) {
            val p = Cube.perm(t)
            assertEquals(t, 54, p.toSet().size)
            var s = solved
            repeat(4) { s = s.apply(t) }
            assertEquals(t, solved, s)
            assertNotEquals(t, solved, solved.apply(t))
        }
    }

    @Test fun primeAndDoubleAreConsistent() {
        for (c in Cube.MOVE_LETTERS) {
            assertEquals(solved.apply("$c $c"), solved.apply("${c}2"))
            assertEquals(solved.apply("$c $c $c"), solved.apply("$c'"))
        }
    }

    @Test fun knownIdentities() {
        assertEquals(solved, solved.apply("R U R' U' ".repeat(6)))
        assertNotEquals(solved, solved.apply("R U R' U' ".repeat(3)))
        assertEquals(solved, solved.apply("R U R' U R U2 R' ".repeat(6)))
        // T-perm is its own inverse
        val t = "R U R' U' R' F R2 U' R' U' R U R' F'"
        assertEquals(solved, solved.apply("$t $t"))
        assertEquals(solved.apply("x"), solved.apply("R M' L'"))
        assertEquals(solved.apply("y"), solved.apply("U E' D'"))
        assertEquals(solved.apply("z"), solved.apply("F S B'"))
        assertEquals(solved.apply("r"), solved.apply("R M'"))
        assertEquals(solved.apply("l"), solved.apply("L M"))
        assertEquals(solved.apply("u"), solved.apply("U E'"))
        assertEquals(solved.apply("d"), solved.apply("D E"))
        assertEquals(solved.apply("f"), solved.apply("F S"))
        assertEquals(solved.apply("b"), solved.apply("B S'"))
        // superflip has order 2 and flips all edges in place
        val superflip = "U R2 F B R B2 R U2 L B2 R U' D' R2 F R' L B2 U2 F2"
        assertEquals(solved, solved.apply("$superflip $superflip"))
    }

    @Test fun directionsOfBasicMoves() {
        // R moves the front-up sticker of the right layer to the top: F -> U
        val s = solved.apply("R")
        assertEquals(Cube.F, s[Cube.facelet(Cube.cubie("UFR"), Cube.NORMALS[Cube.U])] / 9)
        // U moves the front stickers to the left: F -> L
        assertEquals(Cube.F, solved.apply("U")[4 * 9 + 1] / 9)
        // F moves U stickers to R
        assertEquals(Cube.U, solved.apply("F")[1 * 9 + 3] / 9)
        // y rotates like U: front centre goes to the left
        assertEquals(Cube.F, solved.apply("y")[4 * 9 + 4] / 9)
    }

    @Test fun parsing() {
        assertEquals(listOf("R", "U", "R'", "U2", "r", "R2"), Alg.parse("(R U R') U2' Rw R2"))
        assertEquals(listOf("R", "U", "R'"), Alg.parse("RUR'"))
        assertEquals(listOf("R'", "U"), Alg.parse("R’ U"))
        assertThrows(IllegalArgumentException::class.java) { Alg.parse("R Q") }
        assertThrows(IllegalArgumentException::class.java) { Alg.parse("R3") }
    }

    @Test fun inverseUndoes() {
        val r = Random(1)
        repeat(200) {
            val seq = List(r.nextInt(1, 25)) { allTokens[r.nextInt(allTokens.size)] }
            assertEquals(solved, solved.apply(seq).apply(Alg.invert(seq)))
        }
    }

    @Test fun toFaceMovesIsEquivalent() {
        val r = Random(2)
        for (t in allTokens) {
            val (faces, rots) = Alg.toFaceMoves(listOf(t))
            assertTrue(faces.all { it[0] in "URFDLB" })
            assertEquals(t, solved.apply(t), solved.apply(faces).apply(rots))
        }
        repeat(300) {
            val seq = List(r.nextInt(1, 30)) { allTokens[r.nextInt(allTokens.size)] }
            val (faces, rots) = Alg.toFaceMoves(seq)
            assertEquals(seq.toString(), solved.apply(seq), solved.apply(faces).apply(rots))
        }
        assertEquals(listOf("F'", "U'", "F"), Alg.toFaceMoves(Alg.parse("y' R' U' R")).first)
    }

    @Test fun simplifyKeepsState() {
        val r = Random(3)
        val faceTokens = allTokens.filter { it[0] in "URFDLB" }
        repeat(300) {
            val seq = List(r.nextInt(0, 30)) { faceTokens[r.nextInt(faceTokens.size)] }
            val simp = Alg.simplify(seq)
            assertEquals(solved.apply(seq), solved.apply(simp))
            for (i in 1 until simp.size) assertNotEquals(simp[i - 1][0], simp[i][0])
        }
        assertEquals(listOf("U'"), Alg.simplify(Alg.parse("U U2")))
        assertEquals(emptyList<String>(), Alg.simplify(Alg.parse("R U U' R'")))
    }

    @Test fun prefixUIsEquivalent() {
        val algs = listOf("R U R'", "U R U' R'", "y' U R' U R", "y U2 R", "d R' U' R", "d2 R", "d' R' U R",
            "U2 F' U F", "F R", "y2 d R", "D R' U' R D'", "U")
        for (a in algs) for (j in 0 until 4) {
            val tokens = Alg.parse(a)
            val expected = solved.apply(List(j) { "U" }).apply(tokens)
            assertEquals("$a / $j", expected, solved.apply(Alg.prefixU(j, tokens)))
        }
        assertEquals("y' U2 R' U R", Alg.format(Alg.prefixU(1, Alg.parse("y' U R' U R"))))
        assertEquals("y' U2 R' U' R", Alg.format(Alg.prefixU(1, Alg.parse("d R' U' R"))))
        assertEquals("R U R'", Alg.format(Alg.prefixU(2, Alg.parse("U2 R U R'"))))
    }

    @Test fun f2lChecksAreRotationIndependent() {
        assertTrue(F2l.isSolved(solved))
        assertTrue(F2l.isSolved(solved.apply("y")))
        assertTrue(F2l.isSolved(solved.apply("U R U R' U R U2 R'")))
        assertFalse(F2l.isSolved(solved.apply("R U R'")))
        assertTrue(F2l.isSolved(solved.apply("R U R'"), setOf(F2l.Slot.FR, F2l.Slot.BR)))
        assertTrue(F2l.isSolved(solved.apply("x2")))  // only a rotation
        assertTrue(F2l.isPairConnected(solved))
        assertTrue(F2l.isPairConnected(solved.apply("R U")))
        assertTrue(F2l.isPairConnected(solved.apply("R U R' U'")))  // pair is lifted and comes back
        assertFalse(F2l.isPairConnected(solved.apply("R U' R'")))   // R' takes only the corner
        assertTrue(F2l.isPairConnected(solved.apply("x y2")))
        assertTrue(F2l.isPairConnected(solved.apply("R")))  // R moves corner and edge together
        assertTrue(F2l.isPairConnected(solved.apply("R U R'")))   // pair moved out of the R layer

    }
}
