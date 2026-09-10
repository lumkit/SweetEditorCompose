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
    @JvmStatic external fun editorTickAnimations(editor: Long): ByteArray?
    @JvmStatic external fun editorInsertText(editor: Long, text: ByteArray): ByteArray?
    @JvmStatic external fun editorBackspace(editor: Long): ByteArray?
    @JvmStatic external fun editorUndo(editor: Long): ByteArray?
    @JvmStatic external fun editorRedo(editor: Long): ByteArray?
    @JvmStatic external fun editorCanUndo(editor: Long): Boolean
    @JvmStatic external fun editorCanRedo(editor: Long): Boolean
    @JvmStatic external fun editorSetGutterSticky(editor: Long, sticky: Boolean): ByteArray?
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
}
