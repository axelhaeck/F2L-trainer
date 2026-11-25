package app.threestyle

import app.threestyle.cube.Alg
import app.threestyle.cube.Cube
import app.threestyle.cube.CubeState
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
        assertEquals(listOf("R'"), Alg.parse("R3"))
        assertEquals(listOf("R", "U", "R", "U"), Alg.parse("(R U)2"))
        assertThrows(IllegalArgumentException::class.java) { Alg.parse("R Q") }
        assertThrows(IllegalArgumentException::class.java) { Alg.parse("[R, U") }
        assertThrows(IllegalArgumentException::class.java) { Alg.parse("R, U") }
        assertThrows(IllegalArgumentException::class.java) { Alg.parse("Mw") }
    }

    @Test fun commutatorNotation() {
        assertEquals(Alg.parse("R U R' U'"), Alg.parse("[R, U]"))
        assertEquals(Alg.parse("R U R' D R U' R' D'"), Alg.parse("[R U R', D]"))
        assertEquals(Alg.parse("R' D R U2 R' D' R U2"), Alg.parse("[R' D R, U2]"))
        assertEquals(Alg.parse("R' D R U R' D' R U'"), Alg.simplify(Alg.parse("[R': [D, R U R']]")))
        assertEquals(Alg.parse("R' D R U R' D' R U'"), Alg.simplify(Alg.parse("[R'; [D, R U R']]")))
        assertEquals(Alg.parse("U2 M' U2 M"), Alg.parse("[U2, M']"))
        assertEquals(Alg.parse("x R U R' x'"), Alg.parse("[x: R U R']"))
        // nested and with spaces / repetition
        assertEquals(Alg.parse("U R U' R' U U'"), Alg.parse("[ U : [R, U'] ]"))
        assertEquals(Alg.parse("R' D R U R' U' D' U R U' R' R"), Alg.parse("[R': [D, [R, U]]]"))
        assertEquals(Alg.parse("R U R' U' R U R' U'"), Alg.parse("([R, U])2"))
        assertEquals(Alg.parse("R"), Alg.parse("[R]"))
    }

    @Test fun inverseUndoes() {
        val rnd = Random(7)
        repeat(200) {
            val seq = List(12) { allTokens[rnd.nextInt(allTokens.size)] }
            assertEquals(solved, solved.apply(seq).apply(Alg.invert(seq)))
        }
    }

    @Test fun simplifyKeepsState() {
        val rnd = Random(3)
        val letters = "URFDLBMESrludfbxyz"
        repeat(2000) {
            val seq = List(10) { val c = letters[rnd.nextInt(letters.length)]; Alg.token(c, rnd.nextInt(1, 4))!! }
            val simp = Alg.simplify(seq)
            assertEquals(solved.apply(seq), solved.apply(simp))
            assertTrue(simp.size <= seq.size)
        }
        assertEquals(listOf("U'"), Alg.simplify(Alg.parse("U U2")))
        assertEquals(emptyList<String>(), Alg.simplify(Alg.parse("R U U' R'")))
        assertEquals(listOf("L"), Alg.simplify(Alg.parse("R L R'")))
        assertEquals(listOf("M2", "R"), Alg.simplify(Alg.parse("M R' M R2")))
    }

    @Test fun moveCountIsSliceTurnMetric() {
        assertEquals(4, Alg.moveCount(Alg.parse("[U2, M']")))
        assertEquals(8, Alg.moveCount(Alg.parse("[R U R', D]")))
        assertEquals(3, Alg.moveCount(Alg.parse("x R y U z R")))
    }
}
