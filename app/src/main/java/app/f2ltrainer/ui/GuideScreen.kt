package app.f2ltrainer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private val SECTIONS = listOf(
    "Colours and orientation" to
        "All pictures show the cube with the white cross on the bottom, yellow on top, green in front and orange on " +
        "the right. The target is always the front-right slot (white-green-orange corner and green-orange edge). " +
        "Grey stickers belong to the last layer and do not matter. \"Back view\" shows the same cube from behind.",
    "Notation" to
        "R, L, U, D, F, B: turn that face 90° clockwise (looking at the face). ' = counter-clockwise, 2 = half turn.\n" +
        "y: rotate the whole cube like U.  y' the other way.\n" +
        "d: turn the bottom two layers like D (d = U turn + y' rotation).\n" +
        "A U turn at the start is the AUF (adjusting the top layer); when the app shows a case from another angle, " +
        "it changes that first U turn for you.",
    "How to learn a case" to
        "1. Look at the picture and the recognition notes.\n" +
        "2. Play the algorithm step by step. Grey moves adjust, blue moves pair corner and edge, " +
        "green moves insert the pair.\n" +
        "3. Do it slowly on your cube until you understand why it works.\n" +
        "4. Set the case to Learning and practise it in small batches.\n" +
        "5. When you recognise it instantly and execute it without thinking, mark it as Known.",
    "Practice" to
        "Choose which statuses and groups to practise and a small batch size (4–6 is a good start). Every case in a " +
        "batch must be answered correctly several times in a row. Known cases are left out unless you select " +
        "\"Known\", so you can review them now and then without them crowding your practice.\n" +
        "Flashcards train recognition. Timed mode gives a setup to apply to a solved cube, so you can practise " +
        "real execution with a timer.",
    "Free back-slot shortcuts" to
        "If the back-right slot is still unsolved, its space can be used to solve the front-right pair with fewer " +
        "moves. 17 cases have such a shortcut. They are optional: learn them once the normal algorithms are " +
        "comfortable. Switch on \"Include free back-slot shortcuts\" in Practice to train them.",
    "Tips for sub-10 averages" to
        "• Plan the cross and, if possible, the first pair during inspection.\n" +
        "• Look ahead: while inserting one pair, look for the pieces of the next pair. Turning slower without " +
        "pauses is faster than turning fast with pauses.\n" +
        "• Avoid cube rotations where a rotationless algorithm is just as good.\n" +
        "• Recognise every case from any angle (keep \"Random U-layer angle\" switched on).\n" +
        "• Use empty slots: the free-slot shortcuts save moves.\n" +
        "• Your algorithms should feel fast to you: you can choose a different one or add your own for every case.",
    "Permissions and privacy" to
        "This app uses no permissions: no internet, no storage access. Your progress is stored only inside the app.",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuideScreen(vm: AppViewModel) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Guide") },
                navigationIcon = { TextButton(onClick = { vm.back() }) { Text("← Back") } },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            for ((title, body) in SECTIONS) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Text(body, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
