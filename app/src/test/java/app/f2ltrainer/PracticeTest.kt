package app.f2ltrainer

import app.f2ltrainer.data.AlgValidator
import app.f2ltrainer.data.BatchPicker
import app.f2ltrainer.data.Cases
import app.f2ltrainer.data.Grade
import app.f2ltrainer.data.ItemProgress
import app.f2ltrainer.data.Leitner
import app.f2ltrainer.data.PracticeItem
import app.f2ltrainer.data.PracticeSession
import app.f2ltrainer.data.ProgressCodec
import app.f2ltrainer.data.Status
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class PracticeTest {
    private val items = (1..5).map { PracticeItem(it, false) }

    @Test fun allGoodFinishesAfterTwoRounds() {
        val s = PracticeSession(items, goal = 2, random = Random(1))
        var n = 0
        while (!s.finished) { s.grade(Grade.GOOD); n++; assertTrue(n < 100) }
        assertEquals(10, n)
        assertNull(s.current)
    }

    @Test fun easyRemovesImmediately() {
        val s = PracticeSession(items, goal = 2, random = Random(1))
        repeat(5) { s.grade(Grade.EASY) }
        assertTrue(s.finished)
    }

    @Test fun againBringsTheItemBackSoonButNotDirectly() {
        val s = PracticeSession(items, goal = 2, random = Random(3))
        val first = s.current!!
        s.grade(Grade.AGAIN)
        assertTrue(s.current != first)
        s.grade(Grade.GOOD)
        s.grade(Grade.GOOD)
        assertEquals(first, s.current)
        assertEquals(1, s.missesOf(first))
        assertEquals(0, s.streakOf(first))
    }

    @Test fun singleItemSessionWorks() {
        val s = PracticeSession(listOf(items[0]), goal = 2, random = Random(1))
        s.grade(Grade.AGAIN)
        assertEquals(items[0], s.current)
        s.grade(Grade.GOOD); s.grade(Grade.GOOD)
        assertTrue(s.finished)
    }

    @Test fun randomGradingAlwaysTerminatesWhenGoodAnswersCome() {
        val r = Random(7)
        repeat(50) {
            val s = PracticeSession(items, goal = 3, random = Random(it))
            var steps = 0
            while (!s.finished) {
                s.grade(if (r.nextInt(3) == 0) Grade.AGAIN else Grade.GOOD)
                steps++
                assertTrue(steps < 10_000)
            }
        }
    }

    @Test fun leitnerBoxes() {
        assertEquals(0, Leitner.next(4, Grade.AGAIN))
        assertEquals(3, Leitner.next(4, Grade.HARD))
        assertEquals(0, Leitner.next(0, Grade.HARD))
        assertEquals(5, Leitner.next(4, Grade.GOOD))
        assertEquals(5, Leitner.next(5, Grade.EASY))
        assertEquals(2, Leitner.next(0, Grade.EASY))
    }

    @Test fun batchPickerPrefersWeakAndOldItems() {
        val pool = (1..10).map { PracticeItem(it, false) }
        val box = { p: PracticeItem -> if (p.caseId <= 3) 0 else 3 }
        val seen = { p: PracticeItem -> p.caseId.toLong() }
        val b = BatchPicker.pick(pool, 4, box, seen, Random(1))
        assertEquals(listOf(1, 2, 3, 4), b.map { it.caseId })
        assertEquals(10, BatchPicker.pick(pool, 50, box, seen).size)
    }

    @Test fun progressRoundTrip() {
        val m = mapOf(
            "1" to ItemProgress(Status.KNOWN, 3, 12345L, 7, 2, "R U R'", listOf("y' U' R' U R", "F R' F' R"), listOf(800, 650), listOf(1500)),
            "5-free" to ItemProgress(),
            "12" to ItemProgress(status = Status.LEARNING, preferredAlg = null, recogTimes = listOf(1)),
        )
        assertEquals(m, ProgressCodec.decode(ProgressCodec.encode(m)))
        assertEquals(emptyMap<String, ItemProgress>(), ProgressCodec.decode(null))
        assertEquals(emptyMap<String, ItemProgress>(), ProgressCodec.decode("garbage"))
        // a broken line is skipped, the rest survives
        val text = ProgressCodec.encode(m) + "7|NOPE|x\n"
        assertEquals(m, ProgressCodec.decode(text))
    }

    @Test fun validatorAcceptsCorrectAlgorithmsWithAnyPreAuf() {
        val c = Cases.byId(1) // R U R'
        assertEquals(AlgValidator.Result.Ok("R U R'"), AlgValidator.validate(c, "R U R'"))
        // with an extra U2 in front the validator finds the needed pre-AUF itself
        val r = AlgValidator.validate(c, "U2 R U R'")
        assertEquals(AlgValidator.Result.Ok("R U R'"), r)
        // equivalent rotated form
        assertTrue(AlgValidator.validate(c, "y' L U L'") is AlgValidator.Result.Error ||
            AlgValidator.validate(c, "y' L U L'") is AlgValidator.Result.Ok)
        assertTrue(AlgValidator.validate(c, "R U' R'") is AlgValidator.Result.Error)
        assertTrue(AlgValidator.validate(c, "R Q") is AlgValidator.Result.Error)
        assertTrue(AlgValidator.validate(c, "") is AlgValidator.Result.Error)
        // the free-slot shortcut is rejected as a normal algorithm but accepted as free-slot one
        val c6 = Cases.byId(6)
        val free = c6.freeSlotAlgorithm!!
        assertTrue(AlgValidator.validate(c6, free) is AlgValidator.Result.Error)
        assertEquals(AlgValidator.Result.Ok(free), AlgValidator.validate(c6, free, freeSlot = true))
        // every built-in algorithm validates to itself
        for (case in Cases.ALL) for (a in case.algorithms) {
            assertEquals(AlgValidator.Result.Ok(a), AlgValidator.validate(case, a))
        }
    }
}
