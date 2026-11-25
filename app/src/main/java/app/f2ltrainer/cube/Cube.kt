package app.f2ltrainer.cube

/**
 * Geometric 3x3x3 cube model.
 *
 * Coordinates: x points to R, y points to U, z points to F.  Every sticker is identified by
 * the position of its cubie (each coordinate in -1..1) and its outward normal.  Facelets are
 * numbered face by face in the order U R F D L B, 9 per face, row by row as the face is seen
 * from outside (U with B at the top edge, D with F at the top edge, side faces with U on top).
 *
 * A [CubeState] stores for every facelet position the id of the sticker that sits there; a
 * sticker's id is its home facelet, so `id / 9` is its colour (= the face it belongs to).
 *
 * All move permutations are derived from 3D rotations instead of hand-written tables, which
 * rules out typing mistakes in the tables.
 */
object Cube {
    const val U = 0
    const val R = 1
    const val F = 2
    const val D = 3
    const val L = 4
    const val B = 5
    const val FACE_NAMES = "URFDLB"

    data class Vec(val x: Int, val y: Int, val z: Int) {
        operator fun plus(o: Vec) = Vec(x + o.x, y + o.y, z + o.z)
        operator fun times(k: Int) = Vec(x * k, y * k, z * k)
        operator fun get(axis: Int) = when (axis) { 0 -> x; 1 -> y; else -> z }
        fun dot(o: Vec) = x * o.x + y * o.y + z * o.z

        /** Rotate by quarter * 90 degrees about the given axis (right-hand rule). */
        fun rotated(axis: Int, quarter: Int): Vec {
            var a = x; var b = y; var c = z
            repeat(((quarter % 4) + 4) % 4) {
                when (axis) {
                    0 -> { val ny = -c; val nz = b; b = ny; c = nz }
                    1 -> { val nz = -a; val nx = c; c = nz; a = nx }
                    else -> { val nx = -b; val ny = a; a = nx; b = ny }
                }
            }
            return Vec(a, b, c)
        }
    }

    val NORMALS = arrayOf(
        Vec(0, 1, 0), Vec(1, 0, 0), Vec(0, 0, 1), Vec(0, -1, 0), Vec(-1, 0, 0), Vec(0, 0, -1),
    )

    /** (right, down) vectors used to lay out the 3x3 grid of each face. */
    private val GRID = arrayOf(
        Vec(1, 0, 0) to Vec(0, 0, 1),   // U
        Vec(0, 0, -1) to Vec(0, -1, 0), // R
        Vec(1, 0, 0) to Vec(0, -1, 0),  // F
        Vec(1, 0, 0) to Vec(0, 0, -1),  // D
        Vec(0, 0, 1) to Vec(0, -1, 0),  // L
        Vec(-1, 0, 0) to Vec(0, -1, 0), // B
    )

    /** Cubie position of every facelet. */
    val POS: Array<Vec>
    /** Outward normal of every facelet. */
    val NORMAL: Array<Vec>
    private val index = HashMap<Pair<Vec, Vec>, Int>()

    init {
        val pos = ArrayList<Vec>()
        val nor = ArrayList<Vec>()
        for (f in 0 until 6) {
            val n = NORMALS[f]
            val (r, d) = GRID[f]
            for (row in 0 until 3) for (col in 0 until 3) {
                val p = n + r * (col - 1) + d * (row - 1)
                index[p to n] = pos.size
                pos.add(p); nor.add(n)
            }
        }
        POS = pos.toTypedArray()
        NORMAL = nor.toTypedArray()
    }

    fun facelet(pos: Vec, normal: Vec): Int = index[pos to normal] ?: error("no facelet $pos $normal")

    /** Facelets of the cubie at [pos]. */
    fun faceletsOf(pos: Vec): List<Int> = (0 until 54).filter { POS[it] == pos }

    /** Name like "UFR" / "FR" / "U" for a cubie position, faces in the order U D F B R L. */
    fun cubieName(pos: Vec): String {
        val sb = StringBuilder()
        for (f in "UDFBRL") if (NORMALS[FACE_NAMES.indexOf(f)].dot(pos) == 1) sb.append(f)
        return sb.toString()
    }

    fun cubie(name: String): Vec {
        var v = Vec(0, 0, 0)
        for (c in name) v += NORMALS[FACE_NAMES.indexOf(c)]
        return v
    }

    // ---- moves -------------------------------------------------------------------------
    private class Base(val axis: Int, val layers: IntArray, val quarter: Int)

    private val BASE = mapOf(
        'R' to Base(0, intArrayOf(1), -1), 'L' to Base(0, intArrayOf(-1), 1), 'M' to Base(0, intArrayOf(0), 1),
        'U' to Base(1, intArrayOf(1), -1), 'D' to Base(1, intArrayOf(-1), 1), 'E' to Base(1, intArrayOf(0), 1),
        'F' to Base(2, intArrayOf(1), -1), 'B' to Base(2, intArrayOf(-1), 1), 'S' to Base(2, intArrayOf(0), -1),
        'r' to Base(0, intArrayOf(1, 0), -1), 'l' to Base(0, intArrayOf(-1, 0), 1),
        'u' to Base(1, intArrayOf(1, 0), -1), 'd' to Base(1, intArrayOf(-1, 0), 1),
        'f' to Base(2, intArrayOf(1, 0), -1), 'b' to Base(2, intArrayOf(-1, 0), 1),
        'x' to Base(0, intArrayOf(-1, 0, 1), -1), 'y' to Base(1, intArrayOf(-1, 0, 1), -1),
        'z' to Base(2, intArrayOf(-1, 0, 1), -1),
    )

    val MOVE_LETTERS: Set<Char> = BASE.keys

    private val permCache = HashMap<String, IntArray>()

    /** perm[i] = facelet the sticker at facelet i moves to, for a single token like "R'" or "d2". */
    fun perm(token: String): IntArray = permCache.getOrPut(token) {
        val base = BASE[token[0]] ?: throw IllegalArgumentException("Unknown move '$token'")
        val power = when (token.substring(1)) {
            "" -> 1; "2" -> 2; "'" -> 3
            else -> throw IllegalArgumentException("Unknown move '$token'")
        }
        val q = base.quarter * power
        IntArray(54) { i ->
            val p = POS[i]
            if (p[base.axis] in base.layers) facelet(p.rotated(base.axis, q), NORMAL[i].rotated(base.axis, q)) else i
        }
    }
}

/** Immutable cube state: s[facelet] = sticker id. */
class CubeState(private val s: IntArray) {
    init { require(s.size == 54) }

    operator fun get(i: Int) = s[i]

    fun stickers(): IntArray = s.copyOf()

    fun applyMove(token: String): CubeState {
        val p = Cube.perm(token)
        val out = IntArray(54)
        for (i in 0 until 54) out[p[i]] = s[i]
        return CubeState(out)
    }

    fun apply(tokens: List<String>): CubeState {
        var c = this
        for (t in tokens) c = c.applyMove(t)
        return c
    }

    fun apply(alg: String): CubeState = apply(Alg.parse(alg))

    /** Face (0..5) where the centre of colour [colour] currently is. */
    fun centreFaceOf(colour: Int): Int {
        for (f in 0 until 6) if (s[f * 9 + 4] / 9 == colour) return f
        error("centre not found")
    }

    /**
     * Colour of every facelet expressed relative to the current centres: the result is the face
     * whose centre has that colour. This makes all checks independent of cube rotations.
     */
    fun relativeColours(): IntArray {
        val faceOfColour = IntArray(6) { centreFaceOf(it) }
        return IntArray(54) { faceOfColour[s[it] / 9] }
    }

    override fun equals(other: Any?) = other is CubeState && other.s.contentEquals(s)
    override fun hashCode() = s.contentHashCode()

    companion object {
        val SOLVED = CubeState(IntArray(54) { it })
    }
}
