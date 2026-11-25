package app.f2ltrainer.data

/** Learning state of one practice item (a case or its free-slot variant). */
data class ItemProgress(
    val status: Status = Status.NEW,
    val box: Int = 0,
    val lastSeen: Long = 0L,
    val attempts: Int = 0,
    val misses: Int = 0,
    /** Algorithm the user chose to use (null = the main algorithm). */
    val preferredAlg: String? = null,
    /** Algorithms the user added (verified when added). */
    val customAlgs: List<String> = emptyList(),
    /** Most recent recognition times in ms (flashcard mode). */
    val recogTimes: List<Long> = emptyList(),
    /** Most recent solve times in ms (timed mode with a real cube). */
    val solveTimes: List<Long> = emptyList(),
) {
    val avgRecog: Long? get() = if (recogTimes.isEmpty()) null else recogTimes.sum() / recogTimes.size
    val avgSolve: Long? get() = if (solveTimes.isEmpty()) null else solveTimes.sum() / solveTimes.size

    companion object {
        const val KEEP_TIMES = 12
    }
}

/** Serialisation of all progress into a simple, robust line based text format. */
object ProgressCodec {
    private const val HEADER = "f2ltrainer-progress-v1"

    fun encode(map: Map<String, ItemProgress>): String {
        val sb = StringBuilder(HEADER).append('\n')
        for ((key, p) in map.toSortedMap()) {
            sb.append(key).append('|')
                .append(p.status.name).append('|')
                .append(p.box).append('|')
                .append(p.lastSeen).append('|')
                .append(p.attempts).append('|')
                .append(p.misses).append('|')
                .append(p.preferredAlg ?: "").append('|')
                .append(p.customAlgs.joinToString(";")).append('|')
                .append(p.recogTimes.joinToString(",")).append('|')
                .append(p.solveTimes.joinToString(","))
                .append('\n')
        }
        return sb.toString()
    }

    /** Lenient decoding: malformed lines are skipped instead of failing. */
    fun decode(text: String?): Map<String, ItemProgress> {
        if (text.isNullOrBlank()) return emptyMap()
        val lines = text.split('\n')
        if (lines.first().trim() != HEADER) return emptyMap()
        val out = HashMap<String, ItemProgress>()
        for (line in lines.drop(1)) {
            if (line.isBlank()) continue
            val f = line.split('|')
            if (f.size != 10) continue
            try {
                out[f[0]] = ItemProgress(
                    status = Status.valueOf(f[1]),
                    box = f[2].toInt().coerceIn(0, Leitner.MAX),
                    lastSeen = f[3].toLong(),
                    attempts = f[4].toInt(),
                    misses = f[5].toInt(),
                    preferredAlg = f[6].ifEmpty { null },
                    customAlgs = if (f[7].isEmpty()) emptyList() else f[7].split(';'),
                    recogTimes = if (f[8].isEmpty()) emptyList() else f[8].split(',').map { it.toLong() },
                    solveTimes = if (f[9].isEmpty()) emptyList() else f[9].split(',').map { it.toLong() },
                )
            } catch (_: Exception) {
                // skip the broken line
            }
        }
        return out
    }
}
