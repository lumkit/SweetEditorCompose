package io.github.lumkit.sweeteditor.highlight.internal

import kotlin.test.Test
import kotlin.test.assertEquals

class LanguageIdTableTest {
    @Test
    fun resolveUsesLowercaseAliasesAndOverrides() {
        assertEquals("kotlin", LanguageIdTable.resolve("KT"))
        assertEquals("javascript", LanguageIdTable.resolve("js"))
        assertEquals("cpp", LanguageIdTable.resolve("c++"))
        assertEquals("yaml", LanguageIdTable.resolve("yml"))
        assertEquals("custom", LanguageIdTable.resolve("kotlin", mapOf("Kotlin" to "custom")))
    }

    @Test
    fun suffixTableMapsKotlinAndJsonFiles() {
        assertEquals(listOf("kotlin.json"), LanguageIdTable.syntaxFilesForFileName("Main.kt"))
        assertEquals(listOf("json-sweetline.json"), LanguageIdTable.suffixToSyntaxFiles[".json"])
    }
}
