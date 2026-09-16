package io.github.lumkit.sweeteditor.highlight.internal

import io.github.lumkit.sweeteditor.EditorTextStyle
import io.github.lumkit.sweeteditor.highlight.HighlightTheme

internal class StyleRegistry(
    theme: HighlightTheme,
) {
    private var theme: HighlightTheme = theme
    private val dynamicIds = LinkedHashMap<String, Int>()
    private var nextDynamicId: Int = HighlightStyleIds.DYNAMIC_START

    fun updateTheme(theme: HighlightTheme) {
        this.theme = theme
    }

    fun idFor(name: String): Int {
        HighlightStyleIds.VOCABULARY[name]?.let { return it }
        return dynamicIds.getOrPut(name) { nextDynamicId++ }
    }

    fun textStyle(styleId: Int): EditorTextStyle {
        if (styleId in HighlightStyleIds.RAINBOW_0..HighlightStyleIds.RAINBOW_5) {
            val index = styleId - HighlightStyleIds.RAINBOW_0
            val name = HighlightStyleIds.RAINBOW_NAMES[index]
            return theme.styles[name] ?: EditorTextStyle(color = theme.rainbowBracketColors[index])
        }
        val name = HighlightStyleIds.NAME_BY_ID[styleId]
        if (name != null) {
            return theme.styles[name] ?: theme.fallback
        }
        return theme.fallback
    }

    fun batchTextStyles(): Map<Int, EditorTextStyle> {
        val styles = LinkedHashMap<Int, EditorTextStyle>(HighlightStyleIds.VOCABULARY.size + dynamicIds.size)
        HighlightStyleIds.VOCABULARY.forEach { (_, id) ->
            styles[id] = textStyle(id)
        }
        dynamicIds.values.forEach { id ->
            styles[id] = theme.fallback
        }
        return styles
    }

    fun namesToRegister(): Map<String, Int> {
        val names = LinkedHashMap<String, Int>(HighlightStyleIds.VOCABULARY.size + dynamicIds.size)
        names.putAll(HighlightStyleIds.VOCABULARY)
        names.putAll(dynamicIds)
        return names
    }
}
