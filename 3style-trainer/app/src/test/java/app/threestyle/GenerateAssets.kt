package app.threestyle

import app.threestyle.bld.AlgAssets
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/** Regenerates the algorithm files (only with -PgenerateAssets). */
class GenerateAssets {
    @Test fun generate() {
        assumeTrue(System.getProperty("generateAssets") == "true")
        val dir = File("src/main/assets")
        for ((name, content) in AlgAssets.generateAll()) {
            val f = File(dir, name)
            f.parentFile.mkdirs()
            f.writeText(content)
            println("wrote $f")
        }
    }
}
