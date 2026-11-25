package app.threestyle.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

private val GUIDE = listOf(
    "What is 3-style?" to "In blindfolded solving you memorise the scramble as letters (one letter per sticker, here Speffz) " +
        "and solve the pieces two at a time. Every letter pair is a 3-cycle: the piece on the buffer goes to the " +
        "first letter, that piece to the second letter, and that one back to the buffer. 3-style solves each pair " +
        "with a short commutator instead of a fixed swap algorithm.",
    "Commutators" to "[A, B] = A B A' B'. A is the insertion: it moves one target into the interchange layer " +
        "without disturbing the rest of that layer. B is the interchange: one turn of that layer that swaps the " +
        "target places. [S: [A, B]] = S [A, B] S' adds a setup S that first brings the pieces into a position where " +
        "a pure commutator works. Example: [R' D R, U2] is a pure corner commutator, [U2, M'] a 4-move edge commutator.",
    "How the cases are grouped" to "Cases are sorted by commutator class (4-movers, pure commutators, cancelling " +
        "setups (A9 type), 1-, 2-, 3-move setups) and within a class by the layer of the interchange. Each set has " +
        "at most 8 cases, and a case is always in the same set as its inverse (e.g. AB and BA): the inverse is the " +
        "same commutator with A and B swapped, so you learn two cases at once.",
    "Learning" to "Open a set in Learn, look at each case: the picture shows the cube before the algorithm, the " +
        "arrows show where the stickers go. Step through the algorithm, then press \"Practise this set\". " +
        "If you prefer another commutator, pick one of the alternatives or type your own; it is checked by " +
        "simulation before it is accepted.",
    "Practice" to "Practice works in small batches. A case leaves the batch when you answered it correctly " +
        "1-3 times in a row (setting). Again/Hard bring it back soon. The weakest and longest-unseen cases come " +
        "first. Cases marked as Known are left out unless you include them (review). They become known when their " +
        "strength reaches 5/5 (or when you mark them), and fall back to Learning when you forget them.",
    "Letters and memo" to "In the Letters tab you drill the letter scheme in both directions, give letter pairs " +
        "words or images and drill them, and trace random scrambles: the app checks your memo letter by letter. " +
        "At a cycle break any unsolved piece is accepted.",
    "Buffers and parity" to "Choose the buffers in Settings (default UFR and UF). With an odd number of targets " +
        "the last corner letter is solved with a parity algorithm that also swaps the edge buffer with the parity " +
        "edge (UF buffer: usually UR). With parity, add the parity edge letter after the last edge letter.",
    "Twists and flips" to "Pieces that are in place but twisted or flipped are solved with the twist and flip " +
        "algorithms (two pieces at a time), or by cycle breaking into them.",
    "Notation" to "U D F B R L: outer layers clockwise; ' = counter-clockwise, 2 = half turn. M (like L), E (like D), " +
        "S (like F): middle layers. r u f l d b (or Rw...): two layers. x y z: whole cube rotations. Moves are " +
        "counted in the slice turn metric.",
    "About the algorithms" to "All commutators were found by a computer search over setups of up to three moves " +
        "and pure commutators, preferring short algorithms with fast moves (R U D M first). They are correct by " +
        "construction and every one is checked again by simulation, but they are not hand-picked by a world-class " +
        "solver: if you know a better commutator for a case, add it.",
)

@Composable
fun GuideScreen(vm: AppViewModel) {
    SubScreen("Guide", { vm.back() }) {
        for ((title, text) in GUIDE) {
            item {
                SectionTitle(title)
                Text(text, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
