package io.github.lumkit.sweeteditor.highlight.internal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class SyntaxCatalogTest {
    @Test
    fun kotlinJsonStartsWithObjectBrace() = runBlocking {
        val json = SyntaxCatalog().loadJson("kotlin")
        assertNotNull(json)
        assertTrue(json.trimStart().startsWith("{"), json.take(32))
        assertEquals("kotlin", syntaxNameOfJson(json))
    }
}
