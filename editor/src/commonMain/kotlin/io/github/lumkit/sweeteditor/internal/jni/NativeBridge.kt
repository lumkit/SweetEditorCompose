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
    fun editorTickAnimations(editor: Long): ByteArray?
    fun editorInsertText(editor: Long, text: ByteArray): ByteArray?
    fun editorBackspace(editor: Long): ByteArray?
    fun editorUndo(editor: Long): ByteArray?
    fun editorRedo(editor: Long): ByteArray?
    fun editorCanUndo(editor: Long): Boolean
    fun editorCanRedo(editor: Long): Boolean
    fun editorSetGutterSticky(editor: Long, sticky: Boolean): ByteArray?
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
}
