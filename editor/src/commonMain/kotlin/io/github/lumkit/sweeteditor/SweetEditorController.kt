package io.github.lumkit.sweeteditor

import androidx.compose.runtime.Stable
import io.github.lumkit.sweeteditor.session.RememberedEditorSession

@Stable
class SweetEditorController(
    internal val initialText: String = "",
) {
    private var session: RememberedEditorSession? = null
    private val readyCallbacks = mutableListOf<() -> Unit>()
    private var languageConfiguration: LanguageConfiguration? = null
    private var metadata: EditorMetadata? = null
    private var iconProvider: EditorIconProvider? = null
    internal var selectionMenuItemProvider: SelectionMenuItemProvider? = null
        private set
    val events = EditorEventBus()

    val isReady: Boolean get() = session?.isReady == true

    fun whenReady(block: () -> Unit) {
        val current = session
        if (current != null && current.isReady) {
            block()
        } else {
            readyCallbacks += block
        }
    }

    fun insertText(text: String) {
        session?.insertText(text)
    }

    fun undo() {
        session?.undo()
    }

    fun redo() {
        session?.redo()
    }

    fun backspace() {
        session?.backspace()
    }

    fun moveLineUp() {
        session?.moveLineUp()
    }

    fun moveLineDown() {
        session?.moveLineDown()
    }

    fun copyLineUp() {
        session?.copyLineUp()
    }

    fun copyLineDown() {
        session?.copyLineDown()
    }

    fun deleteLine() {
        session?.deleteLine()
    }

    fun insertLineAbove() {
        session?.insertLineAbove()
    }

    fun insertLineBelow() {
        session?.insertLineBelow()
    }

    fun getCursorRect(): EditorCursorRect? = session?.getCursorRect()

    fun getPositionRect(line: Int, column: Int): EditorCursorRect? = session?.getPositionRect(line, column)

    fun getVisibleLineRange(): VisibleLineRange? = session?.getVisibleLineRange()

    fun getScrollMetrics(): EditorScrollMetrics? = session?.getScrollMetrics()

    fun getSelectedText(): String = session?.getSelectedText().orEmpty()

    fun getCursorPosition(): TextPosition? = session?.getCursorPosition()

    fun copy(): Boolean = session?.copyToClipboard() == true

    fun cut(): Boolean = session?.cutToClipboard() == true

    fun paste() {
        session?.pasteFromClipboard()
    }

    fun selectAll() {
        session?.selectAll()
    }

    fun setSelectionMenuItemProvider(provider: SelectionMenuItemProvider?) {
        selectionMenuItemProvider = provider
        session?.applySelectionMenuProvider(provider)
    }

    fun onSelectionMenuItemClick(listener: (SelectionMenuItemClickEvent) -> Unit): () -> Unit =
        events.subscribe(listener)

    fun registerTextStyle(styleId: Int, color: Int, backgroundColor: Int = 0, fontStyle: Int = 0) {
        session?.registerTextStyle(styleId, color, backgroundColor, fontStyle)
    }

    fun registerBatchTextStyles(styles: Map<Int, EditorTextStyle>) {
        session?.registerBatchTextStyles(styles)
    }

    fun setLineSpans(line: Int, layer: EditorSpanLayer, spans: List<StyleSpan>) {
        session?.setLineSpans(line, layer, spans)
    }

    fun setBatchLineSpans(layer: EditorSpanLayer, spansByLine: Map<Int, List<StyleSpan>>) {
        session?.setBatchLineSpans(layer, spansByLine)
    }

    fun clearLineSpans(line: Int, layer: EditorSpanLayer) {
        session?.clearLineSpans(line, layer)
    }

    fun clearHighlights() {
        session?.clearHighlights()
    }

    fun clearHighlights(layer: EditorSpanLayer) {
        session?.clearHighlights(layer)
    }

    fun setLineInlayHints(line: Int, hints: List<InlayHint>) {
        session?.setLineInlayHints(line, hints)
    }

    fun setBatchLineInlayHints(hintsByLine: Map<Int, List<InlayHint>>) {
        session?.setBatchLineInlayHints(hintsByLine)
    }

    fun clearInlayHints() {
        session?.clearInlayHints()
    }

    fun setLinePhantomTexts(line: Int, phantoms: List<PhantomText>) {
        session?.setLinePhantomTexts(line, phantoms)
    }

    fun setBatchLinePhantomTexts(phantomsByLine: Map<Int, List<PhantomText>>) {
        session?.setBatchLinePhantomTexts(phantomsByLine)
    }

    fun clearPhantomTexts() {
        session?.clearPhantomTexts()
    }

    fun setLineGutterIcons(line: Int, icons: List<GutterIcon>) {
        session?.setLineGutterIcons(line, icons)
    }

    fun setBatchLineGutterIcons(iconsByLine: Map<Int, List<GutterIcon>>) {
        session?.setBatchLineGutterIcons(iconsByLine)
    }

    fun setMaxGutterIcons(count: Int) {
        session?.setMaxGutterIcons(count)
    }

    fun clearGutterIcons() {
        session?.clearGutterIcons()
    }

    fun setLineCodeLens(line: Int, items: List<CodeLensItem>) {
        session?.setLineCodeLens(line, items)
    }

    fun setBatchLineCodeLens(itemsByLine: Map<Int, List<CodeLensItem>>) {
        session?.setBatchLineCodeLens(itemsByLine)
    }

    fun clearCodeLens() {
        session?.clearCodeLens()
    }

    fun setLineLinks(line: Int, links: List<LinkSpan>) {
        session?.setLineLinks(line, links)
    }

    fun setBatchLineLinks(linksByLine: Map<Int, List<LinkSpan>>) {
        session?.setBatchLineLinks(linksByLine)
    }

    fun clearLinks() {
        session?.clearLinks()
    }

    fun getLinkTargetAt(line: Int, column: Int): String = session?.getLinkTargetAt(line, column).orEmpty()

    fun setLineDiagnostics(line: Int, items: List<Diagnostic>) {
        session?.setLineDiagnostics(line, items)
    }

    fun setBatchLineDiagnostics(itemsByLine: Map<Int, List<Diagnostic>>) {
        session?.setBatchLineDiagnostics(itemsByLine)
    }

    fun clearDiagnostics() {
        session?.clearDiagnostics()
    }

    fun setLineDocumentHighlights(line: Int, items: List<DocumentHighlight>) {
        session?.setLineDocumentHighlights(line, items)
    }

    fun setBatchLineDocumentHighlights(itemsByLine: Map<Int, List<DocumentHighlight>>) {
        session?.setBatchLineDocumentHighlights(itemsByLine)
    }

    fun clearDocumentHighlights() {
        session?.clearDocumentHighlights()
    }

    fun clearAllDecorations() {
        session?.clearAllDecorations()
    }

    fun addDecorationProvider(provider: DecorationProvider) {
        session?.addDecorationProvider(provider)
    }

    fun removeDecorationProvider(provider: DecorationProvider) {
        session?.removeDecorationProvider(provider)
    }

    fun requestDecorationRefresh() {
        session?.requestDecorationRefresh()
    }

    fun search(pattern: String, options: EditorSearchOptions = EditorSearchOptions()) {
        session?.search(pattern, options)
    }

    fun findNextSearchMatch() {
        session?.findNextSearchMatch()
    }

    fun findPreviousSearchMatch() {
        session?.findPreviousSearchMatch()
    }

    fun replaceCurrentSearchMatch(replacement: String) {
        session?.replaceCurrentSearchMatch(replacement)
    }

    fun replaceAllSearchMatches(replacement: String) {
        session?.replaceAllSearchMatches(replacement)
    }

    fun clearSearch() {
        session?.clearSearch()
    }

    fun getSearchState(): EditorSearchState? = session?.getSearchState()

    fun setFoldRegions(regions: List<FoldRegion>) {
        session?.setFoldRegions(regions)
    }

    fun toggleFold(line: Int) {
        session?.toggleFold(line)
    }

    fun foldAt(line: Int) {
        session?.foldAt(line)
    }

    fun unfoldAt(line: Int) {
        session?.unfoldAt(line)
    }

    fun foldAll() {
        session?.foldAll()
    }

    fun unfoldAll() {
        session?.unfoldAll()
    }

    fun isLineVisible(line: Int): Boolean = session?.isLineVisible(line) ?: true

    fun setIndentGuides(guides: List<IndentGuide>) {
        session?.setIndentGuides(guides)
    }

    fun setBracketGuides(guides: List<BracketGuide>) {
        session?.setBracketGuides(guides)
    }

    fun setFlowGuides(guides: List<FlowGuide>) {
        session?.setFlowGuides(guides)
    }

    fun setSeparatorGuides(guides: List<SeparatorGuide>) {
        session?.setSeparatorGuides(guides)
    }

    fun clearGuides() {
        session?.clearGuides()
    }

    fun setBracketPairs(pairs: List<BracketPair>) {
        session?.setBracketPairs(pairs)
    }

    fun setAutoClosingPairs(pairs: List<BracketPair>) {
        session?.setAutoClosingPairs(pairs)
    }

    fun setMatchedBrackets(openLine: Int, openColumn: Int, closeLine: Int, closeColumn: Int) {
        session?.setMatchedBrackets(openLine, openColumn, closeLine, closeColumn)
    }

    fun clearMatchedBrackets() {
        session?.clearMatchedBrackets()
    }

    fun setDiffChanges(changes: List<DiffChange>) {
        session?.setDiffChanges(changes)
    }

    fun computeDiff(originalText: String) {
        session?.computeDiff(originalText)
    }

    fun setBatchDiffLineSpans(layer: EditorSpanLayer, spansByOriginalLine: Map<Int, List<StyleSpan>>) {
        session?.setBatchDiffLineSpans(layer, spansByOriginalLine)
    }

    fun clearDiff() {
        session?.clearDiff()
    }

    fun insertSnippet(template: String) {
        session?.insertSnippet(template)
    }

    fun startLinkedEditing(groups: List<TabStopGroup>) {
        session?.startLinkedEditing(groups)
    }

    fun isInLinkedEditing(): Boolean = session?.isInLinkedEditing() == true

    fun linkedEditingNext() {
        session?.linkedEditingNext()
    }

    fun linkedEditingPrev() {
        session?.linkedEditingPrev()
    }

    fun cancelLinkedEditing() {
        session?.cancelLinkedEditing()
    }

    fun setLanguageConfiguration(config: LanguageConfiguration?) {
        languageConfiguration = config
        session?.applyLanguageConfiguration(config)
    }

    fun getLanguageConfiguration(): LanguageConfiguration? =
        session?.getLanguageConfiguration() ?: languageConfiguration

    fun setMetadata(metadata: EditorMetadata?) {
        this.metadata = metadata
        session?.setMetadata(metadata)
    }

    fun getMetadata(): EditorMetadata? = session?.getMetadata() ?: metadata

    fun setEditorIconProvider(provider: EditorIconProvider?) {
        iconProvider = provider
        session?.setEditorIconProvider(provider)
    }

    fun getEditorIconProvider(): EditorIconProvider? =
        session?.getEditorIconProvider() ?: iconProvider

    fun addCompletionProvider(provider: CompletionProvider) {
        session?.addCompletionProvider(provider)
    }

    fun removeCompletionProvider(provider: CompletionProvider) {
        session?.removeCompletionProvider(provider)
    }

    fun addNewLineActionProvider(provider: NewLineActionProvider) {
        session?.addNewLineActionProvider(provider)
    }

    fun removeNewLineActionProvider(provider: NewLineActionProvider) {
        session?.removeNewLineActionProvider(provider)
    }

    fun triggerCompletion() {
        session?.triggerCompletion()
    }

    fun showCompletionItems(items: List<CompletionItem>) {
        session?.showCompletionItems(items)
    }

    fun dismissCompletion() {
        session?.dismissCompletion()
    }

    fun applyCompletionItem(item: CompletionItem) {
        session?.applyCompletionItem(item)
    }

    fun onTextChanged(listener: (TextChangedEvent) -> Unit): () -> Unit =
        events.subscribe(listener)

    fun onCursorChanged(listener: (CursorChangedEvent) -> Unit): () -> Unit =
        events.subscribe(listener)

    fun onSelectionChanged(listener: (SelectionChangedEvent) -> Unit): () -> Unit =
        events.subscribe(listener)

    fun onScrollChanged(listener: (ScrollChangedEvent) -> Unit): () -> Unit =
        events.subscribe(listener)

    fun onScaleChanged(listener: (ScaleChangedEvent) -> Unit): () -> Unit =
        events.subscribe(listener)

    fun dispose() {
        readyCallbacks.clear()
        events.clear()
    }

    internal fun attach(next: RememberedEditorSession) {
        check(session == null || session === next) {
            "SweetEditorController is already attached to another SweetEditor"
        }
        session = next
        next.applyLanguageConfiguration(languageConfiguration)
        next.setMetadata(metadata)
        next.setEditorIconProvider(iconProvider)
        next.applySelectionMenuProvider(selectionMenuItemProvider)
        val pending = readyCallbacks.toList()
        readyCallbacks.clear()
        pending.forEach { it() }
    }

    internal fun detach(current: RememberedEditorSession) {
        if (session === current) {
            session = null
        }
    }
}
