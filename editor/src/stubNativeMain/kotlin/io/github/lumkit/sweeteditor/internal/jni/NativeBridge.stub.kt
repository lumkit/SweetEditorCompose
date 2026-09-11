package io.github.lumkit.sweeteditor.internal.jni

import io.github.lumkit.sweeteditor.core.HostTextMeasurer

internal actual object NativeBridge {
    actual val isAvailable: Boolean = false

    actual fun createDocumentFromUtf8(utf8: ByteArray): Long {
        unsupported()
    }
    actual fun freeDocument(handle: Long) {
        unsupported()
    }
    actual fun getDocumentUtf8(handle: Long): ByteArray {
        unsupported()
    }
    actual fun createEditor(measurer: HostTextMeasurer, options: ByteArray): Long {
        unsupported()
    }
    actual fun freeEditor(handle: Long) {
        unsupported()
    }
    actual fun editorSetDocument(editor: Long, document: Long): ByteArray? {
        unsupported()
    }
    actual fun editorSetViewport(editor: Long, width: Int, height: Int): ByteArray? {
        unsupported()
    }
    actual fun editorOnFontMetricsChanged(editor: Long): ByteArray? {
        unsupported()
    }
    actual fun editorBuildRenderModel(editor: Long): ByteArray? {
        unsupported()
    }
    actual fun editorHandleGestureEvent(editor: Long, payload: ByteArray): ByteArray? {
        unsupported()
    }
    actual fun editorHandleKeyEvent(editor: Long, keyCode: Int, text: ByteArray?, modifiers: Int): ByteArray? {
        unsupported()
    }
    actual fun editorSetKeyMap(editor: Long, payload: ByteArray): ByteArray? {
        unsupported()
    }
    actual fun editorUpdatePointerModifiers(editor: Long, modifiers: Int): ByteArray? {
        unsupported()
    }
    actual fun editorTickAnimations(editor: Long): ByteArray? {
        unsupported()
    }
    actual fun editorInsertText(editor: Long, text: ByteArray): ByteArray? {
        unsupported()
    }
    actual fun editorBackspace(editor: Long): ByteArray? {
        unsupported()
    }
    actual fun editorUndo(editor: Long): ByteArray? {
        unsupported()
    }
    actual fun editorRedo(editor: Long): ByteArray? {
        unsupported()
    }
    actual fun editorCanUndo(editor: Long): Boolean {
        unsupported()
    }
    actual fun editorCanRedo(editor: Long): Boolean {
        unsupported()
    }
    actual fun editorSetGutterSticky(editor: Long, sticky: Boolean): ByteArray? {
        unsupported()
    }
    actual fun editorSetGutterVisible(editor: Long, visible: Boolean): ByteArray? {
        unsupported()
    }
    actual fun editorSetWrapMode(editor: Long, mode: Int): ByteArray? {
        unsupported()
    }
    actual fun editorSetTabSize(editor: Long, tabSize: Int): ByteArray? {
        unsupported()
    }
    actual fun editorSetInsertSpaces(editor: Long, enabled: Boolean): ByteArray? {
        unsupported()
    }
    actual fun editorSetAutoIndentMode(editor: Long, mode: Int): ByteArray? {
        unsupported()
    }
    actual fun editorSetBackspaceUnindent(editor: Long, enabled: Boolean): ByteArray? {
        unsupported()
    }
    actual fun editorMoveLineUp(editor: Long): ByteArray? {
        unsupported()
    }
    actual fun editorMoveLineDown(editor: Long): ByteArray? {
        unsupported()
    }
    actual fun editorCopyLineUp(editor: Long): ByteArray? {
        unsupported()
    }
    actual fun editorCopyLineDown(editor: Long): ByteArray? {
        unsupported()
    }
    actual fun editorDeleteLine(editor: Long): ByteArray? {
        unsupported()
    }
    actual fun editorInsertLineAbove(editor: Long): ByteArray? {
        unsupported()
    }
    actual fun editorInsertLineBelow(editor: Long): ByteArray? {
        unsupported()
    }
    actual fun editorSetScale(editor: Long, scale: Float): ByteArray? {
        unsupported()
    }
    actual fun editorSetLineSpacing(editor: Long, add: Float, mult: Float): ByteArray? {
        unsupported()
    }
    actual fun editorSetReadOnly(editor: Long, readOnly: Boolean): ByteArray? {
        unsupported()
    }
    actual fun editorSetCurrentLineRenderMode(editor: Long, mode: Int): ByteArray? {
        unsupported()
    }
    actual fun editorSetEditorRenderColors(editor: Long, payload: ByteArray): ByteArray? {
        unsupported()
    }
    actual fun editorSetEditorRangeEffectStyles(editor: Long, payload: ByteArray): ByteArray? {
        unsupported()
    }
    actual fun editorImeBeginSession(editor: Long, mutationModel: Int): ByteArray? {
        unsupported()
    }
    actual fun editorImeEndSession(editor: Long, sessionId: Long): ByteArray? {
        unsupported()
    }
    actual fun editorImeApplyCommands(editor: Long, payload: ByteArray): ByteArray? {
        unsupported()
    }
    actual fun editorImeGetState(editor: Long, sessionId: Long): ByteArray? {
        unsupported()
    }
    actual fun editorImeGetContext(
        editor: Long,
        sessionId: Long,
        source: Int,
        startUtf16: Long,
        lengthUtf16: Long,
    ): ByteArray? {
        unsupported()
    }
    actual fun editorGetCursorRect(editor: Long): FloatArray {
        unsupported()
    }
    actual fun editorGetPositionRect(editor: Long, line: Int, column: Int): FloatArray {
        unsupported()
    }
    actual fun editorGetVisibleLineRange(editor: Long): IntArray {
        unsupported()
    }
    actual fun editorGetScrollMetrics(editor: Long): ByteArray? {
        unsupported()
    }
    actual fun editorGetSelectedText(editor: Long): ByteArray {
        unsupported()
    }
    actual fun editorDecorationOp(
        editor: Long,
        op: Int,
        payload: ByteArray?,
        a: Int,
        b: Int,
        c: Int,
        d: Int,
    ): ByteArray? {
        unsupported()
    }
    actual fun editorGetLinkTargetAt(editor: Long, line: Int, column: Int): ByteArray {
        unsupported()
    }

    private fun unsupported(): Nothing {
        throw IllegalStateException("SweetEditor native core is not wired on this target yet")
    }
}
