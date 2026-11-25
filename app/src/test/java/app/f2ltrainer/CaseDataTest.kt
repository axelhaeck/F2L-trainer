package app.f2ltrainer

import app.f2ltrainer.cube.Alg
import app.f2ltrainer.cube.Cube
import app.f2ltrainer.cube.CubeState
import app.f2ltrainer.cube.F2l
import app.f2ltrainer.data.CaseGroup
import app.f2ltrainer.data.Cases
import app.f2ltrainer.data.Exercise
import app.f2ltrainer.data.F2lCase
import app.f2ltrainer.data.PracticeItem
import app.f2ltrainer.data.Setup
import app.f2ltrainer.data.Teaching
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CaseDataTest {
    private val cases = Cases.ALL
    private val br = setOf(F2l.Slot.BR)

    @Test fun thereAre41CasesNumberedInOrder() {
        assertEquals(41, cases.size)
        assertEquals((1..41).toList(), cases.map { it.id })
        // groups are contiguous and have the expected sizes
        val sizes = cases.groupBy { it.group }.mapValues { it.value.size }
        assertEquals(mapOf(CaseGroup.BASIC to 4, CaseGroup.SPLIT to 12, CaseGroup.CROSSUP to 8,
            CaseGroup.EDGESLOT to 6, CaseGroup.CORNERSLOT to 6, CaseGroup.BOTHSLOT to 5), sizes)
        assertEquals(CaseGroup.entries.toList(), cases.map { it.group }.distinct())
    }

    @Test fun displayStateIsAnFrCaseWithOnlyThePairUnsolved() {
        for (c in cases) {
            val s = c.displayState
            assertTrue(c.name, F2l.isFrCase(s))
            assertFalse(c.name, F2l.isSlotSolved(s, F2l.Slot.FR))
            // centres are in standard orientation
            for (f in 0 until 6) assertEquals(f, s[f * 9 + 4] / 9)
        }
    }

    @Test fun everyAlgorithmSolvesItsCase() {
        for (c in cases) {
            for (a in c.algorithms) {
                val end = c.displayState.apply(a)
                assertTrue("${c.name}: $a", F2l.isSolved(end))
            }
        }
    }

    @Test fun freeSlotAlgorithmsSolveTheCaseButNeedTheFreeSlot() {
        var count = 0
        for (c in cases) {
            val a = c.freeSlotAlgorithm ?: continue
            count++
            val end = c.displayState.apply(a)
            assertTrue("${c.name}: $a", F2l.isSolved(end, br))
            assertFalse("${c.name}: $a keeps BR, so it is not a free-slot alg", F2l.isSolved(end))
            // and it is strictly shorter than every normal algorithm (pre-AUF not counted)
            fun core(alg: String): Int {
                val t = Alg.parse(alg).filter { !Alg.isRotation(it) }
                return if (t.isNotEmpty() && t[0][0] == 'U') t.size - 1 else t.size
            }
            assertTrue(c.name, c.algorithms.all { core(a) < core(it) })
        }
        assertEquals(17, count)
    }

    @Test fun picturesMatchTheIndependentPythonModel() {
        for (c in cases) {
            assertEquals(c.name, ExpectedPairs.BY_ID[c.id], F2l.pairKey(c.displayState))
        }
    }

    /** Canonical key of a pair placement modulo U turns (which only move top-layer pieces). */
    private fun canonical(key: Pair<Int, Int>): Pair<Int, Int> {
        val u = Cube.perm("U")
        var (c, e) = key
        var best = key
        repeat(4) {
            if (c < best.first || (c == best.first && e < best.second)) best = c to e
            c = u[c]; e = u[e]
        }
        return best
    }

    @Test fun casesAreDistinctAndCoverEveryPossibleSituation() {
        val cornerCubies = listOf("UFR", "UFL", "UBR", "UBL", "DFR").map { Cube.cubie(it) }
        val edgeCubies = listOf("UF", "UR", "UB", "UL", "FR").map { Cube.cubie(it) }
        val all = HashSet<Pair<Int, Int>>()
        for (cp in cornerCubies) for (cf in Cube.faceletsOf(cp))
            for (ep in edgeCubies) for (ef in Cube.faceletsOf(ep)) all.add(canonical(cf to ef))
        val solvedKey = canonical(F2l.pairKey(CubeState.SOLVED))
        assertEquals(42, all.size) // 41 cases + solved
        val caseKeys = cases.map { canonical(F2l.pairKey(it.displayState)) }
        assertEquals(41, caseKeys.toSet().size)
        assertEquals(all - solvedKey, caseKeys.toSet())
    }

    @Test fun cornerOnTopIsShownAtFrontRight() {
        for (c in cases) {
            val (cf, _) = F2l.pairKey(c.displayState)
            val name = Cube.cubieName(Cube.POS[cf])
            assertTrue(c.name + " " + name, name == "UFR" || name == "DFR")
            assertEquals(c.name, c.group in setOf(CaseGroup.CORNERSLOT, CaseGroup.BOTHSLOT), name == "DFR")
        }
    }

    @Test fun groupsMatchPiecePositions() {
        for (c in cases) {
            val (cf, ef) = F2l.pairKey(c.displayState)
            val cornerInSlot = Cube.cubieName(Cube.POS[cf]) == "DFR"
            val edgeInSlot = Cube.cubieName(Cube.POS[ef]) == "FR"
            val crossUp = cf / 9 == Cube.U
            val expected = when {
                cornerInSlot && edgeInSlot -> CaseGroup.BOTHSLOT
                cornerInSlot -> CaseGroup.CORNERSLOT
                edgeInSlot -> CaseGroup.EDGESLOT
                crossUp -> CaseGroup.CROSSUP
                else -> null
            }
            if (expected != null) assertEquals(c.name, expected, c.group)
            else assertTrue(c.name, c.group == CaseGroup.BASIC || c.group == CaseGroup.SPLIT)
        }
    }

    @Test fun exercisesWithEveryAufAreSolvedByTheAdjustedAlgorithms() {
        for (c in cases) for (auf in 0 until 4) {
            val ex = Exercise(PracticeItem(c.id, false), auf)
            for (a in c.algorithms) assertTrue("${c.name} $auf $a", F2l.isSolved(ex.state.apply(ex.adjust(a))))
            val fa = c.freeSlotAlgorithm ?: continue
            val exf = Exercise(PracticeItem(c.id, true), auf)
            assertTrue("${c.name} $auf $fa", F2l.isSolved(exf.state.apply(exf.adjust(fa)), br))
        }
    }

    @Test fun setupScramblesCreateTheExercise() {
        for (d in Setup.DISGUISES.indices) assertTrue(Setup.DISGUISES[d], Setup.keepsF2l(Setup.DISGUISES[d]))
        for (c in cases) for (auf in 0 until 4) for (d in Setup.DISGUISES.indices) {
            val ex = Exercise(PracticeItem(c.id, false), auf)
            val solving = ex.adjust(c.algorithms[0])
            val scramble = Setup.scramble(solving, d)
            val scrambled = CubeState.SOLVED.apply(scramble)
            // same F2L situation as the picture, and the shown algorithm solves it
            assertEquals("${c.name} $auf $d", F2l.pairKey(ex.state), F2l.pairKey(scrambled))
            assertTrue(F2l.isFrCase(scrambled))
            for (a in c.algorithms) assertTrue(F2l.isSolved(scrambled.apply(ex.adjust(a))))
            // scramble contains no rotations, so it is done holding the cube in standard orientation
            assertTrue(Alg.parse(scramble).all { it[0] in "URFDLB" })
        }
    }

    @Test fun recognitionTextsExist() {
        for (c in cases) {
            assertEquals(3, c.recognition.size)
            assertTrue(c.recognition.none { it.contains("null") })
        }
        // spot checks
        val c1 = Cases.byId(1).recognition
        assertEquals("Corner: top layer, front-right, white faces to the right", c1[0])
        assertEquals("Edge: top layer, back, green on top", c1[1])
        assertEquals("Edge: in the slot, flipped", Cases.byId(37).recognition[1])
        assertEquals("Corner: in the slot, solved", Cases.byId(37).recognition[0])
    }

    @Test fun teachingPhasesCoverTheWholeAlgorithm() {
        for (c in cases) {
            val algs = c.algorithms + listOfNotNull(c.freeSlotAlgorithm)
            for (a in algs) {
                val tokens = Alg.parse(a)
                val ph = Teaching.phases(c.displayState, tokens)
                assertTrue(ph.isNotEmpty())
                assertEquals(0, ph.first().from)
                assertEquals(tokens.size, ph.last().to)
                for (i in 1 until ph.size) assertEquals(ph[i - 1].to, ph[i].from)
                assertTrue(ph.all { it.to > it.from })
                assertEquals("${c.name} $a", Teaching.Kind.INSERT, ph.last().kind)
            }
        }
        // R U R' : pair is formed during "R U", the last move inserts it
        val p = Teaching.phases(Cases.byId(1).displayState, "R U R'")
        assertEquals(listOf(Teaching.Kind.PAIR, Teaching.Kind.INSERT), p.map { it.kind })
        assertNotNull(p)
    }

    @Test fun displayStateDoesNotDependOnWhichAlgorithmIsUsed() {
        // every algorithm (with rotations removed) produces the same F2L situation
        for (c in cases) for (a in c.algorithms) {
            assertEquals(c.name, F2l.pairKey(c.displayState), F2l.pairKey(F2lCase.stateSolvedBy(a)))
        }
    }
}
