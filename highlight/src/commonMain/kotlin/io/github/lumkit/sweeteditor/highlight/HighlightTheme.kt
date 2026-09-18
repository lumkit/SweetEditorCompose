package io.github.lumkit.sweeteditor.highlight

import io.github.lumkit.sweeteditor.EditorTextStyle
import io.github.lumkit.sweeteditor.highlight.internal.HighlightStyleIds
import io.github.lumkit.sweeteditor.highlight.internal.dimArgb

data class HighlightTheme(
    val styles: Map<String, EditorTextStyle>,
    val fallback: EditorTextStyle,
    val rainbowBracketColors: List<Int>,
    val unmatchedBracketColor: Int,
) {
    init {
        require(rainbowBracketColors.size == 6) { "rainbowBracketColors must contain exactly 6 colors" }
    }
    companion object {
        // SweetLine KMP CodeView.kt BracketPalette + unmatched 0xFFFF6B6B.
        private val OFFICIAL_RAINBOW = listOf(
            argb(0xFF7DD3FC),
            argb(0xFFF9A8D4),
            argb(0xFFFDE047),
            argb(0xFF86EFAC),
            argb(0xFFC4B5FD),
            argb(0xFFFDBA74),
        )
        private val OFFICIAL_UNMATCHED = argb(0xFFFF6B6B)

        fun dark(): HighlightTheme = theme(
            fallback = style(0xFFD4D4D4),
            vocab = mapOf(
                HighlightStyleIds.KEYWORD to style(0xFF569CD6),
                HighlightStyleIds.STRING to style(0xFFCE9178),
                HighlightStyleIds.NUMBER to style(0xFFB5CEA8),
                HighlightStyleIds.COMMENT to style(0xFF6A9955),
                HighlightStyleIds.CLASS to style(0xFF4EC9B0),
                HighlightStyleIds.METHOD to style(0xFFDCDCAA),
                HighlightStyleIds.VARIABLE to style(0xFF9CDCFE),
                HighlightStyleIds.PUNCTUATION to style(0xFFD4D4D4),
                HighlightStyleIds.ANNOTATION to style(0xFFC586C0),
                HighlightStyleIds.BUILTIN to style(0xFF4FC1FF),
                HighlightStyleIds.PREPROCESSOR to style(0xFF9B9B9B),
                HighlightStyleIds.MACRO to style(0xFFDCDCAA),
                HighlightStyleIds.PROPERTY to style(0xFF9CDCFE),
                HighlightStyleIds.LIFETIME to style(0xFF4EC9B0),
                HighlightStyleIds.SELECTOR to style(0xFFD7BA7D),
                HighlightStyleIds.URL to style(0xFF3794FF),
            ),
            rainbow = OFFICIAL_RAINBOW,
            unmatched = OFFICIAL_UNMATCHED,
        )

        fun light(): HighlightTheme = theme(
            fallback = style(0xFF24292F),
            vocab = mapOf(
                HighlightStyleIds.KEYWORD to style(0xFF0000FF),
                HighlightStyleIds.STRING to style(0xFFA31515),
                HighlightStyleIds.NUMBER to style(0xFF098658),
                HighlightStyleIds.COMMENT to style(0xFF008000),
                HighlightStyleIds.CLASS to style(0xFF267F99),
                HighlightStyleIds.METHOD to style(0xFF795E26),
                HighlightStyleIds.VARIABLE to style(0xFF001080),
                HighlightStyleIds.PUNCTUATION to style(0xFF24292F),
                HighlightStyleIds.ANNOTATION to style(0xFF800000),
                HighlightStyleIds.BUILTIN to style(0xFF0000FF),
                HighlightStyleIds.PREPROCESSOR to style(0xFF808080),
                HighlightStyleIds.MACRO to style(0xFF795E26),
                HighlightStyleIds.PROPERTY to style(0xFF001080),
                HighlightStyleIds.LIFETIME to style(0xFF267F99),
                HighlightStyleIds.SELECTOR to style(0xFF800000),
                HighlightStyleIds.URL to style(0xFF0000EE),
            ),
            rainbow = OFFICIAL_RAINBOW,
            unmatched = OFFICIAL_UNMATCHED,
        )

        private fun theme(
            fallback: EditorTextStyle,
            vocab: Map<String, EditorTextStyle>,
            rainbow: List<Int>,
            unmatched: Int,
        ): HighlightTheme {
            require(rainbow.size == 6) { "rainbowBracketColors must contain exactly 6 colors" }
            val styles = LinkedHashMap<String, EditorTextStyle>(vocab.size + rainbow.size * 2 + 1)
            styles.putAll(vocab)
            HighlightStyleIds.RAINBOW_NAMES.forEachIndexed { index, name ->
                styles[name] = EditorTextStyle(color = rainbow[index])
            }
            HighlightStyleIds.RAINBOW_UNKNOWN_NAMES.forEachIndexed { index, name ->
                styles[name] = EditorTextStyle(color = dimArgb(rainbow[index]))
            }
            styles[HighlightStyleIds.BRACKET_UNMATCHED_NAME] = EditorTextStyle(color = unmatched)
            return HighlightTheme(
                styles = styles,
                fallback = fallback,
                rainbowBracketColors = rainbow,
                unmatchedBracketColor = unmatched,
            )
        }

        private fun argb(value: Long): Int = value.toInt()

        private fun style(value: Long): EditorTextStyle = EditorTextStyle(color = argb(value))
    }
}
