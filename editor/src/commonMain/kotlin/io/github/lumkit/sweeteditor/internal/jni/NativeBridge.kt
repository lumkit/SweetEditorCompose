package io.github.lumkit.sweeteditor.internal.jni

import io.github.lumkit.sweeteditor.core.HostTextMeasurer

internal expect object NativeBridge {
    val isAvailable: Boolean
    fun createDocumentFromUtf8(utf8: ByteArray): Long
    fun freeDocument(handle: Long)
    fun getDocumentUtf8(handle: Long): ByteArray
    fun createEditor(measurer: HostTextMeasurer, options: ByteArray): Long
    fun freeEditor(handle: Long)
    fun editorSetDocument(editor: Long, document: Long): ByteArray?
    fun editorSetViewport(editor: Long, width: Int, height: Int): ByteArray?
    fun editorOnFontMetricsChanged(editor: Long): ByteArray?
    fun editorBuildRenderModel(editor: Long): ByteArray?
    fun editorHandleGestureEvent(editor: Long, payload: ByteArray): ByteArray?
    fun editorHandleKeyEvent(editor: Long, keyCode: Int, text: ByteArray?, modifiers: Int): ByteArray?
    fun editorSetKeyMap(editor: Long, payload: ByteArray): ByteArray?
    fun editorUpdatePointerModifiers(editor: Long, modifiers: Int): ByteArray?
    fun editorTickAnimations(editor: Long): ByteArray?
    fun editorInsertText(editor: Long, text: ByteArray): ByteArray?
    fun editorBackspace(editor: Long): ByteArray?
    fun editorUndo(editor: Long): ByteArray?
    fun editorRedo(editor: Long): ByteArray?
    fun editorCanUndo(editor: Long): Boolean
    fun editorCanRedo(editor: Long): Boolean
    fun editorSetGutterSticky(editor: Long, sticky: Boolean): ByteArray?
    fun editorSetGutterVisible(editor: Long, visible: Boolean): ByteArray?
    fun editorSetWrapMode(editor: Long, mode: Int): ByteArray?
    fun editorSetTabSize(editor: Long, tabSize: Int): ByteArray?
    fun editorSetInsertSpaces(editor: Long, enabled: Boolean): ByteArray?
    fun editorSetAutoIndentMode(editor: Long, mode: Int): ByteArray?
    fun editorSetBackspaceUnindent(editor: Long, enabled: Boolean): ByteArray?
    fun editorMoveLineUp(editor: Long): ByteArray?
    fun editorMoveLineDown(editor: Long): ByteArray?
    fun editorCopyLineUp(editor: Long): ByteArray?
    fun editorCopyLineDown(editor: Long): ByteArray?
    fun editorDeleteLine(editor: Long): ByteArray?
    fun editorInsertLineAbove(editor: Long): ByteArray?
    fun editorInsertLineBelow(editor: Long): ByteArray?
    fun editorSetScale(editor: Long, scale: Float): ByteArray?
    fun editorSetLineSpacing(editor: Long, add: Float, mult: Float): ByteArray?
    fun editorSetReadOnly(editor: Long, readOnly: Boolean): ByteArray?
    fun editorSetCurrentLineRenderMode(editor: Long, mode: Int): ByteArray?
    fun editorSetEditorRenderColors(editor: Long, payload: ByteArray): ByteArray?
    fun editorSetEditorRangeEffectStyles(editor: Long, payload: ByteArray): ByteArray?
    fun editorSearch(editor: Long, payload: ByteArray): ByteArray?
    fun editorFindNextSearchMatch(editor: Long): ByteArray?
    fun editorFindPreviousSearchMatch(editor: Long): ByteArray?
    fun editorReplaceCurrentSearchMatch(editor: Long, payload: ByteArray): ByteArray?
    fun editorReplaceAllSearchMatches(editor: Long, payload: ByteArray): ByteArray?
    fun editorClearSearch(editor: Long): ByteArray?
    fun editorGetSearchState(editor: Long): ByteArray?
    fun editorImeBeginSession(editor: Long, mutationModel: Int): ByteArray?
    fun editorImeEndSession(editor: Long, sessionId: Long): ByteArray?
    fun editorImeApplyCommands(editor: Long, payload: ByteArray): ByteArray?
    fun editorImeGetState(editor: Long, sessionId: Long): ByteArray?
    fun editorImeGetContext(
        editor: Long,
        sessionId: Long,
        source: Int,
        startUtf16: Long,
        lengthUtf16: Long,
    ): ByteArray?
    fun editorGetCursorRect(editor: Long): FloatArray
    fun editorGetPositionRect(editor: Long, line: Int, column: Int): FloatArray
    fun editorGetVisibleLineRange(editor: Long): IntArray
    fun editorGetScrollMetrics(editor: Long): ByteArray?
    fun editorGetSelectedText(editor: Long): ByteArray
    fun editorDecorationOp(
        editor: Long,
        op: Int,
        payload: ByteArray?,
        a: Int,
        b: Int,
        c: Int,
        d: Int,
    ): ByteArray?
    fun editorGetLinkTargetAt(editor: Long, line: Int, column: Int): ByteArray
}

internal object NativeDecorationOp {
    const val SET_LINE_SPANS: Int = 1
    const val SET_BATCH_LINE_SPANS: Int = 2
    const val REGISTER_BATCH_TEXT_STYLES: Int = 3
    const val SET_LINE_INLAY_HINTS: Int = 4
    const val SET_BATCH_LINE_INLAY_HINTS: Int = 5
    const val SET_LINE_PHANTOM_TEXTS: Int = 6
    const val SET_BATCH_LINE_PHANTOM_TEXTS: Int = 7
    const val SET_LINE_GUTTER_ICONS: Int = 8
    const val SET_BATCH_LINE_GUTTER_ICONS: Int = 9
    const val SET_LINE_CODELENS: Int = 10
    const val SET_BATCH_LINE_CODELENS: Int = 11
    const val SET_LINE_LINKS: Int = 12
    const val SET_BATCH_LINE_LINKS: Int = 13
    const val SET_LINE_DIAGNOSTICS: Int = 14
    const val SET_BATCH_LINE_DIAGNOSTICS: Int = 15
    const val SET_LINE_DOCUMENT_HIGHLIGHTS: Int = 16
    const val SET_BATCH_LINE_DOCUMENT_HIGHLIGHTS: Int = 17
    const val CLEAR_HIGHLIGHTS: Int = 18
    const val CLEAR_HIGHLIGHTS_LAYER: Int = 19
    const val CLEAR_LINE_SPANS: Int = 20
    const val CLEAR_INLAY_HINTS: Int = 21
    const val CLEAR_PHANTOM_TEXTS: Int = 22
    const val CLEAR_GUTTER_ICONS: Int = 23
    const val CLEAR_CODELENS: Int = 24
    const val CLEAR_LINKS: Int = 25
    const val CLEAR_DIAGNOSTICS: Int = 26
    const val CLEAR_DOCUMENT_HIGHLIGHTS: Int = 27
    const val CLEAR_ALL_DECORATIONS: Int = 28
    const val REGISTER_TEXT_STYLE: Int = 29
    const val SET_MAX_GUTTER_ICONS: Int = 30
}
