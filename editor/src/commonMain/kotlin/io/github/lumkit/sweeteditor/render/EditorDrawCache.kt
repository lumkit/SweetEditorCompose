package io.github.lumkit.sweeteditor.render

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints

/**
 * Reuses [TextStyle] copies and line-number layouts across scroll frames.
 * Body glyphs go through [TextMeasurer]'s own cache; this map only keeps the
 * style objects stable so that cache can hit.
 */
internal class EditorDrawCache {
    private var baseStyle: TextStyle? = null
    private val runStyles = HashMap<StyleKey, TextStyle>(32)
    private val lineNumbers = object : LinkedHashMap<LineKey, TextLayoutResult>(128, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<LineKey, TextLayoutResult>?) = size > 256
    }

    fun bind(style: TextStyle) {
        if (baseStyle == style) return
        baseStyle = style
        runStyles.clear()
        lineNumbers.clear()
    }

    fun runStyle(color: Color, fontFlags: Int): TextStyle {
        val base = baseStyle ?: TextStyle()
        val key = StyleKey(color.value, fontFlags)
        return runStyles.getOrPut(key) {
            base.copy(
                color = color,
                fontWeight = if ((fontFlags and 1) != 0) FontWeight.Bold else FontWeight.Normal,
                fontStyle = if ((fontFlags and 2) != 0) FontStyle.Italic else FontStyle.Normal,
                fontFamily = base.fontFamily ?: FontFamily.Monospace,
            )
        }
    }

    fun lineNumberLayout(measurer: TextMeasurer, lineNumber: Int, color: Color): TextLayoutResult {
        val key = LineKey(lineNumber, color.value)
        return lineNumbers.getOrPut(key) {
            measurer.measure(
                lineNumber.toString(),
                runStyle(color, fontFlags = 0),
                constraints = Constraints(),
            )
        }
    }

    private data class StyleKey(val color: ULong, val fontFlags: Int)
    private data class LineKey(val lineNumber: Int, val color: ULong)
}
