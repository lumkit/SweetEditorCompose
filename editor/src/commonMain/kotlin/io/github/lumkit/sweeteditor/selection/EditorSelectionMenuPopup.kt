package io.github.lumkit.sweeteditor.selection

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import io.github.lumkit.sweeteditor.EditorTheme
import io.github.lumkit.sweeteditor.SelectionMenuItem
import io.github.lumkit.sweeteditor.render.SelectionHandleCenterDist
import io.github.lumkit.sweeteditor.render.toComposeColor
import kotlin.math.roundToInt

@Composable
internal fun EditorSelectionMenuPopup(
    items: List<SelectionMenuItem>,
    anchor: SelectionMenuAnchor?,
    theme: EditorTheme,
    onItemClick: (SelectionMenuItem) -> Unit,
    onDismiss: () -> Unit,
) {
    if (items.isEmpty() || anchor == null) return
    val density = LocalDensity.current
    val gapPx = with(density) { SelectionMenuGap.roundToPx() }
    val handleClearancePx = SelectionHandleCenterDist.roundToInt()
    val positionProvider = remember(anchor, gapPx, handleClearancePx) {
        SelectionMenuPopupPositionProvider(anchor, gapPx, handleClearancePx)
    }
    Popup(
        popupPositionProvider = positionProvider,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = false, clippingEnabled = false),
    ) {
        Row(
            modifier = Modifier
                .shadow(4.dp, RoundedCornerShape(8.dp))
                .clip(RoundedCornerShape(8.dp))
                .background(theme.selectionMenuBgColor.toComposeColor())
                .height(36.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEachIndexed { index, item ->
                if (index > 0) {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 2.dp)
                            .width(1.dp)
                            .height(20.dp)
                            .background(theme.selectionMenuDividerColor.toComposeColor()),
                    )
                }
                val textColor = theme.selectionMenuTextColor.toComposeColor()
                    .copy(alpha = if (item.enabled) 1f else 0.31f)
                BasicText(
                    text = item.label,
                    modifier = Modifier
                        .clickable(enabled = item.enabled) { onItemClick(item) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    style = TextStyle(
                        color = textColor,
                        fontSize = 12.sp,
                    ),
                )
            }
        }
    }
}

private val SelectionMenuGap = 8.dp

private class SelectionMenuPopupPositionProvider(
    private val anchor: SelectionMenuAnchor,
    private val gapPx: Int,
    private val handleClearancePx: Int,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val local = computeSelectionMenuOffset(
            viewportWidth = anchorBounds.width,
            viewportHeight = anchorBounds.height,
            anchor = anchor,
            menuWidth = popupContentSize.width,
            menuHeight = popupContentSize.height,
            gap = gapPx,
            handleClearance = handleClearancePx,
        )
        return IntOffset(
            x = anchorBounds.left + local.x,
            y = anchorBounds.top + local.y,
        )
    }
}
