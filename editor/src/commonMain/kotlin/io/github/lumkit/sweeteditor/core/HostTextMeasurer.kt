package io.github.lumkit.sweeteditor.core

import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Constraints
import io.github.lumkit.sweeteditor.EditorIconProvider

internal class HostTextMeasurer private constructor(
    private var textMeasurer: TextMeasurer?,
    private var baseStyle: TextStyle?,
    private val fallbackCharWidth: Float,
    private val fallbackAscent: Float,
    private val fallbackDescent: Float,
) {
    constructor(textMeasurer: TextMeasurer, baseStyle: TextStyle) : this(
        textMeasurer = textMeasurer,
        baseStyle = baseStyle,
        fallbackCharWidth = 8f,
        fallbackAscent = 12f,
        fallbackDescent = 4f,
    )

    constructor(
        charWidth: Float = 8f,
        ascent: Float = 12f,
        descent: Float = 4f,
    ) : this(
        textMeasurer = null,
        baseStyle = null,
        fallbackCharWidth = charWidth,
        fallbackAscent = ascent,
        fallbackDescent = descent,
    )

    private var densitySnapshot = 0f
    private var fontScaleSnapshot = 0f
    private var cachedMetrics: TextLayoutResult? = null
    private var iconProvider: EditorIconProvider? = null

    fun bind(
        textMeasurer: TextMeasurer,
        baseStyle: TextStyle,
        density: Float,
        fontScale: Float,
    ): Boolean {
        val unchanged = this.textMeasurer === textMeasurer &&
            this.baseStyle == baseStyle &&
            densitySnapshot == density &&
            fontScaleSnapshot == fontScale
        if (unchanged) return false
        this.textMeasurer = textMeasurer
        this.baseStyle = baseStyle
        densitySnapshot = density
        fontScaleSnapshot = fontScale
        cachedMetrics = null
        return true
    }

    fun bindIconProvider(provider: EditorIconProvider?): Boolean {
        if (iconProvider === provider) return false
        iconProvider = provider
        return true
    }

    fun measureTextWidth(text: String, fontStyle: Int): Float {
        if (text.isEmpty()) return 0f
        if (textMeasurer == null) return text.length * fallbackCharWidth
        return layout(text, fontStyle).size.width.toFloat()
    }

    fun measureInlayHintWidth(text: String): Float {
        if (text.isEmpty()) return 0f
        val measurer = textMeasurer ?: return text.length * fallbackCharWidth * 0.86f
        val style = baseStyle ?: return text.length * fallbackCharWidth * 0.86f
        val inlay = style.copy(fontSize = style.fontSize * 0.86f)
        return measurer.measure(text, inlay, constraints = Constraints()).size.width.toFloat()
    }

    fun measureIconWidth(iconId: Int): Float {
        val icon = iconProvider?.getIcon(iconId)
        if (icon != null && icon.width > 0) return icon.width.toFloat()
        if (textMeasurer == null) return fallbackAscent + fallbackDescent
        return metricsLayout().size.height.toFloat()
    }

    fun fontAscent(): Float {
        // Core FontMetrics.ascent is negative (same convention as c_api_smoke).
        if (textMeasurer == null) return -kotlin.math.abs(fallbackAscent)
        return -metricsLayout().firstBaseline
    }

    fun fontDescent(): Float {
        if (textMeasurer == null) return fallbackDescent
        val metrics = metricsLayout()
        return metrics.size.height - metrics.firstBaseline
    }

    private fun metricsLayout(): TextLayoutResult {
        return cachedMetrics ?: layout("Hg", 0).also { cachedMetrics = it }
    }

    private fun layout(text: String, fontStyle: Int): TextLayoutResult {
        val measurer = textMeasurer ?: error("HostTextMeasurer has no Compose TextMeasurer")
        val style = (baseStyle ?: TextStyle()).copy(
            fontWeight = if ((fontStyle and 1) != 0) FontWeight.Bold else FontWeight.Normal,
            fontStyle = if ((fontStyle and 2) != 0) FontStyle.Italic else FontStyle.Normal,
            textDecoration = if ((fontStyle and 4) != 0) TextDecoration.LineThrough else TextDecoration.None,
            fontFamily = baseStyle?.fontFamily ?: FontFamily.Monospace,
        )
        return measurer.measure(text, style, constraints = Constraints())
    }
}
