package app.threestyle.data

import app.threestyle.bld.CaseType
import app.threestyle.bld.LetterScheme

/** Learning state of one case. */
data class ItemProgress(
    val status: Status = Status.NEW,
    val box: Int = 0,
    val lastSeen: Long = 0L,
    val attempts: Int = 0,
    val misses: Int = 0,
    /** Algorithm the user chose (null = the first built-in algorithm). */
    val preferredAlg: String? = null,
    /** Algorithms the user added (verified when added). */
    val customAlgs: List<String> = emptyList(),
    /** Most recent recall times in ms (practice without cube). */
    val recallTimes: List<Long> = emptyList(),
    /** Most recent execution times in ms (practice with cube). */
    val execTimes: List<Long> = emptyList(),
) {
    val avgRecall: Long? get() = if (recallTimes.isEmpty()) null else recallTimes.sum() / recallTimes.size
    val avgExec: Long? get() = if (execTimes.isEmpty()) null else execTimes.sum() / execTimes.size

    companion object {
        const val KEEP_TIMES = 12
    }
}

/** Line based text format for progress: robust against partial corruption (bad lines are skipped). */
object ProgressCodec {
    private const val HEADER = "3style-progress-v1"
    private const val LIST_SEP = '\u001F'

    /** Algorithms are stored on one line: tabs, new lines and the list separator are not allowed. */
    fun cleanAlg(alg: String): String = alg.replace(Regex("[\\s\u001F]+"), " ").trim()

    fun encodeLine(key: String, p: ItemProgress): String = listOf(
        key, p.status.name, p.box.toString(), p.lastSeen.toString(), p.attempts.toString(), p.misses.toString(),
        p.preferredAlg?.let { cleanAlg(it) } ?: "",
        p.customAlgs.joinToString(LIST_SEP.toString()) { cleanAlg(it) },
        p.recallTimes.joinToString(","), p.execTimes.joinToString(","),
    ).joinToString("\t")

    fun decodeLine(line: String): Pair<String, ItemProgress>? {
        val f = line.split('\t')
        if (f.size != 10 || f[0].isEmpty()) return null
        return try {
            f[0] to ItemProgress(
                status = Status.valueOf(f[1]),
                box = f[2].toInt().coerceIn(0, Leitner.MAX),
                lastSeen = f[3].toLong(),
                attempts = f[4].toInt(),
                misses = f[5].toInt(),
                preferredAlg = f[6].ifEmpty { null },
                customAlgs = if (f[7].isEmpty()) emptyList() else f[7].split(LIST_SEP),
                recallTimes = if (f[8].isEmpty()) emptyList() else f[8].split(',').map { it.toLong() },
                execTimes = if (f[9].isEmpty()) emptyList() else f[9].split(',').map { it.toLong() },
            )
        } catch (_: Exception) {
            null
        }
    }

    fun encode(map: Map<String, ItemProgress>): String =
        (listOf(HEADER) + map.toSortedMap().map { (k, v) -> encodeLine(k, v) }).joinToString("\n")

    fun decode(text: String?): Map<String, ItemProgress> {
        if (text.isNullOrBlank()) return emptyMap()
        val lines = text.split('\n')
        if (lines.first().trim() != HEADER) return emptyMap()
        return lines.drop(1).mapNotNull { decodeLine(it) }.toMap()
    }
}

/** Letter pair -> the user's word / image for it. */
object WordsCodec {
    fun encode(words: Map<String, String>): String =
        words.toSortedMap().filter { it.value.isNotBlank() }
            .map { (k, v) -> "$k\t${v.replace(Regex("[\t\n\r]+"), " ").trim()}" }.joinToString("\n")

    fun decode(text: String?): Map<String, String> {
        if (text.isNullOrBlank()) return emptyMap()
        return text.split('\n').mapNotNull { line ->
            val i = line.indexOf('\t')
            if (i <= 0) null else line.substring(0, i) to line.substring(i + 1)
        }.filter { it.second.isNotBlank() }.toMap()
    }
}

enum class PracticeMode(val label: String, val hint: String) {
    RECALL("Recall", "Say the commutator to yourself, then reveal it."),
    EXECUTE("Execute on a cube", "Do the algorithm on a solved cube (or after its inverse), then tap."),
}

data class PracticeConfig(
    val types: Set<CaseType> = setOf(CaseType.CORNER, CaseType.EDGE),
    val statuses: Set<Status> = setOf(Status.NEW, Status.LEARNING),
    /** Only these sets (ids); empty = all sets of the chosen types. */
    val sets: Set<String> = emptySet(),
    val batchSize: Int = 6,
    val goal: Int = 2,
    val mode: PracticeMode = PracticeMode.RECALL,
    val showPicture: Boolean = false,
)

/** Cube colours: index = colour of that face in the standard scheme (white top, green front). */
object Colours {
    val NAMES = listOf("White", "Red", "Green", "Yellow", "Orange", "Blue")
}

data class Settings(
    val cornerBuffer: String = "UFR",
    val edgeBuffer: String = "UF",
    val parityEdge: String = "UR",
    val schemeCorners: String = LetterScheme.SPEFFZ,
    val schemeEdges: String = LetterScheme.SPEFFZ,
    /** Colour (index into [Colours.NAMES]) on top and in front. */
    val topColour: Int = 0,
    val frontColour: Int = 2,
    /** Mark a case as known automatically when its strength reaches the maximum. */
    val autoKnown: Boolean = true,
) {
    val scheme: LetterScheme
        get() = if (LetterScheme.isValid(schemeCorners) && LetterScheme.isValid(schemeEdges))
            LetterScheme(schemeCorners, schemeEdges) else LetterScheme()

    fun toMap(): Map<String, String> = mapOf(
        "cornerBuffer" to cornerBuffer, "edgeBuffer" to edgeBuffer, "parityEdge" to parityEdge,
        "schemeCorners" to schemeCorners, "schemeEdges" to schemeEdges,
        "topColour" to topColour.toString(), "frontColour" to frontColour.toString(),
        "autoKnown" to autoKnown.toString(),
    )

    companion object {
        /** Lenient: unknown or broken values keep their defaults. */
        fun fromMap(m: Map<String, String>): Settings {
            val d = Settings()
            fun int(k: String, def: Int) = m[k]?.toIntOrNull()?.takeIf { it in 0..5 } ?: def
            return Settings(
                cornerBuffer = m["cornerBuffer"] ?: d.cornerBuffer,
                edgeBuffer = m["edgeBuffer"] ?: d.edgeBuffer,
                parityEdge = m["parityEdge"] ?: d.parityEdge,
                schemeCorners = m["schemeCorners"]?.takeIf { LetterScheme.isValid(it) } ?: d.schemeCorners,
                schemeEdges = m["schemeEdges"]?.takeIf { LetterScheme.isValid(it) } ?: d.schemeEdges,
                topColour = int("topColour", d.topColour),
                frontColour = int("frontColour", d.frontColour),
                autoKnown = m["autoKnown"]?.toBooleanStrictOrNull() ?: d.autoKnown,
            )
        }
    }
}

/** Backup file: settings, progress (incl. own algorithms) and letter pair words. */
object Backup {
    private const val HEADER = "3style-trainer-backup-v1"

    fun encode(settings: Settings, progress: Map<String, ItemProgress>, words: Map<String, String>): String {
        val sb = StringBuilder(HEADER).append('\n')
        sb.append("[settings]\n")
        for ((k, v) in settings.toMap()) sb.append(k).append('=').append(v).append('\n')
        sb.append("[progress]\n")
        for ((k, v) in progress.toSortedMap()) sb.append(ProgressCodec.encodeLine(k, v)).append('\n')
        sb.append("[words]\n")
        sb.append(WordsCodec.encode(words)).append('\n')
        return sb.toString()
    }

    data class Content(val settings: Settings, val progress: Map<String, ItemProgress>, val words: Map<String, String>)

    /** @return null if the text is not a backup of this app. */
    fun decode(text: String): Content? {
        val lines = text.replace("\r\n", "\n").split('\n')
        if (lines.firstOrNull()?.trim() != HEADER) return null
        var section = ""
        val settings = HashMap<String, String>()
        val progress = HashMap<String, ItemProgress>()
        val words = StringBuilder()
        for (line in lines.drop(1)) {
            if (line.startsWith("[") && line.endsWith("]")) { section = line; continue }
            if (line.isBlank()) continue
            when (section) {
                "[settings]" -> line.indexOf('=').takeIf { it > 0 }?.let { settings[line.substring(0, it)] = line.substring(it + 1) }
                "[progress]" -> ProgressCodec.decodeLine(line)?.let { progress[it.first] = it.second }
                "[words]" -> words.append(line).append('\n')
            }
        }
        return Content(Settings.fromMap(settings), progress, WordsCodec.decode(words.toString()))
    }
}
