package app.threestyle.cube

/**
 * Move sequences in standard notation, including commutator notation:
 *
 *  * `[A, B]`  = A B A' B'   (commutator)
 *  * `[A: B]`  = A B A'      (conjugate; `[A; B]` is accepted too)
 *  * `(A)n`    = A repeated n times
 *
 * Moves: U D F B R L (faces), M E S (slices), u d f b r l or Uw.. (wide), x y z (rotations), with
 * the suffixes 2, ' and 2'. Nesting is allowed, e.g. `[R' D: [R U R', D2]]`.
 */
object Alg {

    class ParseException(message: String) : IllegalArgumentException(message)

    /** Parse an algorithm into single-move tokens like "R", "U2", "M'". */
    fun parse(alg: String): List<String> {
        val p = Parser(normalise(alg))
        val tokens = p.sequence()
        p.skipSpace()
        if (!p.atEnd()) throw ParseException("Unexpected '${p.peek()}'")
        return tokens
    }

    /** true if [alg] can be parsed. */
    fun isValid(alg: String): Boolean = try { parse(alg); true } catch (_: IllegalArgumentException) { false }

    private fun normalise(s: String) = s
        .replace('’', '\'').replace('′', '\'').replace('`', '\'').replace('´', '\'')
        .replace('‘', '\'')

    private class Parser(val s: String) {
        var i = 0
        fun atEnd() = i >= s.length
        fun peek() = s[i]
        fun skipSpace() { while (i < s.length && s[i].isWhitespace()) i++ }

        fun sequence(): List<String> {
            val out = ArrayList<String>()
            while (true) {
                skipSpace()
                if (atEnd()) break
                val c = s[i]
                when {
                    c == '[' -> out += bracket()
                    c == '(' -> out += paren()
                    c in Cube.MOVE_LETTERS -> move()?.let { out += it }
                    else -> break
                }
            }
            return out
        }

        private fun expect(c: Char) {
            skipSpace()
            if (atEnd()) throw ParseException("Missing '$c'")
            if (s[i] != c) throw ParseException("Expected '$c' but found '${s[i]}'")
            i++
        }

        private fun bracket(): List<String> {
            i++ // [
            val a = sequence()
            skipSpace()
            if (atEnd()) throw ParseException("Missing ']'")
            return when (s[i]) {
                ',' -> { i++; val b = sequence(); expect(']'); a + b + invert(a) + invert(b) }
                ':', ';' -> { i++; val b = sequence(); expect(']'); a + b + invert(a) }
                ']' -> { i++; a }
                else -> throw ParseException("Unexpected '${s[i]}'")
            }
        }

        private fun paren(): List<String> {
            i++ // (
            val a = sequence()
            expect(')')
            var n = 0
            var digits = 0
            while (i < s.length && s[i].isDigit()) { n = n * 10 + (s[i] - '0'); i++; digits++ }
            if (digits == 0) return a
            if (n > 99) throw ParseException("Repetition too large")
            val out = ArrayList<String>()
            repeat(n) { out += a }
            return out
        }

        private fun move(): String? {
            var letter = s[i]
            i++
            if (i < s.length && s[i] == 'w') {
                if (letter !in "RLUDFB") throw ParseException("Unknown move '${letter}w'")
                letter = letter.lowercaseChar(); i++
            }
            var power = 1
            if (i < s.length && s[i].isDigit()) {
                var n = 0
                while (i < s.length && s[i].isDigit()) { n = (n * 10 + (s[i] - '0')) % 4; i++ }
                power = n
            }
            if (i < s.length && s[i] == '\'') { power = -power; i++ }
            if (i < s.length && s[i].isLetterOrDigit() && s[i] !in Cube.MOVE_LETTERS) {
                throw ParseException("Unknown move '$letter${s[i]}'")
            }
            return token(letter, power)
        }
    }

    fun format(tokens: List<String>): String = tokens.joinToString(" ")

    fun power(token: String): Int = when (token.substring(1)) {
        "" -> 1; "2" -> 2; "'" -> 3
        else -> throw IllegalArgumentException(token)
    }

    fun token(letter: Char, power: Int): String? = when (((power % 4) + 4) % 4) {
        0 -> null; 1 -> "$letter"; 2 -> "${letter}2"; else -> "$letter'"
    }

    fun invertToken(t: String): String = token(t[0], 4 - power(t))!!

    fun invert(tokens: List<String>): List<String> = tokens.reversed().map { invertToken(it) }

    fun isRotation(t: String) = t[0] in "xyz"

    fun isSlice(t: String) = t[0] in "MES"

    /** Axis of a move: 0 = x (R L M r l x), 1 = y (U D E u d y), 2 = z (F B S f b z). */
    fun axis(letter: Char): Int = when (letter) {
        'R', 'L', 'M', 'r', 'l', 'x' -> 0
        'U', 'D', 'E', 'u', 'd', 'y' -> 1
        'F', 'B', 'S', 'f', 'b', 'z' -> 2
        else -> throw IllegalArgumentException("Unknown move '$letter'")
    }

    /** Slice turn metric: every turn counts 1 (also slices and wide turns), rotations are free. */
    fun moveCount(tokens: List<String>): Int = tokens.count { !isRotation(it) }

    /**
     * Cancel and merge moves: turns of the same layer are merged even when separated by turns that
     * commute with them (turns about the same axis), e.g. "R L R'" -> "L", "U U2" -> "U'".
     */
    fun simplify(tokens: List<String>): List<String> {
        val out = ArrayList<String>()
        for (t in tokens) {
            var j = out.size - 1
            var merged = false
            val ax = axis(t[0])
            while (j >= 0 && axis(out[j][0]) == ax) {
                if (out[j][0] == t[0]) {
                    val m = token(t[0], power(out[j]) + power(t))
                    if (m == null) out.removeAt(j) else out[j] = m
                    merged = true
                    break
                }
                j--
            }
            if (!merged) out.add(t)
        }
        return out
    }
}

/** Permutations of the 54 facelets: p[i] = facelet that the sticker at i moves to. */
object Perm {
    val IDENTITY: IntArray = IntArray(54) { it }

    /** First [a], then [b]. */
    fun then(a: IntArray, b: IntArray): IntArray = IntArray(54) { b[a[it]] }

    fun inverse(p: IntArray): IntArray {
        val r = IntArray(54)
        for (i in 0 until 54) r[p[i]] = i
        return r
    }

    fun of(tokens: List<String>): IntArray {
        var p = IDENTITY
        for (t in tokens) p = then(p, Cube.perm(t))
        return p
    }

    fun of(alg: String): IntArray = of(Alg.parse(alg))

    /** Facelets that are moved. */
    fun moved(p: IntArray): List<Int> = (0 until 54).filter { p[it] != it }
}
