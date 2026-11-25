package app.f2ltrainer.ui

import android.app.Application
import android.content.Context
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import app.f2ltrainer.data.BatchPicker
import app.f2ltrainer.data.CaseGroup
import app.f2ltrainer.data.Cases
import app.f2ltrainer.data.Exercise
import app.f2ltrainer.data.Grade
import app.f2ltrainer.data.ItemProgress
import app.f2ltrainer.data.Leitner
import app.f2ltrainer.data.PracticeItem
import app.f2ltrainer.data.PracticeSession
import app.f2ltrainer.data.ProgressCodec
import app.f2ltrainer.data.Setup
import app.f2ltrainer.data.Status
import kotlin.random.Random

enum class Tab(val label: String) { LEARN("Learn"), PRACTICE("Practice"), PROGRESS("Progress") }

sealed interface Screen {
    data class Home(val tab: Tab) : Screen
    data class CaseDetail(val caseId: Int) : Screen
    data object Guide : Screen
    data object Session : Screen
}

enum class PracticeMode(val label: String) { FLASHCARD("Flashcards"), TIMED("Timed with cube") }

data class PracticeConfig(
    val statuses: Set<Status> = setOf(Status.NEW, Status.LEARNING),
    val groups: Set<CaseGroup> = CaseGroup.entries.toSet(),
    val includeFree: Boolean = false,
    val batchSize: Int = 5,
    val goal: Int = 2,
    val mode: PracticeMode = PracticeMode.FLASHCARD,
    val randomAuf: Boolean = true,
    val showAlternatives: Boolean = true,
)

/** UI state of a running practice batch. */
class SessionUi(
    val session: PracticeSession,
    val config: PracticeConfig,
    val random: Random,
) {
    var exercise by mutableStateOf<Exercise?>(null)
    var disguise by mutableStateOf(0)
    var revealed by mutableStateOf(false)
    /** Timed mode: timer start (elapsedRealtime) or null; result in ms after stopping. */
    var timerStart by mutableStateOf<Long?>(null)
    var timeMs by mutableStateOf<Long?>(null)
    var shownAt by mutableStateOf(0L)
    var version by mutableStateOf(0)

    fun next() {
        val item = session.current
        exercise = item?.let { Exercise(it, if (config.randomAuf) random.nextInt(4) else 0) }
        disguise = random.nextInt(Setup.DISGUISES.size)
        revealed = false
        timerStart = null
        timeMs = null
        shownAt = SystemClock.elapsedRealtime()
        version++
    }
}

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = app.getSharedPreferences("f2l_trainer", Context.MODE_PRIVATE)

    val stack = mutableStateListOf<Screen>(Screen.Home(Tab.LEARN))
    val current: Screen get() = stack.last()

    var progress by mutableStateOf(ProgressCodec.decode(prefs.getString(KEY_PROGRESS, null)))
        private set

    var config by mutableStateOf(loadConfig())
        private set

    var session by mutableStateOf<SessionUi?>(null)
        private set

    // ---------------- navigation
    fun open(screen: Screen) { stack.add(screen) }

    fun selectTab(tab: Tab) {
        stack.clear(); stack.add(Screen.Home(tab))
    }

    /** @return false if there is nothing to go back to */
    fun back(): Boolean {
        if (stack.size <= 1) return false
        val top = stack.removeAt(stack.size - 1)
        if (top == Screen.Session) session = null
        return true
    }

    // ---------------- progress
    fun progressOf(item: PracticeItem): ItemProgress = progress[item.key] ?: ItemProgress()

    private fun update(item: PracticeItem, f: (ItemProgress) -> ItemProgress) {
        progress = progress + (item.key to f(progressOf(item)))
        prefs.edit().putString(KEY_PROGRESS, ProgressCodec.encode(progress)).apply()
    }

    fun setStatus(item: PracticeItem, status: Status) = update(item) { it.copy(status = status) }

    /** All algorithms available for an item: built-in ones first, then the user's own. */
    fun algorithmsOf(item: PracticeItem): List<String> {
        val c = item.case
        val builtIn = if (item.freeSlot) listOfNotNull(c.freeSlotAlgorithm) else c.algorithms
        return builtIn + progressOf(item).customAlgs.filter { it !in builtIn }
    }

    fun preferredAlgOf(item: PracticeItem): String {
        val all = algorithmsOf(item)
        val p = progressOf(item).preferredAlg
        return if (p != null && p in all) p else all.first()
    }

    fun setPreferred(item: PracticeItem, alg: String) = update(item) { it.copy(preferredAlg = alg) }

    fun addCustom(item: PracticeItem, alg: String) = update(item) {
        if (alg in it.customAlgs) it else it.copy(customAlgs = it.customAlgs + alg)
    }

    fun removeCustom(item: PracticeItem, alg: String) = update(item) {
        it.copy(customAlgs = it.customAlgs - alg, preferredAlg = if (it.preferredAlg == alg) null else it.preferredAlg)
    }

    fun resetProgress() {
        progress = emptyMap()
        prefs.edit().remove(KEY_PROGRESS).apply()
    }

    // ---------------- practice
    fun updateConfig(c: PracticeConfig) {
        config = c
        prefs.edit()
            .putString("cfg_status", c.statuses.joinToString(",") { it.name })
            .putString("cfg_groups", c.groups.joinToString(",") { it.name })
            .putBoolean("cfg_free", c.includeFree)
            .putInt("cfg_batch", c.batchSize)
            .putInt("cfg_goal", c.goal)
            .putString("cfg_mode", c.mode.name)
            .putBoolean("cfg_auf", c.randomAuf)
            .putBoolean("cfg_alts", c.showAlternatives)
            .apply()
    }

    private fun loadConfig(): PracticeConfig {
        val d = PracticeConfig()
        fun <T> names(key: String, parse: (String) -> T?, def: Set<T>): Set<T> {
            val s = prefs.getString(key, null) ?: return def
            return s.split(',').filter { it.isNotEmpty() }.mapNotNull { runCatching { parse(it) }.getOrNull() }.toSet()
        }
        return PracticeConfig(
            statuses = names("cfg_status", { Status.valueOf(it) }, d.statuses),
            groups = names("cfg_groups", { CaseGroup.valueOf(it) }, d.groups),
            includeFree = prefs.getBoolean("cfg_free", d.includeFree),
            batchSize = prefs.getInt("cfg_batch", d.batchSize).coerceIn(MIN_BATCH, MAX_BATCH),
            goal = prefs.getInt("cfg_goal", d.goal).coerceIn(1, 3),
            mode = runCatching { PracticeMode.valueOf(prefs.getString("cfg_mode", d.mode.name)!!) }.getOrDefault(d.mode),
            randomAuf = prefs.getBoolean("cfg_auf", d.randomAuf),
            showAlternatives = prefs.getBoolean("cfg_alts", d.showAlternatives),
        )
    }

    /** All items matching the configuration. */
    fun pool(c: PracticeConfig = config): List<PracticeItem> {
        val items = ArrayList<PracticeItem>()
        for (case in Cases.ALL) {
            if (case.group !in c.groups) continue
            val std = PracticeItem(case.id, false)
            if (progressOf(std).status in c.statuses) items.add(std)
            if (c.includeFree && case.freeSlotAlgorithm != null) {
                val free = PracticeItem(case.id, true)
                if (progressOf(free).status in c.statuses) items.add(free)
            }
        }
        return items
    }

    fun startBatch(c: PracticeConfig = config) {
        val picked = BatchPicker.pick(pool(c), c.batchSize, { progressOf(it).box }, { progressOf(it).lastSeen })
        if (picked.isNotEmpty()) startSession(picked, c)
    }

    fun startSingle(item: PracticeItem) = startSession(listOf(item), config.copy(mode = PracticeMode.FLASHCARD))

    private fun startSession(items: List<PracticeItem>, c: PracticeConfig) {
        // practising a new case means it is now being learned
        for (it in items) if (progressOf(it).status == Status.NEW) setStatus(it, Status.LEARNING)
        val random = Random(SystemClock.elapsedRealtimeNanos())
        val s = SessionUi(PracticeSession(items, c.goal, random), c, random)
        s.next()
        session = s
        if (current != Screen.Session) open(Screen.Session)
    }

    fun reveal() {
        val s = session ?: return
        if (s.revealed) return
        s.revealed = true
        val ex = s.exercise ?: return
        if (s.config.mode == PracticeMode.FLASHCARD) {
            val t = SystemClock.elapsedRealtime() - s.shownAt
            update(ex.item) { it.copy(recogTimes = (it.recogTimes + t).takeLast(ItemProgress.KEEP_TIMES)) }
        }
    }

    fun startTimer() {
        val s = session ?: return
        s.timerStart = SystemClock.elapsedRealtime()
        s.timeMs = null
    }

    fun stopTimer() {
        val s = session ?: return
        val start = s.timerStart ?: return
        val t = SystemClock.elapsedRealtime() - start
        s.timerStart = null
        s.timeMs = t
        s.revealed = true
        val ex = s.exercise ?: return
        update(ex.item) { it.copy(solveTimes = (it.solveTimes + t).takeLast(ItemProgress.KEEP_TIMES)) }
    }

    fun grade(g: Grade) {
        val s = session ?: return
        val ex = s.exercise ?: return
        update(ex.item) {
            it.copy(
                box = Leitner.next(it.box, g),
                attempts = it.attempts + 1,
                misses = it.misses + if (g == Grade.AGAIN || g == Grade.HARD) 1 else 0,
                lastSeen = System.currentTimeMillis(),
            )
        }
        s.session.grade(g)
        s.next()
    }

    companion object {
        private const val KEY_PROGRESS = "progress"
        const val MIN_BATCH = 2
        const val MAX_BATCH = 12
    }
}
