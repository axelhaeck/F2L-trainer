package app.f2ltrainer.data

import app.f2ltrainer.cube.Alg
import app.f2ltrainer.cube.CubeState
import app.f2ltrainer.cube.F2l
import kotlin.random.Random

enum class Status(val label: String) { NEW("New"), LEARNING("Learning"), KNOWN("Known") }

enum class Grade(val label: String) { AGAIN("Again"), HARD("Hard"), GOOD("Good"), EASY("Easy") }

/** One thing to practise: a case, optionally its free back-slot variant. */
data class PracticeItem(val caseId: Int, val freeSlot: Boolean) {
    val case: F2lCase get() = Cases.byId(caseId)
    val key: String get() = if (freeSlot) "$caseId-free" else "$caseId"
}

/** A concrete exercise: an item shown with a specific U-layer angle (AUF). */
data class Exercise(val item: PracticeItem, val auf: Int) {
    /** The state to recognise: the case picture turned by [auf] quarter turns of U. */
    val state: CubeState get() = item.case.displayState.apply(List(auf) { "U" })

    /** An algorithm for the case, adjusted so that it solves [state] (pre-AUF merged in). */
    fun adjust(alg: String): String = Alg.format(Alg.prefixU(4 - auf, Alg.parse(alg)))
}

/** Leitner-style memory strength 0..5 per item. */
object Leitner {
    const val MAX = 5
    fun next(box: Int, grade: Grade): Int = when (grade) {
        Grade.AGAIN -> 0
        Grade.HARD -> maxOf(box - 1, 0)
        Grade.GOOD -> minOf(box + 1, MAX)
        Grade.EASY -> minOf(box + 2, MAX)
    }
}

/**
 * A practice batch: every item must be answered "Good"/"Easy" [goal] times in a row (Easy counts
 * as fully done) before it leaves the batch; "Again" and "Hard" bring it back soon.
 */
class PracticeSession(items: List<PracticeItem>, private val goal: Int = 2, private val random: Random = Random.Default) {
    init { require(items.isNotEmpty()) }

    val items: List<PracticeItem> = items.distinct()
    private val queue = ArrayDeque(this.items.shuffled(random))
    private val streak = HashMap<PracticeItem, Int>()
    private val misses = HashMap<PracticeItem, Int>()
    var answered = 0
        private set

    val finished get() = queue.isEmpty()
    val current: PracticeItem? get() = queue.firstOrNull()
    val remaining: Int get() = queue.distinct().size
    fun streakOf(item: PracticeItem) = streak[item] ?: 0
    fun missesOf(item: PracticeItem) = misses[item] ?: 0
    val goalStreak get() = goal

    fun grade(g: Grade) {
        val item = queue.removeFirstOrNull() ?: return
        answered++
        when (g) {
            Grade.AGAIN, Grade.HARD -> {
                streak[item] = 0
                misses[item] = missesOf(item) + 1
                // bring it back soon, but not immediately (unless it is the only item left)
                val pos = if (g == Grade.AGAIN) 2 else 3
                queue.add(minOf(pos, queue.size), item)
            }
            Grade.GOOD -> {
                val s = streakOf(item) + 1
                streak[item] = s
                if (s < goal) {
                    // re-insert at a random later position, never directly next
                    val lo = minOf(2, queue.size)
                    val hi = queue.size
                    queue.add(if (hi > lo) random.nextInt(lo, hi + 1) else hi, item)
                }
            }
            Grade.EASY -> streak[item] = goal
        }
    }
}

object BatchPicker {
    /**
     * Choose up to [size] items from [pool]: weakest first (lowest Leitner box), then the ones not
     * seen for the longest time; ties are broken randomly.
     */
    fun pick(pool: List<PracticeItem>, size: Int, boxOf: (PracticeItem) -> Int, lastSeenOf: (PracticeItem) -> Long, random: Random = Random.Default): List<PracticeItem> {
        val shuffled = pool.distinct().shuffled(random)
        return shuffled.sortedWith(compareBy({ boxOf(it) }, { lastSeenOf(it) })).take(size)
    }
}

/** Setup scramble that creates a case on a solved cube (applied to solved -> case state). */
object Setup {
    /** Last-layer algorithms used to disguise the setup; they keep the first two layers intact. */
    val DISGUISES = listOf(
        "",
        "R U R' U R U2 R'",
        "R U2 R' U' R U' R'",
        "F R U R' U' F'",
        "R U R' U' R' F R2 U' R' U' R U R' F'",
        "R U R' F' R U R' U' R' F R2 U' R'",
        "F R U' R' U' R U R' F' R U R' U' R' F R F'",
    )

    /**
     * @param solvingAlg an algorithm that solves the exercise state
     * @param disguise index into [DISGUISES]
     */
    fun scramble(solvingAlg: String, disguise: Int): String {
        val (faces, _) = Alg.toFaceMoves(Alg.parse(solvingAlg))
        val pre = Alg.parse(DISGUISES[disguise])
        return Alg.format(Alg.simplify(pre + Alg.invert(faces)))
    }

    fun keepsF2l(alg: String): Boolean = F2l.isSolved(CubeState.SOLVED.apply(alg))
}
