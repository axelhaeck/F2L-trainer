package app.threestyle.bld

import app.threestyle.cube.Alg

enum class CaseType(val label: String, val short: String) {
    CORNER("Corners", "Corner"),
    EDGE("Edges", "Edge"),
    TWIST("Corner twists", "Twist"),
    FLIP("Edge flips", "Flip"),
    PARITY("Parity", "Parity"),
}

/**
 * One case.
 *
 *  * CORNER / EDGE: [stickers] = (x, y), the letter pair; buffer = buffer sticker.
 *  * TWIST: [stickers] = (p, q) reference stickers; p is twisted clockwise, q counter-clockwise.
 *  * FLIP: [stickers] = (p, q) reference stickers of the two flipped edges.
 *  * PARITY: [stickers] = (x, e, eb): corner target x, parity edge sticker e, edge buffer eb.
 */
class BldCase(
    val key: String,
    val type: CaseType,
    val buffer: Int,
    val stickers: List<Int>,
    val effect: IntArray,
    val solutions: List<Solution>,
    val group: String,
    val subgroup: String,
    /** Key of the inverse case (the same pieces, the other way round), if it is a different case. */
    val inverseKey: String?,
) {
    /** Short name shown on cards: the letter pair or piece names. */
    fun label(scheme: LetterScheme): String = when (type) {
        CaseType.CORNER, CaseType.EDGE -> "${scheme.letterOf(stickers[0])}${scheme.letterOf(stickers[1])}"
        CaseType.TWIST -> "${Stickers.pieceName(stickers[0])}↻ ${Stickers.pieceName(stickers[1])}↺"
        CaseType.FLIP -> "${Stickers.pieceName(stickers[0])} + ${Stickers.pieceName(stickers[1])}"
        CaseType.PARITY -> "${scheme.letterOf(stickers[0])}"
    }

    /** Longer description of what the case is. */
    fun describe(scheme: LetterScheme): String = when (type) {
        CaseType.CORNER, CaseType.EDGE -> {
            val (x, y) = stickers
            "${Stickers.stickerName(buffer)} → ${Stickers.stickerName(x)} (${scheme.letterOf(x)}) → " +
                "${Stickers.stickerName(y)} (${scheme.letterOf(y)})"
        }
        CaseType.TWIST -> "${Stickers.pieceName(stickers[0])} twisted clockwise, " +
            "${Stickers.pieceName(stickers[1])} twisted counter-clockwise"
        CaseType.FLIP -> "${Stickers.pieceName(stickers[0])} and ${Stickers.pieceName(stickers[1])} flipped"
        CaseType.PARITY -> "Swap ${Stickers.pieceName(buffer)} ↔ ${Stickers.stickerName(stickers[0])} " +
            "(${scheme.letterOf(stickers[0])}) and ${Stickers.pieceName(stickers[2])} ↔ " +
            Stickers.stickerName(stickers[1])
    }

    /** true if [alg] solves this case (checked by simulation). */
    fun isSolvedBy(alg: String): Boolean = Effects.solves(effect, alg)

    val state get() = Effects.caseState(effect)
}

/** A small set of cases to learn together. */
data class CaseSet(val id: String, val type: CaseType, val group: String, val title: String, val keys: List<String>)

/** Settings that determine the cases. */
data class BufferConfig(
    val cornerBuffer: Int = Stickers.byName("UFR"),
    val edgeBuffer: Int = Stickers.byName("UF"),
    /** The parity edge sticker: parity swaps the edge buffer with this sticker. */
    val parityEdge: Int = defaultParityEdge(Stickers.byName("UF")),
) {
    init {
        require(Stickers.typeOf(cornerBuffer) == PieceType.CORNER && Stickers.isReference(cornerBuffer))
        require(Stickers.typeOf(edgeBuffer) == PieceType.EDGE && Stickers.isReference(edgeBuffer))
        require(Stickers.typeOf(parityEdge) == PieceType.EDGE && !Stickers.samePiece(parityEdge, edgeBuffer))
    }

    companion object {
        /**
         * The usual parity edge: for the UF buffer UR (the J-perm swap); in general the next edge
         * clockwise on the face of the buffer sticker.
         */
        fun defaultParityEdge(edgeBuffer: Int): Int {
            val face = edgeBuffer / 9
            val ring = Stickers.SPEFFZ_EDGES.filter { it / 9 == face }
            val i = ring.indexOf(edgeBuffer)
            return ring[(i + 3) % 4]
        }
    }
}

/**
 * All cases for one [BufferConfig]. The commutators are loaded from the pre-generated asset files
 * via [readAsset] (file name -> content, null if missing); every algorithm is checked again by
 * simulation and dropped if it does not solve its case. If a file is missing the cases are solved
 * by the search instead (slower).
 */
class Library(val config: BufferConfig, private val readAsset: (String) -> String? = { null }) {
    private val loaded = HashMap<String, Map<String, List<Solution>>>()

    private fun file(name: String): Map<String, List<Solution>>? = loaded.getOrPut(name) {
        readAsset(name)?.let { AlgAssets.parse(it) } ?: emptyMap()
    }.takeIf { it.isNotEmpty() }

    val corners: List<BldCase> = cycleCases(CaseType.CORNER, config.cornerBuffer)
    val edges: List<BldCase> = cycleCases(CaseType.EDGE, config.edgeBuffer)
    val twists: List<BldCase> = twistCases(config.cornerBuffer)
    val flips: List<BldCase> = flipCases(config.edgeBuffer)
    private val byKey = HashMap<String, BldCase>().apply { for (c in corners + edges + twists + flips) put(c.key, c) }
    val parity: List<BldCase> = parityCases(config).onEach { byKey[it.key] = it }

    val all: List<BldCase> = corners + edges + twists + flips + parity

    fun case(key: String): BldCase? = byKey[key]

    fun cases(type: CaseType): List<BldCase> = when (type) {
        CaseType.CORNER -> corners
        CaseType.EDGE -> edges
        CaseType.TWIST -> twists
        CaseType.FLIP -> flips
        CaseType.PARITY -> parity
    }

    val sets: List<CaseSet> = CaseType.entries.flatMap { buildSets(it, cases(it)) }

    fun groups(type: CaseType): List<String> = sets.filter { it.type == type }.map { it.group }.distinct()

    fun setOf(caseKey: String): CaseSet? = sets.firstOrNull { caseKey in it.keys }

    private fun cycleCases(type: CaseType, buffer: Int): List<BldCase> {
        val pt = if (type == CaseType.CORNER) PieceType.CORNER else PieceType.EDGE
        val stored = file(AlgAssets.cycleFile(pt, buffer))
        val search by lazy { if (pt == PieceType.CORNER) cornerSearch else edgeSearch }
        val targets = Stickers.speffzOrder(pt).filter { !Stickers.samePiece(it, buffer) }
        val out = ArrayList<BldCase>()
        val solved = HashMap<Pair<Int, Int>, List<Solution>>()
        for (x in targets) for (y in targets) {
            if (Stickers.samePiece(x, y)) continue
            val effect = Effects.cycle(buffer, x, y)
            val sols = verified(effect, stored?.get("${Stickers.stickerName(x)} ${Stickers.stickerName(y)}"))
                ?: solved[y to x]?.map { it.inverse() }
                ?: search.solve(buffer, x, y)
            solved[x to y] = sols
            val (g, sub) = cycleGroup(sols.firstOrNull())
            out.add(BldCase(cycleKey(type, buffer, x, y), type, buffer, listOf(x, y), effect,
                verified(effect, sols) ?: emptyList(), g, sub, cycleKey(type, buffer, y, x)))
        }
        return out
    }

    private fun twistCases(buffer: Int): List<BldCase> {
        val stored = file(AlgAssets.TWIST_FILE)
        val pieces = Stickers.pieces(PieceType.CORNER)
        val out = ArrayList<BldCase>()
        for (p in pieces) for (q in pieces) {
            if (p == q) continue
            val effect = Effects.twistFix(p, q)
            val sols = verified(effect, stored?.get("${Stickers.pieceName(p)} ${Stickers.pieceName(q)}"))
                ?: twistSearch.solve(effect)
            val g = if (p == buffer || q == buffer) "With buffer" else "Without buffer"
            out.add(BldCase(twistKey(p, q), CaseType.TWIST, buffer, listOf(p, q), effect,
                verified(effect, sols) ?: emptyList(), g, g, twistKey(q, p)))
        }
        return out
    }

    private fun flipCases(buffer: Int): List<BldCase> {
        val stored = file(AlgAssets.FLIP_FILE)
        val pieces = Stickers.pieces(PieceType.EDGE)
        val out = ArrayList<BldCase>()
        for ((i, p) in pieces.withIndex()) for (q in pieces.drop(i + 1)) {
            val effect = Effects.flip(p, q)
            val sols = verified(effect, stored?.get("${Stickers.pieceName(p)} ${Stickers.pieceName(q)}"))
                ?: flipSearch.solve(effect)
            val g = if (p == buffer || q == buffer) "With buffer" else "Without buffer"
            out.add(BldCase(flipKey(p, q), CaseType.FLIP, buffer, listOf(p, q), effect,
                verified(effect, sols) ?: emptyList(), g, g, null))
        }
        return out
    }

    private fun parityCases(c: BufferConfig): List<BldCase> {
        val targets = Stickers.SPEFFZ_CORNERS.filter { !Stickers.samePiece(it, c.cornerBuffer) }
        fun effect(x: Int) = Effects.parity(c.cornerBuffer, x, c.edgeBuffer, c.parityEdge)
        val direct = targets.associateWith { x -> verified(effect(x), paritySearch.solve(effect(x))) }
        return targets.map { x ->
            val sols = direct[x] ?: verified(effect(x), composedParity(x, targets, direct))
            BldCase(parityKey(c, x), CaseType.PARITY, c.cornerBuffer, listOf(x, c.parityEdge, c.edgeBuffer), effect(x),
                sols ?: emptyList(), "Parity", "Parity", null)
        }
    }

    /**
     * For the few buffer combinations where no single parity algorithm is found for target x:
     * the corner commutator for the pair (x, z) followed by the parity algorithm for z. This is
     * correct because the 3-cycle buffer -> x -> z followed by the swap buffer <-> z equals the
     * swap buffer <-> x.
     */
    private fun composedParity(x: Int, targets: List<Int>, direct: Map<Int, List<Solution>?>): List<Solution> =
        targets.filter { z -> !Stickers.samePiece(z, x) && direct[z] != null }.mapNotNull { z ->
            val comm = byKey[cycleKey(CaseType.CORNER, config.cornerBuffer, x, z)]?.solutions?.firstOrNull()
                ?: return@mapNotNull null
            val par = direct.getValue(z)!!.first()
            val notation = "${comm.notation} ${par.notation}"
            val tokens = Alg.simplify(comm.moves + par.moves)
            Solution(notation, tokens, MoveCost.score(tokens), emptyList(), primitive = notation)
        }.sortedBy { it.score }.take(3)

    companion object {
        private val cornerSearch by lazy { CycleSearch(PieceType.CORNER) }
        private val edgeSearch by lazy { CycleSearch(PieceType.EDGE) }
        private val twistSearch by lazy { PrimitiveSearch(Primitives.TWISTS) }
        private val flipSearch by lazy { PrimitiveSearch(Primitives.FLIPS) }
        private val paritySearch by lazy { PrimitiveSearch(Primitives.PARITY_VARIANTS) }

        /** Order in which the commutator classes are taught. */
        val GROUP_ORDER = listOf(
            "4-movers", "Pure commutators", "Cancelling setup (A9 type)", "1-move setup",
            "2-move setup", "3-move setup",
            "With buffer", "Without buffer", "Parity",
        )
        private const val INTERCHANGE_ORDER = "UDRFLBMES"
        const val MAX_SET = 8

        fun cycleKey(type: CaseType, buffer: Int, x: Int, y: Int) =
            "${if (type == CaseType.CORNER) "C" else "E"}:${Stickers.pieceName(buffer)}:" +
                "${Stickers.stickerName(x)}:${Stickers.stickerName(y)}"

        fun twistKey(p: Int, q: Int) = "T:${Stickers.pieceName(p)}:${Stickers.pieceName(q)}"
        fun flipKey(p: Int, q: Int) = "F:${Stickers.pieceName(p)}:${Stickers.pieceName(q)}"
        fun parityKey(c: BufferConfig, x: Int) = "P:${Stickers.pieceName(c.cornerBuffer)}:" +
            "${Stickers.pieceName(c.edgeBuffer)}:${Stickers.stickerName(c.parityEdge)}:${Stickers.stickerName(x)}"

        /** Keeps the algorithms that really solve the case; null if none is left. */
        private fun verified(effect: IntArray, sols: List<Solution>?): List<Solution>? =
            sols?.filter { Effects.solves(effect, it.notation) && Effects.solves(effect, Alg.format(it.moves)) }
                ?.takeIf { it.isNotEmpty() }

        private fun cycleGroup(s: Solution?): Pair<String, String> {
            if (s == null) return "3-move setup" to "No algorithm found"
            val group = when {
                s.setup.isEmpty() && s.partA.size == 1 && s.partB.size == 1 -> "4-movers"
                s.setup.isEmpty() -> "Pure commutators"
                s.setup.size == 1 && s.cancels -> "Cancelling setup (A9 type)"
                s.setup.size == 1 -> "1-move setup"
                s.setup.size == 2 -> "2-move setup"
                else -> "3-move setup"
            }
            val ic = s.interchange
            val sub = if (ic == null) "Two insertions" else "${ic[0][0]} interchange"
            return group to sub
        }

        /**
         * Sets: cases of one class and one interchange layer, at most [MAX_SET] per set, a case and
         * its inverse always in the same set.
         */
        private fun buildSets(type: CaseType, cases: List<BldCase>): List<CaseSet> {
            val out = ArrayList<CaseSet>()
            val byGroup = cases.groupBy { it.group }
            val groups = byGroup.keys.sortedBy { GROUP_ORDER.indexOf(it).let { i -> if (i < 0) 99 else i } }
            for (g in groups) {
                val bySub = byGroup.getValue(g).groupBy { it.subgroup }
                val subs = bySub.keys.sortedBy { s ->
                    if (s.endsWith(" interchange")) INTERCHANGE_ORDER.indexOf(s[0]).let { if (it < 0) 98 else it } else 99
                }
                for (sub in subs) {
                    // units = a case together with its inverse
                    val units = ArrayList<List<BldCase>>()
                    val seen = HashSet<String>()
                    val list = bySub.getValue(sub)
                    val keys = list.associateBy { it.key }
                    for (c in list) {
                        if (c.key in seen) continue
                        val inv = c.inverseKey?.let { keys[it] }
                        val unit = if (inv != null && inv.key !in seen && inv !== c) listOf(c, inv) else listOf(c)
                        unit.forEach { seen.add(it.key) }
                        units.add(unit)
                    }
                    val total = units.sumOf { it.size }
                    val nSets = (total + MAX_SET - 1) / MAX_SET
                    val target = (total + nSets - 1) / nSets
                    val chunks = ArrayList<ArrayList<BldCase>>()
                    chunks.add(ArrayList())
                    for (u in units) {
                        if (chunks.last().size + u.size > target) chunks.add(ArrayList())
                        chunks.last().addAll(u)
                    }
                    for ((i, ch) in chunks.withIndex()) {
                        val subTitle = if (sub == g) "" else " · $sub"
                        val num = if (chunks.size > 1) " (${i + 1}/${chunks.size})" else ""
                        out.add(CaseSet("${type.name}/$g/$sub/$i", type, g, "$g$subTitle$num", ch.map { it.key }))
                    }
                }
            }
            return out
        }
    }
}
