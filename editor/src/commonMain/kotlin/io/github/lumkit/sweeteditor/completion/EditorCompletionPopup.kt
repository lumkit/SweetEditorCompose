package io.github.lumkit.sweeteditor.completion

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import io.github.lumkit.sweeteditor.CompletionItem
import io.github.lumkit.sweeteditor.EditorCursorRect
import io.github.lumkit.sweeteditor.EditorTheme
import io.github.lumkit.sweeteditor.render.toComposeColor
import kotlin.math.roundToInt

@Composable
internal fun EditorCompletionPopup(
    items: List<CompletionItem>,
    selectedIndex: Int,
    anchor: EditorCursorRect?,
    theme: EditorTheme,
    onSelect: (Int) -> Unit,
    onConfirm: (CompletionItem) -> Unit,
    onDismiss: () -> Unit,
) {
    if (items.isEmpty() || anchor == null) return
    Popup(
        alignment = Alignment.TopStart,
        offset = IntOffset(
            x = anchor.x.roundToInt(),
            y = (anchor.y + anchor.height).roundToInt(),
        ),
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = false, clippingEnabled = false),
    ) {
        Column(
            modifier = Modifier
                .widthIn(min = 160.dp, max = 320.dp)
                .background(theme.backgroundColor.toComposeColor())
                .border(1.dp, theme.splitLineColor.toComposeColor()),
        ) {
            items.forEachIndexed { index, item ->
                val selected = index == selectedIndex
                val background = if (selected) {
                    theme.selectionColor.toComposeColor()
                } else {
                    Color.Transparent
                }
                Column(
                    modifier = Modifier
                        .background(background)
                        .clickable {
                            onSelect(index)
                            onConfirm(item)
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    BasicText(
                        text = item.label,
                        style = TextStyle(
                            color = theme.textColor.toComposeColor(),
                            fontSize = 13.sp,
                        ),
                    )
                    val detail = item.detail
                    if (!detail.isNullOrEmpty()) {
                        BasicText(
                            text = detail,
                            style = TextStyle(
                                color = theme.lineNumberColor.toComposeColor(),
                                fontSize = 11.sp,
                            ),
                        )
                    }
                }
            }
        }
    }
}
