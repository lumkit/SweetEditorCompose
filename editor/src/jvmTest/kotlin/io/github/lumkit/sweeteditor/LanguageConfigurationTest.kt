package io.github.lumkit.sweeteditor

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LanguageConfigurationTest {
    @Test
    fun builderKeepsNullListsUntilAdded() {
        val empty = LanguageConfiguration.builder("kotlin").build()
        assertEquals("kotlin", empty.languageId)
        assertNull(empty.brackets)
        assertNull(empty.autoClosingPairs)
        assertNull(empty.tabSize)
        assertNull(empty.insertSpaces)
    }

    @Test
    fun builderCopiesPairsAndCodePoints() {
        val config = LanguageConfiguration.builder("kotlin")
            .addBracket("{", "}")
            .addAutoClosingPair("(", ")")
            .setTabSize(2)
            .setInsertSpaces(true)
            .build()
        assertEquals(listOf(BracketPair("{", "}")), config.brackets)
        assertEquals(listOf(BracketPair("(", ")")), config.autoClosingPairs)
        val (opens, closes) = requireNotNull(config.brackets).toCodePointArrays()
        assertEquals('{'.code, opens.single())
        assertEquals('}'.code, closes.single())
    }
}
