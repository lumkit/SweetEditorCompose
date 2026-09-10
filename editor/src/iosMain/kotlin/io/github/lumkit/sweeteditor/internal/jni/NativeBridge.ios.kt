package io.github.lumkit.sweeteditor.internal.jni

import io.github.lumkit.sweeteditor.core.HostTextMeasurer
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.FloatVar
import kotlinx.cinterop.UByteVar
import kotlinx.cinterop.UShortVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.cValue
import kotlinx.cinterop.convert
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.set
import kotlinx.cinterop.staticCFunction
import kotlinx.cinterop.toKString
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import platform.posix.size_tVar
import sweeteditor.cinterop.create_document_from_utf8
import sweeteditor.cinterop.create_editor
import sweeteditor.cinterop.editor_backspace
import sweeteditor.cinterop.editor_build_render_model
import sweeteditor.cinterop.editor_can_redo
import sweeteditor.cinterop.editor_can_undo
import sweeteditor.cinterop.editor_handle_gesture_event
import sweeteditor.cinterop.editor_handle_key_event
import sweeteditor.cinterop.editor_ime_apply_commands
import sweeteditor.cinterop.editor_ime_begin_session
import sweeteditor.cinterop.editor_ime_end_session
import sweeteditor.cinterop.editor_ime_get_context
import sweeteditor.cinterop.editor_ime_get_state
import sweeteditor.cinterop.editor_insert_text
import sweeteditor.cinterop.editor_on_font_metrics_changed
import sweeteditor.cinterop.editor_redo
import sweeteditor.cinterop.editor_set_document
import sweeteditor.cinterop.editor_set_gutter_sticky
import sweeteditor.cinterop.editor_set_viewport
import sweeteditor.cinterop.editor_tick_animations
import sweeteditor.cinterop.editor_undo
import sweeteditor.cinterop.free_binary_data
import sweeteditor.cinterop.free_document
import sweeteditor.cinterop.free_editor
import sweeteditor.cinterop.free_u8_string
import sweeteditor.cinterop.get_document_utf8
import sweeteditor.cinterop.text_measurer_t
@OptIn(ExperimentalForeignApi::class)
internal actual object NativeBridge {
    actual val isAvailable: Boolean = true

    private val measurers = HashMap<Long, HostTextMeasurer>()
    private val activeStack = ArrayDeque<HostTextMeasurer>()
    private var pendingMeasurer: HostTextMeasurer? = null

    private fun currentMeasurer(): HostTextMeasurer? =
        pendingMeasurer ?: activeStack.lastOrNull()

    private val measureTextWidth = staticCFunction { text: CPointer<UShortVar>?, fontStyle: Int ->
        currentMeasurer()?.measureTextWidth(text.readUtf16(), fontStyle) ?: 0f
    }

    private val measureInlayHintWidth = staticCFunction { text: CPointer<UShortVar>? ->
        currentMeasurer()?.measureInlayHintWidth(text.readUtf16()) ?: 0f
    }

    private val measureIconWidth = staticCFunction { iconId: Int ->
        currentMeasurer()?.measureIconWidth(iconId) ?: 0f
    }

    private val getFontMetrics = staticCFunction { arr: CPointer<FloatVar>?, length: ULong ->
        if (arr == null || length < 2u) return@staticCFunction
        val measurer = currentMeasurer()
        arr[0] = measurer?.fontAscent() ?: 0f
        arr[1] = measurer?.fontDescent() ?: 0f
    }

    actual fun createDocumentFromUtf8(utf8: ByteArray): Long = memScoped {
        val text = utf8.decodeToString()
        val bytes = text.encodeToByteArray() + 0
        bytes.usePinned { pinned ->
            create_document_from_utf8(pinned.addressOf(0))
        }
    }

    actual fun freeDocument(handle: Long) {
        free_document(handle)
    }

    actual fun getDocumentUtf8(handle: Long): ByteArray {
        val ptr = get_document_utf8(handle) ?: return ByteArray(0)
        val text = ptr.toKString()
        free_u8_string(ptr.rawValue.toLong())
        return text.encodeToByteArray()
    }

    actual fun createEditor(measurer: HostTextMeasurer, options: ByteArray): Long {
        pendingMeasurer = measurer
        val handle = memScoped {
            val native = cValue<text_measurer_t> {
                measure_text_width = measureTextWidth
                measure_inlay_hint_width = measureInlayHintWidth
                measure_icon_width = measureIconWidth
                get_font_metrics = getFontMetrics
            }
            if (options.isEmpty()) {
                create_editor(native, null, 0u)
            } else {
                options.usePinned { pinned ->
                    create_editor(native, pinned.addressOf(0).reinterpret(), options.size.convert())
                }
            }
        }
        pendingMeasurer = null
        if (handle != 0L) {
            measurers[handle] = measurer
        }
        return handle
    }

    actual fun freeEditor(handle: Long) {
        free_editor(handle)
        measurers.remove(handle)
    }

    actual fun editorSetDocument(editor: Long, document: Long): ByteArray? =
        withActive(editor) { adoptBinary { size -> editor_set_document(editor, document, size) } }

    actual fun editorSetViewport(editor: Long, width: Int, height: Int): ByteArray? =
        withActive(editor) { adoptBinary { size -> editor_set_viewport(editor, width, height, size) } }

    actual fun editorOnFontMetricsChanged(editor: Long): ByteArray? =
        withActive(editor) { adoptBinary { size -> editor_on_font_metrics_changed(editor, size) } }

    actual fun editorBuildRenderModel(editor: Long): ByteArray? =
        withActive(editor) { adoptBinary { size -> editor_build_render_model(editor, size) } }

    actual fun editorHandleGestureEvent(editor: Long, payload: ByteArray): ByteArray? =
        withActive(editor) {
            adoptBinary { size ->
                payload.usePinned { pinned ->
                    editor_handle_gesture_event(
                        editor,
                        pinned.addressOf(0).reinterpret(),
                        payload.size.convert(),
                        size,
                    )
                }
            }
        }

    actual fun editorHandleKeyEvent(editor: Long, keyCode: Int, text: ByteArray?, modifiers: Int): ByteArray? =
        withActive(editor) {
            adoptBinary { size ->
                if (text == null || text.isEmpty()) {
                    editor_handle_key_event(editor, keyCode.toUShort(), null, modifiers.toUByte(), size)
                } else {
                    val terminated = text + 0
                    terminated.usePinned { pinned ->
                        editor_handle_key_event(
                            editor,
                            keyCode.toUShort(),
                            pinned.addressOf(0),
                            modifiers.toUByte(),
                            size,
                        )
                    }
                }
            }
        }

    actual fun editorTickAnimations(editor: Long): ByteArray? =
        withActive(editor) { adoptBinary { size -> editor_tick_animations(editor, size) } }

    actual fun editorInsertText(editor: Long, text: ByteArray): ByteArray? =
        withActive(editor) {
            adoptBinary { size ->
                val terminated = text + 0
                terminated.usePinned { pinned ->
                    editor_insert_text(editor, pinned.addressOf(0), size)
                }
            }
        }

    actual fun editorBackspace(editor: Long): ByteArray? =
        withActive(editor) { adoptBinary { size -> editor_backspace(editor, size) } }

    actual fun editorUndo(editor: Long): ByteArray? =
        withActive(editor) { adoptBinary { size -> editor_undo(editor, size) } }

    actual fun editorRedo(editor: Long): ByteArray? =
        withActive(editor) { adoptBinary { size -> editor_redo(editor, size) } }

    actual fun editorCanUndo(editor: Long): Boolean = editor_can_undo(editor) != 0

    actual fun editorCanRedo(editor: Long): Boolean = editor_can_redo(editor) != 0

    actual fun editorSetGutterSticky(editor: Long, sticky: Boolean): ByteArray? =
        withActive(editor) {
            adoptBinary { size -> editor_set_gutter_sticky(editor, if (sticky) 1 else 0, size) }
        }

    actual fun editorImeBeginSession(editor: Long, mutationModel: Int): ByteArray? =
        withActive(editor) { adoptBinary { size -> editor_ime_begin_session(editor, mutationModel, size) } }

    actual fun editorImeEndSession(editor: Long, sessionId: Long): ByteArray? =
        withActive(editor) {
            adoptBinary { size -> editor_ime_end_session(editor, sessionId.toULong(), size) }
        }

    actual fun editorImeApplyCommands(editor: Long, payload: ByteArray): ByteArray? =
        withActive(editor) {
            adoptBinary { size ->
                payload.usePinned { pinned ->
                    editor_ime_apply_commands(
                        editor,
                        pinned.addressOf(0).reinterpret(),
                        payload.size.convert(),
                        size,
                    )
                }
            }
        }

    actual fun editorImeGetState(editor: Long, sessionId: Long): ByteArray? =
        withActive(editor) {
            adoptBinary { size -> editor_ime_get_state(editor, sessionId.toULong(), size) }
        }

    actual fun editorImeGetContext(
        editor: Long,
        sessionId: Long,
        source: Int,
        startUtf16: Long,
        lengthUtf16: Long,
    ): ByteArray? = withActive(editor) {
        adoptBinary { size ->
            editor_ime_get_context(editor, sessionId.toULong(), source, startUtf16, lengthUtf16, size)
        }
    }

    private inline fun <T> withActive(handle: Long, block: () -> T): T {
        val measurer = measurers[handle]
        if (measurer != null) activeStack.addLast(measurer)
        return try {
            block()
        } finally {
            if (measurer != null) activeStack.removeLast()
        }
    }

    private inline fun adoptBinary(
        block: (CPointer<size_tVar>) -> CPointer<UByteVar>?,
    ): ByteArray? = memScoped {
        val size = alloc<size_tVar>()
        val ptr = block(size.ptr) ?: return@memScoped null
        val n = size.value.toInt()
        val bytes = ByteArray(n) { index -> ptr[index].toByte() }
        free_binary_data(ptr.rawValue.toLong())
        bytes
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun CPointer<UShortVar>?.readUtf16(): String {
    if (this == null) return ""
    val chars = StringBuilder()
    var index = 0
    while (true) {
        val unit = this[index].toInt()
        if (unit == 0) break
        chars.append(Char(unit))
        index++
    }
    return chars.toString()
}
