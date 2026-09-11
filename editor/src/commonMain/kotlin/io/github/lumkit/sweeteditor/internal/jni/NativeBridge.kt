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
}
