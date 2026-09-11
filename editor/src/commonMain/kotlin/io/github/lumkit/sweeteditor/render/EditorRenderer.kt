package io.github.lumkit.sweeteditor.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import io.github.lumkit.sweeteditor.EditorTheme
import io.github.lumkit.sweeteditor.core.protocol.CurrentLineRenderMode as CoreCurrentLineRenderMode
import io.github.lumkit.sweeteditor.core.protocol.EditorRenderModel
import io.github.lumkit.sweeteditor.core.protocol.RangeEffectKind
import io.github.lumkit.sweeteditor.core.protocol.VisualRunType
import io.github.lumkit.sweeteditor.core.protocol.TextStyle as RunTextStyle

internal fun DrawScope.drawEditor(
    model: EditorRenderModel,
    textMeasurer: TextMeasurer,
    baseStyle: TextStyle,
    fontAscent: Float,
    theme: EditorTheme,
) {
    drawRect(theme.backgroundColor.toComposeColor())

    val lineHeight = model.cursor.height.takeIf { it > 0f } ?: (fontAscent * 1.4f)
    if (model.currentLineRenderMode != CoreCurrentLineRenderMode.NONE) {
        drawRect(
            color = theme.currentLineColor.toComposeColor(),
            topLeft = Offset(0f, model.currentLine.y),
            size = Size(size.width, lineHeight),
        )
    }

    for (effect in model.rangeEffects) {
        if (effect.kind != RangeEffectKind.SELECTION) continue
        val color = effect.style.backgroundColor.toComposeColor()
            .takeUnless { it == Color.Unspecified }
            ?: Color(0x664C9AFF)
        drawRect(
            color = color,
            topLeft = Offset(effect.rect.origin.x, effect.rect.origin.y),
            size = Size(effect.rect.width, effect.rect.height),
        )
    }

    val drawRuns = {
        for (line in model.lines) {
            for (run in line.runs) {
                when (run.type) {
                    VisualRunType.TEXT,
                    VisualRunType.WHITESPACE,
                    VisualRunType.TAB,
                    VisualRunType.NEWLINE,
                    VisualRunType.INLAY_HINT,
                    VisualRunType.PHANTOM_TEXT,
                    VisualRunType.FOLD_PLACEHOLDER,
                    -> drawRun(run.x, run.y, run.text, run.style, textMeasurer, baseStyle, fontAscent)
                    else -> Unit
                }
            }
        }
    }

    if (model.gutterSticky && model.splitX > 0f) {
        clipRect(left = model.splitX, top = 0f, right = size.width, bottom = size.height) {
            drawRuns()
        }
    } else {
        drawRuns()
    }

    val cursor = model.cursor
    if (cursor.visible) {
        drawRect(
            color = theme.cursorColor.toComposeColor(),
            topLeft = Offset(cursor.position.x, cursor.position.y),
            size = Size(2f, cursor.height.coerceAtLeast(1f)),
        )
    }
}

private fun DrawScope.drawRun(
    x: Float,
    y: Float,
    text: String,
    runStyle: RunTextStyle,
    textMeasurer: TextMeasurer,
    baseStyle: TextStyle,
    fontAscent: Float,
) {
    if (text.isEmpty()) return
    val color = runStyle.color.toComposeColor().takeUnless { it == Color.Unspecified } ?: Color(0xFFD4D4D4)
    val style = baseStyle.copy(
        color = color,
        fontWeight = if ((runStyle.fontStyle and 1) != 0) FontWeight.Bold else FontWeight.Normal,
        fontStyle = if ((runStyle.fontStyle and 2) != 0) FontStyle.Italic else FontStyle.Normal,
        fontFamily = baseStyle.fontFamily ?: FontFamily.Monospace,
    )
    val layout = textMeasurer.measure(text, style, constraints = Constraints())
    drawText(
        textLayoutResult = layout,
        topLeft = Offset(x, y - fontAscent),
    )
}

internal fun Int.toComposeColor(): Color {
    if (this == 0) return Color.Unspecified
    val a = ((this ushr 24) and 0xFF) / 255f
    val r = ((this ushr 16) and 0xFF) / 255f
    val g = ((this ushr 8) and 0xFF) / 255f
    val b = (this and 0xFF) / 255f
    val alpha = if ((this ushr 24) == 0) 1f else a
    return Color(red = r, green = g, blue = b, alpha = alpha)
}
