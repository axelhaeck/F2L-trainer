package app.threestyle

import app.threestyle.bld.AlgAssets
import app.threestyle.bld.BufferConfig
import app.threestyle.bld.Effects
import app.threestyle.bld.Library
import app.threestyle.bld.PieceType
import app.threestyle.bld.Stickers
import app.threestyle.cube.Alg
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Every shipped algorithm, for every buffer, must solve its case. */
class AssetsTest {
    private fun read(name: String) = TestAssets.reader(name) ?: error("missing asset $name")

    private fun checkFile(name: String, expectedLines: Int, effectOf: (String, String) -> IntArray) {
        val lines = read(name).lines().filter { it.isNotBlank() && !it.startsWith("#") }
        assertEquals(name, expectedLines, lines.size)
        val names = HashSet<String>()
        for (line in lines) {
            val f = line.split('\t')
            val (a, b) = f[0].split(' ')
            assertTrue("$name: duplicate ${f[0]}", names.add(f[0]))
            assertTrue("$name ${f[0]}: no algorithm", f.size >= 2)
            val effect = effectOf(a, b)
            for (enc in f.drop(1)) {
                val sol = AlgAssets.decode(enc)
                assertTrue("$name ${f[0]}: ${sol.notation}", Effects.solves(effect, sol.notation))
                assertTrue("$name ${f[0]}: moves", Effects.solves(effect, Alg.format(sol.moves)))
            }
        }
    }

    @Test fun cornerFilesForEveryBuffer() {
        for (buf in Stickers.pieces(PieceType.CORNER)) {
            checkFile(AlgAssets.cycleFile(PieceType.CORNER, buf), 378) { x, y ->
                Effects.cycle(buf, Stickers.byName(x), Stickers.byName(y))
            }
        }
    }

    @Test fun edgeFilesForEveryBuffer() {
        for (buf in Stickers.pieces(PieceType.EDGE)) {
            checkFile(AlgAssets.cycleFile(PieceType.EDGE, buf), 440) { x, y ->
                Effects.cycle(buf, Stickers.byName(x), Stickers.byName(y))
            }
        }
    }

    @Test fun twistAndFlipFiles() {
        checkFile(AlgAssets.TWIST_FILE, 56) { p, q -> Effects.twistFix(Stickers.byName(p), Stickers.byName(q)) }
        checkFile(AlgAssets.FLIP_FILE, 66) { p, q -> Effects.flip(Stickers.byName(p), Stickers.byName(q)) }
    }

    /** Libraries for all buffer combinations load completely from the assets (no search needed). */
    @Test fun everyBufferLoads() {
        val corners = Stickers.pieces(PieceType.CORNER)
        val edges = Stickers.pieces(PieceType.EDGE)
        for (i in 0 until maxOf(corners.size, edges.size)) {
            val eb = edges[i % edges.size]
            val cfg = BufferConfig(corners[i % corners.size], eb, BufferConfig.defaultParityEdge(eb))
            val lib = Library(cfg, TestAssets.reader)
            for (c in lib.all) assertTrue("${c.key} has no algorithm", c.solutions.isNotEmpty())
            assertEquals(378 + 440 + 56 + 66 + 21, lib.all.size)
            for (s in lib.sets) assertTrue(s.title, s.keys.size in 1..Library.MAX_SET)
        }
    }

    /** Parity is solved at runtime: check all corner buffers with all edge buffers and all parity edges of UF. */
    @Test fun parityForAllBufferCombinations() {
        for (cb in Stickers.pieces(PieceType.CORNER)) for (eb in Stickers.pieces(PieceType.EDGE)) {
            val cfg = BufferConfig(cb, eb, BufferConfig.defaultParityEdge(eb))
            val lib = Library(cfg, TestAssets.reader)
            for (c in lib.parity) {
                assertTrue("${c.key} has no algorithm", c.solutions.isNotEmpty())
                for (s in c.solutions) assertTrue(c.key, c.isSolvedBy(s.notation))
            }
        }
        val uf = Stickers.byName("UF")
        for (pe in Stickers.SPEFFZ_EDGES.filter { !Stickers.samePiece(it, uf) }) {
            val lib = Library(BufferConfig(Stickers.byName("UFR"), uf, pe), TestAssets.reader)
            for (c in lib.parity) assertTrue("${c.key} has no algorithm", c.solutions.isNotEmpty())
        }
    }
}
