package io.github.lumkit.sweeteditor.contextmenu

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
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
import io.github.lumkit.sweeteditor.ContextMenuItem
import io.github.lumkit.sweeteditor.ContextMenuSection
import io.github.lumkit.sweeteditor.EditorPoint
import io.github.lumkit.sweeteditor.EditorTheme
import io.github.lumkit.sweeteditor.render.toComposeColor
import kotlin.math.roundToInt

@Composable
internal fun EditorContextMenuPopup(
    sections: List<ContextMenuSection>,
    location: EditorPoint?,
    theme: EditorTheme,
    onItemClick: (ContextMenuItem) -> Unit,
    onDismiss: () -> Unit,
) {
    if (sections.isEmpty() || location == null) return
    val density = LocalDensity.current
    val gapPx = with(density) { 4.dp.roundToPx() }
    val positionProvider = remember(location, gapPx) {
        ContextMenuPopupPositionProvider(location, gapPx)
    }
    Popup(
        popupPositionProvider = positionProvider,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true, clippingEnabled = true),
    ) {
        Column(
            modifier = Modifier
                .shadow(6.dp, RoundedCornerShape(8.dp))
                .clip(RoundedCornerShape(8.dp))
                .background(theme.contextMenuBgColor.toComposeColor())
                .widthIn(min = 120.dp),
        ) {
            sections.forEachIndexed { sectionIndex, section ->
                section.items.forEach { item ->
                    val textColor = theme.contextMenuTextColor.toComposeColor()
                        .copy(alpha = if (item.enabled) 1f else 0.31f)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = item.enabled) { onItemClick(item) }
                            .height(36.dp)
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        BasicText(
                            text = item.label,
                            style = TextStyle(color = textColor, fontSize = 13.sp),
                            modifier = Modifier.weight(1f),
                        )
                        val secondary = item.secondaryLabel
                        if (!secondary.isNullOrEmpty()) {
                            BasicText(
                                text = secondary,
                                style = TextStyle(
                                    color = textColor.copy(alpha = 0.55f),
                                    fontSize = 12.sp,
                                ),
                            )
                        }
                    }
                }
                if (sectionIndex < sections.lastIndex) {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(theme.contextMenuDividerColor.toComposeColor()),
                    )
                }
            }
        }
    }
}

private class ContextMenuPopupPositionProvider(
    private val location: EditorPoint,
    private val gapPx: Int,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val localX = location.x.roundToInt() + gapPx
        val belowY = location.y.roundToInt() + gapPx
        val aboveY = location.y.roundToInt() - gapPx - popupContentSize.height
        val maxX = (anchorBounds.width - popupContentSize.width).coerceAtLeast(0)
        val maxY = (anchorBounds.height - popupContentSize.height).coerceAtLeast(0)
        val y = if (belowY + popupContentSize.height <= anchorBounds.height) belowY else aboveY
        return IntOffset(
            x = anchorBounds.left + localX.coerceIn(0, maxX),
            y = anchorBounds.top + y.coerceIn(0, maxY),
        )
    }
}
