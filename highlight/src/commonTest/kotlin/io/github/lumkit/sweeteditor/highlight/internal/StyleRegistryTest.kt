package io.github.lumkit.sweeteditor.highlight.internal

import io.github.lumkit.sweeteditor.highlight.HighlightTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StyleRegistryTest {
    @Test
    fun keywordIdIsOne() {
        val registry = StyleRegistry(HighlightTheme.dark())
        assertEquals(1, registry.idFor(HighlightStyleIds.KEYWORD))
        assertEquals(1, registry.idFor("keyword"))
    }

    @Test
    fun unknownNamesAllocateFromTwoHundred() {
        val registry = StyleRegistry(HighlightTheme.dark())
        assertEquals(200, registry.idFor("custom.token"))
        assertEquals(201, registry.idFor("another.token"))
        assertEquals(200, registry.idFor("custom.token"))
        assertTrue(registry.idFor("custom.token") >= 200)
        assertTrue(registry.idFor("another.token") >= 200)
    }

    @Test
    fun darkThemeGivesNonZeroForegroundForEveryVocabName() {
        val dark = HighlightTheme.dark()
        val light = HighlightTheme.light()
        assertEquals(6, dark.rainbowBracketColors.size)
        assertEquals(6, light.rainbowBracketColors.size)
        HighlightStyleIds.VOCABULARY.keys.forEach { name ->
            val darkStyle = requireNotNull(dark.styles[name]) { "dark missing $name" }
            val lightStyle = requireNotNull(light.styles[name]) { "light missing $name" }
            assertTrue(darkStyle.color != 0, "dark $name")
            assertTrue(lightStyle.color != 0, "light $name")
        }
        assertTrue(dark.fallback.color != 0)
        assertTrue(light.fallback.color != 0)
    }
}
