package io.github.lumkit.sweeteditor.render

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
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
import io.github.lumkit.sweeteditor.core.protocol.RangeEffectRenderItem
import io.github.lumkit.sweeteditor.core.protocol.Rect
import io.github.lumkit.sweeteditor.core.protocol.ScrollbarModel
import io.github.lumkit.sweeteditor.core.protocol.SelectionHandle
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
    drawCurrentLine(model, theme, 0f, size.width, lineHeight)

    val drawRangeBackgrounds = { drawRangeEffectBackgrounds(model, theme) }
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
                    VisualRunType.CODELENS,
                    -> drawRun(run.x, run.y, run.text, run.style, textMeasurer, baseStyle, fontAscent)
                    else -> Unit
                }
            }
        }
    }

    if (model.gutterSticky && model.splitX > 0f) {
        clipRect(left = model.splitX, top = 0f, right = size.width, bottom = size.height) {
            drawRangeBackgrounds()
            drawRuns()
        }
    } else {
        drawRangeBackgrounds()
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

    drawGutterOverlay(model, theme, lineHeight)
    drawLineNumbers(model, textMeasurer, baseStyle, fontAscent, theme)
    drawSelectionHandles(model, theme)
    drawScrollbars(model, theme)
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

private fun DrawScope.drawCurrentLine(
    model: EditorRenderModel,
    theme: EditorTheme,
    left: Float,
    right: Float,
    lineHeight: Float,
) {
    if (right <= left) return
    val width = right - left
    when (model.currentLineRenderMode) {
        CoreCurrentLineRenderMode.NONE -> Unit
        CoreCurrentLineRenderMode.BACKGROUND -> drawRect(
            color = theme.currentLineColor.toComposeColor(),
            topLeft = Offset(left, model.currentLine.y),
            size = Size(width, lineHeight),
        )
        CoreCurrentLineRenderMode.BORDER -> drawRect(
            color = theme.currentLineColor.toComposeColor(),
            topLeft = Offset(left, model.currentLine.y),
            size = Size(width, lineHeight),
            style = Stroke(width = 1f),
        )
    }
}

private fun DrawScope.drawGutterOverlay(
    model: EditorRenderModel,
    theme: EditorTheme,
    lineHeight: Float,
) {
    if (!model.gutterVisible || model.splitX <= 0f) return
    drawRect(
        color = theme.backgroundColor.toComposeColor(),
        topLeft = Offset.Zero,
        size = Size(model.splitX, size.height),
    )
    drawCurrentLine(model, theme, 0f, model.splitX, lineHeight)
    if (model.splitLineVisible) {
        drawLine(
            color = theme.splitLineColor.toComposeColor(),
            start = Offset(model.splitX, 0f),
            end = Offset(model.splitX, size.height),
            strokeWidth = 1f,
        )
    }
}

private fun DrawScope.drawLineNumbers(
    model: EditorRenderModel,
    textMeasurer: TextMeasurer,
    baseStyle: TextStyle,
    fontAscent: Float,
    theme: EditorTheme,
) {
    if (!model.gutterVisible) return
    val activeLogicalLine = model.cursor.textPosition.line
    for (line in model.lines) {
        if (line.lineNumber < 0) continue
        val isCurrent = line.ownsGutterSemantics && line.logicalLine == activeLogicalLine
        val color = if (isCurrent) theme.currentLineNumberColor else theme.lineNumberColor
        val style = baseStyle.copy(
            color = color.toComposeColor(),
            fontFamily = baseStyle.fontFamily ?: FontFamily.Monospace,
        )
        val layout = textMeasurer.measure(
            line.lineNumber.toString(),
            style,
            constraints = Constraints(),
        )
        drawText(
            textLayoutResult = layout,
            topLeft = Offset(line.lineNumberPosition.x, line.lineNumberPosition.y - fontAscent),
        )
    }
}

private fun DrawScope.drawScrollbars(model: EditorRenderModel, theme: EditorTheme) {
    val vertical = model.verticalScrollbar
    val horizontal = model.horizontalScrollbar
    val hasVertical = vertical.isDrawable()
    val hasHorizontal = horizontal.isDrawable()
    if (hasVertical) drawScrollbar(vertical, theme)
    if (hasHorizontal) drawScrollbar(horizontal, theme)
    if (hasVertical && hasHorizontal) {
        drawRect(
            color = theme.scrollbarTrackColor.toComposeColor().withScrollbarAlpha(vertical.alpha),
            topLeft = Offset(vertical.track.origin.x, horizontal.track.origin.y),
            size = Size(vertical.track.width, horizontal.track.height),
        )
    }
}

private fun ScrollbarModel.isDrawable(): Boolean =
    visible && alpha > 0f && track.width > 0f && track.height > 0f && thumb.width > 0f && thumb.height > 0f

private fun DrawScope.drawScrollbar(bar: ScrollbarModel, theme: EditorTheme) {
    drawRect(
        color = theme.scrollbarTrackColor.toComposeColor().withScrollbarAlpha(bar.alpha),
        topLeft = bar.track.toOffset(),
        size = bar.track.toSize(),
    )
    val thumbColor = if (bar.thumbActive) theme.scrollbarThumbActiveColor else theme.scrollbarThumbColor
    drawRoundRect(
        color = thumbColor.toComposeColor().withScrollbarAlpha(bar.alpha),
        topLeft = bar.thumb.toOffset(),
        size = bar.thumb.toSize(),
        cornerRadius = CornerRadius(3f, 3f),
    )
}

private fun DrawScope.drawRangeEffectBackgrounds(model: EditorRenderModel, theme: EditorTheme) {
    for (effect in model.rangeEffects) {
        val color = effect.backgroundColor(theme) ?: continue
        if (effect.rect.width <= 0f || effect.rect.height <= 0f) continue
        drawRect(
            color = color,
            topLeft = Offset(effect.rect.origin.x, effect.rect.origin.y),
            size = Size(effect.rect.width, effect.rect.height),
        )
    }
}

private fun RangeEffectRenderItem.backgroundColor(theme: EditorTheme): Color? {
    val fromCore = style.backgroundColor.toComposeColor().takeUnless { it == Color.Unspecified }
    if (fromCore != null) return fromCore
    if (kind == RangeEffectKind.SELECTION) {
        return theme.selectionColor.toComposeColor().takeUnless { it == Color.Unspecified }
            ?: Color(0x664C9AFF)
    }
    return null
}

private const val SelectionHandleLineWidth = 1.5f
private const val SelectionHandleDropRadius = 10f
private const val SelectionHandleCenterDist = 24f

private fun DrawScope.drawSelectionHandles(model: EditorRenderModel, theme: EditorTheme) {
    val color = theme.cursorColor.toComposeColor().takeUnless { it == Color.Unspecified } ?: Color.White
    if (model.selectionStartHandle.visible) {
        drawSelectionHandle(model.selectionStartHandle, isStart = true, color = color)
    }
    if (model.selectionEndHandle.visible) {
        drawSelectionHandle(model.selectionEndHandle, isStart = false, color = color)
    }
}

private fun DrawScope.drawSelectionHandle(handle: SelectionHandle, isStart: Boolean, color: Color) {
    val x = handle.position.x
    val y = handle.position.y
    val height = handle.height
    if (height <= 0f) return
    drawRect(
        color = color,
        topLeft = Offset(x - SelectionHandleLineWidth / 2f, y),
        size = Size(SelectionHandleLineWidth, height),
    )
    val dropLength = SelectionHandleCenterDist
    val r = SelectionHandleDropRadius
    val k = r * 0.5522f
    val path = Path().apply {
        moveTo(0f, 0f)
        cubicTo(0f, dropLength * 0.4f, -r, dropLength - r * 0.8f, -r, dropLength)
        cubicTo(-r, dropLength + k, -k, dropLength + r, 0f, dropLength + r)
        cubicTo(k, dropLength + r, r, dropLength + k, r, dropLength)
        cubicTo(r, dropLength - r * 0.8f, 0f, dropLength * 0.4f, 0f, 0f)
        close()
    }
    translate(left = x, top = y + height) {
        rotate(degrees = if (isStart) 45f else -45f, pivot = Offset.Zero) {
            drawPath(path, color)
        }
    }
}

private fun Rect.toOffset(): Offset = Offset(origin.x, origin.y)

private fun Rect.toSize(): Size = Size(width, height)

private fun Color.withScrollbarAlpha(alpha: Float): Color =
    copy(alpha = this.alpha * alpha.coerceIn(0f, 1f))

internal fun Int.toComposeColor(): Color {
    if (this == 0) return Color.Unspecified
    val a = ((this ushr 24) and 0xFF) / 255f
    val r = ((this ushr 16) and 0xFF) / 255f
    val g = ((this ushr 8) and 0xFF) / 255f
    val b = (this and 0xFF) / 255f
    val alpha = if ((this ushr 24) == 0) 1f else a
    return Color(red = r, green = g, blue = b, alpha = alpha)
}
