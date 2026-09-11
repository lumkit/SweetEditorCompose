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

enum class FoldArrowMode(val value: Int) {
    AUTO(0),
    ALWAYS(1),
    HIDDEN(2),
}

enum class AutoIndentMode(val value: Int) {
    NONE(0),
    KEEP_INDENT(1),
}

enum class WhitespaceRenderMode(val value: Int) {
    NONE(0),
    BOUNDARY(1),
    SELECTION(2),
    TRAILING(3),
    ALL(4),
}

enum class EditorRangeUnderlineStyle(val value: Int) {
    NONE(0),
    SOLID(1),
    DASHED(2),
    WAVY(3),
}

@Immutable
data class EditorRangeEffectStyle(
    val foregroundColor: Int = 0,
    val backgroundColor: Int = 0,
    val borderColor: Int = 0,
    val underlineColor: Int = 0,
    val underlineStyle: EditorRangeUnderlineStyle = EditorRangeUnderlineStyle.NONE,
)

@Immutable
data class EditorRangeEffects(
    val selection: EditorRangeEffectStyle? = null,
    val searchMatch: EditorRangeEffectStyle? = null,
    val searchCurrent: EditorRangeEffectStyle? = null,
    val documentHighlightText: EditorRangeEffectStyle? = null,
    val documentHighlightRead: EditorRangeEffectStyle? = null,
    val documentHighlightWrite: EditorRangeEffectStyle? = null,
    val linkedEditingActive: EditorRangeEffectStyle? = null,
    val linkedEditingInactive: EditorRangeEffectStyle? = null,
    val imeComposition: EditorRangeEffectStyle? = null,
    val bracketMatch: EditorRangeEffectStyle? = null,
    val diagnosticError: EditorRangeEffectStyle? = null,
    val diagnosticWarning: EditorRangeEffectStyle? = null,
    val diagnosticInfo: EditorRangeEffectStyle? = null,
    val diagnosticHint: EditorRangeEffectStyle? = null,
)

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
    val foldArrowMode: FoldArrowMode = FoldArrowMode.ALWAYS,
    val renderWhitespace: WhitespaceRenderMode = WhitespaceRenderMode.NONE,
    val renderLineBreaks: Boolean = false,
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
    val selectionTextColor: Int = 0,
    val invisibleCharacterColor: Int = 0x66808080,
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
    val selectionMenuBgColor: Int = 0xFF2B2B2B.toInt(),
    val selectionMenuTextColor: Int = 0xFFE6E6E6.toInt(),
    val selectionMenuDividerColor: Int = 0x33FFFFFF,
    val contextMenuBgColor: Int = 0xFF2B2B2B.toInt(),
    val contextMenuTextColor: Int = 0xFFE6E6E6.toInt(),
    val contextMenuDividerColor: Int = 0x33FFFFFF,
    val fontFamily: FontFamily = FontFamily.Monospace,
    val rangeEffects: EditorRangeEffects? = null,
) {
    companion object {
        fun dark(): EditorTheme = EditorTheme()

        fun light(): EditorTheme = EditorTheme(
            backgroundColor = 0xFFFFFFFF.toInt(),
            textColor = 0xFF1E1E1E.toInt(),
            cursorColor = 0xFF1E1E1E.toInt(),
            currentLineColor = 0xFFF3F6FB.toInt(),
            lineNumberColor = 0xFF8A93A3.toInt(),
            currentLineNumberColor = 0xFF3D4F6F.toInt(),
            splitLineColor = 0x33202838,
            scrollbarTrackColor = 0x22000000,
            scrollbarThumbColor = 0x66858585,
            scrollbarThumbActiveColor = 0xFF7A7A7A.toInt(),
            selectionColor = 0x664C9AFF,
            selectionTextColor = 0,
            invisibleCharacterColor = 0x66808080,
            linkColor = 0xFF0B67D3.toInt(),
            activeLinkColor = 0xFF094EA3.toInt(),
            codeLensColor = 0xFF6B6B6B.toInt(),
            activeCodeLensColor = 0xFF3D3D3D.toInt(),
            searchMatchBgColor = 0x33D4A017,
            searchCurrentBgColor = 0x55D4A017,
            searchCurrentBorderColor = 0xFFB8860B.toInt(),
            diagnosticErrorColor = 0xFFC62828.toInt(),
            diagnosticWarningColor = 0xFFB26A00.toInt(),
            diagnosticInfoColor = 0xFF0277BD.toInt(),
            diagnosticHintColor = 0xFF546E7A.toInt(),
            selectionMenuBgColor = 0xFFF5F5F5.toInt(),
            selectionMenuTextColor = 0xFF1E1E1E.toInt(),
            selectionMenuDividerColor = 0x33000000,
            contextMenuBgColor = 0xFFF5F5F5.toInt(),
            contextMenuTextColor = 0xFF1E1E1E.toInt(),
            contextMenuDividerColor = 0x33000000,
        )
    }
}

internal expect fun platformDefaultGutterSticky(): Boolean

internal expect fun platformSelectionMenuEnabled(): Boolean

internal expect fun platformContextMenuEnabled(): Boolean

internal fun EditorTheme.toRangeEffectStyles(): EditorRangeEffectStyles {
    val derived = EditorRangeEffectStyles(
        selection = RangeEffectStyle(
            foregroundColor = selectionTextColor,
            backgroundColor = selectionColor,
            borderColor = 0,
            underlineColor = 0,
            underlineStyle = RangeEffectUnderlineStyle.NONE,
        ),
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
    val overrides = rangeEffects ?: return derived
    return EditorRangeEffectStyles(
        selection = overrides.selection.toProtocolOr(derived.selection),
        searchMatch = overrides.searchMatch.toProtocolOr(derived.searchMatch),
        searchCurrent = overrides.searchCurrent.toProtocolOr(derived.searchCurrent),
        documentHighlightText = overrides.documentHighlightText.toProtocolOr(derived.documentHighlightText),
        documentHighlightRead = overrides.documentHighlightRead.toProtocolOr(derived.documentHighlightRead),
        documentHighlightWrite = overrides.documentHighlightWrite.toProtocolOr(derived.documentHighlightWrite),
        linkedEditingActive = overrides.linkedEditingActive.toProtocolOr(derived.linkedEditingActive),
        linkedEditingInactive = overrides.linkedEditingInactive.toProtocolOr(derived.linkedEditingInactive),
        imeComposition = overrides.imeComposition.toProtocolOr(derived.imeComposition),
        bracketMatch = overrides.bracketMatch.toProtocolOr(derived.bracketMatch),
        diagnosticError = overrides.diagnosticError.toProtocolOr(derived.diagnosticError),
        diagnosticWarning = overrides.diagnosticWarning.toProtocolOr(derived.diagnosticWarning),
        diagnosticInfo = overrides.diagnosticInfo.toProtocolOr(derived.diagnosticInfo),
        diagnosticHint = overrides.diagnosticHint.toProtocolOr(derived.diagnosticHint),
    )
}

private fun EditorRangeEffectStyle?.toProtocolOr(fallback: RangeEffectStyle): RangeEffectStyle =
    this?.toProtocol() ?: fallback

private fun EditorRangeEffectStyle.toProtocol(): RangeEffectStyle = RangeEffectStyle(
    foregroundColor = foregroundColor,
    backgroundColor = backgroundColor,
    borderColor = borderColor,
    underlineColor = underlineColor,
    underlineStyle = RangeEffectUnderlineStyle.fromValue(underlineStyle.value),
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
