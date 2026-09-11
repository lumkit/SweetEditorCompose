package io.github.lumkit.sweeteditor.session

import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.lumkit.sweeteditor.EditorCursorRect
import io.github.lumkit.sweeteditor.EditorScrollMetrics
import io.github.lumkit.sweeteditor.CodeLensItem
import io.github.lumkit.sweeteditor.Diagnostic
import io.github.lumkit.sweeteditor.DocumentHighlight
import io.github.lumkit.sweeteditor.EditorKeyBinding
import io.github.lumkit.sweeteditor.EditorKeyChord
import io.github.lumkit.sweeteditor.EditorKeyMap
import io.github.lumkit.sweeteditor.EditorSettings
import io.github.lumkit.sweeteditor.EditorSpanLayer
import io.github.lumkit.sweeteditor.EditorTextStyle
import io.github.lumkit.sweeteditor.EditorTheme
import io.github.lumkit.sweeteditor.GutterIcon
import io.github.lumkit.sweeteditor.InlayHint
import io.github.lumkit.sweeteditor.LinkSpan
import io.github.lumkit.sweeteditor.PhantomText
import io.github.lumkit.sweeteditor.StyleSpan
import io.github.lumkit.sweeteditor.SweetEditorController
import io.github.lumkit.sweeteditor.VisibleLineRange
import io.github.lumkit.sweeteditor.collectStateEvents
import io.github.lumkit.sweeteditor.core.protocol.CoreProtocol
import io.github.lumkit.sweeteditor.toRangeEffectStyles
import io.github.lumkit.sweeteditor.toRenderColors
import io.github.lumkit.sweeteditor.core.Document
import io.github.lumkit.sweeteditor.core.EditorCore
import io.github.lumkit.sweeteditor.core.HostTextMeasurer
import io.github.lumkit.sweeteditor.core.protocol.AnimationFlag
import io.github.lumkit.sweeteditor.core.protocol.EditorActionResult
import io.github.lumkit.sweeteditor.core.protocol.EditorBuiltinCommand
import io.github.lumkit.sweeteditor.core.protocol.encodeSetKeyMapPayload
import io.github.lumkit.sweeteditor.input.EditorClipboard
import io.github.lumkit.sweeteditor.core.protocol.EditorRenderModel
import io.github.lumkit.sweeteditor.core.protocol.EventType
import io.github.lumkit.sweeteditor.core.protocol.GestureType
import io.github.lumkit.sweeteditor.core.protocol.HitTargetType
import io.github.lumkit.sweeteditor.core.protocol.PointF
import io.github.lumkit.sweeteditor.input.encodeGesture
import io.github.lumkit.sweeteditor.core.protocol.ImeCommand
import io.github.lumkit.sweeteditor.core.protocol.ImeCommandBatch
import io.github.lumkit.sweeteditor.core.protocol.ImeMutationModel
import io.github.lumkit.sweeteditor.core.protocol.ImeState
import io.github.lumkit.sweeteditor.core.protocol.ImeTextContext
import io.github.lumkit.sweeteditor.core.protocol.ImeTextSource
import io.github.lumkit.sweeteditor.core.protocol.PointerCursorType
import io.github.lumkit.sweeteditor.input.EditorImeAdapter
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
    var visualScale by mutableStateOf(1f)
        private set
    internal var lastPointer = PointF(0f, 0f)
    internal var pointerHovering = false
    private var hostScaleGestureActive = false

    val isReady: Boolean get() = editor != null && !disposed

    private var editor: EditorCore? = null
    private var document: Document? = null
    private var disposed = false
    private var viewportWidth = 0
    private var viewportHeight = 0
    private var imeAdapter: EditorImeAdapter? = null
    private var clipboard: EditorClipboard? = null
    internal var onTap: (() -> Unit)? = null
    internal var imeTapHandler: (() -> Unit)? = null
    private var appliedTheme: EditorTheme? = null
    private var appliedSettings: EditorSettings? = null
    private var appliedKeyMap: EditorKeyMap? = null
    private var appliedKeyMapRevision = -1

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

    fun applyKeyMap(keyMap: EditorKeyMap) {
        val core = editor ?: return
        if (appliedKeyMap === keyMap && appliedKeyMapRevision == keyMap.revision) return
        dispatchActionResult(core.setKeyMap(encodeSetKeyMapPayload(keyMap.toProtocolBindings())))
        appliedKeyMap = keyMap
        appliedKeyMapRevision = keyMap.revision
    }

    fun applyAppearance(theme: EditorTheme, settings: EditorSettings) {
        val core = editor ?: return
        if (appliedTheme != theme) {
            dispatchActionResult(core.setEditorRenderColors(CoreProtocol.encodeEditorRenderColors(theme.toRenderColors())))
            dispatchActionResult(
                core.setEditorRangeEffectStyles(
                    CoreProtocol.encodeEditorRangeEffectStyles(theme.toRangeEffectStyles()),
                ),
            )
            appliedTheme = theme
        }
        val previous = appliedSettings
        if (previous != settings) {
            if (previous == null || previous.wrapMode != settings.wrapMode) {
                dispatchActionResult(core.setWrapMode(settings.wrapMode.value))
            }
            if (previous == null || previous.tabSize != settings.tabSize) {
                dispatchActionResult(core.setTabSize(settings.tabSize.coerceAtLeast(1)))
            }
            if (previous == null || previous.insertSpaces != settings.insertSpaces) {
                dispatchActionResult(core.setInsertSpaces(settings.insertSpaces))
            }
            if (previous == null ||
                previous.lineSpacingAdd != settings.lineSpacingAdd ||
                previous.lineSpacingMult != settings.lineSpacingMult
            ) {
                dispatchActionResult(core.setLineSpacing(settings.lineSpacingAdd, settings.lineSpacingMult))
            }
            val nextScale = settings.scale.coerceAtLeast(0.1f)
            if (previous == null || previous.scale != settings.scale || visualScale != nextScale) {
                dispatchActionResult(core.setScale(nextScale))
                if (visualScale != nextScale) {
                    visualScale = nextScale
                }
            }
            if (previous == null || previous.readOnly != settings.readOnly) {
                dispatchActionResult(core.setReadOnly(settings.readOnly))
            }
            if (previous == null || previous.gutterVisible != settings.gutterVisible) {
                dispatchActionResult(core.setGutterVisible(settings.gutterVisible))
            }
            if (previous == null || previous.gutterSticky != settings.gutterSticky) {
                dispatchActionResult(core.setGutterSticky(settings.gutterSticky))
            }
            if (previous == null || previous.currentLineRenderMode != settings.currentLineRenderMode) {
                dispatchActionResult(core.setCurrentLineRenderMode(settings.currentLineRenderMode.value))
            }
            if (previous == null || previous.autoIndentMode != settings.autoIndentMode) {
                dispatchActionResult(core.setAutoIndentMode(settings.autoIndentMode.value))
            }
            if (previous == null || previous.backspaceUnindent != settings.backspaceUnindent) {
                dispatchActionResult(core.setBackspaceUnindent(settings.backspaceUnindent))
            }
            appliedSettings = settings
        }
    }

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

    fun notePointer(point: PointF, hovering: Boolean) {
        lastPointer = point
        pointerHovering = hovering
    }

    fun beginHostScaleGesture() {
        if (hostScaleGestureActive || !pointerHovering) return
        hostScaleGestureActive = true
        handleGesture(encodeGesture(EventType.DIRECT_GESTURE_BEGIN, listOf(lastPointer)))
    }

    fun handleDirectScale(directScale: Float) {
        if (!pointerHovering) return
        if (!hostScaleGestureActive) {
            beginHostScaleGesture()
        }
        handleGesture(
            encodeGesture(
                type = EventType.DIRECT_SCALE,
                points = listOf(lastPointer),
                directScale = directScale,
            ),
        )
    }

    fun endHostScaleGesture() {
        if (!hostScaleGestureActive) return
        hostScaleGestureActive = false
        handleGesture(encodeGesture(EventType.DIRECT_GESTURE_END, listOf(lastPointer)))
    }

    fun handleKey(keyCode: Int, text: ByteArray?, modifiers: Int) {
        val core = editor ?: return
        dispatchActionResult(core.handleKeyEvent(keyCode, text, modifiers))
    }

    fun updatePointerModifiers(modifiers: Int) {
        val core = editor ?: return
        dispatchActionResult(core.updatePointerModifiers(modifiers))
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

    fun moveLineUp() {
        val core = editor ?: return
        dispatchActionResult(core.moveLineUp())
    }

    fun moveLineDown() {
        val core = editor ?: return
        dispatchActionResult(core.moveLineDown())
    }

    fun copyLineUp() {
        val core = editor ?: return
        dispatchActionResult(core.copyLineUp())
    }

    fun copyLineDown() {
        val core = editor ?: return
        dispatchActionResult(core.copyLineDown())
    }

    fun deleteLine() {
        val core = editor ?: return
        dispatchActionResult(core.deleteLine())
    }

    fun insertLineAbove() {
        val core = editor ?: return
        dispatchActionResult(core.insertLineAbove())
    }

    fun insertLineBelow() {
        val core = editor ?: return
        dispatchActionResult(core.insertLineBelow())
    }

    fun registerTextStyle(styleId: Int, color: Int, backgroundColor: Int = 0, fontStyle: Int = 0) =
        mutate { registerTextStyle(styleId, color, backgroundColor, fontStyle) }

    fun registerBatchTextStyles(styles: Map<Int, EditorTextStyle>) =
        mutate { registerBatchTextStyles(styles) }

    fun setLineSpans(line: Int, layer: EditorSpanLayer, spans: List<StyleSpan>) =
        mutate { setLineSpans(line, layer, spans) }

    fun setBatchLineSpans(layer: EditorSpanLayer, spansByLine: Map<Int, List<StyleSpan>>) =
        mutate { setBatchLineSpans(layer, spansByLine) }

    fun clearLineSpans(line: Int, layer: EditorSpanLayer) = mutate { clearLineSpans(line, layer) }

    fun clearHighlights() = mutate { clearHighlights() }

    fun clearHighlights(layer: EditorSpanLayer) = mutate { clearHighlights(layer) }

    fun setLineInlayHints(line: Int, hints: List<InlayHint>) = mutate { setLineInlayHints(line, hints) }

    fun setBatchLineInlayHints(hintsByLine: Map<Int, List<InlayHint>>) =
        mutate { setBatchLineInlayHints(hintsByLine) }

    fun clearInlayHints() = mutate { clearInlayHints() }

    fun setLinePhantomTexts(line: Int, phantoms: List<PhantomText>) =
        mutate { setLinePhantomTexts(line, phantoms) }

    fun setBatchLinePhantomTexts(phantomsByLine: Map<Int, List<PhantomText>>) =
        mutate { setBatchLinePhantomTexts(phantomsByLine) }

    fun clearPhantomTexts() = mutate { clearPhantomTexts() }

    fun setLineGutterIcons(line: Int, icons: List<GutterIcon>) = mutate { setLineGutterIcons(line, icons) }

    fun setBatchLineGutterIcons(iconsByLine: Map<Int, List<GutterIcon>>) =
        mutate { setBatchLineGutterIcons(iconsByLine) }

    fun setMaxGutterIcons(count: Int) = mutate { setMaxGutterIcons(count) }

    fun clearGutterIcons() = mutate { clearGutterIcons() }

    fun setLineCodeLens(line: Int, items: List<CodeLensItem>) = mutate { setLineCodeLens(line, items) }

    fun setBatchLineCodeLens(itemsByLine: Map<Int, List<CodeLensItem>>) =
        mutate { setBatchLineCodeLens(itemsByLine) }

    fun clearCodeLens() = mutate { clearCodeLens() }

    fun setLineLinks(line: Int, links: List<LinkSpan>) = mutate { setLineLinks(line, links) }

    fun setBatchLineLinks(linksByLine: Map<Int, List<LinkSpan>>) = mutate { setBatchLineLinks(linksByLine) }

    fun clearLinks() = mutate { clearLinks() }

    fun getLinkTargetAt(line: Int, column: Int): String = editor?.getLinkTargetAt(line, column).orEmpty()

    fun setLineDiagnostics(line: Int, items: List<Diagnostic>) = mutate { setLineDiagnostics(line, items) }

    fun setBatchLineDiagnostics(itemsByLine: Map<Int, List<Diagnostic>>) =
        mutate { setBatchLineDiagnostics(itemsByLine) }

    fun clearDiagnostics() = mutate { clearDiagnostics() }

    fun setLineDocumentHighlights(line: Int, items: List<DocumentHighlight>) =
        mutate { setLineDocumentHighlights(line, items) }

    fun setBatchLineDocumentHighlights(itemsByLine: Map<Int, List<DocumentHighlight>>) =
        mutate { setBatchLineDocumentHighlights(itemsByLine) }

    fun clearDocumentHighlights() = mutate { clearDocumentHighlights() }

    fun clearAllDecorations() = mutate { clearAllDecorations() }

    private fun mutate(block: EditorCore.() -> EditorActionResult?) {
        val core = editor ?: return
        dispatchActionResult(core.block())
    }

    fun tickAnimations() {
        val core = editor ?: return
        dispatchActionResult(core.tickAnimations())
    }

    fun documentUtf8(): String? = document?.utf8Text()

    fun getCursorRect(): EditorCursorRect? = editor?.getCursorRect()

    fun getPositionRect(line: Int, column: Int): EditorCursorRect? = editor?.getPositionRect(line, column)

    fun getVisibleLineRange(): VisibleLineRange? = editor?.getVisibleLineRange()

    fun getScrollMetrics(): EditorScrollMetrics? = editor?.getScrollMetrics()

    fun bindClipboard(next: EditorClipboard?) {
        clipboard = next
    }

    fun getSelectedText(): String = editor?.getSelectedText().orEmpty()

    fun copyToClipboard(): Boolean {
        val text = getSelectedText()
        if (text.isEmpty() || text.length > MaxClipboardChars) return false
        return clipboard?.setText(text) == true
    }

    fun cutToClipboard(): Boolean {
        if (!copyToClipboard()) return false
        val core = editor ?: return false
        dispatchActionResult(core.backspace())
        return true
    }

    fun pasteFromClipboard() {
        val text = clipboard?.getText()?.takeIf { it.isNotEmpty() } ?: return
        insertText(text)
    }

    fun bindImeAdapter(adapter: EditorImeAdapter?) {
        imeAdapter = adapter
    }

    fun isCurrentImeAdapter(adapter: EditorImeAdapter): Boolean = imeAdapter === adapter

    fun beginImeSession(): ImeState? = editor?.beginImeSession(ImeMutationModel.COMMAND)

    fun endImeSession(sessionId: Long): EditorActionResult? {
        val result = editor?.endImeSession(sessionId)
        dispatchActionResult(result)
        return result
    }

    fun applyImeCommands(sessionId: Long, commands: List<ImeCommand>): EditorActionResult? {
        val result = editor?.applyImeCommands(ImeCommandBatch(sessionId, commands))
        dispatchActionResult(result)
        return result
    }

    fun getImeState(sessionId: Long): ImeState? = editor?.getImeState(sessionId)

    fun getImeContext(
        sessionId: Long,
        source: ImeTextSource,
        startUtf16: Long,
        lengthUtf16: Long,
    ): ImeTextContext? = editor?.getImeContext(sessionId, source, startUtf16, lengthUtf16)

    private fun disposeSession() {
        if (disposed) return
        disposed = true
        wantsAnimation = false
        imeAdapter?.closeOwnedSession()
        imeAdapter = null
        controller.detach(this)
        editor?.close()
        editor = null
        document?.close()
        document = null
        clipboard = null
        appliedKeyMap = null
        appliedKeyMapRevision = -1
    }

    private fun dispatchKeyMapCommand(command: Int): Boolean {
        if (command == EditorBuiltinCommand.NONE.value) return false
        val keyMap = appliedKeyMap ?: return false
        val handler = keyMap.handlerFor(command) ?: return false
        handler.onShortcut(
            EditorKeyBinding(EditorKeyChord(0, 0), command = command),
            controller,
        )
        return true
    }

    private companion object {
        const val MaxClipboardChars = 1_000_000
    }

    private fun dispatchActionResult(result: EditorActionResult?) {
        if (disposed || result == null) return
        imeAdapter?.onEditorActionResult(result)
        if (result.gestureType == GestureType.TAP || result.gestureType == GestureType.DOUBLE_TAP) {
            onTap?.invoke()
            if (result.hitTarget.type == HitTargetType.NONE) {
                imeTapHandler?.invoke()
            }
        }
        wantsAnimation = result.animationFlags != AnimationFlag.NONE
        if (result.scaleChanged && result.scaleAfter > 0f) {
            visualScale = result.scaleAfter
        }
        if (result.pointerCursorChanged) {
            pointerCursor = result.pointerCursorAfter
        }
        collectStateEvents(result).forEach { controller.events.publish(it) }
        if (!dispatchKeyMapCommand(result.command)) {
            when (result.command) {
                EditorBuiltinCommand.COPY.value -> copyToClipboard()
                EditorBuiltinCommand.CUT.value -> cutToClipboard()
                EditorBuiltinCommand.PASTE.value -> pasteFromClipboard()
            }
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
