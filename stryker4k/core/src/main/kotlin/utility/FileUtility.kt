package utility

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

object FileUtility {
    fun getAbsolutePath(string: String): String = File(string).absolutePath
    fun readDir(path: String): Iterable<String> = File(path).walk().map { it.path }.asIterable()
    fun readFile(file: File): String = file.readText(Charsets.UTF_8)
    fun readFile(file: String): String = File(file).readText(Charsets.UTF_8).replace("\r\n", "\n")
    fun writeFile(path: String, text: String) = File(path).writeText(text)
    fun createTempDir(): String = Files.createTempDirectory("stryker4k-").toString()
    fun createDir(path: String): String = Files.createDirectories(Paths.get(path)).toString()
    fun copyFileTo(from: String, to: String): String = Files.copy(Paths.get(from), Paths.get(to)).toString()

    fun deleteTempDir(path: String) = Files.walk(Paths.get(path))
        .sorted(Comparator.reverseOrder())
        .map(Path::toFile)
        .forEach { it.delete() }
}
