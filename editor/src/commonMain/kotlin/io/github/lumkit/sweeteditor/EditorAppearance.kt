package io.github.lumkit.sweeteditor

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.font.FontFamily
import io.github.lumkit.sweeteditor.core.protocol.EditorRangeEffectStyles
import io.github.lumkit.sweeteditor.core.protocol.EditorRenderColors
import io.github.lumkit.sweeteditor.core.protocol.RangeEffectStyle
import io.github.lumkit.sweeteditor.core.protocol.RangeEffectUnderlineStyle

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

enum class AutoIndentMode(val value: Int) {
    NONE(0),
    KEEP_INDENT(1),
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
    val autoIndentMode: AutoIndentMode = AutoIndentMode.KEEP_INDENT,
    val backspaceUnindent: Boolean = true,
    val decorationOverscanViewportMultiplier: Float = 1f,
    val decorationScrollRefreshMinIntervalMs: Int = 50,
)

@Immutable
data class EditorTheme(
    val backgroundColor: Int = 0xFF1E1E1E.toInt(),
    val textColor: Int = 0xFFD4D4D4.toInt(),
    val cursorColor: Int = 0xFFCCCCCC.toInt(),
    val currentLineColor: Int = 0xFF2A2A2A.toInt(),
    val lineNumberColor: Int = 0xFF5E6778.toInt(),
    val currentLineNumberColor: Int = 0xFF9CB3D6.toInt(),
    val splitLineColor: Int = 0x3356617A,
    val scrollbarTrackColor: Int = 0x48FFFFFF,
    val scrollbarThumbColor: Int = 0xAA858585.toInt(),
    val scrollbarThumbActiveColor: Int = 0xFFBBBBBB.toInt(),
    val selectionColor: Int = 0x664C9AFF,
    val linkColor: Int = 0xFF4EA1FF.toInt(),
    val activeLinkColor: Int = 0xFF82C0FF.toInt(),
    val codeLensColor: Int = 0xFF8A8A8A.toInt(),
    val activeCodeLensColor: Int = 0xFFB0B0B0.toInt(),
    val diffAddedLineBackground: Int = 0x332D5A2D,
    val diffRemovedLineBackground: Int = 0x335A2D2D,
    val diffAddedGutterBackground: Int = 0x442D5A2D,
    val diffRemovedGutterBackground: Int = 0x445A2D2D,
    val guideColor: Int = 0x8056617A.toInt(),
    val separatorLineColor: Int = 0xFF4A8F7A.toInt(),
    val compositionUnderlineColor: Int = 0xFF7AA2F7.toInt(),
    val diagnosticErrorColor: Int = 0xFFF7768E.toInt(),
    val diagnosticWarningColor: Int = 0xFFE0AF68.toInt(),
    val diagnosticInfoColor: Int = 0xFF7DCFFF.toInt(),
    val diagnosticHintColor: Int = 0xFF8FA3BF.toInt(),
    val searchMatchBgColor: Int = 0x33E0AF68,
    val searchCurrentBgColor: Int = 0x55E0AF68,
    val searchCurrentBorderColor: Int = 0xFFE0AF68.toInt(),
    val documentHighlightTextBgColor: Int = 0x1C7AA2F7,
    val documentHighlightReadBgColor: Int = 0x267AA2F7,
    val documentHighlightWriteBgColor: Int = 0x337AA2F7,
    val linkedEditingActiveColor: Int = 0xCC7AA2F7.toInt(),
    val linkedEditingInactiveColor: Int = 0x667AA2F7,
    val bracketHighlightBgColor: Int = 0x2A9ECE6A,
    val bracketHighlightBorderColor: Int = 0xCC9ECE6A.toInt(),
    val gutterIconColor: Int = 0xCC9CB0CD.toInt(),
    val fontFamily: FontFamily = FontFamily.Monospace,
)

internal expect fun platformDefaultGutterSticky(): Boolean

internal fun EditorTheme.toRangeEffectStyles(): EditorRangeEffectStyles = EditorRangeEffectStyles(
    selection = rangeBackground(selectionColor),
    searchMatch = rangeBackground(searchMatchBgColor),
    searchCurrent = RangeEffectStyle(
        foregroundColor = 0,
        backgroundColor = searchCurrentBgColor,
        borderColor = searchCurrentBorderColor,
        underlineColor = 0,
        underlineStyle = RangeEffectUnderlineStyle.NONE,
    ),
    documentHighlightText = rangeBackground(documentHighlightTextBgColor),
    documentHighlightRead = rangeBackground(documentHighlightReadBgColor),
    documentHighlightWrite = rangeBackground(documentHighlightWriteBgColor),
    linkedEditingActive = RangeEffectStyle(
        foregroundColor = 0,
        backgroundColor = linkedEditingActiveColor.withAlphaByte(0x20),
        borderColor = linkedEditingActiveColor,
        underlineColor = 0,
        underlineStyle = RangeEffectUnderlineStyle.NONE,
    ),
    linkedEditingInactive = RangeEffectStyle(
        foregroundColor = 0,
        backgroundColor = 0,
        borderColor = linkedEditingInactiveColor,
        underlineColor = 0,
        underlineStyle = RangeEffectUnderlineStyle.NONE,
    ),
    imeComposition = rangeUnderline(compositionUnderlineColor, RangeEffectUnderlineStyle.SOLID),
    bracketMatch = RangeEffectStyle(
        foregroundColor = 0,
        backgroundColor = bracketHighlightBgColor,
        borderColor = bracketHighlightBorderColor,
        underlineColor = 0,
        underlineStyle = RangeEffectUnderlineStyle.NONE,
    ),
    diagnosticError = rangeUnderline(diagnosticErrorColor, RangeEffectUnderlineStyle.WAVY),
    diagnosticWarning = rangeUnderline(diagnosticWarningColor, RangeEffectUnderlineStyle.WAVY),
    diagnosticInfo = rangeUnderline(diagnosticInfoColor, RangeEffectUnderlineStyle.WAVY),
    diagnosticHint = rangeUnderline(diagnosticHintColor, RangeEffectUnderlineStyle.DASHED),
)

private fun rangeBackground(background: Int): RangeEffectStyle = RangeEffectStyle(
    foregroundColor = 0,
    backgroundColor = background,
    borderColor = 0,
    underlineColor = 0,
    underlineStyle = RangeEffectUnderlineStyle.NONE,
)

private fun rangeUnderline(color: Int, style: RangeEffectUnderlineStyle): RangeEffectStyle = RangeEffectStyle(
    foregroundColor = 0,
    backgroundColor = 0,
    borderColor = 0,
    underlineColor = color,
    underlineStyle = style,
)

private fun Int.withAlphaByte(alpha: Int): Int = if (this == 0) 0 else (this and 0x00FFFFFF) or ((alpha and 0xFF) shl 24)

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
