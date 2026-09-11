package io.github.lumkit.sweeteditor.selection

import androidx.compose.ui.unit.IntOffset
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

internal data class SelectionMenuAnchor(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
)

internal fun computeSelectionMenuOffset(
    viewportWidth: Int,
    viewportHeight: Int,
    anchor: SelectionMenuAnchor,
    menuWidth: Int,
    menuHeight: Int,
    gap: Int,
    handleClearance: Int,
): IntOffset {
    val maxX = (viewportWidth - menuWidth).coerceAtLeast(0)
    val maxY = (viewportHeight - menuHeight).coerceAtLeast(0)
    val centerX = ((anchor.left + anchor.right) * 0.5f - menuWidth * 0.5f).roundToInt().coerceIn(0, maxX)
    val centerY = ((anchor.top + anchor.bottom) * 0.5f - menuHeight * 0.5f).roundToInt().coerceIn(0, maxY)
    val candidates = listOf(
        IntOffset(centerX, (anchor.top - gap - menuHeight).roundToInt()),
        IntOffset(centerX, (anchor.bottom + gap + handleClearance).roundToInt()),
        IntOffset((anchor.right + gap).roundToInt(), centerY),
        IntOffset((anchor.left - gap - menuWidth).roundToInt(), centerY),
    )

    fun fits(position: IntOffset): Boolean {
        if (position.x < 0 || position.y < 0) return false
        if (position.x + menuWidth > viewportWidth) return false
        if (position.y + menuHeight > viewportHeight) return false
        return overlapArea(
            left = position.x,
            top = position.y,
            right = position.x + menuWidth,
            bottom = position.y + menuHeight,
            otherLeft = anchor.left.roundToInt(),
            otherTop = anchor.top.roundToInt(),
            otherRight = anchor.right.roundToInt(),
            otherBottom = anchor.bottom.roundToInt(),
        ) == 0
    }

    candidates.firstOrNull(::fits)?.let { return it }

    return candidates
        .map { IntOffset(it.x.coerceIn(0, maxX), it.y.coerceIn(0, maxY)) }
        .minBy { position ->
            overlapArea(
                left = position.x,
                top = position.y,
                right = position.x + menuWidth,
                bottom = position.y + menuHeight,
                otherLeft = anchor.left.roundToInt(),
                otherTop = anchor.top.roundToInt(),
                otherRight = anchor.right.roundToInt(),
                otherBottom = anchor.bottom.roundToInt(),
            )
        }
}

private fun overlapArea(
    left: Int,
    top: Int,
    right: Int,
    bottom: Int,
    otherLeft: Int,
    otherTop: Int,
    otherRight: Int,
    otherBottom: Int,
): Int {
    val width = min(right, otherRight) - max(left, otherLeft)
    val height = min(bottom, otherBottom) - max(top, otherTop)
    if (width <= 0 || height <= 0) return 0
    return width * height
}
