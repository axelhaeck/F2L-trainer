package app.threestyle.ui

import android.app.Application
import android.content.Context
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.threestyle.bld.BldCase
import app.threestyle.bld.BufferConfig
import app.threestyle.bld.CaseType
import app.threestyle.bld.Library
import app.threestyle.bld.PieceType
import app.threestyle.bld.Stickers
import app.threestyle.data.Backup
import app.threestyle.data.BatchPicker
import app.threestyle.data.Grade
import app.threestyle.data.ItemProgress
import app.threestyle.data.Leitner
import app.threestyle.data.PracticeConfig
import app.threestyle.data.PracticeMode
import app.threestyle.data.PracticeSession
import app.threestyle.data.ProgressCodec
import app.threestyle.data.QuizMode
import app.threestyle.data.QuizStats
import app.threestyle.data.StickerKnowledge
import app.threestyle.data.Settings
import app.threestyle.data.Status
import app.threestyle.data.WordsCodec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlin.random.Random

enum class Tab(val label: String, val icon: String) {
    LEARN("Learn", "📖"), PRACTICE("Practice", "⏱"), LETTERS("Letters", "🔤"), PROGRESS("Progress", "📈"), SETTINGS("Settings", "⚙")
}

sealed interface Screen {
    data class Home(val tab: Tab) : Screen
    data class SetDetail(val setId: String) : Screen
    data class CaseDetail(val key: String) : Screen
    data object Session : Screen
    data class LetterQuiz(val toLetter: Boolean, val types: Set<PieceType>) : Screen
    data object PairWords : Screen
    data object PairDrill : Screen
    data object Memo : Screen
    data object Guide : Screen
}

/** UI state of a running practice batch. */
class SessionUi(val session: PracticeSession, val config: PracticeConfig, val title: String) {
    var current by mutableStateOf<String?>(null)
    var revealed by mutableStateOf(false)
    var shownAt by mutableStateOf(0L)
    var timeMs by mutableStateOf<Long?>(null)
    var version by mutableStateOf(0)

    fun next() {
        current = session.current
        revealed = false
        timeMs = null
        shownAt = SystemClock.elapsedRealtime()
        version++
    }
}

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = app.getSharedPreferences("threestyle", Context.MODE_PRIVATE)

    private val readAsset: (String) -> String? = { name ->
        try {
            app.assets.open(name).bufferedReader().use { it.readText() }
        } catch (_: IOException) {
            null
        }
    }

    val stack = mutableStateListOf<Screen>(Screen.Home(Tab.LEARN))
    val current: Screen get() = stack.last()

    var settings by mutableStateOf(Settings.fromMap(loadMap("settings")))
        private set

    /** All cases for the current buffers; null while loading. */
    var library by mutableStateOf<Library?>(null)
        private set

    var progress by mutableStateOf(ProgressCodec.decode(prefs.getString(KEY_PROGRESS, null)))
        private set

    var words by mutableStateOf(WordsCodec.decode(prefs.getString(KEY_WORDS, null)))
        private set

    /** Letter quiz: how well each sticker is known (per quiz direction). */
    var quizStats by mutableStateOf(QuizStats.decode(prefs.getString(KEY_QUIZ, null)))
        private set

    var practiceConfig by mutableStateOf(loadPracticeConfig())
        private set

    var session by mutableStateOf<SessionUi?>(null)
        private set

    /** Case type shown in the Learn tab. */
    var learnType by mutableStateOf(CaseType.CORNER)

    init { rebuildLibrary() }

    // ---------------------------------------------------------------- buffers & library
    val bufferConfig: BufferConfig get() = configOf(settings)

    private fun configOf(s: Settings): BufferConfig {
        val d = BufferConfig()
        val cb = Stickers.byNameOrNull(s.cornerBuffer)?.takeIf { Stickers.typeOf(it) == PieceType.CORNER && Stickers.isReference(it) }
            ?: d.cornerBuffer
        val eb = Stickers.byNameOrNull(s.edgeBuffer)?.takeIf { Stickers.typeOf(it) == PieceType.EDGE && Stickers.isReference(it) }
            ?: d.edgeBuffer
        val pe = Stickers.byNameOrNull(s.parityEdge)?.takeIf { Stickers.typeOf(it) == PieceType.EDGE && !Stickers.samePiece(it, eb) }
            ?: BufferConfig.defaultParityEdge(eb)
        return BufferConfig(cb, eb, pe)
    }

    private fun rebuildLibrary() {
        val cfg = bufferConfig
        if (library?.config == cfg) return
        library = null
        viewModelScope.launch {
            val lib = withContext(Dispatchers.Default) {
                synchronized(cache) { cache[cfg] } ?: Library(cfg, readAsset).also { synchronized(cache) { cache[cfg] = it } }
            }
            if (bufferConfig == cfg) library = lib
        }
    }

    fun updateSettings(s: Settings) {
        settings = s
        saveMap("settings", s.toMap())
        rebuildLibrary()
    }

    // ---------------------------------------------------------------- navigation
    fun open(screen: Screen) { stack.add(screen) }

    fun selectTab(tab: Tab) { stack.clear(); stack.add(Screen.Home(tab)) }

    /** @return false if there is nothing to go back to */
    fun back(): Boolean {
        if (stack.size <= 1) return false
        val top = stack.removeAt(stack.size - 1)
        if (top == Screen.Session) session = null
        return true
    }

    // ---------------------------------------------------------------- progress
    fun progressOf(key: String): ItemProgress = progress[key] ?: ItemProgress()

    private fun update(key: String, f: (ItemProgress) -> ItemProgress) {
        progress = progress + (key to f(progressOf(key)))
        saveProgress()
    }

    private fun saveProgress() { prefs.edit().putString(KEY_PROGRESS, ProgressCodec.encode(progress)).apply() }

    fun setStatus(key: String, status: Status) = update(key) { it.copy(status = status) }

    fun setStatus(keys: Collection<String>, status: Status) {
        progress = progress + keys.associateWith { progressOf(it).copy(status = status) }
        saveProgress()
    }

    /** Algorithms for a case: built-in ones first, then the user's own (notation strings). */
    fun algorithmsOf(case: BldCase): List<String> {
        val builtIn = case.solutions.map { it.notation }
        return builtIn + progressOf(case.key).customAlgs.filter { it !in builtIn }
    }

    fun preferredAlgOf(case: BldCase): String? {
        val all = algorithmsOf(case)
        val p = progressOf(case.key).preferredAlg
        return if (p != null && p in all) p else all.firstOrNull()
    }

    fun setPreferred(key: String, alg: String) = update(key) { it.copy(preferredAlg = alg) }

    /** Adds an own algorithm after checking it. @return an error message or null if it was added. */
    fun addCustom(case: BldCase, alg: String): String? {
        val clean = ProgressCodec.cleanAlg(alg)
        if (clean.isEmpty()) return "Enter an algorithm."
        val error = try {
            app.threestyle.cube.Alg.parse(clean); null
        } catch (e: IllegalArgumentException) {
            e.message ?: "Cannot read this algorithm."
        }
        if (error != null) return error
        if (!case.isSolvedBy(clean)) return "This algorithm does not solve the case."
        update(case.key) {
            val customs = if (clean in it.customAlgs) it.customAlgs else it.customAlgs + clean
            it.copy(customAlgs = customs, preferredAlg = clean)
        }
        return null
    }

    fun removeCustom(key: String, alg: String) = update(key) {
        it.copy(customAlgs = it.customAlgs - alg, preferredAlg = if (it.preferredAlg == alg) null else it.preferredAlg)
    }

    fun resetProgress() {
        // keeps the user's own algorithms and choices, resets the learning state
        progress = progress.mapValues { (_, p) -> ItemProgress(preferredAlg = p.preferredAlg, customAlgs = p.customAlgs) }
            .filterValues { it.customAlgs.isNotEmpty() || it.preferredAlg != null }
        saveProgress()
    }

    // ---------------------------------------------------------------- letter pair words
    fun setWord(pair: String, word: String) {
        words = if (word.isBlank()) words - pair else words + (pair to word)
        prefs.edit().putString(KEY_WORDS, WordsCodec.encode(words)).apply()
    }

    // ---------------------------------------------------------------- letter quiz
    fun knowledge(mode: QuizMode, sticker: Int): StickerKnowledge =
        quizStats[QuizStats.key(mode, Stickers.stickerName(sticker))] ?: StickerKnowledge()

    fun slowLimit(mode: QuizMode): Long = QuizStats.slowLimit(quizStats, mode)

    /** Records an answer; @return true if it was correct but slow (relative to the user's usual time). */
    fun quizAnswer(mode: QuizMode, sticker: Int, correct: Boolean, timeMs: Long): Boolean {
        val limit = slowLimit(mode)  // measured before this answer is added
        val k = QuizStats.key(mode, Stickers.stickerName(sticker))
        quizStats = quizStats + (k to knowledge(mode, sticker).answered(correct, timeMs, limit))
        saveQuiz()
        return correct && timeMs > limit
    }

    /** Forgets what the quiz learned about these stickers in this direction. */
    fun resetQuiz(mode: QuizMode, stickers: Collection<Int>) {
        quizStats = quizStats - stickers.map { QuizStats.key(mode, Stickers.stickerName(it)) }.toSet()
        saveQuiz()
    }

    private fun saveQuiz() { prefs.edit().putString(KEY_QUIZ, QuizStats.encode(quizStats)).apply() }

    // ---------------------------------------------------------------- backup
    fun exportText(): String = Backup.encode(settings, progress, words, quizStats)

    /** @return a message for the user */
    fun importText(text: String): String {
        val c = Backup.decode(text) ?: return "This file is not a 3Style Trainer backup."
        progress = c.progress
        saveProgress()
        words = c.words
        prefs.edit().putString(KEY_WORDS, WordsCodec.encode(words)).apply()
        quizStats = c.quiz
        saveQuiz()
        updateSettings(c.settings)
        return "Imported ${c.progress.size} cases and ${c.words.size} letter pair words."
    }

    // ---------------------------------------------------------------- practice
    fun updatePracticeConfig(c: PracticeConfig) {
        practiceConfig = c
        prefs.edit()
            .putString("p_types", c.types.joinToString(",") { it.name })
            .putString("p_status", c.statuses.joinToString(",") { it.name })
            .putString("p_sets", c.sets.joinToString("\u001F"))
            .putInt("p_batch", c.batchSize)
            .putInt("p_goal", c.goal)
            .putString("p_mode", c.mode.name)
            .putBoolean("p_picture", c.showPicture)
            .apply()
    }

    private fun loadPracticeConfig(): PracticeConfig {
        val d = PracticeConfig()
        fun <T> names(key: String, parse: (String) -> T, def: Set<T>): Set<T> {
            val s = prefs.getString(key, null) ?: return def
            return s.split(',').filter { it.isNotEmpty() }.mapNotNull { runCatching { parse(it) }.getOrNull() }.toSet()
        }
        return PracticeConfig(
            types = names("p_types", { CaseType.valueOf(it) }, d.types).ifEmpty { d.types },
            statuses = names("p_status", { Status.valueOf(it) }, d.statuses).ifEmpty { d.statuses },
            sets = prefs.getString("p_sets", null)?.split('\u001F')?.filter { it.isNotEmpty() }?.toSet() ?: d.sets,
            batchSize = prefs.getInt("p_batch", d.batchSize).coerceIn(MIN_BATCH, MAX_BATCH),
            goal = prefs.getInt("p_goal", d.goal).coerceIn(1, 3),
            mode = runCatching { PracticeMode.valueOf(prefs.getString("p_mode", d.mode.name)!!) }.getOrDefault(d.mode),
            showPicture = prefs.getBoolean("p_picture", d.showPicture),
        )
    }

    /** All case keys matching the configuration. */
    fun pool(c: PracticeConfig = practiceConfig): List<String> {
        val lib = library ?: return emptyList()
        val setKeys = if (c.sets.isEmpty()) null else lib.sets.filter { it.id in c.sets }.flatMap { it.keys }.toSet()
        return lib.all.filter { case ->
            case.type in c.types && (setKeys == null || case.key in setKeys) &&
                progressOf(case.key).status in c.statuses && case.solutions.isNotEmpty()
        }.map { it.key }
    }

    fun startBatch(c: PracticeConfig = practiceConfig) {
        val lib = library ?: return
        val picked = BatchPicker.pick(pool(c), c.batchSize, { progressOf(it).box }, { progressOf(it).lastSeen },
            Random(SystemClock.elapsedRealtimeNanos()), { lib.case(it)?.inverseKey })
        if (picked.isNotEmpty()) startSession(picked, c, "Batch")
    }

    /** Practise one set: its cases that are not known yet (or all of them if all are known). */
    fun startSet(setId: String) {
        val set = library?.sets?.firstOrNull { it.id == setId } ?: return
        val open = set.keys.filter { progressOf(it).status != Status.KNOWN }
        startSession(open.ifEmpty { set.keys }, practiceConfig, set.title)
    }

    fun startSingle(key: String) = startSession(listOf(key), practiceConfig, "Single case")

    private fun startSession(keys: List<String>, c: PracticeConfig, title: String) {
        val valid = keys.filter { library?.case(it)?.solutions?.isNotEmpty() == true }
        if (valid.isEmpty()) return
        val s = SessionUi(PracticeSession(valid, c.goal, Random(SystemClock.elapsedRealtimeNanos())), c, title)
        s.next()
        session = s
        if (current != Screen.Session) open(Screen.Session)
    }

    fun reveal() {
        val s = session ?: return
        if (s.revealed) return
        val key = s.current ?: return
        val t = SystemClock.elapsedRealtime() - s.shownAt
        s.revealed = true
        s.timeMs = t
        update(key) {
            if (s.config.mode == PracticeMode.RECALL) it.copy(recallTimes = (it.recallTimes + t).takeLast(ItemProgress.KEEP_TIMES))
            else it.copy(execTimes = (it.execTimes + t).takeLast(ItemProgress.KEEP_TIMES))
        }
    }

    fun grade(g: Grade) {
        val s = session ?: return
        val key = s.current ?: return
        val auto = settings.autoKnown
        update(key) {
            val box = Leitner.next(it.box, g)
            it.copy(
                box = box,
                status = Leitner.nextStatus(it.status, box, g, auto),
                attempts = it.attempts + 1,
                misses = it.misses + if (g == Grade.AGAIN || g == Grade.HARD) 1 else 0,
                lastSeen = System.currentTimeMillis(),
            )
        }
        s.session.grade(g)
        s.next()
    }

    // ---------------------------------------------------------------- storage helpers
    private fun loadMap(name: String): Map<String, String> =
        prefs.getStringSet(name, null)?.mapNotNull { e -> e.indexOf('=').takeIf { it > 0 }?.let { e.substring(0, it) to e.substring(it + 1) } }?.toMap()
            ?: emptyMap()

    private fun saveMap(name: String, m: Map<String, String>) {
        prefs.edit().putStringSet(name, m.map { (k, v) -> "$k=$v" }.toSet()).apply()
    }

    companion object {
        private const val KEY_PROGRESS = "progress"
        private const val KEY_WORDS = "words"
        private const val KEY_QUIZ = "quiz"
        const val MIN_BATCH = 2
        const val MAX_BATCH = 12

        /** Libraries already built in this process (switching buffers back and forth is instant). */
        private val cache = HashMap<BufferConfig, Library>()
    }
}
