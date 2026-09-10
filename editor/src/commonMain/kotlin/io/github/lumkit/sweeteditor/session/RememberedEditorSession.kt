package io.github.lumkit.sweeteditor.session

import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.lumkit.sweeteditor.SweetEditorController
import io.github.lumkit.sweeteditor.core.Document
import io.github.lumkit.sweeteditor.core.EditorCore
import io.github.lumkit.sweeteditor.core.HostTextMeasurer
import io.github.lumkit.sweeteditor.core.protocol.AnimationFlag
import io.github.lumkit.sweeteditor.core.protocol.EditorActionResult
import io.github.lumkit.sweeteditor.core.protocol.EditorRenderModel
import io.github.lumkit.sweeteditor.core.protocol.ImeHostAction
import io.github.lumkit.sweeteditor.core.protocol.PointerCursorType
import io.github.lumkit.sweeteditor.internal.jni.NativeBridge

internal class RememberedEditorSession(
    private val controller: SweetEditorController,
    private val initialText: String,
    private var measurer: HostTextMeasurer,
) : RememberObserver {
    var renderModel by mutableStateOf<EditorRenderModel?>(null)
        private set
    var loadError by mutableStateOf<String?>(null)
        private set
    var wantsAnimation by mutableStateOf(false)
        private set
    var pointerCursor by mutableStateOf(PointerCursorType.TEXT)
        private set

    val isReady: Boolean get() = editor != null && !disposed

    private var editor: EditorCore? = null
    private var document: Document? = null
    private var disposed = false
    private var viewportWidth = 0
    private var viewportHeight = 0

    override fun onRemembered() {
        if (disposed || editor != null) return
        try {
            if (!NativeBridge.isAvailable) {
                loadError = "SweetEditor native core is not available on this target yet"
                return
            }
            val createdDocument = Document.fromUtf8(initialText)
            val createdEditor = EditorCore.create(measurer)
            document = createdDocument
            editor = createdEditor
            dispatchActionResult(createdEditor.setDocument(createdDocument))
            if (viewportWidth > 0 && viewportHeight > 0) {
                dispatchActionResult(createdEditor.setViewport(viewportWidth, viewportHeight))
            }
            controller.attach(this)
        } catch (error: Throwable) {
            loadError = error.message ?: error.toString()
        }
    }

    override fun onForgotten() = disposeSession()

    override fun onAbandoned() = disposeSession()

    fun notifyFontMetricsChanged() {
        val core = editor ?: return
        dispatchActionResult(core.onFontMetricsChanged())
        if (viewportWidth > 0 && viewportHeight > 0) {
            dispatchActionResult(core.setViewport(viewportWidth, viewportHeight))
        }
    }

    fun setViewport(width: Int, height: Int) {
        val w = width.coerceAtLeast(1)
        val h = height.coerceAtLeast(1)
        if (w == viewportWidth && h == viewportHeight) return
        viewportWidth = w
        viewportHeight = h
        val core = editor ?: return
        dispatchActionResult(core.setViewport(w, h))
    }

    fun handleGesture(payload: ByteArray) {
        val core = editor ?: return
        dispatchActionResult(core.handleGestureEvent(payload))
    }

    fun handleKey(keyCode: Int, text: ByteArray?, modifiers: Int) {
        val core = editor ?: return
        dispatchActionResult(core.handleKeyEvent(keyCode, text, modifiers))
    }

    fun insertText(text: String) {
        val core = editor ?: return
        dispatchActionResult(core.insertText(text))
    }

    fun backspace() {
        val core = editor ?: return
        dispatchActionResult(core.backspace())
    }

    fun undo() {
        val core = editor ?: return
        dispatchActionResult(core.undo())
    }

    fun redo() {
        val core = editor ?: return
        dispatchActionResult(core.redo())
    }

    fun tickAnimations() {
        val core = editor ?: return
        dispatchActionResult(core.tickAnimations())
    }

    fun documentUtf8(): String? = document?.utf8Text()

    private fun disposeSession() {
        if (disposed) return
        disposed = true
        wantsAnimation = false
        controller.detach(this)
        editor?.close()
        editor = null
        document?.close()
        document = null
    }

    private fun dispatchActionResult(result: EditorActionResult?) {
        if (disposed || result == null) return
        when (result.imeHostAction) {
            ImeHostAction.CLOSE_SESSION, ImeHostAction.RESTART_SESSION -> Unit
            else -> Unit
        }
        wantsAnimation = result.animationFlags != AnimationFlag.NONE
        if (result.pointerCursorChanged) {
            pointerCursor = result.pointerCursorAfter
        }
        if (result.needsRedraw || renderModel == null) {
            try {
                val model = editor?.buildRenderModel()
                if (model != null) {
                    renderModel = model
                    pointerCursor = model.pointerCursorType
                }
            } catch (error: Throwable) {
                loadError = error.message ?: error.toString()
            }
        }
    }
}
