package app.threestyle

import java.io.File

/** Reads the app's asset files from the source tree (unit tests run in the module directory). */
object TestAssets {
    val reader: (String) -> String? = { name -> File("src/main/assets", name).takeIf { it.exists() }?.readText() }
}
