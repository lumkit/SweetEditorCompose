package io.github.lumkit.sweeteditor.copilot

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import io.github.lumkit.sweeteditor.EditorCursorRect
import io.github.lumkit.sweeteditor.EditorTheme
import io.github.lumkit.sweeteditor.render.toComposeColor
import kotlin.math.roundToInt

@Composable
internal fun EditorInlineSuggestionBar(
    anchor: EditorCursorRect?,
    theme: EditorTheme,
    onAccept: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (anchor == null) return
    Popup(
        alignment = Alignment.TopStart,
        offset = IntOffset(
            x = anchor.x.roundToInt(),
            y = (anchor.y + anchor.height + 4f).roundToInt().coerceAtLeast(0),
        ),
        properties = PopupProperties(focusable = false, clippingEnabled = false),
    ) {
        Row(
            modifier = Modifier
                .shadow(6.dp, RoundedCornerShape(6.dp))
                .clip(RoundedCornerShape(6.dp))
                .background(theme.inlineSuggestionBarBgColor.toComposeColor())
                .height(28.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicText(
                text = "Tab  Accept",
                modifier = Modifier
                    .clickable(onClick = onAccept)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                style = TextStyle(
                    color = theme.inlineSuggestionBarAcceptColor.toComposeColor(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(16.dp)
                    .background(theme.inlineSuggestionBarDismissColor.toComposeColor().copy(alpha = 0.19f)),
            )
            BasicText(
                text = "Esc  Dismiss",
                modifier = Modifier
                    .clickable(onClick = onDismiss)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                style = TextStyle(
                    color = theme.inlineSuggestionBarDismissColor.toComposeColor(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )
        }
    }
}
