package io.github.lumkit.sweeteditor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Compose entry for SweetEditor.
 *
 * Native C++ rendering is not wired yet; this composable currently hosts the
 * shared controller state so the four CMP targets can compile and run.
 */
@Composable
fun SweetEditor(
    modifier: Modifier = Modifier,
    controller: SweetEditorController,
) {
    Box(
        modifier = modifier
            .background(Color(0xFF1E1E1E))
            .padding(12.dp),
    ) {
        BasicTextField(
            value = controller.text,
            onValueChange = { controller.text = it },
            modifier = Modifier.fillMaxSize(),
            textStyle = TextStyle(
                color = Color(0xFFD4D4D4),
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            ),
            cursorBrush = SolidColor(Color(0xFFCCCCCC)),
        )
    }
}

@Composable
fun rememberSweetEditorController(
    initialText: String = "",
): SweetEditorController {
    return remember {
        SweetEditorController(initialText)
    }
}
