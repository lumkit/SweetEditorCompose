package io.github.lumkit.sweeteditor.highlight.internal

import io.github.lumkit.sweeteditor.CursorChangedEvent
import io.github.lumkit.sweeteditor.DocumentLoadedEvent
import io.github.lumkit.sweeteditor.DecorationApplyMode
import io.github.lumkit.sweeteditor.DecorationContext
import io.github.lumkit.sweeteditor.DecorationResult
import io.github.lumkit.sweeteditor.DecorationType
import io.github.lumkit.sweeteditor.SweetEditorController
import io.github.lumkit.sweeteditor.TextChangedEvent
import io.github.lumkit.sweeteditor.TextPosition
import io.github.lumkit.sweeteditor.highlight.HighlightDocumentDescriptor
import io.github.lumkit.sweeteditor.highlight.HighlightFeatureFlags
import io.github.lumkit.sweeteditor.highlight.HighlightTheme
import io.github.lumkit.sweeteditor.highlight.runtime.NativeBufferParser

internal class HighlightSession(
    private val native: HighlightNativeOps,
    private val requestRefresh: () -> Unit = {},
    private val bindingId: String,
    private val tabSize: Int = 4,
    descriptor: HighlightDocumentDescriptor = HighlightDocumentDescriptor(),
    var features: HighlightFeatureFlags = HighlightFeatureFlags(),
) {
    private val mirror = TextMirror()
    private val mapping = PositionMapping(mirror)
    private val patches = ArrayDeque<PendingPatch>()
    private val subscriptions = ArrayList<() -> Unit>()
    private val analyzeQueue = SerialAnalyzeQueue { generation }
    private val styleRegistry = StyleRegistry(HighlightTheme.dark())

    private var controller: SweetEditorController? = null
    private var closed = false
    private var engine = 0L
    private var document = 0L
    private var analyzer = 0L
    private var userDisabled = false
    private var stylesRegistered = false
    private var syntaxDirty = true

    var generation: Int = 0
        private set

    var uri: String = uriFor(descriptor)
        private set

    var lastCursor: TextPosition? = null
        private set

    var disabled: Boolean
        get() = userDisabled || closed || !native.isAvailable
        set(value) {
            userDisabled = value
        }

    fun bind(controller: SweetEditorController) {
        check(!closed) { "HighlightSession is closed" }
        unbindEvents()
        this.controller = controller
        subscriptions += controller.onTextChanged(::onTextChanged)
        subscriptions += controller.onDocumentLoaded(::onDocumentLoaded)
        subscriptions += controller.onCursorChanged(::onCursorChanged)
    }

    fun start() {
        if (closed) return
        rebuildOnLoad(controller?.getDocument()?.text.orEmpty())
    }

    fun onTextChanged(event: TextChangedEvent) {
        if (closed) return
        for (change in event.changes) {
            patches += mirror.encodePatch(change, mapping)
            val dirtyFrom = change.range.start.line
            mirror.apply(change)
            mapping.invalidateFrom(dirtyFrom)
        }
        generation += 1
        syntaxDirty = true
        requestRefresh()
    }

    fun onDocumentLoaded(@Suppress("UNUSED_PARAMETER") event: DocumentLoadedEvent) {
        if (closed) return
        rebuildOnLoad(controller?.getDocument()?.text.orEmpty())
        requestRefresh()
    }

    fun onCursorChanged(event: CursorChangedEvent) {
        if (closed) return
        lastCursor = event.cursorPosition
    }

    fun rebuildOnLoad(text: String) {
        if (closed) return
        patches.clear()
        mirror.setText(text)
        mapping.invalidateFrom(0)
        generation += 1
        syntaxDirty = true
        if (!native.isAvailable) return
        ensureEngine()
        registerStylesIfNeeded()
        releaseDocument()
        val created = native.createDocument(uri, text)
        document = created
        analyzer = if (created == 0L) 0L else native.loadDocument(engine, created)
    }

    fun compileSyntaxJson(json: String) {
        if (closed || !native.isAvailable) return
        ensureEngine()
        registerStylesIfNeeded()
        native.compileJson(engine, json)
    }

    fun drainPatches(visibleStartLine: Int, visibleLineCount: Int): IntArray? {
        if (analyzer == 0L) {
            patches.clear()
            return null
        }
        var last: IntArray? = null
        while (patches.isNotEmpty()) {
            val patch = patches.removeFirst()
            last = native.analyzeIncrementalInLineRange(
                analyzer,
                patch.slStartLine,
                patch.slStartColumn,
                patch.slEndLine,
                patch.slEndColumn,
                patch.newText,
                visibleStartLine,
                visibleLineCount,
            )
        }
        return last
    }

    fun pendingPatches(): List<PendingPatch> = patches.toList()

    var decorationOverride: DecorationResult? = null

    fun decorationCapabilities(): Set<DecorationType> {
        if (disabled) return emptySet()
        val types = mutableSetOf<DecorationType>()
        if (features.syntaxHighlight) types += DecorationType.SYNTAX_HIGHLIGHT
        if (features.indentGuides) types += DecorationType.INDENT_GUIDE
        if (features.bracketGuides) types += DecorationType.BRACKET_GUIDE
        if (features.rainbowBrackets) types += DecorationType.OVERLAY_HIGHLIGHT
        return types
    }

    fun submitAnalyze(generation: Int, block: () -> Unit) {
        analyzeQueue.submit(generation, block)
    }

    fun buildDecorationResult(context: DecorationContext): DecorationResult {
        decorationOverride?.let { return it }
        val vis = context.visibleLineRange
        val start = vis.startLine
        val count = if (vis.isEmpty) 0 else vis.endLine - vis.startLine + 1
        val queueWasEmpty = patches.isEmpty()
        val incremental = drainPatches(start, count)
        val buffer = when {
            analyzer == 0L -> null
            incremental != null -> {
                syntaxDirty = false
                incremental
            }
            !syntaxDirty && queueWasEmpty -> {
                val slice = native.getHighlightSlice(analyzer, start, count)
                val parsed = NativeBufferParser.parseHighlightSlice(slice)
                if (parsed.lines.size < count) {
                    native.analyzeLineRange(analyzer, start, count)
                } else {
                    slice
                }
            }
            else -> {
                syntaxDirty = false
                native.analyzeLineRange(analyzer, start, count)
            }
        }
        val spans = if (features.syntaxHighlight) {
            mapSyntaxSpans(NativeBufferParser.parseHighlightSlice(buffer), mapping)
        } else {
            emptyMap()
        }
        val indentGuides = if (features.indentGuides && analyzer != 0L && count > 0) {
            GuideAssembler.assemble(
                guides = NativeBufferParser.parseIndentGuides(
                    native.analyzeIndentGuidesInLineRange(analyzer, start, count),
                ),
                mapping = mapping,
                visibleStartLine = vis.startLine,
                visibleEndLine = vis.endLine,
            )
        } else {
            emptyList()
        }
        return DecorationResult(
            syntaxSpans = spans,
            syntaxSpansMode = DecorationApplyMode.REPLACE_RANGE,
            indentGuides = indentGuides,
            indentGuidesMode = DecorationApplyMode.REPLACE_ALL,
            bracketGuides = emptyList(),
            bracketGuidesMode = DecorationApplyMode.REPLACE_ALL,
            overlaySpans = emptyMap(),
            overlaySpansMode = DecorationApplyMode.REPLACE_RANGE,
        )
    }

    fun close() {
        if (closed) return
        closed = true
        unbindEvents()
        patches.clear()
        releaseDocument()
        if (engine != 0L) {
            native.freeEngine(engine)
            engine = 0L
        }
        analyzeQueue.close()
    }

    private fun ensureEngine() {
        if (engine == 0L) {
            engine = native.createEngine(tabSize)
        }
    }

    private fun registerStylesIfNeeded() {
        if (stylesRegistered || engine == 0L) return
        styleRegistry.namesToRegister().forEach { (name, id) ->
            native.registerStyleName(engine, name, id)
        }
        stylesRegistered = true
    }

    private fun releaseDocument() {
        if (analyzer != 0L) {
            native.freeDocumentAnalyzer(analyzer)
            analyzer = 0L
        }
        if (uri.isNotEmpty() && engine != 0L) {
            native.removeDocument(engine, uri)
        }
        if (document != 0L) {
            native.freeDocument(document)
            document = 0L
        }
    }

    private fun unbindEvents() {
        subscriptions.forEach { unsubscribe -> unsubscribe() }
        subscriptions.clear()
    }

    private fun uriFor(descriptor: HighlightDocumentDescriptor): String =
        descriptor.fileName ?: "sweetline-compose://session/$bindingId"
}
