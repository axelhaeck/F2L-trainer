package app.f2ltrainer.cube

/** Helpers for move sequences written in standard notation (e.g. "R U R' U'", "y' U2 d"). */
object Alg {

    /** Parse an algorithm. Accepts brackets, "2'" (= "2"), typographic apostrophes and "i" suffix. */
    fun parse(alg: String): List<String> {
        val cleaned = alg.replace('(', ' ').replace(')', ' ').replace('[', ' ').replace(']', ' ')
            .replace('’', '\'').replace('′', '\'').replace('`', '\'')
        val out = ArrayList<String>()
        var i = 0
        while (i < cleaned.length) {
            val c = cleaned[i]
            if (c.isWhitespace()) { i++; continue }
            if (c !in Cube.MOVE_LETTERS) throw IllegalArgumentException("Unknown move '$c'")
            var j = i + 1
            // Accept "Rw" as wide move r.
            var letter = c
            if (j < cleaned.length && cleaned[j] == 'w' && c in "RLUDFB") { letter = c.lowercaseChar(); j++ }
            var suffix = ""
            if (j < cleaned.length && cleaned[j] == '2') { suffix = "2"; j++ }
            if (j < cleaned.length && cleaned[j] == '\'') { suffix = if (suffix == "2") "2" else "'"; j++ }
            if (j < cleaned.length && cleaned[j] == 'i' && suffix == "") { suffix = "'"; j++ }
            if (j < cleaned.length && !cleaned[j].isWhitespace()) {
                // allow tokens written without spaces, e.g. "RUR'", by continuing the loop
                if (cleaned[j] !in Cube.MOVE_LETTERS) throw IllegalArgumentException("Unexpected '${cleaned[j]}'")
            }
            out.add("$letter$suffix")
            i = j
        }
        return out
    }

    fun format(tokens: List<String>): String = tokens.joinToString(" ")

    fun power(token: String): Int = when (token.substring(1)) { "" -> 1; "2" -> 2; else -> 3 }

    fun token(letter: Char, power: Int): String? = when (((power % 4) + 4) % 4) {
        0 -> null; 1 -> "$letter"; 2 -> "${letter}2"; else -> "$letter'"
    }

    fun invertToken(t: String): String = token(t[0], 4 - power(t))!!

    fun invert(tokens: List<String>): List<String> = tokens.reversed().map { invertToken(it) }

    fun isRotation(t: String) = t[0] in "xyz"

    /** Moves counted in the half-turn metric (rotations are free, slice moves count as one). */
    fun moveCount(tokens: List<String>): Int = tokens.count { !isRotation(it) }

    /**
     * Rewrite a sequence so that it contains only outer face turns (U R F D L B), followed by
     * the whole-cube rotations that the original sequence performs in total.
     * Returns (faceMoves, rotations). Applying faceMoves then rotations equals the original.
     */
    fun toFaceMoves(tokens: List<String>): Pair<List<String>, List<String>> {
        // orient[p] = original face (colour) currently at position p
        var orient = IntArray(6) { it }
        val faces = ArrayList<String>()
        val rots = ArrayList<String>()

        fun rotate(axisToken: String) {
            val p = Cube.perm(axisToken)
            // centre of face f is facelet f*9+4; find where each position's content goes
            val n = IntArray(6)
            for (f in 0 until 6) n[p[f * 9 + 4] / 9] = orient[f]
            orient = n
            rots.add(axisToken)
        }

        fun face(letter: Char, power: Int) {
            val pos = Cube.FACE_NAMES.indexOf(letter)
            faces.add(token(Cube.FACE_NAMES[orient[pos]], power)!!)
        }

        for (t in tokens) {
            val pw = power(t)
            when (t[0]) {
                'U', 'R', 'F', 'D', 'L', 'B' -> face(t[0], pw)
                'x', 'y', 'z' -> repeat(pw) { rotate("${t[0]}") }
                // wide moves: opposite face turn + rotation
                'r' -> repeat(pw) { face('L', 1); rotate("x") }
                'l' -> repeat(pw) { face('R', 1); rotate("x'") }
                'u' -> repeat(pw) { face('D', 1); rotate("y") }
                'd' -> repeat(pw) { face('U', 1); rotate("y'") }
                'f' -> repeat(pw) { face('B', 1); rotate("z") }
                'b' -> repeat(pw) { face('F', 1); rotate("z'") }
                // slice moves
                'M' -> repeat(pw) { face('R', 1); face('L', 3); rotate("x'") }
                'E' -> repeat(pw) { face('U', 1); face('D', 3); rotate("y'") }
                'S' -> repeat(pw) { face('F', 3); face('B', 1); rotate("z") }
                else -> throw IllegalArgumentException(t)
            }
        }
        return simplify(faces) to rots
    }

    /** Merge consecutive turns of the same layer (e.g. "U U2" -> "U'"), repeatedly. */
    fun simplify(tokens: List<String>): List<String> {
        val out = ArrayList<String>()
        for (t in tokens) {
            if (out.isNotEmpty() && out.last()[0] == t[0]) {
                val merged = token(t[0], power(out.last()) + power(t))
                out.removeAt(out.size - 1)
                if (merged != null) out.add(merged)
            } else out.add(t)
        }
        return out
    }

    /**
     * Prefix an algorithm with [auf] quarter turns of U, merging the turn into the algorithm:
     * leading y-rotations are moved in front (U commutes with them), a following U is merged and
     * a leading d is rewritten as y' U (d = y' U exactly).
     */
    fun prefixU(auf: Int, tokens: List<String>): List<String> {
        val j = ((auf % 4) + 4) % 4
        if (j == 0) return tokens
        val rots = ArrayList<String>()
        var i = 0
        while (i < tokens.size && tokens[i][0] == 'y') { rots.add(tokens[i]); i++ }
        var rest = tokens.subList(i, tokens.size).toMutableList()
        if (rots.isEmpty() && rest.isNotEmpty() && rest[0][0] == 'd') {
            val p = power(rest[0])
            rots.add(token('y', -p)!!)
            rest[0] = token('U', p)!!
        }
        if (rest.isNotEmpty() && rest[0][0] == 'U') {
            val m = token('U', power(rest[0]) + j)
            rest = (listOfNotNull(m) + rest.drop(1)).toMutableList()
        } else {
            rest.add(0, token('U', j)!!)
        }
        return rots + rest
    }
}
