package io.github.lumkit.sweeteditor.internal.jni

import io.github.lumkit.sweeteditor.core.HostTextMeasurer
import kotlin.js.JsAny

internal actual object NativeBridge {
    actual val isAvailable: Boolean
        get() = abiReady() && abiError() == null

    val loadError: String?
        get() = abiError()

    private val measurers = HashMap<Long, HostTextMeasurer>()
    private var callbacksInstalled = false

    actual fun createDocumentFromUtf8(utf8: ByteArray): Long =
        createDocumentFromUtf8Js(utf8.toJsU8()).toLong()

    actual fun freeDocument(handle: Long) {
        freeDocumentJs(handle.toInt())
    }

    actual fun getDocumentUtf8(handle: Long): ByteArray =
        adoptJsBytes(getDocumentUtf8Js(handle.toInt())) ?: ByteArray(0)

    actual fun getDocumentLineCount(handle: Long): Int =
        getDocumentLineCountJs(handle.toInt())

    actual fun createEditor(measurer: HostTextMeasurer, options: ByteArray): Long {
        installCallbacksIfNeeded()
        val previous = currentMeasurer
        currentMeasurer = measurer
        val handle = try {
            createEditorJs(options.toJsU8()).toLong()
        } finally {
            currentMeasurer = previous
        }
        if (handle != 0L) {
            measurers[handle] = measurer
        }
        return handle
    }

    actual fun freeEditor(handle: Long) {
        freeEditorJs(handle.toInt())
        measurers.remove(handle)
    }

    actual fun editorSetDocument(editor: Long, document: Long): ByteArray? =
        withActive(editor) { adoptJsBytes(editorSetDocumentJs(editor.toInt(), document.toInt())) }

    actual fun editorSetViewport(editor: Long, width: Int, height: Int): ByteArray? =
        withActive(editor) { adoptJsBytes(editorSetViewportJs(editor.toInt(), width, height)) }

    actual fun editorOnFontMetricsChanged(editor: Long): ByteArray? =
        withActive(editor) { adoptJsBytes(editorOnFontMetricsChangedJs(editor.toInt())) }

    actual fun editorBuildRenderModel(editor: Long): ByteArray? =
        withActive(editor) { adoptJsBytes(editorBuildRenderModelJs(editor.toInt())) }

    actual fun editorHandleGestureEvent(editor: Long, payload: ByteArray): ByteArray? =
        withActive(editor) { adoptJsBytes(editorHandleGestureEventJs(editor.toInt(), payload.toJsU8())) }

    actual fun editorHandleKeyEvent(editor: Long, keyCode: Int, text: ByteArray?, modifiers: Int): ByteArray? =
        withActive(editor) {
            adoptJsBytes(editorHandleKeyEventJs(editor.toInt(), keyCode, text?.toJsU8(), modifiers))
        }

    actual fun editorSetKeyMap(editor: Long, payload: ByteArray): ByteArray? =
        withActive(editor) { adoptJsBytes(editorSetKeyMapJs(editor.toInt(), payload.toJsU8())) }

    actual fun editorUpdatePointerModifiers(editor: Long, modifiers: Int): ByteArray? =
        withActive(editor) { adoptJsBytes(editorUpdatePointerModifiersJs(editor.toInt(), modifiers)) }

    actual fun editorTickAnimations(editor: Long): ByteArray? =
        withActive(editor) { adoptJsBytes(editorTickAnimationsJs(editor.toInt())) }

    actual fun editorInsertText(editor: Long, text: ByteArray): ByteArray? =
        withActive(editor) { adoptJsBytes(editorInsertTextJs(editor.toInt(), text.toJsU8())) }

    actual fun editorReplaceText(
        editor: Long,
        startLine: Int,
        startColumn: Int,
        endLine: Int,
        endColumn: Int,
        text: ByteArray,
    ): ByteArray? = withActive(editor) {
        adoptJsBytes(
            editorReplaceTextJs(editor.toInt(), startLine, startColumn, endLine, endColumn, text.toJsU8()),
        )
    }

    actual fun editorApplyTextEdits(editor: Long, payload: ByteArray): ByteArray? =
        withActive(editor) { adoptJsBytes(editorApplyTextEditsJs(editor.toInt(), payload.toJsU8())) }

    actual fun editorDeleteText(
        editor: Long,
        startLine: Int,
        startColumn: Int,
        endLine: Int,
        endColumn: Int,
    ): ByteArray? = withActive(editor) {
        adoptJsBytes(editorDeleteTextJs(editor.toInt(), startLine, startColumn, endLine, endColumn))
    }

    actual fun editorDeleteForward(editor: Long): ByteArray? =
        withActive(editor) { adoptJsBytes(editorDeleteForwardJs(editor.toInt())) }

    actual fun editorBackspace(editor: Long): ByteArray? =
        withActive(editor) { adoptJsBytes(editorBackspaceJs(editor.toInt())) }

    actual fun editorUndo(editor: Long): ByteArray? =
        withActive(editor) { adoptJsBytes(editorUndoJs(editor.toInt())) }

    actual fun editorRedo(editor: Long): ByteArray? =
        withActive(editor) { adoptJsBytes(editorRedoJs(editor.toInt())) }

    actual fun editorCanUndo(editor: Long): Boolean = editorCanUndoJs(editor.toInt())

    actual fun editorCanRedo(editor: Long): Boolean = editorCanRedoJs(editor.toInt())

    actual fun editorSetGutterSticky(editor: Long, sticky: Boolean): ByteArray? =
        withActive(editor) { adoptJsBytes(editorSetGutterStickyJs(editor.toInt(), sticky)) }

    actual fun editorSetGutterVisible(editor: Long, visible: Boolean): ByteArray? =
        withActive(editor) { adoptJsBytes(editorSetGutterVisibleJs(editor.toInt(), visible)) }

    actual fun editorSetWrapMode(editor: Long, mode: Int): ByteArray? =
        withActive(editor) { adoptJsBytes(editorSetWrapModeJs(editor.toInt(), mode)) }

    actual fun editorSetTabSize(editor: Long, tabSize: Int): ByteArray? =
        withActive(editor) { adoptJsBytes(editorSetTabSizeJs(editor.toInt(), tabSize)) }

    actual fun editorSetInsertSpaces(editor: Long, enabled: Boolean): ByteArray? =
        withActive(editor) { adoptJsBytes(editorSetInsertSpacesJs(editor.toInt(), enabled)) }

    actual fun editorSetBracketPairs(editor: Long, openChars: IntArray, closeChars: IntArray): ByteArray? =
        withActive(editor) {
            adoptJsBytes(editorSetBracketPairsJs(editor.toInt(), openChars.toJsU32(), closeChars.toJsU32()))
        }

    actual fun editorSetAutoClosingPairs(editor: Long, openChars: IntArray, closeChars: IntArray): ByteArray? =
        withActive(editor) {
            adoptJsBytes(
                editorSetAutoClosingPairsJs(editor.toInt(), openChars.toJsU32(), closeChars.toJsU32()),
            )
        }

    actual fun editorSetMatchedBrackets(
        editor: Long,
        openLine: Int,
        openColumn: Int,
        closeLine: Int,
        closeColumn: Int,
    ): ByteArray? = withActive(editor) {
        adoptJsBytes(
            editorSetMatchedBracketsJs(editor.toInt(), openLine, openColumn, closeLine, closeColumn),
        )
    }

    actual fun editorClearMatchedBrackets(editor: Long): ByteArray? =
        withActive(editor) { adoptJsBytes(editorClearMatchedBracketsJs(editor.toInt())) }

    actual fun editorSetDiffChanges(editor: Long, payload: ByteArray): ByteArray? =
        withActive(editor) { adoptJsBytes(editorSetDiffChangesJs(editor.toInt(), payload.toJsU8())) }

    actual fun editorComputeDiff(editor: Long, originalUtf8: ByteArray): ByteArray? =
        withActive(editor) { adoptJsBytes(editorComputeDiffJs(editor.toInt(), originalUtf8.toJsU8())) }

    actual fun editorSetBatchDiffLineSpans(editor: Long, payload: ByteArray): ByteArray? =
        withActive(editor) { adoptJsBytes(editorSetBatchDiffLineSpansJs(editor.toInt(), payload.toJsU8())) }

    actual fun editorClearDiff(editor: Long): ByteArray? =
        withActive(editor) { adoptJsBytes(editorClearDiffJs(editor.toInt())) }

    actual fun editorInsertSnippet(editor: Long, snippetUtf8: ByteArray): ByteArray? =
        withActive(editor) { adoptJsBytes(editorInsertSnippetJs(editor.toInt(), snippetUtf8.toJsU8())) }

    actual fun editorStartLinkedEditing(editor: Long, payload: ByteArray): ByteArray? =
        withActive(editor) { adoptJsBytes(editorStartLinkedEditingJs(editor.toInt(), payload.toJsU8())) }

    actual fun editorIsInLinkedEditing(editor: Long): Boolean = editorIsInLinkedEditingJs(editor.toInt())

    actual fun editorLinkedEditingNext(editor: Long): ByteArray? =
        withActive(editor) { adoptJsBytes(editorLinkedEditingNextJs(editor.toInt())) }

    actual fun editorLinkedEditingPrev(editor: Long): ByteArray? =
        withActive(editor) { adoptJsBytes(editorLinkedEditingPrevJs(editor.toInt())) }

    actual fun editorCancelLinkedEditing(editor: Long): ByteArray? =
        withActive(editor) { adoptJsBytes(editorCancelLinkedEditingJs(editor.toInt())) }

    actual fun editorSetAutoIndentMode(editor: Long, mode: Int): ByteArray? =
        withActive(editor) { adoptJsBytes(editorSetAutoIndentModeJs(editor.toInt(), mode)) }

    actual fun editorSetBackspaceUnindent(editor: Long, enabled: Boolean): ByteArray? =
        withActive(editor) { adoptJsBytes(editorSetBackspaceUnindentJs(editor.toInt(), enabled)) }

    actual fun editorMoveLineUp(editor: Long): ByteArray? =
        withActive(editor) { adoptJsBytes(editorMoveLineUpJs(editor.toInt())) }

    actual fun editorMoveLineDown(editor: Long): ByteArray? =
        withActive(editor) { adoptJsBytes(editorMoveLineDownJs(editor.toInt())) }

    actual fun editorCopyLineUp(editor: Long): ByteArray? =
        withActive(editor) { adoptJsBytes(editorCopyLineUpJs(editor.toInt())) }

    actual fun editorCopyLineDown(editor: Long): ByteArray? =
        withActive(editor) { adoptJsBytes(editorCopyLineDownJs(editor.toInt())) }

    actual fun editorDeleteLine(editor: Long): ByteArray? =
        withActive(editor) { adoptJsBytes(editorDeleteLineJs(editor.toInt())) }

    actual fun editorInsertLineAbove(editor: Long): ByteArray? =
        withActive(editor) { adoptJsBytes(editorInsertLineAboveJs(editor.toInt())) }

    actual fun editorInsertLineBelow(editor: Long): ByteArray? =
        withActive(editor) { adoptJsBytes(editorInsertLineBelowJs(editor.toInt())) }

    actual fun editorSetScale(editor: Long, scale: Float): ByteArray? =
        withActive(editor) { adoptJsBytes(editorSetScaleJs(editor.toInt(), scale)) }

    actual fun editorSetLineSpacing(editor: Long, add: Float, mult: Float): ByteArray? =
        withActive(editor) { adoptJsBytes(editorSetLineSpacingJs(editor.toInt(), add, mult)) }

    actual fun editorSetReadOnly(editor: Long, readOnly: Boolean): ByteArray? =
        withActive(editor) { adoptJsBytes(editorSetReadOnlyJs(editor.toInt(), readOnly)) }

    actual fun editorSetCurrentLineRenderMode(editor: Long, mode: Int): ByteArray? =
        withActive(editor) { adoptJsBytes(editorSetCurrentLineRenderModeJs(editor.toInt(), mode)) }

    actual fun editorSetFoldArrowMode(editor: Long, mode: Int): ByteArray? =
        withActive(editor) { adoptJsBytes(editorSetFoldArrowModeJs(editor.toInt(), mode)) }

    actual fun editorSetRenderWhitespace(editor: Long, mode: Int): ByteArray? =
        withActive(editor) { adoptJsBytes(editorSetRenderWhitespaceJs(editor.toInt(), mode)) }

    actual fun editorSetRenderLineBreaks(editor: Long, enabled: Boolean): ByteArray? =
        withActive(editor) { adoptJsBytes(editorSetRenderLineBreaksJs(editor.toInt(), enabled)) }

    actual fun editorSetEditorRenderColors(editor: Long, payload: ByteArray): ByteArray? =
        withActive(editor) { adoptJsBytes(editorSetEditorRenderColorsJs(editor.toInt(), payload.toJsU8())) }

    actual fun editorSetEditorRangeEffectStyles(editor: Long, payload: ByteArray): ByteArray? =
        withActive(editor) {
            adoptJsBytes(editorSetEditorRangeEffectStylesJs(editor.toInt(), payload.toJsU8()))
        }

    actual fun editorSearch(editor: Long, payload: ByteArray): ByteArray? =
        withActive(editor) { adoptJsBytes(editorSearchJs(editor.toInt(), payload.toJsU8())) }

    actual fun editorFindNextSearchMatch(editor: Long): ByteArray? =
        withActive(editor) { adoptJsBytes(editorFindNextSearchMatchJs(editor.toInt())) }

    actual fun editorFindPreviousSearchMatch(editor: Long): ByteArray? =
        withActive(editor) { adoptJsBytes(editorFindPreviousSearchMatchJs(editor.toInt())) }

    actual fun editorReplaceCurrentSearchMatch(editor: Long, payload: ByteArray): ByteArray? =
        withActive(editor) { adoptJsBytes(editorReplaceCurrentSearchMatchJs(editor.toInt(), payload.toJsU8())) }

    actual fun editorReplaceAllSearchMatches(editor: Long, payload: ByteArray): ByteArray? =
        withActive(editor) { adoptJsBytes(editorReplaceAllSearchMatchesJs(editor.toInt(), payload.toJsU8())) }

    actual fun editorClearSearch(editor: Long): ByteArray? =
        withActive(editor) { adoptJsBytes(editorClearSearchJs(editor.toInt())) }

    actual fun editorGetSearchState(editor: Long): ByteArray? =
        withActive(editor) { adoptJsBytes(editorGetSearchStateJs(editor.toInt())) }

    actual fun editorImeBeginSession(editor: Long, mutationModel: Int): ByteArray? =
        withActive(editor) { adoptJsBytes(editorImeBeginSessionJs(editor.toInt(), mutationModel)) }

    actual fun editorImeEndSession(editor: Long, sessionId: Long): ByteArray? =
        withActive(editor) { adoptJsBytes(editorImeEndSessionJs(editor.toInt(), sessionId.toDouble())) }

    actual fun editorImeApplyCommands(editor: Long, payload: ByteArray): ByteArray? =
        withActive(editor) { adoptJsBytes(editorImeApplyCommandsJs(editor.toInt(), payload.toJsU8())) }

    actual fun editorImeGetState(editor: Long, sessionId: Long): ByteArray? =
        withActive(editor) { adoptJsBytes(editorImeGetStateJs(editor.toInt(), sessionId.toDouble())) }

    actual fun editorImeGetContext(
        editor: Long,
        sessionId: Long,
        source: Int,
        startUtf16: Long,
        lengthUtf16: Long,
    ): ByteArray? = withActive(editor) {
        adoptJsBytes(
            editorImeGetContextJs(
                editor.toInt(),
                sessionId.toDouble(),
                source,
                startUtf16.toDouble(),
                lengthUtf16.toDouble(),
            ),
        )
    }

    actual fun editorGetCursorRect(editor: Long): FloatArray =
        withActive(editor) { adoptJsFloats(editorGetCursorRectJs(editor.toInt())) }

    actual fun editorGetPositionRect(editor: Long, line: Int, column: Int): FloatArray =
        withActive(editor) { adoptJsFloats(editorGetPositionRectJs(editor.toInt(), line, column)) }

    actual fun editorGetVisibleLineRange(editor: Long): IntArray =
        withActive(editor) { adoptJsInts(editorGetVisibleLineRangeJs(editor.toInt())) }

    actual fun editorGetScrollMetrics(editor: Long): ByteArray? =
        withActive(editor) { adoptJsBytes(editorGetScrollMetricsJs(editor.toInt())) }

    actual fun editorGetSelectedText(editor: Long): ByteArray =
        withActive(editor) { adoptJsBytes(editorGetSelectedTextJs(editor.toInt())) ?: ByteArray(0) }

    actual fun editorGetCursorPosition(editor: Long): IntArray =
        withActive(editor) { adoptJsInts(editorGetCursorPositionJs(editor.toInt())) }

    actual fun editorSetCursorPosition(editor: Long, line: Int, column: Int): ByteArray? =
        withActive(editor) { adoptJsBytes(editorSetCursorPositionJs(editor.toInt(), line, column)) }

    actual fun editorSelectAll(editor: Long): ByteArray? =
        withActive(editor) { adoptJsBytes(editorSelectAllJs(editor.toInt())) }

    actual fun editorSetSelection(
        editor: Long,
        startLine: Int,
        startColumn: Int,
        endLine: Int,
        endColumn: Int,
    ): ByteArray? = withActive(editor) {
        adoptJsBytes(editorSetSelectionJs(editor.toInt(), startLine, startColumn, endLine, endColumn))
    }

    actual fun editorGetSelection(editor: Long): IntArray =
        withActive(editor) { adoptJsInts(editorGetSelectionJs(editor.toInt())) }

    actual fun editorGetWordRangeAtCursor(editor: Long): IntArray =
        withActive(editor) { adoptJsInts(editorGetWordRangeAtCursorJs(editor.toInt())) }

    actual fun editorGetWordAtCursor(editor: Long): ByteArray =
        withActive(editor) { adoptJsBytes(editorGetWordAtCursorJs(editor.toInt())) ?: ByteArray(0) }

    actual fun editorScrollToLine(editor: Long, line: Int, behavior: Int): ByteArray? =
        withActive(editor) { adoptJsBytes(editorScrollToLineJs(editor.toInt(), line, behavior)) }

    actual fun editorGotoPosition(editor: Long, line: Int, column: Int): ByteArray? =
        withActive(editor) { adoptJsBytes(editorGotoPositionJs(editor.toInt(), line, column)) }

    actual fun editorEnsureCursorVisible(editor: Long): ByteArray? =
        withActive(editor) { adoptJsBytes(editorEnsureCursorVisibleJs(editor.toInt())) }

    actual fun editorSetScroll(editor: Long, scrollX: Float, scrollY: Float): ByteArray? =
        withActive(editor) { adoptJsBytes(editorSetScrollJs(editor.toInt(), scrollX, scrollY)) }

    actual fun editorDecorationOp(
        editor: Long,
        op: Int,
        payload: ByteArray?,
        a: Int,
        b: Int,
        c: Int,
        d: Int,
    ): ByteArray? = withActive(editor) {
        adoptJsBytes(editorDecorationOpJs(editor.toInt(), op, payload?.toJsU8(), a, b, c, d))
    }

    actual fun editorGetLinkTargetAt(editor: Long, line: Int, column: Int): ByteArray =
        withActive(editor) { adoptJsBytes(editorGetLinkTargetAtJs(editor.toInt(), line, column)) ?: ByteArray(0) }

    actual fun editorSetFoldRegions(editor: Long, payload: ByteArray): ByteArray? =
        withActive(editor) { adoptJsBytes(editorSetFoldRegionsJs(editor.toInt(), payload.toJsU8())) }

    actual fun editorToggleFold(editor: Long, line: Int): ByteArray? =
        withActive(editor) { adoptJsBytes(editorToggleFoldJs(editor.toInt(), line)) }

    actual fun editorFoldAt(editor: Long, line: Int): ByteArray? =
        withActive(editor) { adoptJsBytes(editorFoldAtJs(editor.toInt(), line)) }

    actual fun editorUnfoldAt(editor: Long, line: Int): ByteArray? =
        withActive(editor) { adoptJsBytes(editorUnfoldAtJs(editor.toInt(), line)) }

    actual fun editorFoldAll(editor: Long): ByteArray? =
        withActive(editor) { adoptJsBytes(editorFoldAllJs(editor.toInt())) }

    actual fun editorUnfoldAll(editor: Long): ByteArray? =
        withActive(editor) { adoptJsBytes(editorUnfoldAllJs(editor.toInt())) }

    actual fun editorIsLineVisible(editor: Long, line: Int): Boolean = editorIsLineVisibleJs(editor.toInt(), line)

    actual fun editorSetIndentGuides(editor: Long, payload: ByteArray): ByteArray? =
        withActive(editor) { adoptJsBytes(editorSetIndentGuidesJs(editor.toInt(), payload.toJsU8())) }

    actual fun editorSetBracketGuides(editor: Long, payload: ByteArray): ByteArray? =
        withActive(editor) { adoptJsBytes(editorSetBracketGuidesJs(editor.toInt(), payload.toJsU8())) }

    actual fun editorSetFlowGuides(editor: Long, payload: ByteArray): ByteArray? =
        withActive(editor) { adoptJsBytes(editorSetFlowGuidesJs(editor.toInt(), payload.toJsU8())) }

    actual fun editorSetSeparatorGuides(editor: Long, payload: ByteArray): ByteArray? =
        withActive(editor) { adoptJsBytes(editorSetSeparatorGuidesJs(editor.toInt(), payload.toJsU8())) }

    actual fun editorClearGuides(editor: Long): ByteArray? =
        withActive(editor) { adoptJsBytes(editorClearGuidesJs(editor.toInt())) }

    private fun installCallbacksIfNeeded() {
        if (callbacksInstalled) return
        callbacksInstalled = true
        val measureText = { text: String, style: Int ->
            currentMeasurer?.measureTextWidth(text, style) ?: 0f
        }
        val measureInlay = { text: String ->
            currentMeasurer?.measureInlayHintWidth(text) ?: 0f
        }
        val measureIcon = { iconId: Int ->
            currentMeasurer?.measureIconWidth(iconId) ?: 0f
        }
        val ascent = { currentMeasurer?.fontAscent() ?: 0f }
        val descent = { currentMeasurer?.fontDescent() ?: 0f }
        installMeasurerCallbacksJs(measureText, measureInlay, measureIcon, ascent, descent)
    }

    private inline fun <T> withActive(handle: Long, block: () -> T): T {
        val previous = currentMeasurer
        currentMeasurer = measurers[handle] ?: previous
        return try {
            block()
        } finally {
            currentMeasurer = previous
        }
    }
}

private var currentMeasurer: HostTextMeasurer? = null

private fun ByteArray.toJsU8(): JsAny {
    val arr = newUint8Array(size)
    for (index in indices) {
        uint8Set(arr, index, this[index].toInt() and 0xFF)
    }
    return arr
}

private fun IntArray.toJsU32(): JsAny {
    val arr = newUint32Array(size)
    for (index in indices) {
        uint32Set(arr, index, this[index])
    }
    return arr
}

private fun adoptJsBytes(value: JsAny?): ByteArray? {
    if (value == null) return null
    val length = u8Length(value)
    return ByteArray(length) { index -> u8Get(value, index).toByte() }
}

private fun adoptJsFloats(value: JsAny): FloatArray {
    val length = typedLength(value)
    return FloatArray(length) { index -> floatGet(value, index) }
}

private fun adoptJsInts(value: JsAny): IntArray {
    val length = typedLength(value)
    return IntArray(length) { index -> intGet(value, index) }
}

private fun abiReady(): Boolean = js("!!(globalThis.SweetEditorWebAbi && globalThis.SweetEditorWebAbi.ready)")

private fun abiError(): String? = js("globalThis.SweetEditorWebAbi && globalThis.SweetEditorWebAbi.error || null")

private fun newUint8Array(length: Int): JsAny = js("new Uint8Array(length)")

private fun newUint32Array(length: Int): JsAny = js("new Uint32Array(length)")

private fun uint8Set(arr: JsAny, index: Int, value: Int): Int = js("arr[index] = value")

private fun uint32Set(arr: JsAny, index: Int, value: Int): Int = js("arr[index] = value")

private fun u8Length(arr: JsAny): Int = js("arr.length")

private fun u8Get(arr: JsAny, index: Int): Int = js("arr[index]")

private fun typedLength(arr: JsAny): Int = js("arr.length")

private fun floatGet(arr: JsAny, index: Int): Float = js("arr[index]")

private fun intGet(arr: JsAny, index: Int): Int = js("arr[index]")

private fun installMeasurerCallbacksJs(
    measureText: (String, Int) -> Float,
    measureInlay: (String) -> Float,
    measureIcon: (Int) -> Float,
    ascent: () -> Float,
    descent: () -> Float,
): Int = js(
    "(globalThis.__seMeasureText = measureText, globalThis.__seMeasureInlay = measureInlay, globalThis.__seMeasureIcon = measureIcon, globalThis.__seFontAscent = ascent, globalThis.__seFontDescent = descent, 0)",
)

private fun createDocumentFromUtf8Js(bytes: JsAny): Int =
    js("globalThis.SweetEditorWebAbi.createDocumentFromUtf8(bytes)")

private fun freeDocumentJs(handle: Int): Int =
    js("(globalThis.SweetEditorWebAbi.freeDocument(handle), 0)")

private fun getDocumentUtf8Js(handle: Int): JsAny? =
    js("globalThis.SweetEditorWebAbi.getDocumentUtf8(handle)")

private fun getDocumentLineCountJs(handle: Int): Int =
    js("globalThis.SweetEditorWebAbi.getDocumentLineCount(handle)")

private fun createEditorJs(options: JsAny): Int =
    js("globalThis.SweetEditorWebAbi.createEditor(options)")

private fun freeEditorJs(handle: Int): Int =
    js("(globalThis.SweetEditorWebAbi.freeEditor(handle), 0)")

private fun editorSetDocumentJs(editor: Int, document: Int): JsAny? =
    js("globalThis.SweetEditorWebAbi.editorSetDocument(editor, document)")

private fun editorSetViewportJs(editor: Int, width: Int, height: Int): JsAny? =
    js("globalThis.SweetEditorWebAbi.editorSetViewport(editor, width, height)")

private fun editorOnFontMetricsChangedJs(editor: Int): JsAny? =
    js("globalThis.SweetEditorWebAbi.editorOnFontMetricsChanged(editor)")

private fun editorBuildRenderModelJs(editor: Int): JsAny? =
    js("globalThis.SweetEditorWebAbi.editorBuildRenderModel(editor)")

private fun editorHandleGestureEventJs(editor: Int, payload: JsAny): JsAny? =
    js("globalThis.SweetEditorWebAbi.editorHandleGestureEvent(editor, payload)")

private fun editorHandleKeyEventJs(editor: Int, keyCode: Int, text: JsAny?, modifiers: Int): JsAny? =
    js("globalThis.SweetEditorWebAbi.editorHandleKeyEvent(editor, keyCode, text, modifiers)")

private fun editorSetKeyMapJs(editor: Int, payload: JsAny): JsAny? =
    js("globalThis.SweetEditorWebAbi.editorSetKeyMap(editor, payload)")

private fun editorUpdatePointerModifiersJs(editor: Int, modifiers: Int): JsAny? =
    js("globalThis.SweetEditorWebAbi.editorUpdatePointerModifiers(editor, modifiers)")

private fun editorTickAnimationsJs(editor: Int): JsAny? =
    js("globalThis.SweetEditorWebAbi.editorTickAnimations(editor)")

private fun editorInsertTextJs(editor: Int, text: JsAny): JsAny? =
    js("globalThis.SweetEditorWebAbi.editorInsertText(editor, text)")

private fun editorReplaceTextJs(
    editor: Int,
    startLine: Int,
    startColumn: Int,
    endLine: Int,
    endColumn: Int,
    text: JsAny,
): JsAny? =
    js("globalThis.SweetEditorWebAbi.editorReplaceText(editor, startLine, startColumn, endLine, endColumn, text)")

private fun editorApplyTextEditsJs(editor: Int, payload: JsAny): JsAny? =
    js("globalThis.SweetEditorWebAbi.editorApplyTextEdits(editor, payload)")

private fun editorDeleteTextJs(
    editor: Int,
    startLine: Int,
    startColumn: Int,
    endLine: Int,
    endColumn: Int,
): JsAny? =
    js("globalThis.SweetEditorWebAbi.editorDeleteText(editor, startLine, startColumn, endLine, endColumn)")

private fun editorDeleteForwardJs(editor: Int): JsAny? =
    js("globalThis.SweetEditorWebAbi.editorDeleteForward(editor)")

private fun editorBackspaceJs(editor: Int): JsAny? =
    js("globalThis.SweetEditorWebAbi.editorBackspace(editor)")

private fun editorUndoJs(editor: Int): JsAny? =
    js("globalThis.SweetEditorWebAbi.editorUndo(editor)")

private fun editorRedoJs(editor: Int): JsAny? =
    js("globalThis.SweetEditorWebAbi.editorRedo(editor)")

private fun editorCanUndoJs(editor: Int): Boolean =
    js("globalThis.SweetEditorWebAbi.editorCanUndo(editor)")

private fun editorCanRedoJs(editor: Int): Boolean =
    js("globalThis.SweetEditorWebAbi.editorCanRedo(editor)")

private fun editorSetGutterStickyJs(editor: Int, sticky: Boolean): JsAny? =
    js("globalThis.SweetEditorWebAbi.editorSetGutterSticky(editor, sticky)")

private fun editorSetGutterVisibleJs(editor: Int, visible: Boolean): JsAny? =
    js("globalThis.SweetEditorWebAbi.editorSetGutterVisible(editor, visible)")

private fun editorSetWrapModeJs(editor: Int, mode: Int): JsAny? =
    js("globalThis.SweetEditorWebAbi.editorSetWrapMode(editor, mode)")

private fun editorSetTabSizeJs(editor: Int, tabSize: Int): JsAny? =
    js("globalThis.SweetEditorWebAbi.editorSetTabSize(editor, tabSize)")

private fun editorSetInsertSpacesJs(editor: Int, enabled: Boolean): JsAny? =
    js("globalThis.SweetEditorWebAbi.editorSetInsertSpaces(editor, enabled)")

private fun editorSetBracketPairsJs(editor: Int, opens: JsAny, closes: JsAny): JsAny? =
    js("globalThis.SweetEditorWebAbi.editorSetBracketPairs(editor, opens, closes)")

private fun editorSetAutoClosingPairsJs(editor: Int, opens: JsAny, closes: JsAny): JsAny? =
    js("globalThis.SweetEditorWebAbi.editorSetAutoClosingPairs(editor, opens, closes)")

private fun editorSetMatchedBracketsJs(editor: Int, oL: Int, oC: Int, cL: Int, cC: Int): JsAny? =
    js("globalThis.SweetEditorWebAbi.editorSetMatchedBrackets(editor, oL, oC, cL, cC)")

private fun editorClearMatchedBracketsJs(editor: Int): JsAny? =
    js("globalThis.SweetEditorWebAbi.editorClearMatchedBrackets(editor)")

private fun editorSetDiffChangesJs(editor: Int, payload: JsAny): JsAny? =
    js("globalThis.SweetEditorWebAbi.editorSetDiffChanges(editor, payload)")

private fun editorComputeDiffJs(editor: Int, originalUtf8: JsAny): JsAny? =
    js("globalThis.SweetEditorWebAbi.editorComputeDiff(editor, originalUtf8)")

private fun editorSetBatchDiffLineSpansJs(editor: Int, payload: JsAny): JsAny? =
    js("globalThis.SweetEditorWebAbi.editorSetBatchDiffLineSpans(editor, payload)")

private fun editorClearDiffJs(editor: Int): JsAny? =
    js("globalThis.SweetEditorWebAbi.editorClearDiff(editor)")

private fun editorInsertSnippetJs(editor: Int, snippetUtf8: JsAny): JsAny? =
    js("globalThis.SweetEditorWebAbi.editorInsertSnippet(editor, snippetUtf8)")

private fun editorStartLinkedEditingJs(editor: Int, payload: JsAny): JsAny? =
    js("globalThis.SweetEditorWebAbi.editorStartLinkedEditing(editor, payload)")

private fun editorIsInLinkedEditingJs(editor: Int): Boolean =
    js("globalThis.SweetEditorWebAbi.editorIsInLinkedEditing(editor)")

private fun editorLinkedEditingNextJs(editor: Int): JsAny? =
    js("globalThis.SweetEditorWebAbi.editorLinkedEditingNext(editor)")

private fun editorLinkedEditingPrevJs(editor: Int): JsAny? =
    js("globalThis.SweetEditorWebAbi.editorLinkedEditingPrev(editor)")

private fun editorCancelLinkedEditingJs(editor: Int): JsAny? =
    js("globalThis.SweetEditorWebAbi.editorCancelLinkedEditing(editor)")

private fun editorSetAutoIndentModeJs(editor: Int, mode: Int): JsAny? =
    js("globalThis.SweetEditorWebAbi.editorSetAutoIndentMode(editor, mode)")

private fun editorSetBackspaceUnindentJs(editor: Int, enabled: Boolean): JsAny? =
    js("globalThis.SweetEditorWebAbi.editorSetBackspaceUnindent(editor, enabled)")

private fun editorMoveLineUpJs(editor: Int): JsAny? =
    js("globalThis.SweetEditorWebAbi.editorMoveLineUp(editor)")

private fun editorMoveLineDownJs(editor: Int): JsAny? =
    js("globalThis.SweetEditorWebAbi.editorMoveLineDown(editor)")

private fun editorCopyLineUpJs(editor: Int): JsAny? =
    js("globalThis.SweetEditorWebAbi.editorCopyLineUp(editor)")

private fun editorCopyLineDownJs(editor: Int): JsAny? =
    js("globalThis.SweetEditorWebAbi.editorCopyLineDown(editor)")

private fun editorDeleteLineJs(editor: Int): JsAny? =
    js("globalThis.SweetEditorWebAbi.editorDeleteLine(editor)")

private fun editorInsertLineAboveJs(editor: Int): JsAny? =
    js("globalThis.SweetEditorWebAbi.editorInsertLineAbove(editor)")

private fun editorInsertLineBelowJs(editor: Int): JsAny? =
    js("globalThis.SweetEditorWebAbi.editorInsertLineBelow(editor)")

private fun editorSetScaleJs(editor: Int, scale: Float): JsAny? =
    js("globalThis.SweetEditorWebAbi.editorSetScale(editor, scale)")

private fun editorSetLineSpacingJs(editor: Int, add: Float, mult: Float): JsAny? =
    js("globalThis.SweetEditorWebAbi.editorSetLineSpacing(editor, add, mult)")

private fun editorSetReadOnlyJs(editor: Int, readOnly: Boolean): JsAny? =
    js("globalThis.SweetEditorWebAbi.editorSetReadOnly(editor, readOnly)")

private fun editorSetCurrentLineRenderModeJs(editor: Int, mode: Int): JsAny? =
    js("globalThis.SweetEditorWebAbi.editorSetCurrentLineRenderMode(editor, mode)")

private fun editorSetFoldArrowModeJs(editor: Int, mode: Int): JsAny? =
    js("globalThis.SweetEditorWebAbi.editorSetFoldArrowMode(editor, mode)")

private fun editorSetRenderWhitespaceJs(editor: Int, mode: Int): JsAny? =
    js("globalThis.SweetEditorWebAbi.editorSetRenderWhitespace(editor, mode)")

private fun editorSetRenderLineBreaksJs(editor: Int, enabled: Boolean): JsAny? =
    js("globalThis.SweetEditorWebAbi.editorSetRenderLineBreaks(editor, enabled)")

private fun editorSetEditorRenderColorsJs(editor: Int, payload: JsAny): JsAny? =
    js("globalThis.SweetEditorWebAbi.editorSetEditorRenderColors(editor, payload)")

private fun editorSetEditorRangeEffectStylesJs(editor: Int, payload: JsAny): JsAny? =
    js("globalThis.SweetEditorWebAbi.editorSetEditorRangeEffectStyles(editor, payload)")

private fun editorSearchJs(editor: Int, payload: JsAny): JsAny? =
    js("globalThis.SweetEditorWebAbi.editorSearch(editor, payload)")

private fun editorFindNextSearchMatchJs(editor: Int): JsAny? =
    js("globalThis.SweetEditorWebAbi.editorFindNextSearchMatch(editor)")

private fun editorFindPreviousSearchMatchJs(editor: Int): JsAny? =
    js("globalThis.SweetEditorWebAbi.editorFindPreviousSearchMatch(editor)")

private fun editorReplaceCurrentSearchMatchJs(editor: Int, payload: JsAny): JsAny? =
    js("globalThis.SweetEditorWebAbi.editorReplaceCurrentSearchMatch(editor, payload)")

private fun editorReplaceAllSearchMatchesJs(editor: Int, payload: JsAny): JsAny? =
    js("globalThis.SweetEditorWebAbi.editorReplaceAllSearchMatches(editor, payload)")

private fun editorClearSearchJs(editor: Int): JsAny? =
    js("globalThis.SweetEditorWebAbi.editorClearSearch(editor)")

private fun editorGetSearchStateJs(editor: Int): JsAny? =
    js("globalThis.SweetEditorWebAbi.editorGetSearchState(editor)")

private fun editorImeBeginSessionJs(editor: Int, mutationModel: Int): JsAny? =
    js("globalThis.SweetEditorWebAbi.editorImeBeginSession(editor, mutationModel)")

private fun editorImeEndSessionJs(editor: Int, sessionId: Double): JsAny? =
    js("globalThis.SweetEditorWebAbi.editorImeEndSession(editor, sessionId)")

private fun editorImeApplyCommandsJs(editor: Int, payload: JsAny): JsAny? =
    js("globalThis.SweetEditorWebAbi.editorImeApplyCommands(editor, payload)")

private fun editorImeGetStateJs(editor: Int, sessionId: Double): JsAny? =
    js("globalThis.SweetEditorWebAbi.editorImeGetState(editor, sessionId)")

private fun editorImeGetContextJs(
    editor: Int,
    sessionId: Double,
    source: Int,
    startUtf16: Double,
    lengthUtf16: Double,
): JsAny? =
    js("globalThis.SweetEditorWebAbi.editorImeGetContext(editor, sessionId, source, startUtf16, lengthUtf16)")

private fun editorGetCursorRectJs(editor: Int): JsAny =
    js("globalThis.SweetEditorWebAbi.editorGetCursorRect(editor)")

private fun editorGetPositionRectJs(editor: Int, line: Int, column: Int): JsAny =
    js("globalThis.SweetEditorWebAbi.editorGetPositionRect(editor, line, column)")

private fun editorGetVisibleLineRangeJs(editor: Int): JsAny =
    js("globalThis.SweetEditorWebAbi.editorGetVisibleLineRange(editor)")

private fun editorGetScrollMetricsJs(editor: Int): JsAny? =
    js("globalThis.SweetEditorWebAbi.editorGetScrollMetrics(editor)")

private fun editorGetSelectedTextJs(editor: Int): JsAny? =
    js("globalThis.SweetEditorWebAbi.editorGetSelectedText(editor)")

private fun editorGetCursorPositionJs(editor: Int): JsAny =
    js("globalThis.SweetEditorWebAbi.editorGetCursorPosition(editor)")

private fun editorGetWordRangeAtCursorJs(editor: Int): JsAny =
    js("globalThis.SweetEditorWebAbi.editorGetWordRangeAtCursor(editor)")

private fun editorSetCursorPositionJs(editor: Int, line: Int, column: Int): JsAny? =
    js("globalThis.SweetEditorWebAbi.editorSetCursorPosition(editor, line, column)")

private fun editorSelectAllJs(editor: Int): JsAny? =
    js("globalThis.SweetEditorWebAbi.editorSelectAll(editor)")

private fun editorSetSelectionJs(
    editor: Int,
    startLine: Int,
    startColumn: Int,
    endLine: Int,
    endColumn: Int,
): JsAny? =
    js("globalThis.SweetEditorWebAbi.editorSetSelection(editor, startLine, startColumn, endLine, endColumn)")

private fun editorGetSelectionJs(editor: Int): JsAny =
    js("globalThis.SweetEditorWebAbi.editorGetSelection(editor)")

private fun editorGetWordAtCursorJs(editor: Int): JsAny? =
    js("globalThis.SweetEditorWebAbi.editorGetWordAtCursor(editor)")

private fun editorScrollToLineJs(editor: Int, line: Int, behavior: Int): JsAny? =
    js("globalThis.SweetEditorWebAbi.editorScrollToLine(editor, line, behavior)")

private fun editorGotoPositionJs(editor: Int, line: Int, column: Int): JsAny? =
    js("globalThis.SweetEditorWebAbi.editorGotoPosition(editor, line, column)")

private fun editorEnsureCursorVisibleJs(editor: Int): JsAny? =
    js("globalThis.SweetEditorWebAbi.editorEnsureCursorVisible(editor)")

private fun editorSetScrollJs(editor: Int, scrollX: Float, scrollY: Float): JsAny? =
    js("globalThis.SweetEditorWebAbi.editorSetScroll(editor, scrollX, scrollY)")

private fun editorDecorationOpJs(
    editor: Int,
    op: Int,
    payload: JsAny?,
    a: Int,
    b: Int,
    c: Int,
    d: Int,
): JsAny? =
    js("globalThis.SweetEditorWebAbi.editorDecorationOp(editor, op, payload, a, b, c, d)")

private fun editorGetLinkTargetAtJs(editor: Int, line: Int, column: Int): JsAny? =
    js("globalThis.SweetEditorWebAbi.editorGetLinkTargetAt(editor, line, column)")

private fun editorSetFoldRegionsJs(editor: Int, payload: JsAny): JsAny? =
    js("globalThis.SweetEditorWebAbi.editorSetFoldRegions(editor, payload)")

private fun editorToggleFoldJs(editor: Int, line: Int): JsAny? =
    js("globalThis.SweetEditorWebAbi.editorToggleFold(editor, line)")

private fun editorFoldAtJs(editor: Int, line: Int): JsAny? =
    js("globalThis.SweetEditorWebAbi.editorFoldAt(editor, line)")

private fun editorUnfoldAtJs(editor: Int, line: Int): JsAny? =
    js("globalThis.SweetEditorWebAbi.editorUnfoldAt(editor, line)")

private fun editorFoldAllJs(editor: Int): JsAny? =
    js("globalThis.SweetEditorWebAbi.editorFoldAll(editor)")

private fun editorUnfoldAllJs(editor: Int): JsAny? =
    js("globalThis.SweetEditorWebAbi.editorUnfoldAll(editor)")

private fun editorIsLineVisibleJs(editor: Int, line: Int): Boolean =
    js("globalThis.SweetEditorWebAbi.editorIsLineVisible(editor, line)")

private fun editorSetIndentGuidesJs(editor: Int, payload: JsAny): JsAny? =
    js("globalThis.SweetEditorWebAbi.editorSetIndentGuides(editor, payload)")

private fun editorSetBracketGuidesJs(editor: Int, payload: JsAny): JsAny? =
    js("globalThis.SweetEditorWebAbi.editorSetBracketGuides(editor, payload)")

private fun editorSetFlowGuidesJs(editor: Int, payload: JsAny): JsAny? =
    js("globalThis.SweetEditorWebAbi.editorSetFlowGuides(editor, payload)")

private fun editorSetSeparatorGuidesJs(editor: Int, payload: JsAny): JsAny? =
    js("globalThis.SweetEditorWebAbi.editorSetSeparatorGuides(editor, payload)")

private fun editorClearGuidesJs(editor: Int): JsAny? =
    js("globalThis.SweetEditorWebAbi.editorClearGuides(editor)")
