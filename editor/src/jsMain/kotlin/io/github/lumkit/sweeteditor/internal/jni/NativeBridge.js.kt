package io.github.lumkit.sweeteditor.internal.jni

import io.github.lumkit.sweeteditor.core.HostTextMeasurer

internal actual object NativeBridge {
    actual val isAvailable: Boolean
        get() = abi()?.ready == true

    val loadError: String?
        get() {
            val message = abi()?.error
            return if (message == null) null else message.toString()
        }

    private val measurers = HashMap<Long, HostTextMeasurer>()
    private var callbacksInstalled = false

    actual fun createDocumentFromUtf8(utf8: ByteArray): Long =
        (abi().createDocumentFromUtf8(utf8.toTyped()) as Number).toLong()

    actual fun freeDocument(handle: Long) {
        abi().freeDocument(handle.toInt())
    }

    actual fun getDocumentUtf8(handle: Long): ByteArray =
        toBytes(abi().getDocumentUtf8(handle.toInt())) ?: ByteArray(0)

    actual fun createEditor(measurer: HostTextMeasurer, options: ByteArray): Long {
        installCallbacksIfNeeded()
        val previous = currentMeasurer
        currentMeasurer = measurer
        val handle = try {
            (abi().createEditor(options.toTyped()) as Number).toLong()
        } finally {
            currentMeasurer = previous
        }
        if (handle != 0L) measurers[handle] = measurer
        return handle
    }

    actual fun freeEditor(handle: Long) {
        abi().freeEditor(handle.toInt())
        measurers.remove(handle)
    }

    actual fun editorSetDocument(editor: Long, document: Long): ByteArray? =
        withActive(editor) { toBytes(abi().editorSetDocument(editor.toInt(), document.toInt())) }

    actual fun editorSetViewport(editor: Long, width: Int, height: Int): ByteArray? =
        withActive(editor) { toBytes(abi().editorSetViewport(editor.toInt(), width, height)) }

    actual fun editorOnFontMetricsChanged(editor: Long): ByteArray? =
        withActive(editor) { toBytes(abi().editorOnFontMetricsChanged(editor.toInt())) }

    actual fun editorBuildRenderModel(editor: Long): ByteArray? =
        withActive(editor) { toBytes(abi().editorBuildRenderModel(editor.toInt())) }

    actual fun editorHandleGestureEvent(editor: Long, payload: ByteArray): ByteArray? =
        withActive(editor) { toBytes(abi().editorHandleGestureEvent(editor.toInt(), payload.toTyped())) }

    actual fun editorHandleKeyEvent(editor: Long, keyCode: Int, text: ByteArray?, modifiers: Int): ByteArray? =
        withActive(editor) {
            toBytes(abi().editorHandleKeyEvent(editor.toInt(), keyCode, text?.toTyped(), modifiers))
        }

    actual fun editorSetKeyMap(editor: Long, payload: ByteArray): ByteArray? =
        withActive(editor) { toBytes(abi().editorSetKeyMap(editor.toInt(), payload.toTyped())) }

    actual fun editorUpdatePointerModifiers(editor: Long, modifiers: Int): ByteArray? =
        withActive(editor) { toBytes(abi().editorUpdatePointerModifiers(editor.toInt(), modifiers)) }

    actual fun editorTickAnimations(editor: Long): ByteArray? =
        withActive(editor) { toBytes(abi().editorTickAnimations(editor.toInt())) }

    actual fun editorInsertText(editor: Long, text: ByteArray): ByteArray? =
        withActive(editor) { toBytes(abi().editorInsertText(editor.toInt(), text.toTyped())) }

    actual fun editorReplaceText(
        editor: Long,
        startLine: Int,
        startColumn: Int,
        endLine: Int,
        endColumn: Int,
        text: ByteArray,
    ): ByteArray? = withActive(editor) {
        toBytes(abi().editorReplaceText(editor.toInt(), startLine, startColumn, endLine, endColumn, text.toTyped()))
    }

    actual fun editorApplyTextEdits(editor: Long, payload: ByteArray): ByteArray? =
        withActive(editor) { toBytes(abi().editorApplyTextEdits(editor.toInt(), payload.toTyped())) }

    actual fun editorBackspace(editor: Long): ByteArray? =
        withActive(editor) { toBytes(abi().editorBackspace(editor.toInt())) }

    actual fun editorUndo(editor: Long): ByteArray? = withActive(editor) { toBytes(abi().editorUndo(editor.toInt())) }

    actual fun editorRedo(editor: Long): ByteArray? = withActive(editor) { toBytes(abi().editorRedo(editor.toInt())) }

    actual fun editorCanUndo(editor: Long): Boolean = abi().editorCanUndo(editor.toInt()) as Boolean

    actual fun editorCanRedo(editor: Long): Boolean = abi().editorCanRedo(editor.toInt()) as Boolean

    actual fun editorSetGutterSticky(editor: Long, sticky: Boolean): ByteArray? =
        withActive(editor) { toBytes(abi().editorSetGutterSticky(editor.toInt(), sticky)) }

    actual fun editorSetGutterVisible(editor: Long, visible: Boolean): ByteArray? =
        withActive(editor) { toBytes(abi().editorSetGutterVisible(editor.toInt(), visible)) }

    actual fun editorSetWrapMode(editor: Long, mode: Int): ByteArray? =
        withActive(editor) { toBytes(abi().editorSetWrapMode(editor.toInt(), mode)) }

    actual fun editorSetTabSize(editor: Long, tabSize: Int): ByteArray? =
        withActive(editor) { toBytes(abi().editorSetTabSize(editor.toInt(), tabSize)) }

    actual fun editorSetInsertSpaces(editor: Long, enabled: Boolean): ByteArray? =
        withActive(editor) { toBytes(abi().editorSetInsertSpaces(editor.toInt(), enabled)) }

    actual fun editorSetBracketPairs(editor: Long, openChars: IntArray, closeChars: IntArray): ByteArray? =
        withActive(editor) { toBytes(abi().editorSetBracketPairs(editor.toInt(), openChars, closeChars)) }

    actual fun editorSetAutoClosingPairs(editor: Long, openChars: IntArray, closeChars: IntArray): ByteArray? =
        withActive(editor) { toBytes(abi().editorSetAutoClosingPairs(editor.toInt(), openChars, closeChars)) }

    actual fun editorSetMatchedBrackets(
        editor: Long,
        openLine: Int,
        openColumn: Int,
        closeLine: Int,
        closeColumn: Int,
    ): ByteArray? = withActive(editor) {
        toBytes(abi().editorSetMatchedBrackets(editor.toInt(), openLine, openColumn, closeLine, closeColumn))
    }

    actual fun editorClearMatchedBrackets(editor: Long): ByteArray? =
        withActive(editor) { toBytes(abi().editorClearMatchedBrackets(editor.toInt())) }

    actual fun editorSetDiffChanges(editor: Long, payload: ByteArray): ByteArray? =
        withActive(editor) { toBytes(abi().editorSetDiffChanges(editor.toInt(), payload.toTyped())) }

    actual fun editorComputeDiff(editor: Long, originalUtf8: ByteArray): ByteArray? =
        withActive(editor) { toBytes(abi().editorComputeDiff(editor.toInt(), originalUtf8.toTyped())) }

    actual fun editorSetBatchDiffLineSpans(editor: Long, payload: ByteArray): ByteArray? =
        withActive(editor) { toBytes(abi().editorSetBatchDiffLineSpans(editor.toInt(), payload.toTyped())) }

    actual fun editorClearDiff(editor: Long): ByteArray? =
        withActive(editor) { toBytes(abi().editorClearDiff(editor.toInt())) }

    actual fun editorInsertSnippet(editor: Long, snippetUtf8: ByteArray): ByteArray? =
        withActive(editor) { toBytes(abi().editorInsertSnippet(editor.toInt(), snippetUtf8.toTyped())) }

    actual fun editorStartLinkedEditing(editor: Long, payload: ByteArray): ByteArray? =
        withActive(editor) { toBytes(abi().editorStartLinkedEditing(editor.toInt(), payload.toTyped())) }

    actual fun editorIsInLinkedEditing(editor: Long): Boolean =
        abi().editorIsInLinkedEditing(editor.toInt()) as Boolean

    actual fun editorLinkedEditingNext(editor: Long): ByteArray? =
        withActive(editor) { toBytes(abi().editorLinkedEditingNext(editor.toInt())) }

    actual fun editorLinkedEditingPrev(editor: Long): ByteArray? =
        withActive(editor) { toBytes(abi().editorLinkedEditingPrev(editor.toInt())) }

    actual fun editorCancelLinkedEditing(editor: Long): ByteArray? =
        withActive(editor) { toBytes(abi().editorCancelLinkedEditing(editor.toInt())) }

    actual fun editorSetAutoIndentMode(editor: Long, mode: Int): ByteArray? =
        withActive(editor) { toBytes(abi().editorSetAutoIndentMode(editor.toInt(), mode)) }

    actual fun editorSetBackspaceUnindent(editor: Long, enabled: Boolean): ByteArray? =
        withActive(editor) { toBytes(abi().editorSetBackspaceUnindent(editor.toInt(), enabled)) }

    actual fun editorMoveLineUp(editor: Long): ByteArray? =
        withActive(editor) { toBytes(abi().editorMoveLineUp(editor.toInt())) }

    actual fun editorMoveLineDown(editor: Long): ByteArray? =
        withActive(editor) { toBytes(abi().editorMoveLineDown(editor.toInt())) }

    actual fun editorCopyLineUp(editor: Long): ByteArray? =
        withActive(editor) { toBytes(abi().editorCopyLineUp(editor.toInt())) }

    actual fun editorCopyLineDown(editor: Long): ByteArray? =
        withActive(editor) { toBytes(abi().editorCopyLineDown(editor.toInt())) }

    actual fun editorDeleteLine(editor: Long): ByteArray? =
        withActive(editor) { toBytes(abi().editorDeleteLine(editor.toInt())) }

    actual fun editorInsertLineAbove(editor: Long): ByteArray? =
        withActive(editor) { toBytes(abi().editorInsertLineAbove(editor.toInt())) }

    actual fun editorInsertLineBelow(editor: Long): ByteArray? =
        withActive(editor) { toBytes(abi().editorInsertLineBelow(editor.toInt())) }

    actual fun editorSetScale(editor: Long, scale: Float): ByteArray? =
        withActive(editor) { toBytes(abi().editorSetScale(editor.toInt(), scale)) }

    actual fun editorSetLineSpacing(editor: Long, add: Float, mult: Float): ByteArray? =
        withActive(editor) { toBytes(abi().editorSetLineSpacing(editor.toInt(), add, mult)) }

    actual fun editorSetReadOnly(editor: Long, readOnly: Boolean): ByteArray? =
        withActive(editor) { toBytes(abi().editorSetReadOnly(editor.toInt(), readOnly)) }

    actual fun editorSetCurrentLineRenderMode(editor: Long, mode: Int): ByteArray? =
        withActive(editor) { toBytes(abi().editorSetCurrentLineRenderMode(editor.toInt(), mode)) }

    actual fun editorSetFoldArrowMode(editor: Long, mode: Int): ByteArray? =
        withActive(editor) { toBytes(abi().editorSetFoldArrowMode(editor.toInt(), mode)) }

    actual fun editorSetRenderWhitespace(editor: Long, mode: Int): ByteArray? =
        withActive(editor) { toBytes(abi().editorSetRenderWhitespace(editor.toInt(), mode)) }

    actual fun editorSetRenderLineBreaks(editor: Long, enabled: Boolean): ByteArray? =
        withActive(editor) { toBytes(abi().editorSetRenderLineBreaks(editor.toInt(), enabled)) }

    actual fun editorSetEditorRenderColors(editor: Long, payload: ByteArray): ByteArray? =
        withActive(editor) { toBytes(abi().editorSetEditorRenderColors(editor.toInt(), payload.toTyped())) }

    actual fun editorSetEditorRangeEffectStyles(editor: Long, payload: ByteArray): ByteArray? =
        withActive(editor) { toBytes(abi().editorSetEditorRangeEffectStyles(editor.toInt(), payload.toTyped())) }

    actual fun editorSearch(editor: Long, payload: ByteArray): ByteArray? =
        withActive(editor) { toBytes(abi().editorSearch(editor.toInt(), payload.toTyped())) }

    actual fun editorFindNextSearchMatch(editor: Long): ByteArray? =
        withActive(editor) { toBytes(abi().editorFindNextSearchMatch(editor.toInt())) }

    actual fun editorFindPreviousSearchMatch(editor: Long): ByteArray? =
        withActive(editor) { toBytes(abi().editorFindPreviousSearchMatch(editor.toInt())) }

    actual fun editorReplaceCurrentSearchMatch(editor: Long, payload: ByteArray): ByteArray? =
        withActive(editor) { toBytes(abi().editorReplaceCurrentSearchMatch(editor.toInt(), payload.toTyped())) }

    actual fun editorReplaceAllSearchMatches(editor: Long, payload: ByteArray): ByteArray? =
        withActive(editor) { toBytes(abi().editorReplaceAllSearchMatches(editor.toInt(), payload.toTyped())) }

    actual fun editorClearSearch(editor: Long): ByteArray? =
        withActive(editor) { toBytes(abi().editorClearSearch(editor.toInt())) }

    actual fun editorGetSearchState(editor: Long): ByteArray? =
        withActive(editor) { toBytes(abi().editorGetSearchState(editor.toInt())) }

    actual fun editorImeBeginSession(editor: Long, mutationModel: Int): ByteArray? =
        withActive(editor) { toBytes(abi().editorImeBeginSession(editor.toInt(), mutationModel)) }

    actual fun editorImeEndSession(editor: Long, sessionId: Long): ByteArray? =
        withActive(editor) { toBytes(abi().editorImeEndSession(editor.toInt(), sessionId.toDouble())) }

    actual fun editorImeApplyCommands(editor: Long, payload: ByteArray): ByteArray? =
        withActive(editor) { toBytes(abi().editorImeApplyCommands(editor.toInt(), payload.toTyped())) }

    actual fun editorImeGetState(editor: Long, sessionId: Long): ByteArray? =
        withActive(editor) { toBytes(abi().editorImeGetState(editor.toInt(), sessionId.toDouble())) }

    actual fun editorImeGetContext(
        editor: Long,
        sessionId: Long,
        source: Int,
        startUtf16: Long,
        lengthUtf16: Long,
    ): ByteArray? = withActive(editor) {
        toBytes(
            abi().editorImeGetContext(
                editor.toInt(),
                sessionId.toDouble(),
                source,
                startUtf16.toDouble(),
                lengthUtf16.toDouble(),
            ),
        )
    }

    actual fun editorGetCursorRect(editor: Long): FloatArray =
        withActive(editor) { toFloats(abi().editorGetCursorRect(editor.toInt())) }

    actual fun editorGetPositionRect(editor: Long, line: Int, column: Int): FloatArray =
        withActive(editor) { toFloats(abi().editorGetPositionRect(editor.toInt(), line, column)) }

    actual fun editorGetVisibleLineRange(editor: Long): IntArray =
        withActive(editor) { toInts(abi().editorGetVisibleLineRange(editor.toInt())) }

    actual fun editorGetScrollMetrics(editor: Long): ByteArray? =
        withActive(editor) { toBytes(abi().editorGetScrollMetrics(editor.toInt())) }

    actual fun editorGetSelectedText(editor: Long): ByteArray =
        withActive(editor) { toBytes(abi().editorGetSelectedText(editor.toInt())) ?: ByteArray(0) }

    actual fun editorGetCursorPosition(editor: Long): IntArray =
        withActive(editor) { toInts(abi().editorGetCursorPosition(editor.toInt())) }

    actual fun editorGetWordRangeAtCursor(editor: Long): IntArray =
        withActive(editor) { toInts(abi().editorGetWordRangeAtCursor(editor.toInt())) }

    actual fun editorDecorationOp(
        editor: Long,
        op: Int,
        payload: ByteArray?,
        a: Int,
        b: Int,
        c: Int,
        d: Int,
    ): ByteArray? = withActive(editor) {
        toBytes(abi().editorDecorationOp(editor.toInt(), op, payload?.toTyped(), a, b, c, d))
    }

    actual fun editorGetLinkTargetAt(editor: Long, line: Int, column: Int): ByteArray =
        withActive(editor) { toBytes(abi().editorGetLinkTargetAt(editor.toInt(), line, column)) ?: ByteArray(0) }

    actual fun editorSetFoldRegions(editor: Long, payload: ByteArray): ByteArray? =
        withActive(editor) { toBytes(abi().editorSetFoldRegions(editor.toInt(), payload.toTyped())) }

    actual fun editorToggleFold(editor: Long, line: Int): ByteArray? =
        withActive(editor) { toBytes(abi().editorToggleFold(editor.toInt(), line)) }

    actual fun editorFoldAt(editor: Long, line: Int): ByteArray? =
        withActive(editor) { toBytes(abi().editorFoldAt(editor.toInt(), line)) }

    actual fun editorUnfoldAt(editor: Long, line: Int): ByteArray? =
        withActive(editor) { toBytes(abi().editorUnfoldAt(editor.toInt(), line)) }

    actual fun editorFoldAll(editor: Long): ByteArray? =
        withActive(editor) { toBytes(abi().editorFoldAll(editor.toInt())) }

    actual fun editorUnfoldAll(editor: Long): ByteArray? =
        withActive(editor) { toBytes(abi().editorUnfoldAll(editor.toInt())) }

    actual fun editorIsLineVisible(editor: Long, line: Int): Boolean =
        abi().editorIsLineVisible(editor.toInt(), line) as Boolean

    actual fun editorSetIndentGuides(editor: Long, payload: ByteArray): ByteArray? =
        withActive(editor) { toBytes(abi().editorSetIndentGuides(editor.toInt(), payload.toTyped())) }

    actual fun editorSetBracketGuides(editor: Long, payload: ByteArray): ByteArray? =
        withActive(editor) { toBytes(abi().editorSetBracketGuides(editor.toInt(), payload.toTyped())) }

    actual fun editorSetFlowGuides(editor: Long, payload: ByteArray): ByteArray? =
        withActive(editor) { toBytes(abi().editorSetFlowGuides(editor.toInt(), payload.toTyped())) }

    actual fun editorSetSeparatorGuides(editor: Long, payload: ByteArray): ByteArray? =
        withActive(editor) { toBytes(abi().editorSetSeparatorGuides(editor.toInt(), payload.toTyped())) }

    actual fun editorClearGuides(editor: Long): ByteArray? =
        withActive(editor) { toBytes(abi().editorClearGuides(editor.toInt())) }

    private fun installCallbacksIfNeeded() {
        if (callbacksInstalled) return
        callbacksInstalled = true
        val g = js("globalThis")
        g.__seMeasureText = { text: String, style: Int ->
            currentMeasurer?.measureTextWidth(text, style) ?: 0f
        }
        g.__seMeasureInlay = { text: String ->
            currentMeasurer?.measureInlayHintWidth(text) ?: 0f
        }
        g.__seMeasureIcon = { iconId: Int ->
            currentMeasurer?.measureIconWidth(iconId) ?: 0f
        }
        g.__seFontAscent = { currentMeasurer?.fontAscent() ?: 0f }
        g.__seFontDescent = { currentMeasurer?.fontDescent() ?: 0f }
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

private fun abi(): dynamic = js("globalThis.SweetEditorWebAbi")

private fun ByteArray.toTyped(): dynamic = this

private fun toBytes(value: dynamic): ByteArray? {
    if (value == null) return null
    val length = value.length as Int
    return ByteArray(length) { index -> (value[index] as Int).toByte() }
}

private fun toFloats(value: dynamic): FloatArray {
    val length = value.length as Int
    return FloatArray(length) { index -> (value[index] as Number).toFloat() }
}

private fun toInts(value: dynamic): IntArray {
    val length = value.length as Int
    return IntArray(length) { index -> (value[index] as Number).toInt() }
}
