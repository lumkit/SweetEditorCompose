package io.github.lumkit.sweeteditor.core

import io.github.lumkit.sweeteditor.CodeLensItem
import io.github.lumkit.sweeteditor.Diagnostic
import io.github.lumkit.sweeteditor.DocumentHighlight
import io.github.lumkit.sweeteditor.EditorCursorRect
import io.github.lumkit.sweeteditor.EditorScrollMetrics
import io.github.lumkit.sweeteditor.EditorSpanLayer
import io.github.lumkit.sweeteditor.EditorTextStyle
import io.github.lumkit.sweeteditor.GutterIcon
import io.github.lumkit.sweeteditor.InlayHint
import io.github.lumkit.sweeteditor.LinkSpan
import io.github.lumkit.sweeteditor.PhantomText
import io.github.lumkit.sweeteditor.StyleSpan
import io.github.lumkit.sweeteditor.VisibleLineRange
import io.github.lumkit.sweeteditor.core.protocol.CoreProtocol
import io.github.lumkit.sweeteditor.core.protocol.EditorActionResult
import io.github.lumkit.sweeteditor.core.protocol.EditorOptions
import io.github.lumkit.sweeteditor.core.protocol.EditorRenderModel
import io.github.lumkit.sweeteditor.core.protocol.encodeRegisterBatchTextStylesPayload
import io.github.lumkit.sweeteditor.core.protocol.encodeSetBatchLineCodeLensPayload
import io.github.lumkit.sweeteditor.core.protocol.encodeSetBatchLineDiagnosticsPayload
import io.github.lumkit.sweeteditor.core.protocol.encodeSetBatchLineDocumentHighlightsPayload
import io.github.lumkit.sweeteditor.core.protocol.encodeSetBatchLineGutterIconsPayload
import io.github.lumkit.sweeteditor.core.protocol.encodeSetBatchLineInlayHintsPayload
import io.github.lumkit.sweeteditor.core.protocol.encodeSetBatchLineLinksPayload
import io.github.lumkit.sweeteditor.core.protocol.encodeSetBatchLinePhantomTextsPayload
import io.github.lumkit.sweeteditor.core.protocol.encodeSetBatchLineSpansPayload
import io.github.lumkit.sweeteditor.core.protocol.encodeSetLineCodeLensPayload
import io.github.lumkit.sweeteditor.core.protocol.encodeSetLineDiagnosticsPayload
import io.github.lumkit.sweeteditor.core.protocol.encodeSetLineDocumentHighlightsPayload
import io.github.lumkit.sweeteditor.core.protocol.encodeSetLineGutterIconsPayload
import io.github.lumkit.sweeteditor.core.protocol.encodeSetLineInlayHintsPayload
import io.github.lumkit.sweeteditor.core.protocol.encodeSetLineLinksPayload
import io.github.lumkit.sweeteditor.core.protocol.encodeSetLinePhantomTextsPayload
import io.github.lumkit.sweeteditor.core.protocol.encodeSetLineSpansPayload
import io.github.lumkit.sweeteditor.core.protocol.ImeCommandBatch
import io.github.lumkit.sweeteditor.core.protocol.ImeMutationModel
import io.github.lumkit.sweeteditor.core.protocol.ImeState
import io.github.lumkit.sweeteditor.core.protocol.ImeTextContext
import io.github.lumkit.sweeteditor.core.protocol.ImeTextSource
import io.github.lumkit.sweeteditor.internal.jni.NativeBridge
import io.github.lumkit.sweeteditor.internal.jni.NativeDecorationOp

internal class Document(
    internal val handle: Long,
    private val ownsHandle: Boolean,
) {
    fun utf8(): ByteArray = NativeBridge.getDocumentUtf8(handle)

    fun utf8Text(): String = utf8().decodeToString()

    fun close() {
        if (ownsHandle && handle != 0L) {
            NativeBridge.freeDocument(handle)
        }
    }

    companion object {
        fun fromUtf8(text: String): Document {
            val handle = NativeBridge.createDocumentFromUtf8(text.encodeToByteArray())
            check(handle != 0L) { "create_document_from_utf8 failed" }
            return Document(handle, ownsHandle = true)
        }
    }
}

internal class EditorCore(
    private val editorHandle: Long,
) {
    val handle: Long get() = editorHandle

    fun setDocument(document: Document): EditorActionResult? =
        decodeAction(NativeBridge.editorSetDocument(editorHandle, document.handle))

    fun setViewport(width: Int, height: Int): EditorActionResult? =
        decodeAction(NativeBridge.editorSetViewport(editorHandle, width, height))

    fun onFontMetricsChanged(): EditorActionResult? =
        decodeAction(NativeBridge.editorOnFontMetricsChanged(editorHandle))

    fun buildRenderModel(): EditorRenderModel? {
        val bytes = NativeBridge.editorBuildRenderModel(editorHandle) ?: return null
        return try {
            CoreProtocol.decodeEditorRenderModel(bytes)
        } catch (error: Throwable) {
            val head = bytes.take(96).joinToString(" ") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }
            throw IllegalStateException(
                "Failed to decode EditorRenderModel (${bytes.size} bytes, head=$head)",
                error,
            )
        }
    }

    fun handleGestureEvent(payload: ByteArray): EditorActionResult? =
        decodeAction(NativeBridge.editorHandleGestureEvent(editorHandle, payload))

    fun handleKeyEvent(keyCode: Int, text: ByteArray?, modifiers: Int): EditorActionResult? =
        decodeAction(NativeBridge.editorHandleKeyEvent(editorHandle, keyCode, text, modifiers))

    fun setKeyMap(payload: ByteArray): EditorActionResult? =
        decodeAction(NativeBridge.editorSetKeyMap(editorHandle, payload))

    fun updatePointerModifiers(modifiers: Int): EditorActionResult? =
        decodeAction(NativeBridge.editorUpdatePointerModifiers(editorHandle, modifiers))

    fun tickAnimations(): EditorActionResult? =
        decodeAction(NativeBridge.editorTickAnimations(editorHandle))

    fun insertText(text: String): EditorActionResult? =
        decodeAction(NativeBridge.editorInsertText(editorHandle, text.encodeToByteArray()))

    fun backspace(): EditorActionResult? = decodeAction(NativeBridge.editorBackspace(editorHandle))

    fun undo(): EditorActionResult? = decodeAction(NativeBridge.editorUndo(editorHandle))

    fun redo(): EditorActionResult? = decodeAction(NativeBridge.editorRedo(editorHandle))

    fun canUndo(): Boolean = NativeBridge.editorCanUndo(editorHandle)

    fun canRedo(): Boolean = NativeBridge.editorCanRedo(editorHandle)

    fun setGutterSticky(sticky: Boolean): EditorActionResult? =
        decodeAction(NativeBridge.editorSetGutterSticky(editorHandle, sticky))

    fun setGutterVisible(visible: Boolean): EditorActionResult? =
        decodeAction(NativeBridge.editorSetGutterVisible(editorHandle, visible))

    fun setWrapMode(mode: Int): EditorActionResult? =
        decodeAction(NativeBridge.editorSetWrapMode(editorHandle, mode))

    fun setTabSize(tabSize: Int): EditorActionResult? =
        decodeAction(NativeBridge.editorSetTabSize(editorHandle, tabSize))

    fun setInsertSpaces(enabled: Boolean): EditorActionResult? =
        decodeAction(NativeBridge.editorSetInsertSpaces(editorHandle, enabled))

    fun setAutoIndentMode(mode: Int): EditorActionResult? =
        decodeAction(NativeBridge.editorSetAutoIndentMode(editorHandle, mode))

    fun setBackspaceUnindent(enabled: Boolean): EditorActionResult? =
        decodeAction(NativeBridge.editorSetBackspaceUnindent(editorHandle, enabled))

    fun moveLineUp(): EditorActionResult? = decodeAction(NativeBridge.editorMoveLineUp(editorHandle))

    fun moveLineDown(): EditorActionResult? = decodeAction(NativeBridge.editorMoveLineDown(editorHandle))

    fun copyLineUp(): EditorActionResult? = decodeAction(NativeBridge.editorCopyLineUp(editorHandle))

    fun copyLineDown(): EditorActionResult? = decodeAction(NativeBridge.editorCopyLineDown(editorHandle))

    fun deleteLine(): EditorActionResult? = decodeAction(NativeBridge.editorDeleteLine(editorHandle))

    fun insertLineAbove(): EditorActionResult? = decodeAction(NativeBridge.editorInsertLineAbove(editorHandle))

    fun insertLineBelow(): EditorActionResult? = decodeAction(NativeBridge.editorInsertLineBelow(editorHandle))

    fun getCursorRect(): EditorCursorRect =
        NativeBridge.editorGetCursorRect(editorHandle).toCursorRect()

    fun getPositionRect(line: Int, column: Int): EditorCursorRect =
        NativeBridge.editorGetPositionRect(editorHandle, line, column).toCursorRect()

    fun getVisibleLineRange(): VisibleLineRange {
        val range = NativeBridge.editorGetVisibleLineRange(editorHandle)
        val start = range.getOrElse(0) { 0 }
        val end = range.getOrElse(1) { -1 }
        return VisibleLineRange(start, end)
    }

    fun getSelectedText(): String = NativeBridge.editorGetSelectedText(editorHandle).decodeToString()

    fun registerTextStyle(styleId: Int, color: Int, backgroundColor: Int = 0, fontStyle: Int = 0): EditorActionResult? =
        decoration(NativeDecorationOp.REGISTER_TEXT_STYLE, a = styleId, b = color, c = backgroundColor, d = fontStyle)

    fun registerBatchTextStyles(styles: Map<Int, EditorTextStyle>): EditorActionResult? =
        decoration(NativeDecorationOp.REGISTER_BATCH_TEXT_STYLES, encodeRegisterBatchTextStylesPayload(styles))

    fun setLineSpans(line: Int, layer: EditorSpanLayer, spans: List<StyleSpan>): EditorActionResult? =
        decoration(NativeDecorationOp.SET_LINE_SPANS, encodeSetLineSpansPayload(line, layer.value, spans))

    fun setBatchLineSpans(layer: EditorSpanLayer, spansByLine: Map<Int, List<StyleSpan>>): EditorActionResult? =
        decoration(NativeDecorationOp.SET_BATCH_LINE_SPANS, encodeSetBatchLineSpansPayload(layer.value, spansByLine))

    fun clearLineSpans(line: Int, layer: EditorSpanLayer): EditorActionResult? =
        decoration(NativeDecorationOp.CLEAR_LINE_SPANS, a = line, b = layer.value)

    fun clearHighlights(): EditorActionResult? = decoration(NativeDecorationOp.CLEAR_HIGHLIGHTS)

    fun clearHighlights(layer: EditorSpanLayer): EditorActionResult? =
        decoration(NativeDecorationOp.CLEAR_HIGHLIGHTS_LAYER, a = layer.value)

    fun setLineInlayHints(line: Int, hints: List<InlayHint>): EditorActionResult? =
        decoration(NativeDecorationOp.SET_LINE_INLAY_HINTS, encodeSetLineInlayHintsPayload(line, hints))

    fun setBatchLineInlayHints(hintsByLine: Map<Int, List<InlayHint>>): EditorActionResult? =
        decoration(NativeDecorationOp.SET_BATCH_LINE_INLAY_HINTS, encodeSetBatchLineInlayHintsPayload(hintsByLine))

    fun clearInlayHints(): EditorActionResult? = decoration(NativeDecorationOp.CLEAR_INLAY_HINTS)

    fun setLinePhantomTexts(line: Int, phantoms: List<PhantomText>): EditorActionResult? =
        decoration(NativeDecorationOp.SET_LINE_PHANTOM_TEXTS, encodeSetLinePhantomTextsPayload(line, phantoms))

    fun setBatchLinePhantomTexts(phantomsByLine: Map<Int, List<PhantomText>>): EditorActionResult? =
        decoration(
            NativeDecorationOp.SET_BATCH_LINE_PHANTOM_TEXTS,
            encodeSetBatchLinePhantomTextsPayload(phantomsByLine),
        )

    fun clearPhantomTexts(): EditorActionResult? = decoration(NativeDecorationOp.CLEAR_PHANTOM_TEXTS)

    fun setLineGutterIcons(line: Int, icons: List<GutterIcon>): EditorActionResult? =
        decoration(NativeDecorationOp.SET_LINE_GUTTER_ICONS, encodeSetLineGutterIconsPayload(line, icons))

    fun setBatchLineGutterIcons(iconsByLine: Map<Int, List<GutterIcon>>): EditorActionResult? =
        decoration(NativeDecorationOp.SET_BATCH_LINE_GUTTER_ICONS, encodeSetBatchLineGutterIconsPayload(iconsByLine))

    fun setMaxGutterIcons(count: Int): EditorActionResult? =
        decoration(NativeDecorationOp.SET_MAX_GUTTER_ICONS, a = count)

    fun clearGutterIcons(): EditorActionResult? = decoration(NativeDecorationOp.CLEAR_GUTTER_ICONS)

    fun setLineCodeLens(line: Int, items: List<CodeLensItem>): EditorActionResult? =
        decoration(NativeDecorationOp.SET_LINE_CODELENS, encodeSetLineCodeLensPayload(line, items))

    fun setBatchLineCodeLens(itemsByLine: Map<Int, List<CodeLensItem>>): EditorActionResult? =
        decoration(NativeDecorationOp.SET_BATCH_LINE_CODELENS, encodeSetBatchLineCodeLensPayload(itemsByLine))

    fun clearCodeLens(): EditorActionResult? = decoration(NativeDecorationOp.CLEAR_CODELENS)

    fun setLineLinks(line: Int, links: List<LinkSpan>): EditorActionResult? =
        decoration(NativeDecorationOp.SET_LINE_LINKS, encodeSetLineLinksPayload(line, links))

    fun setBatchLineLinks(linksByLine: Map<Int, List<LinkSpan>>): EditorActionResult? =
        decoration(NativeDecorationOp.SET_BATCH_LINE_LINKS, encodeSetBatchLineLinksPayload(linksByLine))

    fun clearLinks(): EditorActionResult? = decoration(NativeDecorationOp.CLEAR_LINKS)

    fun getLinkTargetAt(line: Int, column: Int): String =
        NativeBridge.editorGetLinkTargetAt(editorHandle, line, column).decodeToString()

    fun setLineDiagnostics(line: Int, items: List<Diagnostic>): EditorActionResult? =
        decoration(NativeDecorationOp.SET_LINE_DIAGNOSTICS, encodeSetLineDiagnosticsPayload(line, items))

    fun setBatchLineDiagnostics(itemsByLine: Map<Int, List<Diagnostic>>): EditorActionResult? =
        decoration(NativeDecorationOp.SET_BATCH_LINE_DIAGNOSTICS, encodeSetBatchLineDiagnosticsPayload(itemsByLine))

    fun clearDiagnostics(): EditorActionResult? = decoration(NativeDecorationOp.CLEAR_DIAGNOSTICS)

    fun setLineDocumentHighlights(line: Int, items: List<DocumentHighlight>): EditorActionResult? =
        decoration(
            NativeDecorationOp.SET_LINE_DOCUMENT_HIGHLIGHTS,
            encodeSetLineDocumentHighlightsPayload(line, items),
        )

    fun setBatchLineDocumentHighlights(itemsByLine: Map<Int, List<DocumentHighlight>>): EditorActionResult? =
        decoration(
            NativeDecorationOp.SET_BATCH_LINE_DOCUMENT_HIGHLIGHTS,
            encodeSetBatchLineDocumentHighlightsPayload(itemsByLine),
        )

    fun clearDocumentHighlights(): EditorActionResult? = decoration(NativeDecorationOp.CLEAR_DOCUMENT_HIGHLIGHTS)

    fun clearAllDecorations(): EditorActionResult? = decoration(NativeDecorationOp.CLEAR_ALL_DECORATIONS)

    private fun decoration(
        op: Int,
        payload: ByteArray? = null,
        a: Int = 0,
        b: Int = 0,
        c: Int = 0,
        d: Int = 0,
    ): EditorActionResult? = decodeAction(NativeBridge.editorDecorationOp(editorHandle, op, payload, a, b, c, d))

    fun getScrollMetrics(): EditorScrollMetrics? {
        val bytes = NativeBridge.editorGetScrollMetrics(editorHandle) ?: return null
        val metrics = CoreProtocol.decodeScrollMetrics(bytes)
        return EditorScrollMetrics(
            scale = metrics.scale,
            scrollX = metrics.scrollX,
            scrollY = metrics.scrollY,
            maxScrollX = metrics.maxScrollX,
            maxScrollY = metrics.maxScrollY,
            contentWidth = metrics.contentSize.width,
            contentHeight = metrics.contentSize.height,
            viewportWidth = metrics.viewportSize.width,
            viewportHeight = metrics.viewportSize.height,
            textAreaX = metrics.textAreaX,
            textAreaWidth = metrics.textAreaWidth,
            canScrollX = metrics.canScrollX,
            canScrollY = metrics.canScrollY,
        )
    }

    fun setScale(scale: Float): EditorActionResult? =
        decodeAction(NativeBridge.editorSetScale(editorHandle, scale))

    fun setLineSpacing(add: Float, mult: Float): EditorActionResult? =
        decodeAction(NativeBridge.editorSetLineSpacing(editorHandle, add, mult))

    fun setReadOnly(readOnly: Boolean): EditorActionResult? =
        decodeAction(NativeBridge.editorSetReadOnly(editorHandle, readOnly))

    fun setCurrentLineRenderMode(mode: Int): EditorActionResult? =
        decodeAction(NativeBridge.editorSetCurrentLineRenderMode(editorHandle, mode))

    fun setEditorRenderColors(payload: ByteArray): EditorActionResult? =
        decodeAction(NativeBridge.editorSetEditorRenderColors(editorHandle, payload))

    fun beginImeSession(model: ImeMutationModel = ImeMutationModel.COMMAND): ImeState? {
        val bytes = NativeBridge.editorImeBeginSession(editorHandle, model.value) ?: return null
        return CoreProtocol.decodeImeState(bytes)
    }

    fun endImeSession(sessionId: Long): EditorActionResult? =
        decodeAction(NativeBridge.editorImeEndSession(editorHandle, sessionId))

    fun applyImeCommands(batch: ImeCommandBatch): EditorActionResult? =
        decodeAction(NativeBridge.editorImeApplyCommands(editorHandle, CoreProtocol.encodeImeCommandBatch(batch)))

    fun getImeState(sessionId: Long): ImeState? {
        val bytes = NativeBridge.editorImeGetState(editorHandle, sessionId) ?: return null
        return CoreProtocol.decodeImeState(bytes)
    }

    fun getImeContext(
        sessionId: Long,
        source: ImeTextSource,
        startUtf16: Long,
        lengthUtf16: Long,
    ): ImeTextContext? {
        val bytes = NativeBridge.editorImeGetContext(
            editorHandle,
            sessionId,
            source.value,
            startUtf16,
            lengthUtf16,
        ) ?: return null
        return CoreProtocol.decodeImeTextContext(bytes)
    }

    fun close() {
        if (editorHandle != 0L) {
            NativeBridge.freeEditor(editorHandle)
        }
    }

    companion object {
        fun create(measurer: HostTextMeasurer, options: EditorOptions = defaultEditorOptions()): EditorCore {
            val handle = NativeBridge.createEditor(measurer, CoreProtocol.encodeEditorOptions(options))
            check(handle != 0L) { "create_editor failed" }
            return EditorCore(handle)
        }
    }
}

internal fun defaultEditorOptions(): EditorOptions = EditorOptions(
    touchSlop = 10f,
    doubleTapTimeout = 300L,
    longPressMs = 500L,
    flingFriction = 3.5f,
    flingMinVelocity = 50f,
    flingMaxVelocity = 8000f,
    maxUndoStackSize = 512L,
    keyChordTimeoutMs = 2000L,
    revealSelectionEndOnSelectAll = false,
)

private fun FloatArray.toCursorRect(): EditorCursorRect = EditorCursorRect(
    x = getOrElse(0) { 0f },
    y = getOrElse(1) { 0f },
    height = getOrElse(2) { 0f },
)

private fun decodeAction(bytes: ByteArray?): EditorActionResult? {
    if (bytes == null) return null
    return CoreProtocol.decodeEditorActionResult(bytes)
}
