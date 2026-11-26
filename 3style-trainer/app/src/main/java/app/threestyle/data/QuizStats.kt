package app.threestyle.data

import kotlin.random.Random

/** Direction of the letter quiz. */
enum class QuizMode(val code: String) { STICKER_TO_LETTER("T"), LETTER_TO_STICKER("L") }

/**
 * How well the user knows one sticker in one quiz direction.
 * [level] 0..[MAX]: +1 for a correct and quick answer, unchanged for a correct but slow one,
 * -2 for a wrong one.
 */
data class StickerKnowledge(
    val level: Int = 0,
    val attempts: Int = 0,
    val misses: Int = 0,
    /** Recent answer times of correct answers (ms). */
    val times: List<Long> = emptyList(),
) {
    val avgTime: Long? get() = if (times.isEmpty()) null else times.sum() / times.size
    val mastered: Boolean get() = level >= MAX

    fun answered(correct: Boolean, timeMs: Long): StickerKnowledge = if (correct) {
        copy(
            level = if (timeMs <= SLOW_MS) minOf(level + 1, MAX) else level,
            attempts = attempts + 1,
            times = (times + timeMs).takeLast(KEEP_TIMES),
        )
    } else {
        copy(level = maxOf(level - 2, 0), attempts = attempts + 1, misses = misses + 1)
    }

    companion object {
        const val MAX = 5
        /** Answers slower than this count as correct but not yet fluent. */
        const val SLOW_MS = 3000L
        const val KEEP_TIMES = 8
    }
}

/** Knowledge of all stickers, keyed by mode and sticker name. */
object QuizStats {
    fun key(mode: QuizMode, stickerName: String) = "${mode.code}:$stickerName"

    /**
     * Weight of a sticker when the next question is chosen: unknown stickers (level 0) are asked
     * 32 times as often as mastered ones (level 5); each level halves the weight.
     */
    fun weight(k: StickerKnowledge): Int = 1 shl (StickerKnowledge.MAX - k.level)

    /** Picks the next sticker by weight, never [previous] (if there is another choice). */
    fun <T> pick(pool: List<T>, knowledgeOf: (T) -> StickerKnowledge, previous: T?, random: Random): T {
        val candidates = if (pool.size > 1) pool.filter { it != previous } else pool
        val weights = candidates.map { weight(knowledgeOf(it)) }
        var r = random.nextInt(weights.sum())
        for ((i, w) in weights.withIndex()) {
            if (r < w) return candidates[i]
            r -= w
        }
        return candidates.last()
    }

    fun encode(map: Map<String, StickerKnowledge>): String =
        map.toSortedMap().map { (k, v) -> "$k\t${v.level}\t${v.attempts}\t${v.misses}\t${v.times.joinToString(",")}" }
            .joinToString("\n")

    fun decode(text: String?): Map<String, StickerKnowledge> {
        if (text.isNullOrBlank()) return emptyMap()
        return text.split('\n').mapNotNull { line ->
            val f = line.split('\t')
            if (f.size != 5) return@mapNotNull null
            try {
                f[0] to StickerKnowledge(
                    level = f[1].toInt().coerceIn(0, StickerKnowledge.MAX),
                    attempts = f[2].toInt(),
                    misses = f[3].toInt(),
                    times = if (f[4].isEmpty()) emptyList() else f[4].split(',').map { it.toLong() },
                )
            } catch (_: NumberFormatException) {
                null
            }
        }.toMap()
    }
}
