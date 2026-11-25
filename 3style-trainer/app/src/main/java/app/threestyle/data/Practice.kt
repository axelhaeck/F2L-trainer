package app.threestyle.data

import kotlin.random.Random

enum class Status(val label: String) { NEW("New"), LEARNING("Learning"), KNOWN("Known") }

enum class Grade(val label: String) { AGAIN("Again"), HARD("Hard"), GOOD("Good"), EASY("Easy") }

/** Leitner-style memory strength 0..5 per case. */
object Leitner {
    const val MAX = 5
    fun next(box: Int, grade: Grade): Int = when (grade) {
        Grade.AGAIN -> 0
        Grade.HARD -> maxOf(box - 1, 0)
        Grade.GOOD -> minOf(box + 1, MAX)
        Grade.EASY -> minOf(box + 2, MAX)
    }

    /**
     * Status after grading: practising a new case makes it "Learning"; reaching full strength makes
     * it "Known" (if [autoKnown]); forgetting a known case ("Again") puts it back to "Learning".
     */
    fun nextStatus(status: Status, newBox: Int, grade: Grade, autoKnown: Boolean): Status = when {
        status == Status.KNOWN && grade == Grade.AGAIN -> Status.LEARNING
        status != Status.KNOWN && autoKnown && newBox >= MAX -> Status.KNOWN
        status == Status.NEW -> Status.LEARNING
        else -> status
    }
}

/**
 * A practice batch of case keys: every case must be answered "Good"/"Easy" [goal] times in a row
 * (Easy counts as done at once) before it leaves the batch; "Again" and "Hard" bring it back soon.
 */
class PracticeSession(items: List<String>, private val goal: Int = 2, private val random: Random = Random.Default) {
    init { require(items.isNotEmpty()) }

    val items: List<String> = items.distinct()
    private val queue = ArrayDeque(this.items.shuffled(random))
    private val streak = HashMap<String, Int>()
    private val misses = HashMap<String, Int>()
    var answered = 0
        private set

    val finished get() = queue.isEmpty()
    val current: String? get() = queue.firstOrNull()
    val remaining: Int get() = queue.distinct().size
    fun streakOf(item: String) = streak[item] ?: 0
    fun missesOf(item: String) = misses[item] ?: 0
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
     * Choose up to [size] cases from [pool]: weakest first (lowest Leitner box), then the ones not
     * seen for the longest time; ties are broken randomly. A case and its inverse are kept together
     * when [inverseOf] is given and both are in the pool (learning them together helps).
     */
    fun pick(
        pool: List<String>,
        size: Int,
        boxOf: (String) -> Int,
        lastSeenOf: (String) -> Long,
        random: Random = Random.Default,
        inverseOf: (String) -> String? = { null },
    ): List<String> {
        val inPool = pool.toSet()
        val sorted = pool.distinct().shuffled(random).sortedWith(compareBy({ boxOf(it) }, { lastSeenOf(it) }))
        val out = LinkedHashSet<String>()
        for (k in sorted) {
            if (out.size >= size) break
            out.add(k)
            val inv = inverseOf(k)
            if (inv != null && inv in inPool && out.size < size) out.add(inv)
        }
        return out.toList()
    }
}
