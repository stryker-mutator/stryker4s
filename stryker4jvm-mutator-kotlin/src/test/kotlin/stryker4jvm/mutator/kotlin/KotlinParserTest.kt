package stryker4jvm.mutator.kotlin

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class KotlinParserTest {
    private val parser = KotlinParser()

    @Test
    fun testParser() {
        val path = Path.of(ClassLoader.getSystemResource("SimpleClass.txt").toURI())
        val ast = parser.parse(path)
        val string = Files.readString(path)
        assertEquals(string, ast.syntax())
    }
}