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
        if (styleId == HighlightStyleIds.BRACKET_UNMATCHED) {
            return theme.styles[HighlightStyleIds.BRACKET_UNMATCHED_NAME]
                ?: EditorTextStyle(color = theme.unmatchedBracketColor)
        }
        if (styleId in HighlightStyleIds.RAINBOW_0..HighlightStyleIds.RAINBOW_5) {
            val index = styleId - HighlightStyleIds.RAINBOW_0
            val name = HighlightStyleIds.RAINBOW_NAMES[index]
            return theme.styles[name] ?: EditorTextStyle(color = theme.rainbowBracketColors[index])
        }
        if (styleId in HighlightStyleIds.RAINBOW_UNKNOWN_0..HighlightStyleIds.RAINBOW_UNKNOWN_5) {
            val index = styleId - HighlightStyleIds.RAINBOW_UNKNOWN_0
            val name = HighlightStyleIds.RAINBOW_UNKNOWN_NAMES[index]
            return theme.styles[name] ?: EditorTextStyle(color = dimArgb(theme.rainbowBracketColors[index]))
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

internal fun dimArgb(color: Int, alpha: Float = 0.68f): Int {
    val a = (alpha * 255f).toInt().coerceIn(0, 255)
    return (a shl 24) or (color and 0x00FFFFFF)
}
