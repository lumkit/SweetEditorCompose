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
    private val lineNumbers = HashMap<LineKey, TextLayoutResult>(128)
    private val lineNumberOrder = ArrayDeque<LineKey>()

    fun bind(style: TextStyle) {
        if (baseStyle == style) return
        baseStyle = style
        runStyles.clear()
        lineNumbers.clear()
        lineNumberOrder.clear()
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
        lineNumbers[key]?.let { cached ->
            lineNumberOrder.remove(key)
            lineNumberOrder.addLast(key)
            return cached
        }
        val layout = measurer.measure(
            lineNumber.toString(),
            runStyle(color, fontFlags = 0),
            constraints = Constraints(),
        )
        lineNumbers[key] = layout
        lineNumberOrder.addLast(key)
        while (lineNumbers.size > MaxLineNumberLayouts) {
            lineNumbers.remove(lineNumberOrder.removeFirst())
        }
        return layout
    }

    private data class StyleKey(val color: ULong, val fontFlags: Int)
    private data class LineKey(val lineNumber: Int, val color: ULong)

    private companion object {
        const val MaxLineNumberLayouts = 256
    }
}
