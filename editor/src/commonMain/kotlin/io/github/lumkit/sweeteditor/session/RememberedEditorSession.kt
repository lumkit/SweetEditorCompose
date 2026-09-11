package io.github.lumkit.sweeteditor.session

import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.lumkit.sweeteditor.CompletionItem
import io.github.lumkit.sweeteditor.CompletionProvider
import io.github.lumkit.sweeteditor.CompletionTriggerKind
import io.github.lumkit.sweeteditor.EditorActionSource
import io.github.lumkit.sweeteditor.EditorCursorRect
import io.github.lumkit.sweeteditor.EditorScrollMetrics
import io.github.lumkit.sweeteditor.EditorTextEdit
import io.github.lumkit.sweeteditor.TextPosition
import io.github.lumkit.sweeteditor.TextRange
import io.github.lumkit.sweeteditor.NewLineActionProvider
import io.github.lumkit.sweeteditor.newline.NewLineActionProviderManager
import io.github.lumkit.sweeteditor.newline.NewLineHost
import io.github.lumkit.sweeteditor.EditorMetadata
import io.github.lumkit.sweeteditor.encodeApplyTextEdits
import io.github.lumkit.sweeteditor.isEmptyRange
import io.github.lumkit.sweeteditor.toCodePointArrays
import io.github.lumkit.sweeteditor.CodeLensItem
import io.github.lumkit.sweeteditor.DecorationProvider
import io.github.lumkit.sweeteditor.Diagnostic
import io.github.lumkit.sweeteditor.EditorSearchOptions
import io.github.lumkit.sweeteditor.EditorSearchState
import io.github.lumkit.sweeteditor.encodeSearchReplacement
import io.github.lumkit.sweeteditor.encodeSearchRequest
import io.github.lumkit.sweeteditor.toPublic
import io.github.lumkit.sweeteditor.DocumentHighlight
import io.github.lumkit.sweeteditor.ScrollChangedEvent
import io.github.lumkit.sweeteditor.TextChangedEvent
import io.github.lumkit.sweeteditor.decoration.DecorationHost
import io.github.lumkit.sweeteditor.decoration.DecorationProviderManager
import io.github.lumkit.sweeteditor.completion.CompletionHost
import io.github.lumkit.sweeteditor.completion.CompletionProviderManager
import io.github.lumkit.sweeteditor.EditorIconProvider
import io.github.lumkit.sweeteditor.EditorKeyBinding
import io.github.lumkit.sweeteditor.EditorKeyChord
import io.github.lumkit.sweeteditor.EditorKeyMap
import io.github.lumkit.sweeteditor.EditorSettings
import io.github.lumkit.sweeteditor.EditorSpanLayer
import io.github.lumkit.sweeteditor.EditorTextStyle
import io.github.lumkit.sweeteditor.EditorTheme
import io.github.lumkit.sweeteditor.FlowGuide
import io.github.lumkit.sweeteditor.FoldRegion
import io.github.lumkit.sweeteditor.GutterIcon
import io.github.lumkit.sweeteditor.IndentGuide
import io.github.lumkit.sweeteditor.BracketGuide
import io.github.lumkit.sweeteditor.SeparatorGuide
import io.github.lumkit.sweeteditor.InlayHint
import io.github.lumkit.sweeteditor.BracketPair
import io.github.lumkit.sweeteditor.DiffChange
import io.github.lumkit.sweeteditor.TabStopGroup
import io.github.lumkit.sweeteditor.LanguageConfiguration
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
import io.github.lumkit.sweeteditor.core.protocol.KeyCode
import io.github.lumkit.sweeteditor.core.protocol.KeyModifier
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

    fun markLoadError(message: String) {
        loadError = message
    }
    var wantsAnimation by mutableStateOf(false)
        private set
    var pointerCursor by mutableStateOf(PointerCursorType.TEXT)
        private set
    var visualScale by mutableStateOf(1f)
        private set
    var completionItems by mutableStateOf<List<CompletionItem>>(emptyList())
        private set
    var completionSelectedIndex by mutableStateOf(0)
        private set
    var completionAnchor by mutableStateOf<EditorCursorRect?>(null)
        private set
    var iconProvider by mutableStateOf<EditorIconProvider?>(null)
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
    private var languageConfiguration: LanguageConfiguration? = null
    private var metadata: EditorMetadata? = null
    private var appliedTabSize: Int? = null
    private var appliedInsertSpaces: Boolean? = null
    private val decorations = DecorationProviderManager(SessionDecorationHost())
    private val newLines = NewLineActionProviderManager(SessionNewLineHost())
    private val completions = CompletionProviderManager(SessionCompletionHost()).also { manager ->
        manager.listener = object : CompletionProviderManager.Listener {
            override fun onCompletionItemsUpdated(items: List<CompletionItem>, anchor: EditorCursorRect?) {
                completionItems = items
                completionSelectedIndex = 0
                completionAnchor = anchor
            }

            override fun onCompletionDismissed() {
                completionItems = emptyList()
                completionSelectedIndex = 0
                completionAnchor = null
            }
        }
    }

    override fun onRemembered() {
        if (disposed || editor != null) return
        try {
            if (!NativeBridge.isAvailable) {
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
            decorations.requestRefresh()
        } catch (error: Throwable) {
            loadError = error.message ?: error.toString()
        }
    }

    override fun onForgotten() = disposeSession()

    override fun onAbandoned() = disposeSession()

    fun applyLanguageConfiguration(config: LanguageConfiguration?) {
        val core = editor ?: return
        languageConfiguration = config
        if (config != null) {
            val brackets = config.brackets
            if (brackets != null) {
                val (opens, closes) = brackets.toCodePointArrays()
                dispatchActionResult(core.setBracketPairs(opens, closes))
            }
            val autoClosing = config.autoClosingPairs
            if (autoClosing != null) {
                val (opens, closes) = autoClosing.toCodePointArrays()
                dispatchActionResult(core.setAutoClosingPairs(opens, closes))
            }
        }
        appliedSettings?.let { syncTabBehavior(it) }
        decorations.requestRefresh()
    }

    fun getLanguageConfiguration(): LanguageConfiguration? = languageConfiguration

    fun setMetadata(value: EditorMetadata?) {
        metadata = value
        decorations.requestRefresh()
    }

    fun getMetadata(): EditorMetadata? = metadata

    fun setEditorIconProvider(provider: EditorIconProvider?) {
        if (iconProvider === provider) return
        iconProvider = provider
        measurer.bindIconProvider(provider)
        notifyFontMetricsChanged()
    }

    fun getEditorIconProvider(): EditorIconProvider? = iconProvider

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
            if (previous == null || previous.foldArrowMode != settings.foldArrowMode) {
                dispatchActionResult(core.setFoldArrowMode(settings.foldArrowMode.value))
            }
            if (previous == null || previous.renderWhitespace != settings.renderWhitespace) {
                dispatchActionResult(core.setRenderWhitespace(settings.renderWhitespace.value))
            }
            if (previous == null || previous.renderLineBreaks != settings.renderLineBreaks) {
                dispatchActionResult(core.setRenderLineBreaks(settings.renderLineBreaks))
            }
            if (previous == null || previous.autoIndentMode != settings.autoIndentMode) {
                dispatchActionResult(core.setAutoIndentMode(settings.autoIndentMode.value))
            }
            if (previous == null || previous.backspaceUnindent != settings.backspaceUnindent) {
                dispatchActionResult(core.setBackspaceUnindent(settings.backspaceUnindent))
            }
            appliedSettings = settings
        }
        syncTabBehavior(settings)
    }

    private fun syncTabBehavior(settings: EditorSettings) {
        val core = editor ?: return
        val tabSize = languageConfiguration?.tabSize?.takeIf { it > 0 }
            ?: settings.tabSize.coerceAtLeast(1)
        val insertSpaces = languageConfiguration?.insertSpaces ?: settings.insertSpaces
        if (appliedTabSize != tabSize) {
            dispatchActionResult(core.setTabSize(tabSize))
            appliedTabSize = tabSize
        }
        if (appliedInsertSpaces != insertSpaces) {
            dispatchActionResult(core.setInsertSpaces(insertSpaces))
            appliedInsertSpaces = insertSpaces
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
        if (handleCompletionKey(keyCode, modifiers)) return
        if (tryHandleNewLine(keyCode, modifiers)) return
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

    fun setFoldRegions(regions: List<FoldRegion>) = mutate { setFoldRegions(regions) }

    fun toggleFold(line: Int) = mutate { toggleFold(line) }

    fun foldAt(line: Int) = mutate { foldAt(line) }

    fun unfoldAt(line: Int) = mutate { unfoldAt(line) }

    fun foldAll() = mutate { foldAll() }

    fun unfoldAll() = mutate { unfoldAll() }

    fun isLineVisible(line: Int): Boolean = editor?.isLineVisible(line) ?: true

    fun setIndentGuides(guides: List<IndentGuide>) = mutate { setIndentGuides(guides) }

    fun setBracketGuides(guides: List<BracketGuide>) = mutate { setBracketGuides(guides) }

    fun setFlowGuides(guides: List<FlowGuide>) = mutate { setFlowGuides(guides) }

    fun setSeparatorGuides(guides: List<SeparatorGuide>) = mutate { setSeparatorGuides(guides) }

    fun clearGuides() = mutate { clearGuides() }

    fun setBracketPairs(pairs: List<BracketPair>) = mutate {
        val (opens, closes) = pairs.toCodePointArrays()
        setBracketPairs(opens, closes)
    }

    fun setAutoClosingPairs(pairs: List<BracketPair>) = mutate {
        val (opens, closes) = pairs.toCodePointArrays()
        setAutoClosingPairs(opens, closes)
    }

    fun setMatchedBrackets(openLine: Int, openColumn: Int, closeLine: Int, closeColumn: Int) =
        mutate { setMatchedBrackets(openLine, openColumn, closeLine, closeColumn) }

    fun clearMatchedBrackets() = mutate { clearMatchedBrackets() }

    fun setDiffChanges(changes: List<DiffChange>) = mutate { setDiffChanges(changes) }

    fun computeDiff(originalText: String) = mutate { computeDiff(originalText) }

    fun setBatchDiffLineSpans(layer: EditorSpanLayer, spansByOriginalLine: Map<Int, List<StyleSpan>>) =
        mutate { setBatchDiffLineSpans(layer, spansByOriginalLine) }

    fun clearDiff() = mutate { clearDiff() }

    fun insertSnippet(template: String) = mutate { insertSnippet(template) }

    fun startLinkedEditing(groups: List<TabStopGroup>) = mutate { startLinkedEditing(groups) }

    fun isInLinkedEditing(): Boolean = editor?.isInLinkedEditing() == true

    fun linkedEditingNext() = mutate { linkedEditingNext() }

    fun linkedEditingPrev() = mutate { linkedEditingPrev() }

    fun cancelLinkedEditing() = mutate { cancelLinkedEditing() }

    fun addDecorationProvider(provider: DecorationProvider) = decorations.addProvider(provider)

    fun removeDecorationProvider(provider: DecorationProvider) = decorations.removeProvider(provider)

    fun requestDecorationRefresh() = decorations.requestRefresh()

    fun addCompletionProvider(provider: CompletionProvider) = completions.addProvider(provider)

    fun removeCompletionProvider(provider: CompletionProvider) = completions.removeProvider(provider)

    fun addNewLineActionProvider(provider: NewLineActionProvider) = newLines.addProvider(provider)

    fun removeNewLineActionProvider(provider: NewLineActionProvider) = newLines.removeProvider(provider)

    fun triggerCompletion() = completions.trigger(CompletionTriggerKind.INVOKED, null)

    fun showCompletionItems(items: List<CompletionItem>) = completions.showItems(items)

    fun dismissCompletion() = completions.dismiss()

    fun selectCompletionIndex(index: Int) {
        if (completionItems.isEmpty()) return
        completionSelectedIndex = index.coerceIn(0, completionItems.lastIndex)
    }

    fun applyCompletionItem(item: CompletionItem) {
        val core = editor ?: return
        val insert = item.insertText ?: item.label
        val primary = item.textEdit
        val additional = item.additionalTextEdits
        when {
            primary != null -> {
                dispatchActionResult(core.applyTextEdits(encodeApplyTextEdits(listOf(primary) + additional)))
            }
            additional.isEmpty() -> {
                val word = core.getWordRangeAtCursor()
                if (!word.isEmptyRange()) {
                    dispatchActionResult(
                        core.replaceText(
                            word.start.line,
                            word.start.column,
                            word.end.line,
                            word.end.column,
                            insert,
                        ),
                    )
                } else {
                    dispatchActionResult(core.insertText(insert))
                }
            }
            else -> {
                val cursor = core.getCursorPosition()
                val edits = listOf(EditorTextEdit(TextRange(cursor, cursor), insert)) + additional
                dispatchActionResult(core.applyTextEdits(encodeApplyTextEdits(edits)))
            }
        }
        completions.dismiss()
    }

    fun search(pattern: String, options: EditorSearchOptions) =
        mutate { search(encodeSearchRequest(pattern, options)) }

    fun findNextSearchMatch() = mutate { findNextSearchMatch() }

    fun findPreviousSearchMatch() = mutate { findPreviousSearchMatch() }

    fun replaceCurrentSearchMatch(replacement: String) =
        mutate { replaceCurrentSearchMatch(encodeSearchReplacement(replacement)) }

    fun replaceAllSearchMatches(replacement: String) =
        mutate { replaceAllSearchMatches(encodeSearchReplacement(replacement)) }

    fun clearSearch() = mutate { clearSearch() }

    fun getSearchState(): EditorSearchState? {
        val bytes = editor?.getSearchState() ?: return null
        return CoreProtocol.decodeSearchState(bytes).toPublic()
    }

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

    fun getCursorPosition(): TextPosition? = editor?.getCursorPosition()

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
        decorations.close()
        completions.close()
        newLines.close()
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
            completions.dismiss()
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
        collectStateEvents(result).forEach { event ->
            controller.events.publish(event)
            when (event) {
                is TextChangedEvent -> {
                    decorations.onTextChanged(event.changes)
                    onCompletionTextChanged(event)
                }
                is ScrollChangedEvent -> {
                    decorations.onScrollChanged()
                    if (completionItems.isNotEmpty()) completions.dismiss()
                }
                else -> Unit
            }
        }
        if (!dispatchKeyMapCommand(result.command)) {
            when (result.command) {
                EditorBuiltinCommand.COPY.value -> copyToClipboard()
                EditorBuiltinCommand.CUT.value -> cutToClipboard()
                EditorBuiltinCommand.PASTE.value -> pasteFromClipboard()
                EditorBuiltinCommand.TRIGGER_COMPLETION.value -> triggerCompletion()
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

    private fun tryHandleNewLine(keyCode: Int, modifiers: Int): Boolean {
        if (keyCode != KeyCode.ENTER) return false
        if (modifiers != KeyModifier.NONE) return false
        val core = editor ?: return false
        val action = newLines.provideNewLineAction() ?: return false
        dispatchActionResult(
            core.handleKeyEvent(KeyCode.NONE, action.text.encodeToByteArray(), modifiers),
        )
        return true
    }

    private fun handleCompletionKey(keyCode: Int, modifiers: Int): Boolean {
        if (completionItems.isEmpty()) return false
        if (modifiers != KeyModifier.NONE && modifiers != KeyModifier.SHIFT) return false
        return when (keyCode) {
            KeyCode.ESCAPE -> {
                completions.dismiss()
                true
            }
            KeyCode.ENTER, KeyCode.TAB -> {
                completionItems.getOrNull(completionSelectedIndex)?.let { applyCompletionItem(it) }
                true
            }
            KeyCode.UP -> {
                completionSelectedIndex =
                    (completionSelectedIndex - 1).mod(completionItems.size)
                true
            }
            KeyCode.DOWN -> {
                completionSelectedIndex =
                    (completionSelectedIndex + 1).mod(completionItems.size)
                true
            }
            else -> false
        }
    }

    private fun onCompletionTextChanged(event: TextChangedEvent) {
        if (event.source != EditorActionSource.KEYBOARD) return
        if (event.changes.size != 1) {
            completions.dismiss()
            return
        }
        val inserted = event.changes[0].newText
        if (inserted.length == 1 && completions.isTriggerCharacter(inserted)) {
            completions.trigger(CompletionTriggerKind.CHARACTER, inserted)
        } else if (completionItems.isNotEmpty()) {
            completions.trigger(CompletionTriggerKind.RETRIGGER, null)
        }
    }

    private inner class SessionCompletionHost : CompletionHost {
        override fun isDisposed(): Boolean = disposed || editor == null
        override fun cursorPosition(): TextPosition? = editor?.getCursorPosition()
        override fun lineText(line: Int): String = documentLineText(line)
        override fun wordRangeAtCursor(): TextRange =
            editor?.getWordRangeAtCursor() ?: TextRange(TextPosition(0, 0), TextPosition(0, 0))
        override fun cursorRect(): EditorCursorRect? = editor?.getCursorRect()
        override fun languageConfiguration(): LanguageConfiguration? = languageConfiguration
        override fun editorMetadata(): EditorMetadata? = metadata
    }

    private inner class SessionNewLineHost : NewLineHost {
        override fun isDisposed(): Boolean = disposed || editor == null
        override fun cursorPosition(): TextPosition? = editor?.getCursorPosition()
        override fun lineText(line: Int): String = documentLineText(line)
        override fun languageConfiguration(): LanguageConfiguration? = languageConfiguration
        override fun editorMetadata(): EditorMetadata? = metadata
    }

    private inner class SessionDecorationHost : DecorationHost {
        override fun isDisposed(): Boolean = disposed || editor == null
        override fun visibleLineRange(): VisibleLineRange =
            editor?.getVisibleLineRange() ?: VisibleLineRange(0, -1)
        override fun totalLineCount(): Int = documentLineCount()
        override fun overscanMultiplier(): Float =
            appliedSettings?.decorationOverscanViewportMultiplier ?: 1f
        override fun scrollRefreshMinIntervalMs(): Int =
            appliedSettings?.decorationScrollRefreshMinIntervalMs ?: 50
        override fun languageConfiguration(): LanguageConfiguration? = languageConfiguration
        override fun editorMetadata(): EditorMetadata? = metadata
        override fun clearHighlights(layer: EditorSpanLayer) {
            mutate { clearHighlights(layer) }
        }
        override fun setBatchLineSpans(layer: EditorSpanLayer, spansByLine: Map<Int, List<StyleSpan>>) {
            mutate { setBatchLineSpans(layer, spansByLine) }
        }
        override fun clearInlayHints() {
            mutate { clearInlayHints() }
        }
        override fun setBatchLineInlayHints(hintsByLine: Map<Int, List<InlayHint>>) {
            mutate { setBatchLineInlayHints(hintsByLine) }
        }
        override fun clearDiagnostics() {
            mutate { clearDiagnostics() }
        }
        override fun setBatchLineDiagnostics(itemsByLine: Map<Int, List<Diagnostic>>) {
            mutate { setBatchLineDiagnostics(itemsByLine) }
        }
        override fun clearDocumentHighlights() {
            mutate { clearDocumentHighlights() }
        }
        override fun setBatchLineDocumentHighlights(itemsByLine: Map<Int, List<DocumentHighlight>>) {
            mutate { setBatchLineDocumentHighlights(itemsByLine) }
        }
        override fun clearGutterIcons() {
            mutate { clearGutterIcons() }
        }
        override fun setBatchLineGutterIcons(iconsByLine: Map<Int, List<GutterIcon>>) {
            mutate { setBatchLineGutterIcons(iconsByLine) }
        }
        override fun clearPhantomTexts() {
            mutate { clearPhantomTexts() }
        }
        override fun setBatchLinePhantomTexts(phantomsByLine: Map<Int, List<PhantomText>>) {
            mutate { setBatchLinePhantomTexts(phantomsByLine) }
        }
        override fun clearCodeLens() {
            mutate { clearCodeLens() }
        }
        override fun setBatchLineCodeLens(itemsByLine: Map<Int, List<CodeLensItem>>) {
            mutate { setBatchLineCodeLens(itemsByLine) }
        }
        override fun clearLinks() {
            mutate { clearLinks() }
        }
        override fun setBatchLineLinks(linksByLine: Map<Int, List<LinkSpan>>) {
            mutate { setBatchLineLinks(linksByLine) }
        }
        override fun setFoldRegions(regions: List<FoldRegion>) {
            mutate { setFoldRegions(regions) }
        }
        override fun setIndentGuides(guides: List<IndentGuide>) {
            mutate { setIndentGuides(guides) }
        }
        override fun setBracketGuides(guides: List<BracketGuide>) {
            mutate { setBracketGuides(guides) }
        }
        override fun setFlowGuides(guides: List<FlowGuide>) {
            mutate { setFlowGuides(guides) }
        }
        override fun setSeparatorGuides(guides: List<SeparatorGuide>) {
            mutate { setSeparatorGuides(guides) }
        }
    }

    private fun documentLineCount(): Int {
        val text = document?.utf8Text().orEmpty()
        if (text.isEmpty()) return 1
        return text.count { it == '\n' } + 1
    }

    private fun documentLineText(line: Int): String {
        val text = document?.utf8Text().orEmpty()
        if (text.isEmpty()) return ""
        var current = 0
        var start = 0
        var index = 0
        while (index <= text.length) {
            if (index == text.length || text[index] == '\n') {
                if (current == line) {
                    return text.substring(start, index).trimEnd('\r')
                }
                current++
                start = index + 1
            }
            index++
        }
        return ""
    }
}
