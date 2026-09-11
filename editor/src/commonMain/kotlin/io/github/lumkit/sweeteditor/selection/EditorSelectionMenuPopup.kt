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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import io.github.lumkit.sweeteditor.EditorCursorRect
import io.github.lumkit.sweeteditor.EditorTheme
import io.github.lumkit.sweeteditor.SelectionMenuItem
import io.github.lumkit.sweeteditor.render.toComposeColor
import kotlin.math.roundToInt

@Composable
internal fun EditorSelectionMenuPopup(
    items: List<SelectionMenuItem>,
    anchor: EditorCursorRect?,
    theme: EditorTheme,
    onItemClick: (SelectionMenuItem) -> Unit,
    onDismiss: () -> Unit,
) {
    if (items.isEmpty() || anchor == null) return
    Popup(
        alignment = Alignment.TopStart,
        offset = IntOffset(
            x = anchor.x.roundToInt(),
            y = (anchor.y - 44f).roundToInt().coerceAtLeast(0),
        ),
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
