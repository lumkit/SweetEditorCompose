package io.github.lumkit.sweeteditor.internal.jni

import io.github.lumkit.sweeteditor.core.HostTextMeasurer
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.FloatVar
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.UByteVar
import kotlinx.cinterop.UIntVar
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
import sweeteditor.cinterop.editor_clear_all_decorations
import sweeteditor.cinterop.editor_clear_guides
import sweeteditor.cinterop.editor_clear_codelens
import sweeteditor.cinterop.editor_clear_diagnostics
import sweeteditor.cinterop.editor_clear_document_highlights
import sweeteditor.cinterop.editor_clear_gutter_icons
import sweeteditor.cinterop.editor_clear_highlights
import sweeteditor.cinterop.editor_clear_highlights_layer
import sweeteditor.cinterop.editor_clear_inlay_hints
import sweeteditor.cinterop.editor_clear_line_spans
import sweeteditor.cinterop.editor_clear_links
import sweeteditor.cinterop.editor_clear_phantom_texts
import sweeteditor.cinterop.editor_fold_all
import sweeteditor.cinterop.editor_fold_at
import sweeteditor.cinterop.editor_get_link_target_at
import sweeteditor.cinterop.editor_is_line_visible
import sweeteditor.cinterop.editor_register_batch_text_styles
import sweeteditor.cinterop.editor_register_text_style
import sweeteditor.cinterop.editor_set_batch_line_codelens
import sweeteditor.cinterop.editor_set_batch_line_diagnostics
import sweeteditor.cinterop.editor_set_batch_line_document_highlights
import sweeteditor.cinterop.editor_set_batch_line_gutter_icons
import sweeteditor.cinterop.editor_set_batch_line_inlay_hints
import sweeteditor.cinterop.editor_set_batch_line_links
import sweeteditor.cinterop.editor_set_batch_line_phantom_texts
import sweeteditor.cinterop.editor_set_batch_line_spans
import sweeteditor.cinterop.editor_set_line_codelens
import sweeteditor.cinterop.editor_set_line_diagnostics
import sweeteditor.cinterop.editor_set_line_document_highlights
import sweeteditor.cinterop.editor_set_line_gutter_icons
import sweeteditor.cinterop.editor_set_line_inlay_hints
import sweeteditor.cinterop.editor_set_line_links
import sweeteditor.cinterop.editor_set_line_phantom_texts
import sweeteditor.cinterop.editor_set_line_spans
import sweeteditor.cinterop.editor_set_max_gutter_icons
import sweeteditor.cinterop.editor_can_redo
import sweeteditor.cinterop.editor_can_undo
import sweeteditor.cinterop.editor_get_cursor_rect
import sweeteditor.cinterop.editor_get_position_rect
import sweeteditor.cinterop.editor_get_scroll_metrics
import sweeteditor.cinterop.editor_get_selected_text
import sweeteditor.cinterop.editor_get_visible_line_range
import sweeteditor.cinterop.editor_handle_gesture_event
import sweeteditor.cinterop.editor_handle_key_event
import sweeteditor.cinterop.editor_set_keymap
import sweeteditor.cinterop.editor_ime_apply_commands
import sweeteditor.cinterop.editor_ime_begin_session
import sweeteditor.cinterop.editor_ime_end_session
import sweeteditor.cinterop.editor_ime_get_context
import sweeteditor.cinterop.editor_ime_get_state
import sweeteditor.cinterop.editor_copy_line_down
import sweeteditor.cinterop.editor_copy_line_up
import sweeteditor.cinterop.editor_delete_line
import sweeteditor.cinterop.editor_insert_line_above
import sweeteditor.cinterop.editor_insert_line_below
import sweeteditor.cinterop.editor_apply_text_edits
import sweeteditor.cinterop.editor_get_cursor_position
import sweeteditor.cinterop.editor_get_word_range_at_cursor
import sweeteditor.cinterop.editor_insert_text
import sweeteditor.cinterop.editor_replace_text
import sweeteditor.cinterop.editor_move_line_down
import sweeteditor.cinterop.editor_move_line_up
import sweeteditor.cinterop.editor_on_font_metrics_changed
import sweeteditor.cinterop.editor_redo
import sweeteditor.cinterop.editor_cancel_linked_editing
import sweeteditor.cinterop.editor_clear_diff
import sweeteditor.cinterop.editor_insert_snippet
import sweeteditor.cinterop.editor_is_in_linked_editing
import sweeteditor.cinterop.editor_linked_editing_next
import sweeteditor.cinterop.editor_linked_editing_prev
import sweeteditor.cinterop.editor_start_linked_editing
import sweeteditor.cinterop.editor_clear_matched_brackets
import sweeteditor.cinterop.editor_compute_diff
import sweeteditor.cinterop.editor_set_batch_diff_line_spans
import sweeteditor.cinterop.editor_set_diff_changes
import sweeteditor.cinterop.editor_set_auto_closing_pairs
import sweeteditor.cinterop.editor_set_matched_brackets
import sweeteditor.cinterop.editor_set_auto_indent_mode
import sweeteditor.cinterop.editor_set_backspace_unindent
import sweeteditor.cinterop.editor_set_bracket_pairs
import sweeteditor.cinterop.editor_set_current_line_render_mode
import sweeteditor.cinterop.editor_set_document
import sweeteditor.cinterop.editor_clear_search
import sweeteditor.cinterop.editor_find_next_search_match
import sweeteditor.cinterop.editor_find_previous_search_match
import sweeteditor.cinterop.editor_get_search_state
import sweeteditor.cinterop.editor_replace_all_search_matches
import sweeteditor.cinterop.editor_replace_current_search_match
import sweeteditor.cinterop.editor_search
import sweeteditor.cinterop.editor_set_editor_range_effect_styles
import sweeteditor.cinterop.editor_set_editor_render_colors
import sweeteditor.cinterop.editor_set_fold_arrow_mode
import sweeteditor.cinterop.editor_set_bracket_guides
import sweeteditor.cinterop.editor_set_flow_guides
import sweeteditor.cinterop.editor_set_fold_regions
import sweeteditor.cinterop.editor_set_indent_guides
import sweeteditor.cinterop.editor_set_separator_guides
import sweeteditor.cinterop.editor_set_gutter_sticky
import sweeteditor.cinterop.editor_set_gutter_visible
import sweeteditor.cinterop.editor_set_insert_spaces
import sweeteditor.cinterop.editor_set_line_spacing
import sweeteditor.cinterop.editor_set_read_only
import sweeteditor.cinterop.editor_set_render_line_breaks
import sweeteditor.cinterop.editor_set_render_whitespace
import sweeteditor.cinterop.editor_set_scale
import sweeteditor.cinterop.editor_set_tab_size
import sweeteditor.cinterop.editor_set_viewport
import sweeteditor.cinterop.editor_set_wrap_mode
import sweeteditor.cinterop.editor_tick_animations
import sweeteditor.cinterop.editor_toggle_fold
import sweeteditor.cinterop.editor_unfold_all
import sweeteditor.cinterop.editor_unfold_at
import sweeteditor.cinterop.editor_update_pointer_modifiers
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

    actual fun editorUpdatePointerModifiers(editor: Long, modifiers: Int): ByteArray? =
        withActive(editor) {
            adoptBinary { size -> editor_update_pointer_modifiers(editor, modifiers.toUByte(), size) }
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

    actual fun editorSetKeyMap(editor: Long, payload: ByteArray): ByteArray? =
        withActive(editor) {
            adoptBinary { size ->
                payload.usePinned { pinned ->
                    editor_set_keymap(
                        editor,
                        pinned.addressOf(0).reinterpret(),
                        payload.size.convert(),
                        size,
                    )
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

    actual fun editorReplaceText(
        editor: Long,
        startLine: Int,
        startColumn: Int,
        endLine: Int,
        endColumn: Int,
        text: ByteArray,
    ): ByteArray? = withActive(editor) {
        adoptBinary { size ->
            val terminated = text + 0
            terminated.usePinned { pinned ->
                editor_replace_text(
                    editor,
                    startLine.convert(),
                    startColumn.convert(),
                    endLine.convert(),
                    endColumn.convert(),
                    pinned.addressOf(0),
                    size,
                )
            }
        }
    }

    actual fun editorApplyTextEdits(editor: Long, payload: ByteArray): ByteArray? =
        withActive(editor) {
            adoptBinary { size ->
                payload.usePinned { pinned ->
                    editor_apply_text_edits(
                        editor,
                        pinned.addressOf(0).reinterpret(),
                        payload.size.convert(),
                        size,
                    )
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

    actual fun editorSetGutterVisible(editor: Long, visible: Boolean): ByteArray? =
        withActive(editor) {
            adoptBinary { size -> editor_set_gutter_visible(editor, if (visible) 1 else 0, size) }
        }

    actual fun editorSetWrapMode(editor: Long, mode: Int): ByteArray? =
        withActive(editor) { adoptBinary { size -> editor_set_wrap_mode(editor, mode, size) } }

    actual fun editorSetTabSize(editor: Long, tabSize: Int): ByteArray? =
        withActive(editor) { adoptBinary { size -> editor_set_tab_size(editor, tabSize, size) } }

    actual fun editorSetInsertSpaces(editor: Long, enabled: Boolean): ByteArray? =
        withActive(editor) {
            adoptBinary { size -> editor_set_insert_spaces(editor, if (enabled) 1 else 0, size) }
        }

    actual fun editorSetBracketPairs(editor: Long, openChars: IntArray, closeChars: IntArray): ByteArray? =
        withActive(editor) {
            adoptBinary { size -> callCharPairs(openChars, closeChars) { opens, closes, count ->
                editor_set_bracket_pairs(editor, opens, closes, count, size)
            } }
        }

    actual fun editorSetAutoClosingPairs(editor: Long, openChars: IntArray, closeChars: IntArray): ByteArray? =
        withActive(editor) {
            adoptBinary { size -> callCharPairs(openChars, closeChars) { opens, closes, count ->
                editor_set_auto_closing_pairs(editor, opens, closes, count, size)
            } }
        }

    actual fun editorSetMatchedBrackets(
        editor: Long,
        openLine: Int,
        openColumn: Int,
        closeLine: Int,
        closeColumn: Int,
    ): ByteArray? =
        withActive(editor) {
            adoptBinary { size ->
                editor_set_matched_brackets(
                    editor,
                    openLine.toULong(),
                    openColumn.toULong(),
                    closeLine.toULong(),
                    closeColumn.toULong(),
                    size,
                )
            }
        }

    actual fun editorClearMatchedBrackets(editor: Long): ByteArray? =
        withActive(editor) { adoptBinary { size -> editor_clear_matched_brackets(editor, size) } }

    actual fun editorSetDiffChanges(editor: Long, payload: ByteArray): ByteArray? =
        withActive(editor) {
            adoptBinary { size ->
                payload.usePinned { pinned ->
                    editor_set_diff_changes(
                        editor,
                        pinned.addressOf(0).reinterpret(),
                        payload.size.convert(),
                        size,
                    )
                }
            }
        }

    actual fun editorComputeDiff(editor: Long, originalUtf8: ByteArray): ByteArray? =
        withActive(editor) {
            adoptBinary { size ->
                val terminated = originalUtf8 + 0
                terminated.usePinned { pinned ->
                    editor_compute_diff(editor, pinned.addressOf(0), size)
                }
            }
        }

    actual fun editorSetBatchDiffLineSpans(editor: Long, payload: ByteArray): ByteArray? =
        withActive(editor) {
            adoptBinary { size ->
                payload.usePinned { pinned ->
                    editor_set_batch_diff_line_spans(
                        editor,
                        pinned.addressOf(0).reinterpret(),
                        payload.size.convert(),
                        size,
                    )
                }
            }
        }

    actual fun editorClearDiff(editor: Long): ByteArray? =
        withActive(editor) { adoptBinary { size -> editor_clear_diff(editor, size) } }

    actual fun editorInsertSnippet(editor: Long, snippetUtf8: ByteArray): ByteArray? =
        withActive(editor) {
            adoptBinary { size ->
                val terminated = snippetUtf8 + 0
                terminated.usePinned { pinned ->
                    editor_insert_snippet(editor, pinned.addressOf(0), size)
                }
            }
        }

    actual fun editorStartLinkedEditing(editor: Long, payload: ByteArray): ByteArray? =
        withActive(editor) {
            adoptBinary { size ->
                payload.usePinned { pinned ->
                    editor_start_linked_editing(
                        editor,
                        pinned.addressOf(0).reinterpret(),
                        payload.size.convert(),
                        size,
                    )
                }
            }
        }

    actual fun editorIsInLinkedEditing(editor: Long): Boolean =
        withActive(editor) { editor_is_in_linked_editing(editor) != 0 }

    actual fun editorLinkedEditingNext(editor: Long): ByteArray? =
        withActive(editor) { adoptBinary { size -> editor_linked_editing_next(editor, size) } }

    actual fun editorLinkedEditingPrev(editor: Long): ByteArray? =
        withActive(editor) { adoptBinary { size -> editor_linked_editing_prev(editor, size) } }

    actual fun editorCancelLinkedEditing(editor: Long): ByteArray? =
        withActive(editor) { adoptBinary { size -> editor_cancel_linked_editing(editor, size) } }

    actual fun editorSetAutoIndentMode(editor: Long, mode: Int): ByteArray? =
        withActive(editor) { adoptBinary { size -> editor_set_auto_indent_mode(editor, mode, size) } }

    actual fun editorSetBackspaceUnindent(editor: Long, enabled: Boolean): ByteArray? =
        withActive(editor) {
            adoptBinary { size -> editor_set_backspace_unindent(editor, if (enabled) 1 else 0, size) }
        }

    actual fun editorMoveLineUp(editor: Long): ByteArray? =
        withActive(editor) { adoptBinary { size -> editor_move_line_up(editor, size) } }

    actual fun editorMoveLineDown(editor: Long): ByteArray? =
        withActive(editor) { adoptBinary { size -> editor_move_line_down(editor, size) } }

    actual fun editorCopyLineUp(editor: Long): ByteArray? =
        withActive(editor) { adoptBinary { size -> editor_copy_line_up(editor, size) } }

    actual fun editorCopyLineDown(editor: Long): ByteArray? =
        withActive(editor) { adoptBinary { size -> editor_copy_line_down(editor, size) } }

    actual fun editorDeleteLine(editor: Long): ByteArray? =
        withActive(editor) { adoptBinary { size -> editor_delete_line(editor, size) } }

    actual fun editorInsertLineAbove(editor: Long): ByteArray? =
        withActive(editor) { adoptBinary { size -> editor_insert_line_above(editor, size) } }

    actual fun editorInsertLineBelow(editor: Long): ByteArray? =
        withActive(editor) { adoptBinary { size -> editor_insert_line_below(editor, size) } }

    actual fun editorSetScale(editor: Long, scale: Float): ByteArray? =
        withActive(editor) { adoptBinary { size -> editor_set_scale(editor, scale, size) } }

    actual fun editorSetLineSpacing(editor: Long, add: Float, mult: Float): ByteArray? =
        withActive(editor) { adoptBinary { size -> editor_set_line_spacing(editor, add, mult, size) } }

    actual fun editorSetReadOnly(editor: Long, readOnly: Boolean): ByteArray? =
        withActive(editor) {
            adoptBinary { size -> editor_set_read_only(editor, if (readOnly) 1 else 0, size) }
        }

    actual fun editorSetCurrentLineRenderMode(editor: Long, mode: Int): ByteArray? =
        withActive(editor) {
            adoptBinary { size -> editor_set_current_line_render_mode(editor, mode, size) }
        }

    actual fun editorSetFoldArrowMode(editor: Long, mode: Int): ByteArray? =
        withActive(editor) { adoptBinary { size -> editor_set_fold_arrow_mode(editor, mode, size) } }

    actual fun editorSetRenderWhitespace(editor: Long, mode: Int): ByteArray? =
        withActive(editor) { adoptBinary { size -> editor_set_render_whitespace(editor, mode, size) } }

    actual fun editorSetRenderLineBreaks(editor: Long, enabled: Boolean): ByteArray? =
        withActive(editor) {
            adoptBinary { size -> editor_set_render_line_breaks(editor, if (enabled) 1 else 0, size) }
        }

    actual fun editorSetEditorRenderColors(editor: Long, payload: ByteArray): ByteArray? =
        withActive(editor) {
            adoptBinary { size ->
                payload.usePinned { pinned ->
                    editor_set_editor_render_colors(
                        editor,
                        pinned.addressOf(0).reinterpret(),
                        payload.size.convert(),
                        size,
                    )
                }
            }
        }

    actual fun editorSetEditorRangeEffectStyles(editor: Long, payload: ByteArray): ByteArray? =
        withActive(editor) {
            adoptBinary { size ->
                payload.usePinned { pinned ->
                    editor_set_editor_range_effect_styles(
                        editor,
                        pinned.addressOf(0).reinterpret(),
                        payload.size.convert(),
                        size,
                    )
                }
            }
        }

    actual fun editorSearch(editor: Long, payload: ByteArray): ByteArray? =
        withActive(editor) {
            adoptBinary { size ->
                payload.usePinned { pinned ->
                    editor_search(
                        editor,
                        pinned.addressOf(0).reinterpret(),
                        payload.size.convert(),
                        size,
                    )
                }
            }
        }

    actual fun editorFindNextSearchMatch(editor: Long): ByteArray? =
        withActive(editor) { adoptBinary { size -> editor_find_next_search_match(editor, size) } }

    actual fun editorFindPreviousSearchMatch(editor: Long): ByteArray? =
        withActive(editor) { adoptBinary { size -> editor_find_previous_search_match(editor, size) } }

    actual fun editorReplaceCurrentSearchMatch(editor: Long, payload: ByteArray): ByteArray? =
        withActive(editor) {
            adoptBinary { size ->
                payload.usePinned { pinned ->
                    editor_replace_current_search_match(
                        editor,
                        pinned.addressOf(0).reinterpret(),
                        payload.size.convert(),
                        size,
                    )
                }
            }
        }

    actual fun editorReplaceAllSearchMatches(editor: Long, payload: ByteArray): ByteArray? =
        withActive(editor) {
            adoptBinary { size ->
                payload.usePinned { pinned ->
                    editor_replace_all_search_matches(
                        editor,
                        pinned.addressOf(0).reinterpret(),
                        payload.size.convert(),
                        size,
                    )
                }
            }
        }

    actual fun editorClearSearch(editor: Long): ByteArray? =
        withActive(editor) { adoptBinary { size -> editor_clear_search(editor, size) } }

    actual fun editorGetSearchState(editor: Long): ByteArray? =
        withActive(editor) { adoptBinary { size -> editor_get_search_state(editor, size) } }

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

    actual fun editorGetCursorRect(editor: Long): FloatArray = withActive(editor) {
        memScoped {
            val x = alloc<FloatVar>()
            val y = alloc<FloatVar>()
            val height = alloc<FloatVar>()
            editor_get_cursor_rect(editor, x.ptr, y.ptr, height.ptr)
            floatArrayOf(x.value, y.value, height.value)
        }
    }

    actual fun editorGetPositionRect(editor: Long, line: Int, column: Int): FloatArray = withActive(editor) {
        memScoped {
            val x = alloc<FloatVar>()
            val y = alloc<FloatVar>()
            val height = alloc<FloatVar>()
            editor_get_position_rect(
                editor,
                line.convert(),
                column.convert(),
                x.ptr,
                y.ptr,
                height.ptr,
            )
            floatArrayOf(x.value, y.value, height.value)
        }
    }

    actual fun editorGetVisibleLineRange(editor: Long): IntArray = withActive(editor) {
        memScoped {
            val start = alloc<IntVar>()
            val end = alloc<IntVar>()
            editor_get_visible_line_range(editor, start.ptr, end.ptr)
            intArrayOf(start.value, end.value)
        }
    }

    actual fun editorGetScrollMetrics(editor: Long): ByteArray? =
        withActive(editor) { adoptBinary { size -> editor_get_scroll_metrics(editor, size) } }

    actual fun editorGetSelectedText(editor: Long): ByteArray = withActive(editor) {
        val ptr = editor_get_selected_text(editor) ?: return@withActive ByteArray(0)
        val text = ptr.toKString()
        free_u8_string(ptr.rawValue.toLong())
        text.encodeToByteArray()
    }

    actual fun editorGetCursorPosition(editor: Long): IntArray = withActive(editor) {
        memScoped {
            val line = alloc<size_tVar>()
            val column = alloc<size_tVar>()
            editor_get_cursor_position(editor, line.ptr, column.ptr)
            intArrayOf(line.value.toInt(), column.value.toInt())
        }
    }

    actual fun editorGetWordRangeAtCursor(editor: Long): IntArray = withActive(editor) {
        memScoped {
            val startLine = alloc<size_tVar>()
            val startColumn = alloc<size_tVar>()
            val endLine = alloc<size_tVar>()
            val endColumn = alloc<size_tVar>()
            editor_get_word_range_at_cursor(
                editor,
                startLine.ptr,
                startColumn.ptr,
                endLine.ptr,
                endColumn.ptr,
            )
            intArrayOf(
                startLine.value.toInt(),
                startColumn.value.toInt(),
                endLine.value.toInt(),
                endColumn.value.toInt(),
            )
        }
    }

    actual fun editorDecorationOp(
        editor: Long,
        op: Int,
        payload: ByteArray?,
        a: Int,
        b: Int,
        c: Int,
        d: Int,
    ): ByteArray? = withActive(editor) {
        adoptBinary { size ->
            when (op) {
                NativeDecorationOp.SET_LINE_SPANS ->
                    callPayload(payload) { ptr, n -> editor_set_line_spans(editor, ptr, n, size) }
                NativeDecorationOp.SET_BATCH_LINE_SPANS ->
                    callPayload(payload) { ptr, n -> editor_set_batch_line_spans(editor, ptr, n, size) }
                NativeDecorationOp.REGISTER_BATCH_TEXT_STYLES ->
                    callPayload(payload) { ptr, n -> editor_register_batch_text_styles(editor, ptr, n, size) }
                NativeDecorationOp.SET_LINE_INLAY_HINTS ->
                    callPayload(payload) { ptr, n -> editor_set_line_inlay_hints(editor, ptr, n, size) }
                NativeDecorationOp.SET_BATCH_LINE_INLAY_HINTS ->
                    callPayload(payload) { ptr, n -> editor_set_batch_line_inlay_hints(editor, ptr, n, size) }
                NativeDecorationOp.SET_LINE_PHANTOM_TEXTS ->
                    callPayload(payload) { ptr, n -> editor_set_line_phantom_texts(editor, ptr, n, size) }
                NativeDecorationOp.SET_BATCH_LINE_PHANTOM_TEXTS ->
                    callPayload(payload) { ptr, n -> editor_set_batch_line_phantom_texts(editor, ptr, n, size) }
                NativeDecorationOp.SET_LINE_GUTTER_ICONS ->
                    callPayload(payload) { ptr, n -> editor_set_line_gutter_icons(editor, ptr, n, size) }
                NativeDecorationOp.SET_BATCH_LINE_GUTTER_ICONS ->
                    callPayload(payload) { ptr, n -> editor_set_batch_line_gutter_icons(editor, ptr, n, size) }
                NativeDecorationOp.SET_LINE_CODELENS ->
                    callPayload(payload) { ptr, n -> editor_set_line_codelens(editor, ptr, n, size) }
                NativeDecorationOp.SET_BATCH_LINE_CODELENS ->
                    callPayload(payload) { ptr, n -> editor_set_batch_line_codelens(editor, ptr, n, size) }
                NativeDecorationOp.SET_LINE_LINKS ->
                    callPayload(payload) { ptr, n -> editor_set_line_links(editor, ptr, n, size) }
                NativeDecorationOp.SET_BATCH_LINE_LINKS ->
                    callPayload(payload) { ptr, n -> editor_set_batch_line_links(editor, ptr, n, size) }
                NativeDecorationOp.SET_LINE_DIAGNOSTICS ->
                    callPayload(payload) { ptr, n -> editor_set_line_diagnostics(editor, ptr, n, size) }
                NativeDecorationOp.SET_BATCH_LINE_DIAGNOSTICS ->
                    callPayload(payload) { ptr, n -> editor_set_batch_line_diagnostics(editor, ptr, n, size) }
                NativeDecorationOp.SET_LINE_DOCUMENT_HIGHLIGHTS ->
                    callPayload(payload) { ptr, n -> editor_set_line_document_highlights(editor, ptr, n, size) }
                NativeDecorationOp.SET_BATCH_LINE_DOCUMENT_HIGHLIGHTS ->
                    callPayload(payload) { ptr, n -> editor_set_batch_line_document_highlights(editor, ptr, n, size) }
                NativeDecorationOp.CLEAR_HIGHLIGHTS -> editor_clear_highlights(editor, size)
                NativeDecorationOp.CLEAR_HIGHLIGHTS_LAYER ->
                    editor_clear_highlights_layer(editor, a.toUByte(), size)
                NativeDecorationOp.CLEAR_LINE_SPANS ->
                    editor_clear_line_spans(editor, a.toULong(), b.toUByte(), size)
                NativeDecorationOp.CLEAR_INLAY_HINTS -> editor_clear_inlay_hints(editor, size)
                NativeDecorationOp.CLEAR_PHANTOM_TEXTS -> editor_clear_phantom_texts(editor, size)
                NativeDecorationOp.CLEAR_GUTTER_ICONS -> editor_clear_gutter_icons(editor, size)
                NativeDecorationOp.CLEAR_CODELENS -> editor_clear_codelens(editor, size)
                NativeDecorationOp.CLEAR_LINKS -> editor_clear_links(editor, size)
                NativeDecorationOp.CLEAR_DIAGNOSTICS -> editor_clear_diagnostics(editor, size)
                NativeDecorationOp.CLEAR_DOCUMENT_HIGHLIGHTS -> editor_clear_document_highlights(editor, size)
                NativeDecorationOp.CLEAR_ALL_DECORATIONS -> editor_clear_all_decorations(editor, size)
                NativeDecorationOp.REGISTER_TEXT_STYLE ->
                    editor_register_text_style(editor, a.toUInt(), b, c, d, size)
                NativeDecorationOp.SET_MAX_GUTTER_ICONS ->
                    editor_set_max_gutter_icons(editor, a.toUInt(), size)
                else -> null
            }
        }
    }

    actual fun editorGetLinkTargetAt(editor: Long, line: Int, column: Int): ByteArray = withActive(editor) {
        val ptr = editor_get_link_target_at(editor, line.toULong(), column.toULong())
            ?: return@withActive ByteArray(0)
        val text = ptr.toKString()
        free_u8_string(ptr.rawValue.toLong())
        text.encodeToByteArray()
    }

    actual fun editorSetFoldRegions(editor: Long, payload: ByteArray): ByteArray? =
        withActive(editor) {
            adoptBinary { size ->
                payload.usePinned { pinned ->
                    editor_set_fold_regions(
                        editor,
                        pinned.addressOf(0).reinterpret(),
                        payload.size.convert(),
                        size,
                    )
                }
            }
        }

    actual fun editorToggleFold(editor: Long, line: Int): ByteArray? =
        withActive(editor) { adoptBinary { size -> editor_toggle_fold(editor, line.toULong(), size) } }

    actual fun editorFoldAt(editor: Long, line: Int): ByteArray? =
        withActive(editor) { adoptBinary { size -> editor_fold_at(editor, line.toULong(), size) } }

    actual fun editorUnfoldAt(editor: Long, line: Int): ByteArray? =
        withActive(editor) { adoptBinary { size -> editor_unfold_at(editor, line.toULong(), size) } }

    actual fun editorFoldAll(editor: Long): ByteArray? =
        withActive(editor) { adoptBinary { size -> editor_fold_all(editor, size) } }

    actual fun editorUnfoldAll(editor: Long): ByteArray? =
        withActive(editor) { adoptBinary { size -> editor_unfold_all(editor, size) } }

    actual fun editorIsLineVisible(editor: Long, line: Int): Boolean =
        withActive(editor) { editor_is_line_visible(editor, line.toULong()) != 0 }

    actual fun editorSetIndentGuides(editor: Long, payload: ByteArray): ByteArray? =
        withActive(editor) {
            adoptBinary { size ->
                payload.usePinned { pinned ->
                    editor_set_indent_guides(
                        editor,
                        pinned.addressOf(0).reinterpret(),
                        payload.size.convert(),
                        size,
                    )
                }
            }
        }

    actual fun editorSetBracketGuides(editor: Long, payload: ByteArray): ByteArray? =
        withActive(editor) {
            adoptBinary { size ->
                payload.usePinned { pinned ->
                    editor_set_bracket_guides(
                        editor,
                        pinned.addressOf(0).reinterpret(),
                        payload.size.convert(),
                        size,
                    )
                }
            }
        }

    actual fun editorSetFlowGuides(editor: Long, payload: ByteArray): ByteArray? =
        withActive(editor) {
            adoptBinary { size ->
                payload.usePinned { pinned ->
                    editor_set_flow_guides(
                        editor,
                        pinned.addressOf(0).reinterpret(),
                        payload.size.convert(),
                        size,
                    )
                }
            }
        }

    actual fun editorSetSeparatorGuides(editor: Long, payload: ByteArray): ByteArray? =
        withActive(editor) {
            adoptBinary { size ->
                payload.usePinned { pinned ->
                    editor_set_separator_guides(
                        editor,
                        pinned.addressOf(0).reinterpret(),
                        payload.size.convert(),
                        size,
                    )
                }
            }
        }

    actual fun editorClearGuides(editor: Long): ByteArray? =
        withActive(editor) { adoptBinary { size -> editor_clear_guides(editor, size) } }

    private inline fun <T> withActive(handle: Long, block: () -> T): T {
        val measurer = measurers[handle]
        if (measurer != null) activeStack.addLast(measurer)
        return try {
            block()
        } finally {
            if (measurer != null) activeStack.removeLast()
        }
    }

    private inline fun callPayload(
        payload: ByteArray?,
        block: (CPointer<UByteVar>?, platform.posix.size_t) -> CPointer<UByteVar>?,
    ): CPointer<UByteVar>? {
        val bytes = payload ?: return block(null, 0u)
        if (bytes.isEmpty()) return block(null, 0u)
        return bytes.usePinned { pinned ->
            block(pinned.addressOf(0).reinterpret(), bytes.size.convert())
        }
    }

    private inline fun callCharPairs(
        openChars: IntArray,
        closeChars: IntArray,
        block: (CPointer<UIntVar>?, CPointer<UIntVar>?, platform.posix.size_t) -> CPointer<UByteVar>?,
    ): CPointer<UByteVar>? {
        val count = minOf(openChars.size, closeChars.size)
        if (count == 0) return block(null, null, 0u)
        return openChars.usePinned { opens ->
            closeChars.usePinned { closes ->
                block(
                    opens.addressOf(0).reinterpret(),
                    closes.addressOf(0).reinterpret(),
                    count.convert(),
                )
            }
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
