package app.threestyle.ui

import android.os.SystemClock
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.Dp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import app.threestyle.data.QuizMode
import app.threestyle.data.QuizStats
import app.threestyle.data.StickerKnowledge
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.threestyle.bld.LetterScheme
import app.threestyle.bld.MemoCheck
import app.threestyle.bld.Misoriented
import app.threestyle.bld.PieceType
import app.threestyle.bld.Scrambler
import app.threestyle.bld.Stickers
import app.threestyle.bld.Tracer
import app.threestyle.cube.CubeState
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun LettersScreen(vm: AppViewModel, modifier: Modifier) {
    val scheme = vm.settings.scheme
    var refType by rememberSaveable { mutableStateOf(PieceType.CORNER) }
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        item { Text("Letters", style = MaterialTheme.typography.headlineSmall) }
        item {
            SectionTitle("Letter scheme")
            ChipGroup {
                FilterChip(refType == PieceType.CORNER, { refType = PieceType.CORNER }, label = { Text("Corners") })
                FilterChip(refType == PieceType.EDGE, { refType = PieceType.EDGE }, label = { Text("Edges") })
            }
            val s = vm.settings
            CubeNet(
                state = CubeState.SOLVED,
                faceColours = CubeColours.faceColours(s.topColour, s.frontColour),
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                markers = Stickers.speffzOrder(refType).map { Marker(it, scheme.letterOf(it).toString()) },
                description = "Letter scheme for ${refType.name.lowercase()}s",
            )
            Hint("U on top, L F R B in the middle row, D at the bottom. Change letters in Settings.")
        }
        item {
            SectionTitle("Letter quiz")
            Hint("Sticker → letter: name the highlighted sticker. Letter → sticker: tap the sticker of the letter. " +
                "The quiz remembers which stickers you know: unknown, missed and slow ones come much more often. " +
                "\"Slow\" is relative to your own usual answer time (× 1.3), so the bar rises as you get faster; " +
                "a correct but slow answer (e.g. counted along) counts as not learned yet. A sticker is mastered (●●●●●) " +
                "after 5 fluent answers. \"Reset\" in the quiz starts from scratch.")
            for (toLetter in listOf(true, false)) {
                val mode = if (toLetter) QuizMode.STICKER_TO_LETTER else QuizMode.LETTER_TO_STICKER
                fun mastered(t: PieceType) = Stickers.speffzOrder(t).count { vm.knowledge(mode, it).mastered }
                Text((if (toLetter) "Sticker → letter" else "Letter → sticker") +
                    "  ·  mastered: corners ${mastered(PieceType.CORNER)}/24, edges ${mastered(PieceType.EDGE)}/24",
                    modifier = Modifier.padding(top = 6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = { vm.open(Screen.LetterQuiz(toLetter, setOf(PieceType.CORNER))) }) { Text("Corners") }
                    FilledTonalButton(onClick = { vm.open(Screen.LetterQuiz(toLetter, setOf(PieceType.EDGE))) }) { Text("Edges") }
                    FilledTonalButton(onClick = { vm.open(Screen.LetterQuiz(toLetter, PieceType.entries.toSet())) }) { Text("Both") }
                }
            }
        }
        item {
            SectionTitle("Letter pairs")
            Hint("Give each letter pair a word or image (for memorisation), then drill them. ${vm.words.size} words so far.")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = { vm.open(Screen.PairWords) }) { Text("Edit pair words") }
                FilledTonalButton(onClick = { vm.open(Screen.PairDrill) }, enabled = vm.words.isNotEmpty()) { Text("Drill pair words") }
            }
            Hint("To drill letter pairs as commutators, use Practice (each corner and edge case is a letter pair).")
        }
        item {
            SectionTitle("Memo tracing")
            Hint("Trace a random scramble with your buffers and letter scheme; the app checks your memo step by step.")
            FilledTonalButton(onClick = { vm.open(Screen.Memo) }) { Text("Trace a scramble") }
        }
    }
}

@Composable
fun LetterQuizScreen(vm: AppViewModel, toLetter: Boolean, types: Set<PieceType>) {
    val scheme = vm.settings.scheme
    val s = vm.settings
    val pool = remember(types) { types.flatMap { Stickers.speffzOrder(it).toList() } }
    val mode = if (toLetter) QuizMode.STICKER_TO_LETTER else QuizMode.LETTER_TO_STICKER
    val random = remember { Random(SystemClock.elapsedRealtimeNanos()) }
    // stickers answered wrong in this session come back soon (in addition to the weighting)
    val again = remember { mutableStateListOf<Int>() }
    var target by remember { mutableIntStateOf(QuizStats.pick(pool, { vm.knowledge(mode, it) }, null, random)) }
    var answer by remember { mutableStateOf<Int?>(null) }  // the sticker the user answered with
    var correct by remember { mutableIntStateOf(0) }
    var total by remember { mutableIntStateOf(0) }
    var shownAt by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    var timeSum by remember { mutableLongStateOf(0L) }
    var confirmReset by remember { mutableStateOf(false) }
    var lastTime by remember { mutableLongStateOf(0L) }
    var slow by remember { mutableStateOf(false) }  // last answer correct but slow

    fun next() {
        val fromAgain = again.isNotEmpty() && again.first() != target && random.nextInt(3) == 0
        target = if (fromAgain) again.removeAt(0) else QuizStats.pick(pool, { vm.knowledge(mode, it) }, target, random)
        answer = null
        shownAt = SystemClock.elapsedRealtime()
    }

    fun submit(sticker: Int) {
        if (answer != null) return
        answer = sticker
        total++
        val t = SystemClock.elapsedRealtime() - shownAt
        val right = sticker == target
        if (right) {
            correct++
            timeSum += t
        } else {
            again.add(target)
        }
        lastTime = t
        slow = vm.quizAnswer(mode, target, right, t)
    }

    val type = Stickers.typeOf(target)!!
    val ok = answer == target
    LaunchedEffect(answer) {
        if (answer != null && ok) { delay(if (slow) 1500 else 500); next() }
    }
    @Composable
    fun Header() {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(if (type == PieceType.CORNER) "Corner sticker" else "Edge sticker",
                style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
            Hint("$correct / $total correct" + if (correct > 0) " · Ø ${formatMs(timeSum / correct)}" else "")
        }
        val level = vm.knowledge(mode, target).level
        Hint("This sticker: " + "●".repeat(level) + "○".repeat(StickerKnowledge.MAX - level) +
            " · mastered ${pool.count { vm.knowledge(mode, it).mastered }}/${pool.size}" +
            " · fluent ≤ ${formatMs(vm.slowLimit(mode))}")
    }

    @Composable
    fun BigLetter(modifier: Modifier) {
        Text("${scheme.letterOf(target)}", fontSize = 56.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace,
            modifier = modifier)
    }

    /** The net fills the space it is given (drawn centred, never outside it). */
    @Composable
    fun Net(modifier: Modifier) {
        Box(modifier.fillMaxWidth().clipToBounds(), contentAlignment = Alignment.Center) {
            CubeNet(
                state = CubeState.SOLVED,
                faceColours = CubeColours.faceColours(s.topColour, s.frontColour),
                modifier = Modifier.fillMaxSize(),
                keepAspectRatio = false,
                highlight = if (toLetter || answer != null) target else null,
                markers = if (answer != null && !ok && !toLetter) listOf(Marker(answer!!, "✗")) else emptyList(),
                description = if (toLetter) "Highlighted sticker" else "Tap the sticker",
                onTap = if (toLetter) null else { f -> submit(f) },
            )
        }
    }

    /** Feedback in a fixed place, so that nothing jumps. */
    @Composable
    fun Feedback() {
        Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                when {
                    answer == null -> Hint(if (toLetter) "Which letter is the highlighted sticker?"
                        else "Tap the ${if (type == PieceType.CORNER) "corner" else "edge"} sticker with this letter.")
                    ok && slow -> {
                        Text("✓ ${scheme.letterOf(target)} = ${Stickers.stickerName(target)}, but slow", color = MaterialTheme.colorScheme.tertiary, fontSize = 18.sp)
                        Hint("${formatMs(lastTime)} · fluent is up to ${formatMs(vm.slowLimit(mode))} (your usual time × 1.3) · counts as not learned yet")
                    }
                    ok -> Text("✓ ${scheme.letterOf(target)} = ${Stickers.stickerName(target)}  ${formatMs(lastTime)}", color = MaterialTheme.colorScheme.primary, fontSize = 20.sp)
                    else -> {
                        val a = answer!!
                        val wrong = if (Stickers.typeOf(a) == type) "${scheme.letterOf(a)} = ${Stickers.stickerName(a)}" else "not a ${type.name.lowercase()} sticker"
                        Text("✗ That was $wrong", color = MaterialTheme.colorScheme.error, fontSize = 16.sp)
                        Text("${scheme.letterOf(target)} = ${Stickers.stickerName(target)}", fontSize = 16.sp)
                    }
                }
            }
            if (answer != null && !ok) Button(onClick = { next() }) { Text("Next") }
        }
    }

    @Composable
    fun Keys(modifier: Modifier, keyHeight: Dp) {
        LetterGrid(scheme.alphabet(type).toList(), modifier) { c, m ->
            val sticker = scheme.stickerOf(type, c)
            val isTarget = answer != null && sticker == target
            val isWrong = answer != null && !ok && sticker == answer
            val colours = when {
                isTarget -> ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                isWrong -> ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                else -> ButtonDefaults.outlinedButtonColors()
            }
            OutlinedButton(
                onClick = { sticker?.let { submit(it) } },
                enabled = answer == null || isTarget || isWrong,
                colors = colours,
                modifier = m.height(keyHeight),
                contentPadding = PaddingValues(0.dp),
            ) {
                Text("$c", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Start from scratch?") },
            text = { Text("The quiz forgets which of these ${pool.size} stickers you know (${if (toLetter) "sticker → letter" else "letter → sticker"}). All of them are asked equally often again.") },
            confirmButton = { TextButton(onClick = { vm.resetQuiz(mode, pool); confirmReset = false }) { Text("Reset") } },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel") } },
        )
    }
    FixedScreen(if (toLetter) "Sticker → letter" else "Letter → sticker", { vm.back() },
        actions = { TextButton(onClick = { confirmReset = true }) { Text("Reset") } }) {
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
            val gaps = 6.dp * 5  // between the 6 rows of keys
            if (maxWidth > maxHeight) {
                // landscape: cube on the left, letters on the right
                val keyHeight = ((maxHeight - gaps) / 6).coerceIn(28.dp, 56.dp)
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Header()
                        if (!toLetter) BigLetter(Modifier.align(Alignment.CenterHorizontally))
                        Net(Modifier.weight(1f))
                        if (toLetter) Feedback()
                    }
                    if (toLetter) {
                        Keys(Modifier.weight(1f).fillMaxHeight().wrapContentHeight(), keyHeight)
                    } else {
                        Column(Modifier.weight(0.6f).fillMaxHeight(), verticalArrangement = Arrangement.Center) { Feedback() }
                    }
                }
            } else {
                // portrait: everything below each other; the keys take at most ~45 % of the height
                val keyHeight = ((maxHeight * 0.45f - gaps) / 6).coerceIn(28.dp, 50.dp)
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Header()
                    if (!toLetter) BigLetter(Modifier.align(Alignment.CenterHorizontally))
                    Net(Modifier.weight(1f))
                    Feedback()
                    if (toLetter) Keys(Modifier.fillMaxWidth(), keyHeight)
                }
            }
        }
    }
}

/** All letters that occur in the scheme, in scheme order. */
private fun allLetters(scheme: LetterScheme): List<Char> =
    (scheme.corners + scheme.edges).uppercase().toList().distinct()

@Composable
fun PairWordsScreen(vm: AppViewModel) {
    val letters = allLetters(vm.settings.scheme)
    var first by rememberSaveable { mutableStateOf(letters.first()) }
    SubScreen("Letter pair words", { vm.back() }) {
        item {
            Hint("Choose the first letter, then type a word or image for each pair. Saved automatically.")
            LetterGrid(letters, Modifier.fillMaxWidth().padding(top = 6.dp)) { c, m ->
                val n = letters.count { vm.words.containsKey("$c$it") }
                FilterChip(selected = c == first, onClick = { first = c }, modifier = m,
                    label = { Text(if (n > 0) "$c · $n" else "$c", fontWeight = FontWeight.Bold) })
            }
            Hint("The number is how many pairs with this first letter already have a word.")
        }
        items(letters.filter { it != first }, key = { "$first$it" }) { second ->
            val pair = "$first$second"
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(pair, fontFamily = FontFamily.Monospace, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(48.dp))
                OutlinedTextField(
                    value = vm.words[pair] ?: "",
                    onValueChange = { vm.setWord(pair, it) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                )
            }
        }
    }
}

@Composable
fun PairDrillScreen(vm: AppViewModel) {
    val random = remember { Random(SystemClock.elapsedRealtimeNanos()) }
    val queue = remember { mutableStateListOf<String>().apply { addAll(vm.words.keys.shuffled(random)) } }
    var revealed by remember { mutableStateOf(false) }
    var known by remember { mutableIntStateOf(0) }
    var missed by remember { mutableIntStateOf(0) }
    var shownAt by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    var lastTime by remember { mutableStateOf<Long?>(null) }
    SubScreen("Pair word drill", { vm.back() }) {
        item { Hint("$known known · $missed missed · ${queue.size} left") }
        val pair = queue.firstOrNull()
        if (pair == null) {
            item {
                Text("All pairs done.", style = MaterialTheme.typography.titleLarge)
                Button(onClick = {
                    queue.addAll(vm.words.keys.shuffled(random)); known = 0; missed = 0; revealed = false
                    shownAt = SystemClock.elapsedRealtime()
                }, modifier = Modifier.padding(top = 8.dp)) { Text("Again") }
            }
            return@SubScreen
        }
        item {
            Card(Modifier.fillMaxWidth().heightIn(min = 160.dp).clickable(enabled = !revealed) {
                revealed = true; lastTime = SystemClock.elapsedRealtime() - shownAt
            }) {
                Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(pair, fontSize = 64.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    if (revealed) {
                        Text(vm.words[pair] ?: "", fontSize = 26.sp)
                        Hint(formatMs(lastTime))
                    } else Hint("Think of your word, then tap")
                }
            }
        }
        if (revealed) item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    missed++; queue.removeAt(0); queue.add(minOf(3, queue.size), pair)
                    revealed = false; shownAt = SystemClock.elapsedRealtime()
                }, modifier = Modifier.weight(1f)) { Text("Missed") }
                Button(onClick = {
                    known++; queue.removeAt(0)
                    revealed = false; shownAt = SystemClock.elapsedRealtime()
                }, modifier = Modifier.weight(1f)) { Text("Knew it") }
            }
        }
    }
}

private fun lettersOf(targets: List<Int>, scheme: LetterScheme): String =
    targets.map { scheme.letterOf(it) }.chunked(2).joinToString(" ") { it.joinToString("") }

private fun misorientedText(list: List<Misoriented>, type: PieceType): String =
    if (list.isEmpty()) "" else list.joinToString(", ") {
        Stickers.pieceName(it.piece) + if (type == PieceType.EDGE) " flipped" else if (it.steps == 1) " twisted ↻" else " twisted ↺"
    }

@Composable
fun MemoScreen(vm: AppViewModel) {
    val lib = vm.library ?: return Loading()
    val scheme = vm.settings.scheme
    val s = vm.settings
    var seed by rememberSaveable { mutableStateOf(SystemClock.elapsedRealtimeNanos()) }
    val scramble = remember(seed) { Scrambler.random(Random(seed)) }
    val state = remember(scramble) { CubeState.SOLVED.apply(scramble) }
    var edgeText by rememberSaveable(seed) { mutableStateOf("") }
    var cornerText by rememberSaveable(seed) { mutableStateOf("") }
    var checked by rememberSaveable(seed) { mutableStateOf(false) }
    var showSolution by rememberSaveable(seed) { mutableStateOf(false) }
    var startedAt by rememberSaveable(seed) { mutableStateOf(SystemClock.elapsedRealtime()) }
    var memoTime by rememberSaveable(seed) { mutableStateOf<Long?>(null) }
    val cb = lib.config.cornerBuffer
    val eb = lib.config.edgeBuffer
    val pe = lib.config.parityEdge

    SubScreen("Memo tracing", { vm.back() }) {
        item {
            Text("Scramble", style = MaterialTheme.typography.labelLarge)
            AlgText(scramble, size = 16.sp)
            Hint("Scramble a solved cube (${CubeColours.faceColours(s.topColour, s.frontColour).let { "${app.threestyle.data.Colours.NAMES[it[0]]} top, ${app.threestyle.data.Colours.NAMES[it[2]]} front" }}) or trace from the picture.")
            CubeNet(state, CubeColours.faceColours(s.topColour, s.frontColour), Modifier.fillMaxWidth().padding(top = 6.dp),
                markers = listOf(Marker(cb, "", ring = true), Marker(eb, "", ring = true)),
                description = "Scrambled cube")
            Hint("Rings = buffers (${pieceLabel(cb, scheme)}, ${pieceLabel(eb, scheme)}).")
        }
        item {
            OutlinedTextField(edgeText, { edgeText = it; checked = false }, label = { Text("Edge memo (letters)") },
                singleLine = true, modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters))
            OutlinedTextField(cornerText, { cornerText = it; checked = false }, label = { Text("Corner memo (letters)") },
                singleLine = true, modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                Button(onClick = {
                    checked = true
                    if (memoTime == null) memoTime = SystemClock.elapsedRealtime() - startedAt
                }) { Text("Check") }
                OutlinedButton(onClick = { showSolution = !showSolution }) { Text(if (showSolution) "Hide solution" else "Show solution") }
                OutlinedButton(onClick = { seed = SystemClock.elapsedRealtimeNanos(); startedAt = SystemClock.elapsedRealtime() }) { Text("New scramble") }
            }
        }
        if (checked) {
            item {
                memoTime?.let { Text("Memo time: ${formatMs(it)}") }
                for ((type, text, buf) in listOf(Triple(PieceType.EDGE, edgeText, eb), Triple(PieceType.CORNER, cornerText, cb))) {
                    val name = if (type == PieceType.EDGE) "Edges" else "Corners"
                    val (targets, bad) = Tracer.parse(text, type, buf, scheme)
                    val result = bad ?: Tracer.check(state, type, buf, targets!!)
                    val (msg, good) = when (result) {
                        is MemoCheck.Ok -> buildString {
                            append("✓ $name correct: ${lettersOf(result.targets, scheme)} (${result.targets.size} targets")
                            if (result.parity) append(", parity")
                            append(")")
                            val mis = misorientedText(result.misoriented, type)
                            if (mis.isNotEmpty()) append(". Afterwards still: $mis")
                        } to true
                        is MemoCheck.Wrong -> ("✗ $name: letter ${result.index + 1} is wrong. " +
                            (result.expected?.let { "Expected ${scheme.letterOf(it)} (${Stickers.stickerName(it)})." }
                                ?: "The buffer piece is home here: start a new cycle with any unsolved piece.")) to false
                        is MemoCheck.Incomplete -> ("✗ $name: memo incomplete. " +
                            (result.next?.let { "Next target: ${scheme.letterOf(it)} (${Stickers.stickerName(it)})." }
                                ?: "Start a new cycle: unsolved pieces remain.")) to false
                        is MemoCheck.BadLetter -> "✗ $name: '${result.letter}' ${result.reason}." to false
                    }
                    Text(msg, color = if (good) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
        if (showSolution) {
            item {
                val et = Tracer.trace(state, PieceType.EDGE, eb)
                val ct = Tracer.trace(state, PieceType.CORNER, cb)
                SectionTitle("One correct memo")
                Text("Edges: ${lettersOf(et.targets, scheme).ifEmpty { "–" }}", fontFamily = FontFamily.Monospace)
                misorientedText(et.misoriented, PieceType.EDGE).takeIf { it.isNotEmpty() }?.let { Hint("Flipped: $it") }
                Text("Corners: ${lettersOf(ct.targets, scheme).ifEmpty { "–" }}", fontFamily = FontFamily.Monospace)
                misorientedText(ct.misoriented, PieceType.CORNER).takeIf { it.isNotEmpty() }?.let { Hint("Twisted: $it") }
                if (ct.parity) Hint("Parity: add the parity letter ${scheme.letterOf(pe)} after the last edge letter (unless the last edge letter is ${scheme.letterOf(pe)}); the last corner letter is solved with the parity algorithm.")
                Hint("Cycle breaks use the first unsolved piece in letter order; other cycle breaks are accepted by Check too.")
            }
        }
    }
}
