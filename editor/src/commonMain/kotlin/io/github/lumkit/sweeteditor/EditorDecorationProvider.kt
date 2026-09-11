package io.github.lumkit.sweeteditor

enum class DecorationType {
    SYNTAX_HIGHLIGHT,
    SEMANTIC_HIGHLIGHT,
    OVERLAY_HIGHLIGHT,
    INLAY_HINT,
    DIAGNOSTIC,
    DOCUMENT_HIGHLIGHT,
    FOLD_REGION,
    INDENT_GUIDE,
    BRACKET_GUIDE,
    FLOW_GUIDE,
    SEPARATOR_GUIDE,
    GUTTER_ICON,
    PHANTOM_TEXT,
    CODELENS,
    LINK,
}

enum class DecorationApplyMode {
    MERGE,
    REPLACE_ALL,
    REPLACE_RANGE,
}

data class DecorationContext(
    val visibleLineRange: VisibleLineRange,
    val totalLineCount: Int,
    val textChanges: List<TextChange>,
    val languageConfiguration: LanguageConfiguration? = null,
    val editorMetadata: EditorMetadata? = null,
)

interface DecorationReceiver {
    fun accept(result: DecorationResult): Boolean
    val isCancelled: Boolean
}

interface DecorationProvider {
    fun capabilities(): Set<DecorationType> = DecorationType.entries.toSet()
    fun getCapabilities(): Set<DecorationType> = capabilities()
    fun provideDecorations(context: DecorationContext, receiver: DecorationReceiver)
}

data class DecorationResult(
    val syntaxSpans: Map<Int, List<StyleSpan>>? = null,
    val semanticSpans: Map<Int, List<StyleSpan>>? = null,
    val overlaySpans: Map<Int, List<StyleSpan>>? = null,
    val inlayHints: Map<Int, List<InlayHint>>? = null,
    val diagnostics: Map<Int, List<Diagnostic>>? = null,
    val documentHighlights: Map<Int, List<DocumentHighlight>>? = null,
    val foldRegions: List<FoldRegion>? = null,
    val indentGuides: List<IndentGuide>? = null,
    val bracketGuides: List<BracketGuide>? = null,
    val flowGuides: List<FlowGuide>? = null,
    val separatorGuides: List<SeparatorGuide>? = null,
    val gutterIcons: Map<Int, List<GutterIcon>>? = null,
    val phantomTexts: Map<Int, List<PhantomText>>? = null,
    val codeLensItems: Map<Int, List<CodeLensItem>>? = null,
    val links: Map<Int, List<LinkSpan>>? = null,
    val syntaxSpansMode: DecorationApplyMode = DecorationApplyMode.MERGE,
    val semanticSpansMode: DecorationApplyMode = DecorationApplyMode.MERGE,
    val overlaySpansMode: DecorationApplyMode = DecorationApplyMode.MERGE,
    val inlayHintsMode: DecorationApplyMode = DecorationApplyMode.MERGE,
    val diagnosticsMode: DecorationApplyMode = DecorationApplyMode.MERGE,
    val documentHighlightsMode: DecorationApplyMode = DecorationApplyMode.MERGE,
    val foldRegionsMode: DecorationApplyMode = DecorationApplyMode.MERGE,
    val indentGuidesMode: DecorationApplyMode = DecorationApplyMode.MERGE,
    val bracketGuidesMode: DecorationApplyMode = DecorationApplyMode.MERGE,
    val flowGuidesMode: DecorationApplyMode = DecorationApplyMode.MERGE,
    val separatorGuidesMode: DecorationApplyMode = DecorationApplyMode.MERGE,
    val gutterIconsMode: DecorationApplyMode = DecorationApplyMode.MERGE,
    val phantomTextsMode: DecorationApplyMode = DecorationApplyMode.MERGE,
    val codeLensItemsMode: DecorationApplyMode = DecorationApplyMode.MERGE,
    val linksMode: DecorationApplyMode = DecorationApplyMode.MERGE,
)
