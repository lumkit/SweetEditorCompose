package io.github.lumkit.sweeteditor

enum class EditorSpanLayer(val value: Int) {
    SYNTAX(0),
    SEMANTIC(1),
    OVERLAY(2),
}

enum class EditorDiagnosticSeverity(val value: Int) {
    ERROR(0),
    WARNING(1),
    INFO(2),
    HINT(3),
}

enum class EditorDocumentHighlightKind(val value: Int) {
    TEXT(0),
    READ(1),
    WRITE(2),
}

enum class EditorInlayType(val value: Int) {
    TEXT(0),
    ICON(1),
    COLOR(2),
}

object EditorFontStyle {
    const val NORMAL: Int = 0
    const val BOLD: Int = 1
    const val ITALIC: Int = 2
    const val STRIKETHROUGH: Int = 4
}

data class StyleSpan(
    val column: Int,
    val length: Int,
    val styleId: Int,
)

data class EditorTextStyle(
    val color: Int,
    val backgroundColor: Int = 0,
    val fontStyle: Int = EditorFontStyle.NORMAL,
)

data class InlayHint(
    val type: EditorInlayType,
    val column: Int,
    val text: String = "",
    val intValue: Int = 0,
)

data class PhantomText(
    val column: Int,
    val text: String,
)

data class GutterIcon(
    val iconId: Int,
)

data class CodeLensItem(
    val column: Int,
    val commandId: Int,
    val text: String,
)

data class LinkSpan(
    val column: Int,
    val length: Int,
    val target: String,
)

data class Diagnostic(
    val column: Int,
    val length: Int,
    val severity: EditorDiagnosticSeverity,
)

data class FoldRegion(
    val startLine: Int,
    val endLine: Int,
    val collapsed: Boolean = false,
)

data class DocumentHighlight(
    val column: Int,
    val length: Int,
    val kind: EditorDocumentHighlightKind,
)
