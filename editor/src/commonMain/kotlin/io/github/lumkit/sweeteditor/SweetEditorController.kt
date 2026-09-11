package io.github.lumkit.sweeteditor

import androidx.compose.runtime.Stable
import io.github.lumkit.sweeteditor.session.RememberedEditorSession

@Stable
class SweetEditorController(
    internal val initialText: String = "",
) {
    private var session: RememberedEditorSession? = null
    private val readyCallbacks = mutableListOf<() -> Unit>()
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

    fun copy(): Boolean = session?.copyToClipboard() == true

    fun cut(): Boolean = session?.cutToClipboard() == true

    fun paste() {
        session?.pasteFromClipboard()
    }

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
