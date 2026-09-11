package io.github.lumkit.sweeteditor.decoration

import io.github.lumkit.sweeteditor.CodeLensItem
import io.github.lumkit.sweeteditor.DecorationApplyMode
import io.github.lumkit.sweeteditor.DecorationContext
import io.github.lumkit.sweeteditor.DecorationProvider
import io.github.lumkit.sweeteditor.DecorationReceiver
import io.github.lumkit.sweeteditor.DecorationResult
import io.github.lumkit.sweeteditor.Diagnostic
import io.github.lumkit.sweeteditor.DocumentHighlight
import io.github.lumkit.sweeteditor.EditorMetadata
import io.github.lumkit.sweeteditor.EditorSpanLayer
import io.github.lumkit.sweeteditor.FlowGuide
import io.github.lumkit.sweeteditor.FoldRegion
import io.github.lumkit.sweeteditor.IndentGuide
import io.github.lumkit.sweeteditor.LanguageConfiguration
import io.github.lumkit.sweeteditor.BracketGuide
import io.github.lumkit.sweeteditor.GutterIcon
import io.github.lumkit.sweeteditor.SeparatorGuide
import io.github.lumkit.sweeteditor.InlayHint
import io.github.lumkit.sweeteditor.LinkSpan
import io.github.lumkit.sweeteditor.PhantomText
import io.github.lumkit.sweeteditor.StyleSpan
import io.github.lumkit.sweeteditor.TextChange
import io.github.lumkit.sweeteditor.VisibleLineRange
import io.github.lumkit.sweeteditor.internal.editorNowMs
import io.github.lumkit.sweeteditor.internal.runOnEditorThread
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

internal interface DecorationHost {
    fun isDisposed(): Boolean
    fun visibleLineRange(): VisibleLineRange
    fun totalLineCount(): Int
    fun overscanMultiplier(): Float
    fun scrollRefreshMinIntervalMs(): Int
    fun languageConfiguration(): LanguageConfiguration?
    fun editorMetadata(): EditorMetadata?
    fun clearHighlights(layer: EditorSpanLayer)
    fun setBatchLineSpans(layer: EditorSpanLayer, spansByLine: Map<Int, List<StyleSpan>>)
    fun clearInlayHints()
    fun setBatchLineInlayHints(hintsByLine: Map<Int, List<InlayHint>>)
    fun clearDiagnostics()
    fun setBatchLineDiagnostics(itemsByLine: Map<Int, List<Diagnostic>>)
    fun clearDocumentHighlights()
    fun setBatchLineDocumentHighlights(itemsByLine: Map<Int, List<DocumentHighlight>>)
    fun clearGutterIcons()
    fun setBatchLineGutterIcons(iconsByLine: Map<Int, List<GutterIcon>>)
    fun clearPhantomTexts()
    fun setBatchLinePhantomTexts(phantomsByLine: Map<Int, List<PhantomText>>)
    fun clearCodeLens()
    fun setBatchLineCodeLens(itemsByLine: Map<Int, List<CodeLensItem>>)
    fun clearLinks()
    fun setBatchLineLinks(linksByLine: Map<Int, List<LinkSpan>>)
    fun setFoldRegions(regions: List<FoldRegion>)
    fun setIndentGuides(guides: List<IndentGuide>)
    fun setBracketGuides(guides: List<BracketGuide>)
    fun setFlowGuides(guides: List<FlowGuide>)
    fun setSeparatorGuides(guides: List<SeparatorGuide>)
}

internal class DecorationProviderManager(
    private val host: DecorationHost,
) {
    private val providers = mutableListOf<DecorationProvider>()
    private val states = mutableMapOf<DecorationProvider, ProviderState>()
    private val pendingTextChanges = mutableListOf<TextChange>()
    private var generation = 0
    private var lastVisible = VisibleLineRange(0, -1)
    private var lastContext = VisibleLineRange(0, -1)
    private var lastScrollRefreshMs = 0L
    private var applyScheduled = false
    private var disposed = false

    fun addProvider(provider: DecorationProvider) {
        runOnEditorThread {
            if (disposed || providers.contains(provider)) return@runOnEditorThread
            providers += provider
            states[provider] = ProviderState()
            requestRefresh()
        }
    }

    fun removeProvider(provider: DecorationProvider) {
        runOnEditorThread {
            if (disposed) return@runOnEditorThread
            providers.remove(provider)
            states.remove(provider)?.receiver?.cancel()
            scheduleApply()
        }
    }

    fun requestRefresh() {
        runOnEditorThread { scheduleRefresh(emptyList()) }
    }

    fun onTextChanged(changes: List<TextChange>) {
        runOnEditorThread { scheduleRefresh(changes) }
    }

    fun onScrollChanged() {
        runOnEditorThread {
            if (disposed) return@runOnEditorThread
            val visible = host.visibleLineRange()
            if (visible == lastVisible) return@runOnEditorThread
            val minInterval = host.scrollRefreshMinIntervalMs().coerceAtLeast(0)
            if (minInterval > 0 && editorNowMs() - lastScrollRefreshMs < minInterval) {
                return@runOnEditorThread
            }
            doRefresh()
            lastScrollRefreshMs = editorNowMs()
        }
    }

    fun close() {
        runOnEditorThread {
            if (disposed) return@runOnEditorThread
            disposed = true
            generation++
            pendingTextChanges.clear()
            states.values.forEach { it.receiver?.cancel() }
            providers.clear()
            states.clear()
        }
    }

    private fun scheduleRefresh(changes: List<TextChange>) {
        if (disposed) return
        pendingTextChanges += changes
        doRefresh()
    }

    private fun doRefresh() {
        if (disposed || host.isDisposed()) return
        generation++
        val currentGeneration = generation
        val visible = host.visibleLineRange()
        lastVisible = visible
        val total = host.totalLineCount().coerceAtLeast(1)
        val changes = pendingTextChanges.toList()
        pendingTextChanges.clear()
        var start = visible.startLine
        var end = visible.endLine
        if (!visible.isEmpty && total > 0) {
            val overscan = calculateOverscan(visible.startLine, visible.endLine)
            start = max(0, visible.startLine - overscan)
            end = min(total - 1, visible.endLine + overscan)
        } else if (total > 0) {
            start = 0
            end = total - 1
        }
        lastContext = VisibleLineRange(start, end)
        val context = DecorationContext(
            visibleLineRange = lastContext,
            totalLineCount = total,
            textChanges = changes,
            languageConfiguration = host.languageConfiguration(),
            editorMetadata = host.editorMetadata(),
        )
        for (provider in providers.toList()) {
            val state = states.getOrPut(provider) { ProviderState() }
            state.receiver?.cancel()
            val receiver = ManagedReceiver(provider, currentGeneration)
            state.receiver = receiver
            try {
                provider.provideDecorations(context, receiver)
            } catch (_: Throwable) {
            }
        }
    }

    private fun calculateOverscan(start: Int, end: Int): Int {
        val viewport = if (end >= start) end - start + 1 else 0
        if (viewport <= 0) return 0
        val multiplier = host.overscanMultiplier().coerceAtLeast(0f)
        return max(0, ceil(viewport * multiplier).toInt())
    }

    private fun scheduleApply() {
        if (disposed || applyScheduled) return
        applyScheduled = true
        runOnEditorThread { applyMerged() }
    }

    private fun applyMerged() {
        if (disposed || host.isDisposed()) return
        applyScheduled = false
        val syntax = mutableMapOf<Int, MutableList<StyleSpan>>()
        val semantic = mutableMapOf<Int, MutableList<StyleSpan>>()
        val overlay = mutableMapOf<Int, MutableList<StyleSpan>>()
        val inlays = mutableMapOf<Int, MutableList<InlayHint>>()
        val diagnostics = mutableMapOf<Int, MutableList<Diagnostic>>()
        val highlights = mutableMapOf<Int, MutableList<DocumentHighlight>>()
        val gutters = mutableMapOf<Int, MutableList<GutterIcon>>()
        val phantoms = mutableMapOf<Int, MutableList<PhantomText>>()
        val lenses = mutableMapOf<Int, MutableList<CodeLensItem>>()
        val links = mutableMapOf<Int, MutableList<LinkSpan>>()
        val folds = mutableListOf<FoldRegion>()
        val indents = mutableListOf<IndentGuide>()
        val brackets = mutableListOf<BracketGuide>()
        val flows = mutableListOf<FlowGuide>()
        val separators = mutableListOf<SeparatorGuide>()
        var hasFolds = false
        var hasIndents = false
        var hasBrackets = false
        var hasFlows = false
        var hasSeparators = false
        var syntaxMode = DecorationApplyMode.MERGE
        var semanticMode = DecorationApplyMode.MERGE
        var overlayMode = DecorationApplyMode.MERGE
        var inlayMode = DecorationApplyMode.MERGE
        var diagnosticMode = DecorationApplyMode.MERGE
        var highlightMode = DecorationApplyMode.MERGE
        var gutterMode = DecorationApplyMode.MERGE
        var phantomMode = DecorationApplyMode.MERGE
        var lensMode = DecorationApplyMode.MERGE
        var linksMode = DecorationApplyMode.MERGE
        var foldMode = DecorationApplyMode.MERGE
        var indentMode = DecorationApplyMode.MERGE
        var bracketMode = DecorationApplyMode.MERGE
        var flowMode = DecorationApplyMode.MERGE
        var separatorMode = DecorationApplyMode.MERGE

        for (provider in providers) {
            val snapshot = states[provider]?.snapshot ?: continue
            syntaxMode = mergeMode(syntaxMode, snapshot.syntaxSpansMode)
            appendMap(syntax, snapshot.syntaxSpans)
            semanticMode = mergeMode(semanticMode, snapshot.semanticSpansMode)
            appendMap(semantic, snapshot.semanticSpans)
            overlayMode = mergeMode(overlayMode, snapshot.overlaySpansMode)
            appendMap(overlay, snapshot.overlaySpans)
            inlayMode = mergeMode(inlayMode, snapshot.inlayHintsMode)
            appendMap(inlays, snapshot.inlayHints)
            diagnosticMode = mergeMode(diagnosticMode, snapshot.diagnosticsMode)
            appendMap(diagnostics, snapshot.diagnostics)
            highlightMode = mergeMode(highlightMode, snapshot.documentHighlightsMode)
            appendMap(highlights, snapshot.documentHighlights)
            gutterMode = mergeMode(gutterMode, snapshot.gutterIconsMode)
            appendMap(gutters, snapshot.gutterIcons)
            phantomMode = mergeMode(phantomMode, snapshot.phantomTextsMode)
            appendMap(phantoms, snapshot.phantomTexts)
            lensMode = mergeMode(lensMode, snapshot.codeLensItemsMode)
            appendMap(lenses, snapshot.codeLensItems)
            linksMode = mergeMode(linksMode, snapshot.linksMode)
            appendMap(links, snapshot.links)
            foldMode = mergeMode(foldMode, snapshot.foldRegionsMode)
            snapshot.foldRegions?.let {
                hasFolds = true
                folds += it
            }
            indentMode = mergeMode(indentMode, snapshot.indentGuidesMode)
            snapshot.indentGuides?.let {
                hasIndents = true
                indents += it
            }
            bracketMode = mergeMode(bracketMode, snapshot.bracketGuidesMode)
            snapshot.bracketGuides?.let {
                hasBrackets = true
                brackets += it
            }
            flowMode = mergeMode(flowMode, snapshot.flowGuidesMode)
            snapshot.flowGuides?.let {
                hasFlows = true
                flows += it
            }
            separatorMode = mergeMode(separatorMode, snapshot.separatorGuidesMode)
            snapshot.separatorGuides?.let {
                hasSeparators = true
                separators += it
            }
        }

        applySpanLayer(EditorSpanLayer.SYNTAX, syntaxMode, syntax)
        applySpanLayer(EditorSpanLayer.SEMANTIC, semanticMode, semantic)
        applySpanLayer(EditorSpanLayer.OVERLAY, overlayMode, overlay)
        applyLineMap(inlayMode, host::clearInlayHints, host::setBatchLineInlayHints, inlays)
        applyLineMap(diagnosticMode, host::clearDiagnostics, host::setBatchLineDiagnostics, diagnostics)
        applyLineMap(highlightMode, host::clearDocumentHighlights, host::setBatchLineDocumentHighlights, highlights)
        applyLineMap(gutterMode, host::clearGutterIcons, host::setBatchLineGutterIcons, gutters)
        applyLineMap(phantomMode, host::clearPhantomTexts, host::setBatchLinePhantomTexts, phantoms)
        applyLineMap(lensMode, host::clearCodeLens, host::setBatchLineCodeLens, lenses)
        applyLineMap(linksMode, host::clearLinks, host::setBatchLineLinks, links)
        if (hasFolds || foldMode != DecorationApplyMode.MERGE) {
            host.setFoldRegions(folds)
        }
        applyGuideList(indentMode, hasIndents, indents, host::setIndentGuides)
        applyGuideList(bracketMode, hasBrackets, brackets, host::setBracketGuides)
        applyGuideList(flowMode, hasFlows, flows, host::setFlowGuides)
        applyGuideList(separatorMode, hasSeparators, separators, host::setSeparatorGuides)
    }

    private fun <T> applyGuideList(
        mode: DecorationApplyMode,
        hasData: Boolean,
        values: List<T>,
        set: (List<T>) -> Unit,
    ) {
        if (hasData || mode != DecorationApplyMode.MERGE) {
            set(values)
        }
    }

    private fun applySpanLayer(
        layer: EditorSpanLayer,
        mode: DecorationApplyMode,
        spans: Map<Int, List<StyleSpan>>,
    ) {
        when (mode) {
            DecorationApplyMode.REPLACE_ALL -> host.clearHighlights(layer)
            DecorationApplyMode.REPLACE_RANGE -> host.setBatchLineSpans(layer, emptyRangeMap())
            DecorationApplyMode.MERGE -> Unit
        }
        host.setBatchLineSpans(layer, spans)
    }

    private fun <T> applyLineMap(
        mode: DecorationApplyMode,
        clearAll: () -> Unit,
        setBatch: (Map<Int, List<T>>) -> Unit,
        values: Map<Int, List<T>>,
    ) {
        when (mode) {
            DecorationApplyMode.REPLACE_ALL -> clearAll()
            DecorationApplyMode.REPLACE_RANGE -> setBatch(emptyRangeMap())
            DecorationApplyMode.MERGE -> Unit
        }
        setBatch(values)
    }

    private fun <T> emptyRangeMap(): Map<Int, List<T>> {
        val start = lastContext.startLine
        val end = lastContext.endLine
        if (end < start) return emptyMap()
        return (start..end).associateWith { emptyList() }
    }

    private inner class ManagedReceiver(
        private val provider: DecorationProvider,
        private val receiverGeneration: Int,
    ) : DecorationReceiver {
        private var cancelled = false

        override val isCancelled: Boolean
            get() = disposed || cancelled || receiverGeneration != generation

        override fun accept(result: DecorationResult): Boolean {
            if (isCancelled) return false
            runOnEditorThread {
                if (isCancelled) return@runOnEditorThread
                val state = states.getOrPut(provider) { ProviderState() }
                state.snapshot = mergePatch(state.snapshot, result)
                scheduleApply()
            }
            return true
        }

        fun cancel() {
            cancelled = true
        }
    }

    private class ProviderState {
        var snapshot: DecorationResult? = null
        var receiver: ManagedReceiver? = null
    }
}

internal fun mergeMode(current: DecorationApplyMode, next: DecorationApplyMode): DecorationApplyMode =
    if (priority(next) > priority(current)) next else current

private fun priority(mode: DecorationApplyMode): Int = when (mode) {
    DecorationApplyMode.MERGE -> 0
    DecorationApplyMode.REPLACE_RANGE -> 1
    DecorationApplyMode.REPLACE_ALL -> 2
}

internal fun mergePatch(current: DecorationResult?, patch: DecorationResult): DecorationResult {
    val base = current ?: DecorationResult()
    return base.copy(
        syntaxSpans = pickMap(patch.syntaxSpans, patch.syntaxSpansMode, base.syntaxSpans),
        syntaxSpansMode = pickMode(patch.syntaxSpans, patch.syntaxSpansMode, base.syntaxSpansMode),
        semanticSpans = pickMap(patch.semanticSpans, patch.semanticSpansMode, base.semanticSpans),
        semanticSpansMode = pickMode(patch.semanticSpans, patch.semanticSpansMode, base.semanticSpansMode),
        overlaySpans = pickMap(patch.overlaySpans, patch.overlaySpansMode, base.overlaySpans),
        overlaySpansMode = pickMode(patch.overlaySpans, patch.overlaySpansMode, base.overlaySpansMode),
        inlayHints = pickMap(patch.inlayHints, patch.inlayHintsMode, base.inlayHints),
        inlayHintsMode = pickMode(patch.inlayHints, patch.inlayHintsMode, base.inlayHintsMode),
        diagnostics = pickMap(patch.diagnostics, patch.diagnosticsMode, base.diagnostics),
        diagnosticsMode = pickMode(patch.diagnostics, patch.diagnosticsMode, base.diagnosticsMode),
        documentHighlights = pickMap(patch.documentHighlights, patch.documentHighlightsMode, base.documentHighlights),
        documentHighlightsMode = pickMode(patch.documentHighlights, patch.documentHighlightsMode, base.documentHighlightsMode),
        gutterIcons = pickMap(patch.gutterIcons, patch.gutterIconsMode, base.gutterIcons),
        gutterIconsMode = pickMode(patch.gutterIcons, patch.gutterIconsMode, base.gutterIconsMode),
        phantomTexts = pickMap(patch.phantomTexts, patch.phantomTextsMode, base.phantomTexts),
        phantomTextsMode = pickMode(patch.phantomTexts, patch.phantomTextsMode, base.phantomTextsMode),
        codeLensItems = pickMap(patch.codeLensItems, patch.codeLensItemsMode, base.codeLensItems),
        codeLensItemsMode = pickMode(patch.codeLensItems, patch.codeLensItemsMode, base.codeLensItemsMode),
        links = pickMap(patch.links, patch.linksMode, base.links),
        linksMode = pickMode(patch.links, patch.linksMode, base.linksMode),
        foldRegions = pickList(patch.foldRegions, patch.foldRegionsMode, base.foldRegions),
        foldRegionsMode = pickListMode(patch.foldRegions, patch.foldRegionsMode, base.foldRegionsMode),
        indentGuides = pickList(patch.indentGuides, patch.indentGuidesMode, base.indentGuides),
        indentGuidesMode = pickListMode(patch.indentGuides, patch.indentGuidesMode, base.indentGuidesMode),
        bracketGuides = pickList(patch.bracketGuides, patch.bracketGuidesMode, base.bracketGuides),
        bracketGuidesMode = pickListMode(patch.bracketGuides, patch.bracketGuidesMode, base.bracketGuidesMode),
        flowGuides = pickList(patch.flowGuides, patch.flowGuidesMode, base.flowGuides),
        flowGuidesMode = pickListMode(patch.flowGuides, patch.flowGuidesMode, base.flowGuidesMode),
        separatorGuides = pickList(patch.separatorGuides, patch.separatorGuidesMode, base.separatorGuides),
        separatorGuidesMode = pickListMode(patch.separatorGuides, patch.separatorGuidesMode, base.separatorGuidesMode),
    )
}

private fun <T> pickMap(
    patch: Map<Int, List<T>>?,
    mode: DecorationApplyMode,
    previous: Map<Int, List<T>>?,
): Map<Int, List<T>>? = when {
    patch != null -> patch
    mode != DecorationApplyMode.MERGE -> null
    else -> previous
}

private fun pickMode(
    patch: Map<Int, *>?,
    mode: DecorationApplyMode,
    previous: DecorationApplyMode,
): DecorationApplyMode = if (patch != null || mode != DecorationApplyMode.MERGE) mode else previous

private fun <T> pickList(
    patch: List<T>?,
    mode: DecorationApplyMode,
    previous: List<T>?,
): List<T>? = when {
    patch != null -> patch
    mode != DecorationApplyMode.MERGE -> null
    else -> previous
}

private fun pickListMode(
    patch: List<*>?,
    mode: DecorationApplyMode,
    previous: DecorationApplyMode,
): DecorationApplyMode = if (patch != null || mode != DecorationApplyMode.MERGE) mode else previous

private fun <T> appendMap(
    out: MutableMap<Int, MutableList<T>>,
    patch: Map<Int, List<T>>?,
) {
    if (patch == null) return
    for ((line, items) in patch) {
        out.getOrPut(line) { mutableListOf() }.addAll(items)
    }
}
