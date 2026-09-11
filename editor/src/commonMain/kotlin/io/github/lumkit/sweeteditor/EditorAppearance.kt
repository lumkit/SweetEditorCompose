package io.github.lumkit.sweeteditor

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.font.FontFamily
import io.github.lumkit.sweeteditor.core.protocol.EditorRenderColors

enum class WrapMode(val value: Int) {
    NONE(0),
    CHAR_BREAK(1),
    WORD_BREAK(2),
}

enum class CurrentLineRenderMode(val value: Int) {
    BACKGROUND(0),
    BORDER(1),
    NONE(2),
}

@Immutable
data class EditorSettings(
    val wrapMode: WrapMode = WrapMode.NONE,
    val tabSize: Int = 4,
    val insertSpaces: Boolean = true,
    val lineSpacingAdd: Float = 0f,
    val lineSpacingMult: Float = 1.2f,
    val scale: Float = 1f,
    val fontSizeSp: Float = 14f,
    val readOnly: Boolean = false,
    val gutterVisible: Boolean = true,
    val gutterSticky: Boolean = platformDefaultGutterSticky(),
    val currentLineRenderMode: CurrentLineRenderMode = CurrentLineRenderMode.BACKGROUND,
)

@Immutable
data class EditorTheme(
    val backgroundColor: Int = 0xFF1E1E1E.toInt(),
    val textColor: Int = 0xFFD4D4D4.toInt(),
    val cursorColor: Int = 0xFFCCCCCC.toInt(),
    val currentLineColor: Int = 0xFF2A2A2A.toInt(),
    val selectionColor: Int = 0x664C9AFF,
    val linkColor: Int = 0xFF4EA1FF.toInt(),
    val activeLinkColor: Int = 0xFF82C0FF.toInt(),
    val codeLensColor: Int = 0xFF8A8A8A.toInt(),
    val activeCodeLensColor: Int = 0xFFB0B0B0.toInt(),
    val diffAddedLineBackground: Int = 0x332D5A2D,
    val diffRemovedLineBackground: Int = 0x335A2D2D,
    val diffAddedGutterBackground: Int = 0x442D5A2D,
    val diffRemovedGutterBackground: Int = 0x445A2D2D,
    val fontFamily: FontFamily = FontFamily.Monospace,
)

internal expect fun platformDefaultGutterSticky(): Boolean

internal fun EditorTheme.toRenderColors(): EditorRenderColors = EditorRenderColors(
    textForeground = textColor,
    linkForeground = linkColor,
    activeLinkForeground = activeLinkColor,
    codelensForeground = codeLensColor,
    activeCodelensForeground = activeCodeLensColor,
    diffAddedLineBackground = diffAddedLineBackground,
    diffRemovedLineBackground = diffRemovedLineBackground,
    diffAddedGutterBackground = diffAddedGutterBackground,
    diffRemovedGutterBackground = diffRemovedGutterBackground,
)
