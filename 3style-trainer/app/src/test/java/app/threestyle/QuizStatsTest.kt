package app.threestyle

import app.threestyle.data.Backup
import app.threestyle.data.QuizMode
import app.threestyle.data.QuizStats
import app.threestyle.data.Settings
import app.threestyle.data.StickerKnowledge
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class QuizStatsTest {
    @Test fun levels() {
        val limit = 2000L
        var k = StickerKnowledge()
        k = k.answered(true, 1000, limit); assertEquals(1, k.level)
        k = k.answered(true, 1500, limit); assertEquals(2, k.level)
        k = k.answered(true, 5000, limit); assertEquals(1, k.level)  // correct but slow: not learned yet
        repeat(10) { k = k.answered(true, 800, limit) }
        assertEquals(StickerKnowledge.MAX, k.level)
        assertTrue(k.mastered)
        k = k.answered(false, 900, limit); assertEquals(3, k.level)
        assertEquals(1, k.misses)
        k = StickerKnowledge().answered(false, 100, limit); assertEquals(0, k.level)
        k = StickerKnowledge().answered(true, 9000, limit); assertEquals(0, k.level)
    }

    @Test fun slowLimitIsRelativeToTheUsersOwnTime() {
        fun stats(vararg t: Long) = mapOf(
            "T:UFR" to StickerKnowledge(times = t.toList()),
            "L:UFR" to StickerKnowledge(times = listOf(9000L, 9000L, 9000L, 9000L, 9000L, 9000L, 9000L, 9000L, 9000L, 9000L)),
        )
        // too few answers: generous start limit
        assertEquals(QuizStats.START_LIMIT_MS, QuizStats.slowLimit(stats(1000, 1000), QuizMode.STICKER_TO_LETTER))
        // median 1000 ms -> 1300 ms; the other mode does not count
        assertEquals(1300L, QuizStats.slowLimit(stats(900, 950, 1000, 1000, 1000, 1000, 1000, 1100, 1200, 8000), QuizMode.STICKER_TO_LETTER))
        // faster user -> stricter limit (the bar rises)
        assertEquals(780L, QuizStats.slowLimit(stats(500, 550, 600, 600, 600, 600, 600, 650, 700, 4000), QuizMode.STICKER_TO_LETTER))
        // never below the minimum
        assertEquals(QuizStats.MIN_LIMIT_MS, QuizStats.slowLimit(stats(100, 100, 100, 100, 100, 100, 100, 100, 100, 100), QuizMode.STICKER_TO_LETTER))
        assertEquals(11700L, QuizStats.slowLimit(stats(), QuizMode.LETTER_TO_STICKER))
    }

    @Test fun weakStickersAreAskedMoreOften() {
        val pool = (0 until 24).toList()
        // stickers 0..11 mastered, 12..23 unknown
        val know = pool.associateWith { if (it < 12) StickerKnowledge(level = 5) else StickerKnowledge() }
        val rnd = Random(1)
        var prev: Int? = null
        var weak = 0
        repeat(10_000) {
            val p = QuizStats.pick(pool, { know.getValue(it) }, prev, rnd)
            assertNotEquals(prev, p)  // never the same sticker twice in a row
            if (p >= 12) weak++
            prev = p
        }
        // expected share 32 / 33 = 97 %
        assertTrue("weak share $weak", weak in 9_500..9_900)
        // every sticker is still asked sometimes
        val seen = HashSet<Int>()
        repeat(5_000) { seen.add(QuizStats.pick(pool, { know.getValue(it) }, null, rnd)) }
        assertEquals(24, seen.size)
    }

    @Test fun codecAndBackupRoundTrip() {
        val m = mapOf(
            "T:UFR" to StickerKnowledge(3, 7, 2, listOf(900L, 1200L)),
            "L:DB" to StickerKnowledge(0, 1, 1, emptyList()),
        )
        assertEquals(m, QuizStats.decode(QuizStats.encode(m)))
        assertEquals(emptyMap<String, StickerKnowledge>(), QuizStats.decode("garbage\nmore"))
        val c = Backup.decode(Backup.encode(Settings(), emptyMap(), mapOf("AB" to "Abba"), m))!!
        assertEquals(m, c.quiz)
        assertEquals(mapOf("AB" to "Abba"), c.words)
    }
}
