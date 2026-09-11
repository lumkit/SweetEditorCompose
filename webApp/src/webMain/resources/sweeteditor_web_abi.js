(function (global) {
  const abi = {
    ready: false,
    error: null,
  };
  global.SweetEditorWebAbi = abi;

  const script = document.currentScript;
  const base = script && script.src ? script.src.replace(/[^/]+$/, "") : "./";

  function readU16Z(mod, ptr) {
    let text = "";
    for (let offset = 0; ; offset += 2) {
      const unit = mod.getValue(ptr + offset, "i16") & 0xffff;
      if (unit === 0) return text;
      text += String.fromCharCode(unit);
    }
  }

  function asU8(bytes) {
    if (!bytes) return new Uint8Array(0);
    if (bytes instanceof Uint8Array) return bytes;
    if (ArrayBuffer.isView(bytes)) {
      return new Uint8Array(bytes.buffer, bytes.byteOffset, bytes.byteLength);
    }
    const len = bytes.length | 0;
    const out = new Uint8Array(len);
    for (let i = 0; i < len; i++) out[i] = bytes[i] & 0xff;
    return out;
  }

  function writeBytes(mod, ptr, bytes) {
    const src = asU8(bytes);
    for (let i = 0; i < src.length; i++) {
      mod.setValue(ptr + i, src[i], "i8");
    }
    return src.length;
  }

  function readBytes(mod, ptr, size) {
    const out = new Uint8Array(size);
    for (let i = 0; i < size; i++) {
      out[i] = mod.getValue(ptr + i, "i8") & 0xff;
    }
    return out;
  }

  function readU32(mod, ptr) {
    return mod.getValue(ptr, "i32") >>> 0;
  }

  function writeU32(mod, ptr, value) {
    mod.setValue(ptr, value >>> 0, "i32");
  }

  function readF32(mod, ptr) {
    return mod.getValue(ptr, "float");
  }

  function allocBytes(mod, bytes) {
    const src = asU8(bytes);
    if (src.length === 0) return { ptr: 0, size: 0 };
    const ptr = mod._malloc(src.length);
    if (!ptr) throw new Error("SweetEditor malloc failed");
    writeBytes(mod, ptr, src);
    return { ptr, size: src.length };
  }

  function allocCString(mod, bytes) {
    const src = asU8(bytes);
    const ptr = mod._malloc(src.length + 1);
    if (!ptr) throw new Error("SweetEditor malloc failed");
    writeBytes(mod, ptr, src);
    mod.setValue(ptr + src.length, 0, "i8");
    return ptr;
  }

  function adoptBinary(mod, ptr, sizePtr) {
    if (!ptr) return null;
    const size = readU32(mod, sizePtr);
    const copy = size > 0 ? readBytes(mod, ptr, size) : new Uint8Array(0);
    mod._free_binary_data(ptr);
    return copy;
  }

  function callBinary(mod, fn) {
    const sizePtr = mod._malloc(4);
    try {
      return adoptBinary(mod, fn(sizePtr), sizePtr);
    } finally {
      mod._free(sizePtr);
    }
  }

  function withBytes(mod, bytes, fn) {
    const alloc = allocBytes(mod, bytes);
    try {
      return fn(alloc.ptr, alloc.size);
    } finally {
      if (alloc.ptr) mod._free(alloc.ptr);
    }
  }

  function withCString(mod, bytes, fn) {
    const ptr = allocCString(mod, bytes || new Uint8Array(0));
    try {
      return fn(ptr);
    } finally {
      mod._free(ptr);
    }
  }

  function readU8String(mod, ptr) {
    if (!ptr) return new Uint8Array(0);
    const text = mod.UTF8ToString(ptr);
    mod._free_u8_string(ptr);
    const encoded = new TextEncoder().encode(text);
    return encoded;
  }

  function install(mod) {
    const editorCallbacks = new Map();

    function bindMeasurer(modRef) {
      const measureText = modRef.addFunction((textPtr, fontStyle) => {
        const fn = global.__seMeasureText;
        return fn ? fn(readU16Z(modRef, textPtr), fontStyle) : 0;
      }, "fii");
      const measureInlay = modRef.addFunction((textPtr) => {
        const fn = global.__seMeasureInlay;
        return fn ? fn(readU16Z(modRef, textPtr)) : 0;
      }, "fi");
      const measureIcon = modRef.addFunction((iconId) => {
        const fn = global.__seMeasureIcon;
        return fn ? fn(iconId) : 0;
      }, "fi");
      const fontMetrics = modRef.addFunction((metricsPtr, length) => {
        if (length >= 1) {
          const ascent = global.__seFontAscent ? global.__seFontAscent() : 0;
          modRef.setValue(metricsPtr, ascent, "float");
        }
        if (length >= 2) {
          const descent = global.__seFontDescent ? global.__seFontDescent() : 0;
          modRef.setValue(metricsPtr + 4, descent, "float");
        }
      }, "vii");
      return [measureText, measureInlay, measureIcon, fontMetrics];
    }

    abi.createDocumentFromUtf8 = (bytes) => withCString(mod, bytes, (ptr) => mod._create_document_from_utf8(ptr));
    abi.freeDocument = (handle) => mod._free_document(handle);
    abi.getDocumentUtf8 = (handle) => readU8String(mod, mod._get_document_utf8(handle));

    abi.createEditor = (options) => {
      const callbacks = bindMeasurer(mod);
      const measurerPtr = mod._malloc(16);
      try {
        callbacks.forEach((cb, index) => mod.setValue(measurerPtr + index * 4, cb, "i32"));
        const handle = withBytes(mod, options, (ptr, size) => mod._create_editor(measurerPtr, ptr, size));
        if (handle) editorCallbacks.set(handle, callbacks);
        else callbacks.forEach((cb) => mod.removeFunction(cb));
        return handle || 0;
      } finally {
        mod._free(measurerPtr);
      }
    };

    abi.freeEditor = (handle) => {
      mod._free_editor(handle);
      const callbacks = editorCallbacks.get(handle);
      if (callbacks) {
        callbacks.forEach((cb) => mod.removeFunction(cb));
        editorCallbacks.delete(handle);
      }
    };

    const bin0 = (fn) => (editor) => callBinary(mod, (sz) => fn(editor, sz));
    const binBytes = (fn) => (editor, bytes) =>
      withBytes(mod, bytes, (ptr, size) => callBinary(mod, (sz) => fn(editor, ptr, size, sz)));
    const binCString = (fn) => (editor, bytes) =>
      withCString(mod, bytes, (ptr) => callBinary(mod, (sz) => fn(editor, ptr, sz)));

    abi.editorSetDocument = (editor, document) =>
      callBinary(mod, (sz) => mod._editor_set_document(editor, document, sz));
    abi.editorSetViewport = (editor, width, height) =>
      callBinary(mod, (sz) => mod._editor_set_viewport(editor, width, height, sz));
    abi.editorOnFontMetricsChanged = bin0(mod._editor_on_font_metrics_changed);
    abi.editorBuildRenderModel = bin0(mod._editor_build_render_model);
    abi.editorHandleGestureEvent = binBytes(mod._editor_handle_gesture_event);
    abi.editorHandleKeyEvent = (editor, keyCode, text, modifiers) => {
      if (!text || text.length === 0) {
        return callBinary(mod, (sz) =>
          mod._editor_handle_key_event(editor, keyCode, 0, modifiers, sz),
        );
      }
      return withCString(mod, text, (ptr) =>
        callBinary(mod, (sz) => mod._editor_handle_key_event(editor, keyCode, ptr, modifiers, sz)),
      );
    };
    abi.editorSetKeyMap = binBytes(mod._editor_set_keymap);
    abi.editorUpdatePointerModifiers = (editor, modifiers) =>
      callBinary(mod, (sz) => mod._editor_update_pointer_modifiers(editor, modifiers, sz));
    abi.editorTickAnimations = bin0(mod._editor_tick_animations);
    abi.editorInsertText = binCString(mod._editor_insert_text);
    abi.editorReplaceText = (editor, startLine, startColumn, endLine, endColumn, text) =>
      withCString(mod, text, (ptr) =>
        callBinary(mod, (sz) =>
          mod._editor_replace_text(editor, startLine, startColumn, endLine, endColumn, ptr, sz),
        ),
      );
    abi.editorApplyTextEdits = binBytes(mod._editor_apply_text_edits);
    abi.editorBackspace = bin0(mod._editor_backspace);
    abi.editorUndo = bin0(mod._editor_undo);
    abi.editorRedo = bin0(mod._editor_redo);
    abi.editorCanUndo = (editor) => mod._editor_can_undo(editor) !== 0;
    abi.editorCanRedo = (editor) => mod._editor_can_redo(editor) !== 0;
    abi.editorSetGutterSticky = (editor, sticky) =>
      callBinary(mod, (sz) => mod._editor_set_gutter_sticky(editor, sticky ? 1 : 0, sz));
    abi.editorSetGutterVisible = (editor, visible) =>
      callBinary(mod, (sz) => mod._editor_set_gutter_visible(editor, visible ? 1 : 0, sz));
    abi.editorSetWrapMode = (editor, mode) =>
      callBinary(mod, (sz) => mod._editor_set_wrap_mode(editor, mode, sz));
    abi.editorSetTabSize = (editor, tabSize) =>
      callBinary(mod, (sz) => mod._editor_set_tab_size(editor, tabSize, sz));
    abi.editorSetInsertSpaces = (editor, enabled) =>
      callBinary(mod, (sz) => mod._editor_set_insert_spaces(editor, enabled ? 1 : 0, sz));

    function setCharPairs(fn, editor, opens, closes) {
      const count = Math.min(opens.length, closes.length);
      if (count === 0) return callBinary(mod, (sz) => fn(editor, 0, 0, 0, sz));
      const openPtr = mod._malloc(count * 4);
      const closePtr = mod._malloc(count * 4);
      try {
        for (let i = 0; i < count; i++) {
          writeU32(mod, openPtr + i * 4, opens[i]);
          writeU32(mod, closePtr + i * 4, closes[i]);
        }
        return callBinary(mod, (sz) => fn(editor, openPtr, closePtr, count, sz));
      } finally {
        mod._free(openPtr);
        mod._free(closePtr);
      }
    }

    abi.editorSetBracketPairs = (editor, opens, closes) =>
      setCharPairs(mod._editor_set_bracket_pairs, editor, opens, closes);
    abi.editorSetAutoClosingPairs = (editor, opens, closes) =>
      setCharPairs(mod._editor_set_auto_closing_pairs, editor, opens, closes);
    abi.editorSetMatchedBrackets = (editor, oL, oC, cL, cC) =>
      callBinary(mod, (sz) => mod._editor_set_matched_brackets(editor, oL, oC, cL, cC, sz));
    abi.editorClearMatchedBrackets = bin0(mod._editor_clear_matched_brackets);
    abi.editorSetDiffChanges = binBytes(mod._editor_set_diff_changes);
    abi.editorComputeDiff = binCString(mod._editor_compute_diff);
    abi.editorSetBatchDiffLineSpans = binBytes(mod._editor_set_batch_diff_line_spans);
    abi.editorClearDiff = bin0(mod._editor_clear_diff);
    abi.editorInsertSnippet = binCString(mod._editor_insert_snippet);
    abi.editorStartLinkedEditing = binBytes(mod._editor_start_linked_editing);
    abi.editorIsInLinkedEditing = (editor) => mod._editor_is_in_linked_editing(editor) !== 0;
    abi.editorLinkedEditingNext = bin0(mod._editor_linked_editing_next);
    abi.editorLinkedEditingPrev = bin0(mod._editor_linked_editing_prev);
    abi.editorCancelLinkedEditing = bin0(mod._editor_cancel_linked_editing);
    abi.editorSetAutoIndentMode = (editor, mode) =>
      callBinary(mod, (sz) => mod._editor_set_auto_indent_mode(editor, mode, sz));
    abi.editorSetBackspaceUnindent = (editor, enabled) =>
      callBinary(mod, (sz) => mod._editor_set_backspace_unindent(editor, enabled ? 1 : 0, sz));
    abi.editorMoveLineUp = bin0(mod._editor_move_line_up);
    abi.editorMoveLineDown = bin0(mod._editor_move_line_down);
    abi.editorCopyLineUp = bin0(mod._editor_copy_line_up);
    abi.editorCopyLineDown = bin0(mod._editor_copy_line_down);
    abi.editorDeleteLine = bin0(mod._editor_delete_line);
    abi.editorInsertLineAbove = bin0(mod._editor_insert_line_above);
    abi.editorInsertLineBelow = bin0(mod._editor_insert_line_below);
    abi.editorSetScale = (editor, scale) =>
      callBinary(mod, (sz) => mod._editor_set_scale(editor, scale, sz));
    abi.editorSetLineSpacing = (editor, add, mult) =>
      callBinary(mod, (sz) => mod._editor_set_line_spacing(editor, add, mult, sz));
    abi.editorSetReadOnly = (editor, readOnly) =>
      callBinary(mod, (sz) => mod._editor_set_read_only(editor, readOnly ? 1 : 0, sz));
    abi.editorSetCurrentLineRenderMode = (editor, mode) =>
      callBinary(mod, (sz) => mod._editor_set_current_line_render_mode(editor, mode, sz));
    abi.editorSetFoldArrowMode = (editor, mode) =>
      callBinary(mod, (sz) => mod._editor_set_fold_arrow_mode(editor, mode, sz));
    abi.editorSetRenderWhitespace = (editor, mode) =>
      callBinary(mod, (sz) => mod._editor_set_render_whitespace(editor, mode, sz));
    abi.editorSetRenderLineBreaks = (editor, enabled) =>
      callBinary(mod, (sz) => mod._editor_set_render_line_breaks(editor, enabled ? 1 : 0, sz));
    abi.editorSetEditorRenderColors = binBytes(mod._editor_set_editor_render_colors);
    abi.editorSetEditorRangeEffectStyles = binBytes(mod._editor_set_editor_range_effect_styles);
    abi.editorSearch = binBytes(mod._editor_search);
    abi.editorFindNextSearchMatch = bin0(mod._editor_find_next_search_match);
    abi.editorFindPreviousSearchMatch = bin0(mod._editor_find_previous_search_match);
    abi.editorReplaceCurrentSearchMatch = binBytes(mod._editor_replace_current_search_match);
    abi.editorReplaceAllSearchMatches = binBytes(mod._editor_replace_all_search_matches);
    abi.editorClearSearch = bin0(mod._editor_clear_search);
    abi.editorGetSearchState = bin0(mod._editor_get_search_state);
    abi.editorImeBeginSession = (editor, mutationModel) =>
      callBinary(mod, (sz) => mod._editor_ime_begin_session(editor, mutationModel, sz));
    abi.editorImeEndSession = (editor, sessionId) =>
      callBinary(mod, (sz) => mod._editor_ime_end_session(editor, BigInt(sessionId), sz));
    abi.editorImeApplyCommands = binBytes(mod._editor_ime_apply_commands);
    abi.editorImeGetState = (editor, sessionId) =>
      callBinary(mod, (sz) => mod._editor_ime_get_state(editor, BigInt(sessionId), sz));
    abi.editorImeGetContext = (editor, sessionId, source, startUtf16, lengthUtf16) =>
      callBinary(mod, (sz) =>
        mod._editor_ime_get_context(
          editor,
          BigInt(sessionId),
          source,
          BigInt(startUtf16),
          BigInt(lengthUtf16),
          sz,
        ),
      );

    abi.editorGetCursorRect = (editor) => {
      const ptr = mod._malloc(12);
      try {
        mod._editor_get_cursor_rect(editor, ptr, ptr + 4, ptr + 8);
        return new Float32Array([readF32(mod, ptr), readF32(mod, ptr + 4), readF32(mod, ptr + 8)]);
      } finally {
        mod._free(ptr);
      }
    };
    abi.editorGetPositionRect = (editor, line, column) => {
      const ptr = mod._malloc(12);
      try {
        mod._editor_get_position_rect(editor, line, column, ptr, ptr + 4, ptr + 8);
        return new Float32Array([readF32(mod, ptr), readF32(mod, ptr + 4), readF32(mod, ptr + 8)]);
      } finally {
        mod._free(ptr);
      }
    };
    abi.editorGetVisibleLineRange = (editor) => {
      const ptr = mod._malloc(8);
      try {
        mod._editor_get_visible_line_range(editor, ptr, ptr + 4);
        return new Int32Array([mod.getValue(ptr, "i32"), mod.getValue(ptr + 4, "i32")]);
      } finally {
        mod._free(ptr);
      }
    };
    abi.editorGetScrollMetrics = bin0(mod._editor_get_scroll_metrics);
    abi.editorGetSelectedText = (editor) => readU8String(mod, mod._editor_get_selected_text(editor));
    abi.editorGetCursorPosition = (editor) => {
      const ptr = mod._malloc(8);
      try {
        mod._editor_get_cursor_position(editor, ptr, ptr + 4);
        return new Int32Array([readU32(mod, ptr), readU32(mod, ptr + 4)]);
      } finally {
        mod._free(ptr);
      }
    };
    abi.editorGetWordRangeAtCursor = (editor) => {
      const ptr = mod._malloc(16);
      try {
        mod._editor_get_word_range_at_cursor(editor, ptr, ptr + 4, ptr + 8, ptr + 12);
        return new Int32Array([
          readU32(mod, ptr),
          readU32(mod, ptr + 4),
          readU32(mod, ptr + 8),
          readU32(mod, ptr + 12),
        ]);
      } finally {
        mod._free(ptr);
      }
    };
    abi.editorGetLinkTargetAt = (editor, line, column) =>
      readU8String(mod, mod._editor_get_link_target_at(editor, line, column));

    const decorationFns = {
      1: (editor, payload) =>
        withBytes(mod, payload, (ptr, size) =>
          callBinary(mod, (sz) => mod._editor_set_line_spans(editor, ptr, size, sz)),
        ),
      2: (editor, payload) =>
        withBytes(mod, payload, (ptr, size) =>
          callBinary(mod, (sz) => mod._editor_set_batch_line_spans(editor, ptr, size, sz)),
        ),
      3: (editor, payload) =>
        withBytes(mod, payload, (ptr, size) =>
          callBinary(mod, (sz) => mod._editor_register_batch_text_styles(editor, ptr, size, sz)),
        ),
      4: (editor, payload) =>
        withBytes(mod, payload, (ptr, size) =>
          callBinary(mod, (sz) => mod._editor_set_line_inlay_hints(editor, ptr, size, sz)),
        ),
      5: (editor, payload) =>
        withBytes(mod, payload, (ptr, size) =>
          callBinary(mod, (sz) => mod._editor_set_batch_line_inlay_hints(editor, ptr, size, sz)),
        ),
      6: (editor, payload) =>
        withBytes(mod, payload, (ptr, size) =>
          callBinary(mod, (sz) => mod._editor_set_line_phantom_texts(editor, ptr, size, sz)),
        ),
      7: (editor, payload) =>
        withBytes(mod, payload, (ptr, size) =>
          callBinary(mod, (sz) => mod._editor_set_batch_line_phantom_texts(editor, ptr, size, sz)),
        ),
      8: (editor, payload) =>
        withBytes(mod, payload, (ptr, size) =>
          callBinary(mod, (sz) => mod._editor_set_line_gutter_icons(editor, ptr, size, sz)),
        ),
      9: (editor, payload) =>
        withBytes(mod, payload, (ptr, size) =>
          callBinary(mod, (sz) => mod._editor_set_batch_line_gutter_icons(editor, ptr, size, sz)),
        ),
      10: (editor, payload) =>
        withBytes(mod, payload, (ptr, size) =>
          callBinary(mod, (sz) => mod._editor_set_line_codelens(editor, ptr, size, sz)),
        ),
      11: (editor, payload) =>
        withBytes(mod, payload, (ptr, size) =>
          callBinary(mod, (sz) => mod._editor_set_batch_line_codelens(editor, ptr, size, sz)),
        ),
      12: (editor, payload) =>
        withBytes(mod, payload, (ptr, size) =>
          callBinary(mod, (sz) => mod._editor_set_line_links(editor, ptr, size, sz)),
        ),
      13: (editor, payload) =>
        withBytes(mod, payload, (ptr, size) =>
          callBinary(mod, (sz) => mod._editor_set_batch_line_links(editor, ptr, size, sz)),
        ),
      14: (editor, payload) =>
        withBytes(mod, payload, (ptr, size) =>
          callBinary(mod, (sz) => mod._editor_set_line_diagnostics(editor, ptr, size, sz)),
        ),
      15: (editor, payload) =>
        withBytes(mod, payload, (ptr, size) =>
          callBinary(mod, (sz) => mod._editor_set_batch_line_diagnostics(editor, ptr, size, sz)),
        ),
      16: (editor, payload) =>
        withBytes(mod, payload, (ptr, size) =>
          callBinary(mod, (sz) => mod._editor_set_line_document_highlights(editor, ptr, size, sz)),
        ),
      17: (editor, payload) =>
        withBytes(mod, payload, (ptr, size) =>
          callBinary(mod, (sz) => mod._editor_set_batch_line_document_highlights(editor, ptr, size, sz)),
        ),
      18: (editor) => callBinary(mod, (sz) => mod._editor_clear_highlights(editor, sz)),
      19: (editor, _payload, a) =>
        callBinary(mod, (sz) => mod._editor_clear_highlights_layer(editor, a, sz)),
      20: (editor, _payload, a, b) =>
        callBinary(mod, (sz) => mod._editor_clear_line_spans(editor, a, b, sz)),
      21: (editor) => callBinary(mod, (sz) => mod._editor_clear_inlay_hints(editor, sz)),
      22: (editor) => callBinary(mod, (sz) => mod._editor_clear_phantom_texts(editor, sz)),
      23: (editor) => callBinary(mod, (sz) => mod._editor_clear_gutter_icons(editor, sz)),
      24: (editor) => callBinary(mod, (sz) => mod._editor_clear_codelens(editor, sz)),
      25: (editor) => callBinary(mod, (sz) => mod._editor_clear_links(editor, sz)),
      26: (editor) => callBinary(mod, (sz) => mod._editor_clear_diagnostics(editor, sz)),
      27: (editor) => callBinary(mod, (sz) => mod._editor_clear_document_highlights(editor, sz)),
      28: (editor) => callBinary(mod, (sz) => mod._editor_clear_all_decorations(editor, sz)),
      29: (editor, _payload, a, b, c, d) =>
        callBinary(mod, (sz) => mod._editor_register_text_style(editor, a >>> 0, b, c, d, sz)),
      30: (editor, _payload, a) =>
        callBinary(mod, (sz) => mod._editor_set_max_gutter_icons(editor, a >>> 0, sz)),
    };

    abi.editorDecorationOp = (editor, op, payload, a, b, c, d) => {
      const fn = decorationFns[op];
      return fn ? fn(editor, payload, a, b, c, d) : null;
    };

    abi.editorSetFoldRegions = binBytes(mod._editor_set_fold_regions);
    abi.editorToggleFold = (editor, line) =>
      callBinary(mod, (sz) => mod._editor_toggle_fold(editor, line, sz));
    abi.editorFoldAt = (editor, line) => callBinary(mod, (sz) => mod._editor_fold_at(editor, line, sz));
    abi.editorUnfoldAt = (editor, line) =>
      callBinary(mod, (sz) => mod._editor_unfold_at(editor, line, sz));
    abi.editorFoldAll = bin0(mod._editor_fold_all);
    abi.editorUnfoldAll = bin0(mod._editor_unfold_all);
    abi.editorIsLineVisible = (editor, line) => mod._editor_is_line_visible(editor, line) !== 0;
    abi.editorSetIndentGuides = binBytes(mod._editor_set_indent_guides);
    abi.editorSetBracketGuides = binBytes(mod._editor_set_bracket_guides);
    abi.editorSetFlowGuides = binBytes(mod._editor_set_flow_guides);
    abi.editorSetSeparatorGuides = binBytes(mod._editor_set_separator_guides);
    abi.editorClearGuides = bin0(mod._editor_clear_guides);
  }

  import(base + "sweeteditor_c_abi.js")
    .then((factory) => factory.default({ locateFile: (path) => base + path }))
    .then((mod) => {
      install(mod);
      abi.ready = true;
    })
    .catch((error) => {
      abi.error = String(error && error.message ? error.message : error);
      console.error("Failed to load SweetEditor C ABI", error);
    });
})(typeof globalThis !== "undefined" ? globalThis : window);
