package io.github.lumkit.sweeteditor.render

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import io.github.lumkit.sweeteditor.EditorIconProvider
import io.github.lumkit.sweeteditor.EditorTheme
import io.github.lumkit.sweeteditor.core.protocol.CurrentLineRenderMode as CoreCurrentLineRenderMode
import io.github.lumkit.sweeteditor.core.protocol.EditorRenderModel
import io.github.lumkit.sweeteditor.core.protocol.FoldState
import io.github.lumkit.sweeteditor.core.protocol.GuideType
import io.github.lumkit.sweeteditor.core.protocol.RangeEffectKind
import io.github.lumkit.sweeteditor.core.protocol.RangeEffectRenderItem
import io.github.lumkit.sweeteditor.core.protocol.RangeEffectUnderlineStyle
import io.github.lumkit.sweeteditor.core.protocol.Rect
import io.github.lumkit.sweeteditor.core.protocol.ScrollbarModel
import io.github.lumkit.sweeteditor.core.protocol.SelectionHandle
import io.github.lumkit.sweeteditor.core.protocol.VisualRun
import io.github.lumkit.sweeteditor.core.protocol.VisualRunType
import kotlin.math.max
import kotlin.math.min

internal fun DrawScope.drawEditor(
    model: EditorRenderModel,
    textMeasurer: TextMeasurer,
    baseStyle: TextStyle,
    fontAscent: Float,
    fontDescent: Float,
    theme: EditorTheme,
    iconProvider: EditorIconProvider? = null,
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
                    VisualRunType.INLAY_HINT,
                    VisualRunType.PHANTOM_TEXT,
                    VisualRunType.FOLD_PLACEHOLDER,
                    VisualRunType.CODELENS,
                    VisualRunType.LINK,
                    -> drawTextRun(run, textMeasurer, baseStyle, fontAscent)
                    VisualRunType.WHITESPACE,
                    VisualRunType.TAB,
                    VisualRunType.NEWLINE,
                    -> drawInvisibleCharacterRun(run, textMeasurer, baseStyle, fontAscent, fontDescent, theme)
                }
            }
        }
    }

    val drawContentDecorations = {
        drawRangeBackgrounds()
        drawRuns()
        drawGuideSegments(model, theme)
        drawRangeEffectOverlays(model)
        drawCursor(model, theme)
    }

    if (model.gutterSticky && model.splitX > 0f) {
        clipRect(left = model.splitX, top = 0f, right = size.width, bottom = size.height) {
            drawContentDecorations()
        }
    } else {
        drawContentDecorations()
    }

    drawGutterOverlay(model, theme, lineHeight)
    drawLineNumbers(model, textMeasurer, baseStyle, fontAscent, theme)
    drawGutterIcons(model, theme, iconProvider)
    drawFoldMarkers(model, theme)
    drawSelectionHandles(model, theme)
    drawScrollbars(model, theme)
}

private fun DrawScope.drawTextRun(
    run: VisualRun,
    textMeasurer: TextMeasurer,
    baseStyle: TextStyle,
    fontAscent: Float,
) {
    drawRunBackground(run, fontAscent)
    if (run.text.isEmpty()) return
    val color = run.style.color.toComposeColor().takeUnless { it == Color.Unspecified } ?: Color(0xFFD4D4D4)
    val style = baseStyle.copy(
        color = color,
        fontWeight = if ((run.style.fontStyle and 1) != 0) FontWeight.Bold else FontWeight.Normal,
        fontStyle = if ((run.style.fontStyle and 2) != 0) FontStyle.Italic else FontStyle.Normal,
        fontFamily = baseStyle.fontFamily ?: FontFamily.Monospace,
    )
    val layout = textMeasurer.measure(run.text, style, constraints = Constraints())
    drawText(
        textLayoutResult = layout,
        topLeft = Offset(run.x, run.y - fontAscent),
    )
    if ((run.type == VisualRunType.CODELENS || run.type == VisualRunType.LINK) && run.active) {
        drawLine(
            color = color,
            start = Offset(run.x, run.y + 1f),
            end = Offset(run.x + run.width.coerceAtLeast(layout.size.width.toFloat()), run.y + 1f),
            strokeWidth = 1f,
        )
    }
}

private fun DrawScope.drawInvisibleCharacterRun(
    run: VisualRun,
    textMeasurer: TextMeasurer,
    baseStyle: TextStyle,
    fontAscent: Float,
    fontDescent: Float,
    theme: EditorTheme,
) {
    drawRunBackground(run, fontAscent, fontDescent)
    val color = theme.invisibleCharacterColor.toComposeColor().takeUnless { it == Color.Unspecified } ?: return
    when (run.type) {
        VisualRunType.WHITESPACE -> drawWhitespaceMarkerRun(run, fontAscent, fontDescent, color, baseStyle)
        VisualRunType.TAB -> drawTabMarkerRun(run, fontAscent, fontDescent, color, baseStyle)
        VisualRunType.NEWLINE -> drawLineBreakMarkerRun(run, textMeasurer, baseStyle, fontAscent, color)
        else -> Unit
    }
}

private fun DrawScope.drawRunBackground(run: VisualRun, fontAscent: Float, fontDescent: Float = fontAscent * 0.3f) {
    val background = run.style.backgroundColor.toComposeColor().takeUnless { it == Color.Unspecified } ?: return
    if (run.width <= 0f) return
    drawRect(
        color = background,
        topLeft = Offset(run.x, run.y - fontAscent),
        size = Size(run.width, (fontAscent + fontDescent).coerceAtLeast(1f)),
    )
}

private fun DrawScope.drawWhitespaceMarkerRun(
    run: VisualRun,
    fontAscent: Float,
    fontDescent: Float,
    color: Color,
    baseStyle: TextStyle,
) {
    val markerCount = run.text.length
    if (markerCount <= 0 || run.width <= 0f) return
    val cellWidth = run.width / max(1, markerCount)
    val centerY = run.y + (fontDescent - fontAscent) * 0.5f
    val fontSizePx = baseStyle.fontSize.toPx()
    val radius = max(1f, min(cellWidth, fontSizePx) * 0.08f)
    for (i in 0 until markerCount) {
        val centerX = run.x + cellWidth * (i + 0.5f)
        drawCircle(color = color, radius = radius, center = Offset(centerX, centerY))
    }
}

private fun DrawScope.drawTabMarkerRun(
    run: VisualRun,
    fontAscent: Float,
    fontDescent: Float,
    color: Color,
    baseStyle: TextStyle,
) {
    if (run.text.isEmpty() || run.width <= 0f) return
    val centerY = run.y + (fontDescent - fontAscent) * 0.5f
    val padding = min(run.width * 0.25f, 8f)
    val left = run.x + padding
    val right = max(left, run.x + run.width - padding)
    val arrow = min(5f, max(2f, (right - left) * 0.35f))
    val stroke = max(1f, baseStyle.fontSize.toPx() * 0.06f)
    drawLine(color, Offset(left, centerY), Offset(right, centerY), strokeWidth = stroke, cap = StrokeCap.Round)
    drawLine(color, Offset(right, centerY), Offset(right - arrow, centerY - arrow), strokeWidth = stroke, cap = StrokeCap.Round)
    drawLine(color, Offset(right, centerY), Offset(right - arrow, centerY + arrow), strokeWidth = stroke, cap = StrokeCap.Round)
}

private fun DrawScope.drawLineBreakMarkerRun(
    run: VisualRun,
    textMeasurer: TextMeasurer,
    baseStyle: TextStyle,
    fontAscent: Float,
    color: Color,
) {
    if (run.text.isEmpty()) return
    val style = baseStyle.copy(
        color = color,
        fontFamily = baseStyle.fontFamily ?: FontFamily.Monospace,
    )
    val layout = textMeasurer.measure(run.text, style, constraints = Constraints())
    drawText(
        textLayoutResult = layout,
        topLeft = Offset(run.x, run.y - fontAscent),
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

private fun DrawScope.drawCursor(model: EditorRenderModel, theme: EditorTheme) {
    val cursor = model.cursor
    if (!cursor.visible) return
    drawRect(
        color = theme.cursorColor.toComposeColor(),
        topLeft = Offset(cursor.position.x, cursor.position.y),
        size = Size(2f, cursor.height.coerceAtLeast(1f)),
    )
}

private fun DrawScope.drawGuideSegments(model: EditorRenderModel, theme: EditorTheme) {
    for (segment in model.guideSegments) {
        val color = if (segment.type == GuideType.SEPARATOR) {
            theme.separatorLineColor
        } else {
            theme.guideColor
        }.toComposeColor().takeUnless { it == Color.Unspecified } ?: continue
        drawLine(
            color = color,
            start = Offset(segment.start.x, segment.start.y),
            end = Offset(segment.end.x, segment.end.y),
            strokeWidth = if (segment.type == GuideType.INDENT) 1f else 1.2f,
        )
    }
}

private fun DrawScope.drawRangeEffectOverlays(model: EditorRenderModel) {
    for (effect in model.rangeEffects) {
        val rect = effect.rect
        if (rect.width <= 0f || rect.height <= 0f) continue
        val border = effect.style.borderColor.toComposeColor().takeUnless { it == Color.Unspecified }
        if (border != null) {
            val stroke = if (effect.kind == RangeEffectKind.LINKED_EDITING_ACTIVE) 2f else 1.5f
            drawRect(
                color = border,
                topLeft = Offset(rect.origin.x, rect.origin.y),
                size = Size(rect.width, rect.height),
                style = Stroke(width = stroke),
            )
        }
        val underline = effect.style.underlineColor.toComposeColor().takeUnless { it == Color.Unspecified }
        if (underline != null && effect.style.underlineStyle != RangeEffectUnderlineStyle.NONE) {
            drawRangeEffectUnderline(rect, underline, effect.style.underlineStyle)
        }
    }
}

private fun DrawScope.drawRangeEffectUnderline(
    rect: Rect,
    color: Color,
    style: RangeEffectUnderlineStyle,
) {
    val startX = rect.origin.x
    val endX = startX + rect.width
    val baseY = rect.origin.y + rect.height - 1f
    when (style) {
        RangeEffectUnderlineStyle.SOLID -> drawLine(
            color = color,
            start = Offset(startX, baseY),
            end = Offset(endX, baseY),
            strokeWidth = 2f,
            cap = StrokeCap.Butt,
        )
        RangeEffectUnderlineStyle.DASHED -> drawLine(
            color = color,
            start = Offset(startX, baseY),
            end = Offset(endX, baseY),
            strokeWidth = 2f,
            cap = StrokeCap.Butt,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 2f), 0f),
        )
        RangeEffectUnderlineStyle.WAVY -> {
            val path = Path()
            var x = startX
            var step = 0
            val halfWave = 7f
            val amplitude = 3.5f
            path.moveTo(x, baseY)
            while (x < endX) {
                val nextX = (x + halfWave).coerceAtMost(endX)
                val midX = (x + nextX) / 2f
                val peakY = if (step % 2 == 0) baseY - amplitude else baseY + amplitude
                path.quadraticBezierTo(midX, peakY, nextX, baseY)
                x = nextX
                step++
            }
            drawPath(path, color, style = Stroke(width = 2f, cap = StrokeCap.Butt))
        }
        RangeEffectUnderlineStyle.NONE -> Unit
    }
}

private fun DrawScope.drawGutterIcons(
    model: EditorRenderModel,
    theme: EditorTheme,
    iconProvider: EditorIconProvider?,
) {
    if (!model.gutterVisible) return
    val fallback = theme.gutterIconColor.toComposeColor().takeUnless { it == Color.Unspecified }
    for (icon in model.gutterIcons) {
        val rect = icon.rect
        if (rect.width <= 0f || rect.height <= 0f) continue
        val image = iconProvider?.getIcon(icon.iconId)
        if (image != null) {
            drawImage(
                image = image,
                dstOffset = IntOffset(rect.origin.x.toInt(), rect.origin.y.toInt()),
                dstSize = IntSize(
                    width = rect.width.toInt().coerceAtLeast(1),
                    height = rect.height.toInt().coerceAtLeast(1),
                ),
            )
            continue
        }
        if (fallback == null) continue
        val size = minOf(rect.width, rect.height) * 0.55f
        val cx = rect.origin.x + rect.width / 2f
        val cy = rect.origin.y + rect.height / 2f
        drawCircle(color = fallback, radius = size / 2f, center = Offset(cx, cy))
    }
}

private fun DrawScope.drawFoldMarkers(model: EditorRenderModel, theme: EditorTheme) {
    if (!model.gutterVisible) return
    val activeLogicalLine = model.cursor.textPosition.line
    for (item in model.foldMarkers) {
        if (item.foldState == FoldState.NONE) continue
        val rect = item.rect
        if (rect.width <= 0f || rect.height <= 0f) continue
        val color = if (item.logicalLine == activeLogicalLine) {
            theme.currentLineNumberColor
        } else {
            theme.lineNumberColor
        }.toComposeColor().takeUnless { it == Color.Unspecified } ?: continue
        val centerX = rect.origin.x + rect.width * 0.5f
        val centerY = rect.origin.y + rect.height * 0.5f
        val halfSize = min(rect.width, rect.height) * 0.28f
        val path = Path()
        if (item.foldState == FoldState.COLLAPSED) {
            path.moveTo(centerX - halfSize * 0.5f, centerY - halfSize)
            path.lineTo(centerX + halfSize * 0.5f, centerY)
            path.lineTo(centerX - halfSize * 0.5f, centerY + halfSize)
        } else {
            path.moveTo(centerX - halfSize, centerY - halfSize * 0.5f)
            path.lineTo(centerX, centerY + halfSize * 0.5f)
            path.lineTo(centerX + halfSize, centerY - halfSize * 0.5f)
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = max(1f, rect.height * 0.1f), cap = StrokeCap.Round),
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
