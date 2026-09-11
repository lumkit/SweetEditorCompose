package io.github.lumkit.sweeteditor.internal.jni

import io.github.lumkit.sweeteditor.core.HostTextMeasurer
import io.github.lumkit.sweeteditor.internal.NativeLibraryLoader

internal object SweetEditorJni {
    init {
        NativeLibraryLoader.loadComposeJni()
    }

    @JvmStatic external fun createDocumentFromUtf8(utf8: ByteArray): Long
    @JvmStatic external fun freeDocument(handle: Long)
    @JvmStatic external fun getDocumentUtf8(handle: Long): ByteArray
    @JvmStatic external fun createEditor(measurer: HostTextMeasurer, options: ByteArray): Long
    @JvmStatic external fun freeEditor(handle: Long)
    @JvmStatic external fun editorSetDocument(editor: Long, document: Long): ByteArray?
    @JvmStatic external fun editorSetViewport(editor: Long, width: Int, height: Int): ByteArray?
    @JvmStatic external fun editorOnFontMetricsChanged(editor: Long): ByteArray?
    @JvmStatic external fun editorBuildRenderModel(editor: Long): ByteArray?
    @JvmStatic external fun editorHandleGestureEvent(editor: Long, payload: ByteArray): ByteArray?
    @JvmStatic external fun editorHandleKeyEvent(
        editor: Long,
        keyCode: Int,
        text: ByteArray?,
        modifiers: Int,
    ): ByteArray?
    @JvmStatic external fun editorUpdatePointerModifiers(editor: Long, modifiers: Int): ByteArray?
    @JvmStatic external fun editorTickAnimations(editor: Long): ByteArray?
    @JvmStatic external fun editorInsertText(editor: Long, text: ByteArray): ByteArray?
    @JvmStatic external fun editorBackspace(editor: Long): ByteArray?
    @JvmStatic external fun editorUndo(editor: Long): ByteArray?
    @JvmStatic external fun editorRedo(editor: Long): ByteArray?
    @JvmStatic external fun editorCanUndo(editor: Long): Boolean
    @JvmStatic external fun editorCanRedo(editor: Long): Boolean
    @JvmStatic external fun editorSetGutterSticky(editor: Long, sticky: Boolean): ByteArray?
    @JvmStatic external fun editorSetGutterVisible(editor: Long, visible: Boolean): ByteArray?
    @JvmStatic external fun editorSetWrapMode(editor: Long, mode: Int): ByteArray?
    @JvmStatic external fun editorSetTabSize(editor: Long, tabSize: Int): ByteArray?
    @JvmStatic external fun editorSetInsertSpaces(editor: Long, enabled: Boolean): ByteArray?
    @JvmStatic external fun editorSetAutoIndentMode(editor: Long, mode: Int): ByteArray?
    @JvmStatic external fun editorSetBackspaceUnindent(editor: Long, enabled: Boolean): ByteArray?
    @JvmStatic external fun editorMoveLineUp(editor: Long): ByteArray?
    @JvmStatic external fun editorMoveLineDown(editor: Long): ByteArray?
    @JvmStatic external fun editorCopyLineUp(editor: Long): ByteArray?
    @JvmStatic external fun editorCopyLineDown(editor: Long): ByteArray?
    @JvmStatic external fun editorDeleteLine(editor: Long): ByteArray?
    @JvmStatic external fun editorInsertLineAbove(editor: Long): ByteArray?
    @JvmStatic external fun editorInsertLineBelow(editor: Long): ByteArray?
    @JvmStatic external fun editorSetScale(editor: Long, scale: Float): ByteArray?
    @JvmStatic external fun editorSetLineSpacing(editor: Long, add: Float, mult: Float): ByteArray?
    @JvmStatic external fun editorSetReadOnly(editor: Long, readOnly: Boolean): ByteArray?
    @JvmStatic external fun editorSetCurrentLineRenderMode(editor: Long, mode: Int): ByteArray?
    @JvmStatic external fun editorSetEditorRenderColors(editor: Long, payload: ByteArray): ByteArray?
    @JvmStatic external fun editorImeBeginSession(editor: Long, mutationModel: Int): ByteArray?
    @JvmStatic external fun editorImeEndSession(editor: Long, sessionId: Long): ByteArray?
    @JvmStatic external fun editorImeApplyCommands(editor: Long, payload: ByteArray): ByteArray?
    @JvmStatic external fun editorImeGetState(editor: Long, sessionId: Long): ByteArray?
    @JvmStatic external fun editorImeGetContext(
        editor: Long,
        sessionId: Long,
        source: Int,
        startUtf16: Long,
        lengthUtf16: Long,
    ): ByteArray?
    @JvmStatic external fun editorGetCursorRect(editor: Long): FloatArray
    @JvmStatic external fun editorGetPositionRect(editor: Long, line: Int, column: Int): FloatArray
    @JvmStatic external fun editorGetVisibleLineRange(editor: Long): IntArray
    @JvmStatic external fun editorGetScrollMetrics(editor: Long): ByteArray?
    @JvmStatic external fun editorGetSelectedText(editor: Long): ByteArray
}

internal actual object NativeBridge {
    actual val isAvailable: Boolean = true

    actual fun createDocumentFromUtf8(utf8: ByteArray): Long = SweetEditorJni.createDocumentFromUtf8(utf8)
    actual fun freeDocument(handle: Long) = SweetEditorJni.freeDocument(handle)
    actual fun getDocumentUtf8(handle: Long): ByteArray = SweetEditorJni.getDocumentUtf8(handle)
    actual fun createEditor(measurer: HostTextMeasurer, options: ByteArray): Long =
        SweetEditorJni.createEditor(measurer, options)
    actual fun freeEditor(handle: Long) = SweetEditorJni.freeEditor(handle)
    actual fun editorSetDocument(editor: Long, document: Long): ByteArray? =
        SweetEditorJni.editorSetDocument(editor, document)
    actual fun editorSetViewport(editor: Long, width: Int, height: Int): ByteArray? =
        SweetEditorJni.editorSetViewport(editor, width, height)
    actual fun editorOnFontMetricsChanged(editor: Long): ByteArray? =
        SweetEditorJni.editorOnFontMetricsChanged(editor)
    actual fun editorBuildRenderModel(editor: Long): ByteArray? = SweetEditorJni.editorBuildRenderModel(editor)
    actual fun editorHandleGestureEvent(editor: Long, payload: ByteArray): ByteArray? =
        SweetEditorJni.editorHandleGestureEvent(editor, payload)
    actual fun editorHandleKeyEvent(editor: Long, keyCode: Int, text: ByteArray?, modifiers: Int): ByteArray? =
        SweetEditorJni.editorHandleKeyEvent(editor, keyCode, text, modifiers)
    actual fun editorUpdatePointerModifiers(editor: Long, modifiers: Int): ByteArray? =
        SweetEditorJni.editorUpdatePointerModifiers(editor, modifiers)
    actual fun editorTickAnimations(editor: Long): ByteArray? = SweetEditorJni.editorTickAnimations(editor)
    actual fun editorInsertText(editor: Long, text: ByteArray): ByteArray? =
        SweetEditorJni.editorInsertText(editor, text)
    actual fun editorBackspace(editor: Long): ByteArray? = SweetEditorJni.editorBackspace(editor)
    actual fun editorUndo(editor: Long): ByteArray? = SweetEditorJni.editorUndo(editor)
    actual fun editorRedo(editor: Long): ByteArray? = SweetEditorJni.editorRedo(editor)
    actual fun editorCanUndo(editor: Long): Boolean = SweetEditorJni.editorCanUndo(editor)
    actual fun editorCanRedo(editor: Long): Boolean = SweetEditorJni.editorCanRedo(editor)
    actual fun editorSetGutterSticky(editor: Long, sticky: Boolean): ByteArray? =
        SweetEditorJni.editorSetGutterSticky(editor, sticky)
    actual fun editorSetGutterVisible(editor: Long, visible: Boolean): ByteArray? =
        SweetEditorJni.editorSetGutterVisible(editor, visible)
    actual fun editorSetWrapMode(editor: Long, mode: Int): ByteArray? =
        SweetEditorJni.editorSetWrapMode(editor, mode)
    actual fun editorSetTabSize(editor: Long, tabSize: Int): ByteArray? =
        SweetEditorJni.editorSetTabSize(editor, tabSize)
    actual fun editorSetInsertSpaces(editor: Long, enabled: Boolean): ByteArray? =
        SweetEditorJni.editorSetInsertSpaces(editor, enabled)
    actual fun editorSetAutoIndentMode(editor: Long, mode: Int): ByteArray? =
        SweetEditorJni.editorSetAutoIndentMode(editor, mode)
    actual fun editorSetBackspaceUnindent(editor: Long, enabled: Boolean): ByteArray? =
        SweetEditorJni.editorSetBackspaceUnindent(editor, enabled)
    actual fun editorMoveLineUp(editor: Long): ByteArray? = SweetEditorJni.editorMoveLineUp(editor)
    actual fun editorMoveLineDown(editor: Long): ByteArray? = SweetEditorJni.editorMoveLineDown(editor)
    actual fun editorCopyLineUp(editor: Long): ByteArray? = SweetEditorJni.editorCopyLineUp(editor)
    actual fun editorCopyLineDown(editor: Long): ByteArray? = SweetEditorJni.editorCopyLineDown(editor)
    actual fun editorDeleteLine(editor: Long): ByteArray? = SweetEditorJni.editorDeleteLine(editor)
    actual fun editorInsertLineAbove(editor: Long): ByteArray? = SweetEditorJni.editorInsertLineAbove(editor)
    actual fun editorInsertLineBelow(editor: Long): ByteArray? = SweetEditorJni.editorInsertLineBelow(editor)
    actual fun editorSetScale(editor: Long, scale: Float): ByteArray? =
        SweetEditorJni.editorSetScale(editor, scale)
    actual fun editorSetLineSpacing(editor: Long, add: Float, mult: Float): ByteArray? =
        SweetEditorJni.editorSetLineSpacing(editor, add, mult)
    actual fun editorSetReadOnly(editor: Long, readOnly: Boolean): ByteArray? =
        SweetEditorJni.editorSetReadOnly(editor, readOnly)
    actual fun editorSetCurrentLineRenderMode(editor: Long, mode: Int): ByteArray? =
        SweetEditorJni.editorSetCurrentLineRenderMode(editor, mode)
    actual fun editorSetEditorRenderColors(editor: Long, payload: ByteArray): ByteArray? =
        SweetEditorJni.editorSetEditorRenderColors(editor, payload)
    actual fun editorImeBeginSession(editor: Long, mutationModel: Int): ByteArray? =
        SweetEditorJni.editorImeBeginSession(editor, mutationModel)
    actual fun editorImeEndSession(editor: Long, sessionId: Long): ByteArray? =
        SweetEditorJni.editorImeEndSession(editor, sessionId)
    actual fun editorImeApplyCommands(editor: Long, payload: ByteArray): ByteArray? =
        SweetEditorJni.editorImeApplyCommands(editor, payload)
    actual fun editorImeGetState(editor: Long, sessionId: Long): ByteArray? =
        SweetEditorJni.editorImeGetState(editor, sessionId)
    actual fun editorImeGetContext(
        editor: Long,
        sessionId: Long,
        source: Int,
        startUtf16: Long,
        lengthUtf16: Long,
    ): ByteArray? = SweetEditorJni.editorImeGetContext(editor, sessionId, source, startUtf16, lengthUtf16)
    actual fun editorGetCursorRect(editor: Long): FloatArray = SweetEditorJni.editorGetCursorRect(editor)
    actual fun editorGetPositionRect(editor: Long, line: Int, column: Int): FloatArray =
        SweetEditorJni.editorGetPositionRect(editor, line, column)
    actual fun editorGetVisibleLineRange(editor: Long): IntArray = SweetEditorJni.editorGetVisibleLineRange(editor)
    actual fun editorGetScrollMetrics(editor: Long): ByteArray? = SweetEditorJni.editorGetScrollMetrics(editor)
    actual fun editorGetSelectedText(editor: Long): ByteArray = SweetEditorJni.editorGetSelectedText(editor)
}
