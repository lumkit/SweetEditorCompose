package io.github.lumkit.editor

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.lumkit.sweeteditor.AutoIndentMode
import io.github.lumkit.sweeteditor.CodeLensItem
import io.github.lumkit.sweeteditor.CompletionContext
import io.github.lumkit.sweeteditor.CompletionItem
import io.github.lumkit.sweeteditor.CompletionItemKind
import io.github.lumkit.sweeteditor.CompletionProvider
import io.github.lumkit.sweeteditor.CompletionReceiver
import io.github.lumkit.sweeteditor.CompletionResult
import io.github.lumkit.sweeteditor.EditorTextEdit
import io.github.lumkit.sweeteditor.CurrentLineRenderMode
import io.github.lumkit.sweeteditor.DecorationApplyMode
import io.github.lumkit.sweeteditor.DecorationContext
import io.github.lumkit.sweeteditor.DecorationProvider
import io.github.lumkit.sweeteditor.DecorationReceiver
import io.github.lumkit.sweeteditor.DecorationResult
import io.github.lumkit.sweeteditor.DecorationType
import io.github.lumkit.sweeteditor.Diagnostic
import io.github.lumkit.sweeteditor.DocumentHighlight
import io.github.lumkit.sweeteditor.EditorDiagnosticSeverity
import io.github.lumkit.sweeteditor.EditorDocumentHighlightKind
import io.github.lumkit.sweeteditor.EditorFontStyle
import io.github.lumkit.sweeteditor.FlowGuide
import io.github.lumkit.sweeteditor.FoldRegion
import io.github.lumkit.sweeteditor.IndentGuide
import io.github.lumkit.sweeteditor.BracketGuide
import io.github.lumkit.sweeteditor.EditorIconProvider
import io.github.lumkit.sweeteditor.SeparatorGuide
import io.github.lumkit.sweeteditor.SeparatorStyle
import io.github.lumkit.sweeteditor.TextPosition
import io.github.lumkit.sweeteditor.EditorInlayType
import io.github.lumkit.sweeteditor.EditorKeyMap
import io.github.lumkit.sweeteditor.EditorMetadata
import io.github.lumkit.sweeteditor.LanguageConfiguration
import io.github.lumkit.sweeteditor.NewLineAction
import io.github.lumkit.sweeteditor.NewLineActionProvider
import io.github.lumkit.sweeteditor.NewLineContext
import io.github.lumkit.sweeteditor.EditorSearchStatus
import io.github.lumkit.sweeteditor.EditorSettings
import io.github.lumkit.sweeteditor.EditorTextStyle
import io.github.lumkit.sweeteditor.EditorTheme
import io.github.lumkit.sweeteditor.GutterIcon
import io.github.lumkit.sweeteditor.InlayHint
import io.github.lumkit.sweeteditor.LinkSpan
import io.github.lumkit.sweeteditor.PhantomText
import io.github.lumkit.sweeteditor.StyleSpan
import io.github.lumkit.sweeteditor.SweetEditor
import io.github.lumkit.sweeteditor.SweetEditorController
import io.github.lumkit.sweeteditor.WhitespaceRenderMode
import io.github.lumkit.sweeteditor.WrapMode
import io.github.lumkit.sweeteditor.rememberSweetEditorController

private val SampleSource = buildString {
    appendLine("fun main() {")
    repeat(80) { index ->
        appendLine("    println(\"Hello, SweetEditor! #$index\")")
    }
    appendLine("}")
}

private enum class DemoPage {
    Home,
    Editor,
}

private enum class KeyMapPreset {
    Vscode,
    Jetbrains,
    Sublime,
}

@Composable
@Preview
fun App() {
    MaterialTheme {
        var page by remember { mutableStateOf(DemoPage.Home) }
        when (page) {
            DemoPage.Home -> DemoHome(onOpenEditor = { page = DemoPage.Editor })
            DemoPage.Editor -> DemoEditor(onBack = { page = DemoPage.Home })
        }
    }
}

@Composable
private fun DemoHome(onOpenEditor: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("SweetEditor demo", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Editor page creates the native session and releases it on Back. " +
                "The toolbar exercises settings, keymap, clipboard, line commands, events, and decoration writes.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Button(onClick = onOpenEditor) {
            Text("Open editor")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DemoEditor(onBack: () -> Unit) {
    val controller = rememberSweetEditorController(SampleSource)
    var darkTheme by remember { mutableStateOf(true) }
    var wrapMode by remember { mutableStateOf(WrapMode.NONE) }
    var gutterVisible by remember { mutableStateOf(true) }
    var readOnly by remember { mutableStateOf(false) }
    var renderWhitespace by remember { mutableStateOf(WhitespaceRenderMode.NONE) }
    var renderLineBreaks by remember { mutableStateOf(false) }
    var scale by remember { mutableStateOf(1f) }
    var keyMapPreset by remember { mutableStateOf(KeyMapPreset.Vscode) }
    var status by remember { mutableStateOf("waiting for editor…") }
    val decorationProvider = remember { DemoDecorationProvider() }
    val completionProvider = remember { DemoCompletionProvider() }
    val newLineProvider = remember { DemoNewLineProvider() }

    val theme = if (darkTheme) EditorTheme.dark() else EditorTheme.light()
    val settings = EditorSettings(
        wrapMode = wrapMode,
        scale = scale,
        readOnly = readOnly,
        gutterVisible = gutterVisible,
        currentLineRenderMode = CurrentLineRenderMode.BACKGROUND,
        renderWhitespace = renderWhitespace,
        renderLineBreaks = renderLineBreaks,
        autoIndentMode = AutoIndentMode.KEEP_INDENT,
    )
    val keyMap = remember(keyMapPreset) {
        when (keyMapPreset) {
            KeyMapPreset.Vscode -> EditorKeyMap.vscode()
            KeyMapPreset.Jetbrains -> EditorKeyMap.jetbrains()
            KeyMapPreset.Sublime -> EditorKeyMap.sublime()
        }
    }

    DisposableEffect(controller) {
        val unsubs = mutableListOf<() -> Unit>()
        controller.whenReady {
            registerSampleStyles(controller)
            controller.setMetadata(DemoFileMetadata("sample.kt"))
            controller.setLanguageConfiguration(demoLanguageConfiguration())
            controller.setEditorIconProvider(DemoIconProvider())
            controller.addDecorationProvider(decorationProvider)
            controller.addCompletionProvider(completionProvider)
            controller.addNewLineActionProvider(newLineProvider)
            refreshStatus(controller) { status = it }
            unsubs += controller.onTextChanged { refreshStatus(controller) { status = it } }
            unsubs += controller.onCursorChanged { refreshStatus(controller) { status = it } }
            unsubs += controller.onSelectionChanged { refreshStatus(controller) { status = it } }
            unsubs += controller.onScrollChanged { refreshStatus(controller) { status = it } }
            unsubs += controller.onScaleChanged { event ->
                scale = event.scale
                refreshStatus(controller) { status = it }
            }
        }
        onDispose {
            unsubs.forEach { it() }
            controller.dispose()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding(),
    ) {
        TopAppBar(
            title = { Text("Editor") },
            navigationIcon = {
                TextButton(onClick = onBack) {
                    Text("Back")
                }
            },
        )
        DemoToolbar(
            darkTheme = darkTheme,
            onToggleTheme = { darkTheme = !darkTheme },
            wrapMode = wrapMode,
            onCycleWrap = {
                wrapMode = when (wrapMode) {
                    WrapMode.NONE -> WrapMode.WORD_BREAK
                    WrapMode.WORD_BREAK -> WrapMode.CHAR_BREAK
                    WrapMode.CHAR_BREAK -> WrapMode.NONE
                }
            },
            gutterVisible = gutterVisible,
            onToggleGutter = { gutterVisible = !gutterVisible },
            readOnly = readOnly,
            onToggleReadOnly = { readOnly = !readOnly },
            renderWhitespace = renderWhitespace,
            onCycleWhitespace = {
                renderWhitespace = when (renderWhitespace) {
                    WhitespaceRenderMode.NONE -> WhitespaceRenderMode.ALL
                    WhitespaceRenderMode.ALL -> WhitespaceRenderMode.BOUNDARY
                    WhitespaceRenderMode.BOUNDARY -> WhitespaceRenderMode.SELECTION
                    WhitespaceRenderMode.SELECTION -> WhitespaceRenderMode.TRAILING
                    WhitespaceRenderMode.TRAILING -> WhitespaceRenderMode.NONE
                }
            },
            renderLineBreaks = renderLineBreaks,
            onToggleLineBreaks = { renderLineBreaks = !renderLineBreaks },
            onZoomOut = { scale = (scale - 0.1f).coerceAtLeast(0.5f) },
            onZoomIn = { scale = (scale + 0.1f).coerceAtMost(3f) },
            keyMapPreset = keyMapPreset,
            onKeyMapPreset = { keyMapPreset = it },
            onUndo = controller::undo,
            onRedo = controller::redo,
            onCopy = { controller.copy() },
            onCut = { controller.cut() },
            onPaste = controller::paste,
            onMoveLineUp = controller::moveLineUp,
            onMoveLineDown = controller::moveLineDown,
            onCopyLineUp = controller::copyLineUp,
            onCopyLineDown = controller::copyLineDown,
            onDeleteLine = controller::deleteLine,
            onInsertAbove = controller::insertLineAbove,
            onInsertBelow = controller::insertLineBelow,
            onFindHello = {
                controller.search("Hello")
                refreshStatus(controller) { status = it }
            },
            onFindNext = {
                controller.findNextSearchMatch()
                refreshStatus(controller) { status = it }
            },
            onFindPrev = {
                controller.findPreviousSearchMatch()
                refreshStatus(controller) { status = it }
            },
            onReplaceHi = {
                controller.replaceCurrentSearchMatch("Hi")
                refreshStatus(controller) { status = it }
            },
            onReplaceAllHi = {
                controller.replaceAllSearchMatches("Hi")
                refreshStatus(controller) { status = it }
            },
            onClearSearch = {
                controller.clearSearch()
                refreshStatus(controller) { status = it }
            },
            onApplyDecorations = {
                registerSampleStyles(controller)
                controller.addDecorationProvider(decorationProvider)
                controller.requestDecorationRefresh()
            },
            onClearDecorations = {
                controller.removeDecorationProvider(decorationProvider)
                controller.clearAllDecorations()
            },
            onTriggerCompletion = controller::triggerCompletion,
            onFoldAll = controller::foldAll,
            onUnfoldAll = controller::unfoldAll,
            onToggleFold = {
                val line = controller.getCursorPosition()?.line ?: 0
                controller.toggleFold(line)
            },
            onMatchBrackets = { controller.setMatchedBrackets(0, 11, 81, 0) },
            onClearMatchedBrackets = controller::clearMatchedBrackets,
            onComputeDiff = { controller.computeDiff(SampleSource.replace("Hello", "Hi")) },
            onClearDiff = controller::clearDiff,
        )
        Text(
            text = status,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            maxLines = 2,
        )
        SweetEditor(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            controller = controller,
            theme = theme,
            settings = settings,
            keyMap = keyMap,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DemoToolbar(
    darkTheme: Boolean,
    onToggleTheme: () -> Unit,
    wrapMode: WrapMode,
    onCycleWrap: () -> Unit,
    gutterVisible: Boolean,
    onToggleGutter: () -> Unit,
    readOnly: Boolean,
    onToggleReadOnly: () -> Unit,
    renderWhitespace: WhitespaceRenderMode,
    onCycleWhitespace: () -> Unit,
    renderLineBreaks: Boolean,
    onToggleLineBreaks: () -> Unit,
    onZoomOut: () -> Unit,
    onZoomIn: () -> Unit,
    keyMapPreset: KeyMapPreset,
    onKeyMapPreset: (KeyMapPreset) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onCopy: () -> Unit,
    onCut: () -> Unit,
    onPaste: () -> Unit,
    onMoveLineUp: () -> Unit,
    onMoveLineDown: () -> Unit,
    onCopyLineUp: () -> Unit,
    onCopyLineDown: () -> Unit,
    onDeleteLine: () -> Unit,
    onInsertAbove: () -> Unit,
    onInsertBelow: () -> Unit,
    onFindHello: () -> Unit,
    onFindNext: () -> Unit,
    onFindPrev: () -> Unit,
    onReplaceHi: () -> Unit,
    onReplaceAllHi: () -> Unit,
    onClearSearch: () -> Unit,
    onApplyDecorations: () -> Unit,
    onClearDecorations: () -> Unit,
    onTriggerCompletion: () -> Unit,
    onFoldAll: () -> Unit,
    onUnfoldAll: () -> Unit,
    onToggleFold: () -> Unit,
    onMatchBrackets: () -> Unit,
    onClearMatchedBrackets: () -> Unit,
    onComputeDiff: () -> Unit,
    onClearDiff: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .focusProperties { canFocus = false },
    ) {
        DemoChipRow {
            FilterChip(selected = darkTheme, onClick = onToggleTheme, label = { Text(if (darkTheme) "Dark" else "Light") })
            FilterChip(selected = wrapMode != WrapMode.NONE, onClick = onCycleWrap, label = { Text("Wrap ${wrapMode.name}") })
            FilterChip(selected = gutterVisible, onClick = onToggleGutter, label = { Text("Gutter") })
            FilterChip(selected = readOnly, onClick = onToggleReadOnly, label = { Text("ReadOnly") })
            FilterChip(
                selected = renderWhitespace != WhitespaceRenderMode.NONE,
                onClick = onCycleWhitespace,
                label = { Text("WS ${renderWhitespace.name}") },
            )
            FilterChip(selected = renderLineBreaks, onClick = onToggleLineBreaks, label = { Text("¶") })
            TextButton(onClick = onZoomOut) { Text("A-") }
            TextButton(onClick = onZoomIn) { Text("A+") }
            KeyMapPreset.entries.forEach { preset ->
                FilterChip(
                    selected = keyMapPreset == preset,
                    onClick = { onKeyMapPreset(preset) },
                    label = { Text(preset.name) },
                )
            }
        }
        DemoChipRow {
            TextButton(onClick = onUndo) { Text("Undo") }
            TextButton(onClick = onRedo) { Text("Redo") }
            TextButton(onClick = onCopy) { Text("Copy") }
            TextButton(onClick = onCut) { Text("Cut") }
            TextButton(onClick = onPaste) { Text("Paste") }
            TextButton(onClick = onMoveLineUp) { Text("Line↑") }
            TextButton(onClick = onMoveLineDown) { Text("Line↓") }
            TextButton(onClick = onCopyLineUp) { Text("Dup↑") }
            TextButton(onClick = onCopyLineDown) { Text("Dup↓") }
            TextButton(onClick = onDeleteLine) { Text("Del line") }
            TextButton(onClick = onInsertAbove) { Text("+above") }
            TextButton(onClick = onInsertBelow) { Text("+below") }
            TextButton(onClick = onApplyDecorations) { Text("Decorate") }
            TextButton(onClick = onClearDecorations) { Text("Clear dec") }
            TextButton(onClick = onFindHello) { Text("Find Hello") }
            TextButton(onClick = onFindPrev) { Text("Find↑") }
            TextButton(onClick = onFindNext) { Text("Find↓") }
            TextButton(onClick = onReplaceHi) { Text("Replace Hi") }
            TextButton(onClick = onReplaceAllHi) { Text("Replace all Hi") }
            TextButton(onClick = onClearSearch) { Text("Clear find") }
            TextButton(onClick = onTriggerCompletion) { Text("Complete") }
            TextButton(onClick = onFoldAll) { Text("Fold all") }
            TextButton(onClick = onUnfoldAll) { Text("Unfold") }
            TextButton(onClick = onToggleFold) { Text("Toggle fold") }
            TextButton(onClick = onMatchBrackets) { Text("Match {}") }
            TextButton(onClick = onClearMatchedBrackets) { Text("Clear match") }
            TextButton(onClick = onComputeDiff) { Text("Diff") }
            TextButton(onClick = onClearDiff) { Text("Clear diff") }
        }
    }
}

@Composable
private fun DemoChipRow(content: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        content()
    }
}

private fun registerSampleStyles(controller: SweetEditorController) {
    controller.registerBatchTextStyles(
        mapOf(
            1 to EditorTextStyle(color = 0xFF569CD6.toInt(), fontStyle = EditorFontStyle.BOLD),
            2 to EditorTextStyle(color = 0xFFCE9178.toInt()),
        ),
    )
    controller.setMaxGutterIcons(1)
}

private data class DemoFileMetadata(val path: String) : EditorMetadata

private class DemoIconProvider : EditorIconProvider {
    private val icon = makeDotIcon(Color(0xFF4EA1FF))
    override fun getIcon(iconId: Int): ImageBitmap? = if (iconId == 1) icon else null
}

private fun makeDotIcon(color: Color): ImageBitmap {
    val bitmap = ImageBitmap(16, 16)
    val canvas = Canvas(bitmap)
    val paint = Paint().apply {
        this.color = color
        isAntiAlias = true
    }
    canvas.drawCircle(Offset(8f, 8f), 6f, paint)
    return bitmap
}

private fun demoLanguageConfiguration(): LanguageConfiguration =
    LanguageConfiguration.builder("kotlin")
        .addBracket("(", ")")
        .addBracket("{", "}")
        .addBracket("[", "]")
        .addAutoClosingPair("(", ")")
        .addAutoClosingPair("{", "}")
        .addAutoClosingPair("[", "]")
        .addAutoClosingPair("\"", "\"")
        .setTabSize(4)
        .setInsertSpaces(true)
        .build()

private class DemoNewLineProvider : NewLineActionProvider {
    override fun provideNewLineAction(context: NewLineContext): NewLineAction? {
        val column = context.column.coerceIn(0, context.lineText.length)
        val before = context.lineText.take(column)
        val indent = before.takeWhile { it == ' ' || it == '\t' }
        val extra = if (before.trimEnd().endsWith("{") || before.trimEnd().endsWith("(")) {
            "    "
        } else {
            return null
        }
        return NewLineAction("\n$indent$extra")
    }
}

private class DemoCompletionProvider : CompletionProvider {
    private val catalog = listOf(
        CompletionItem(label = "fun", kind = CompletionItemKind.KEYWORD, insertText = "fun ", detail = "keyword"),
        CompletionItem(label = "println", kind = CompletionItemKind.FUNCTION, insertText = "println()", detail = "print line"),
        CompletionItem(label = "main", kind = CompletionItemKind.FUNCTION, insertText = "main", detail = "entry"),
        CompletionItem(label = "return", kind = CompletionItemKind.KEYWORD, insertText = "return ", detail = "keyword"),
    )

    override fun isTriggerCharacter(ch: String): Boolean = ch == "."

    override fun provideCompletions(context: CompletionContext, receiver: CompletionReceiver) {
        val prefix = context.prefix()
        val items = catalog
            .filter { it.matchText.startsWith(prefix, ignoreCase = true) }
            .map { item ->
                val text = item.insertText ?: item.label
                item.copy(textEdit = EditorTextEdit(context.wordRange, text))
            }
        receiver.accept(CompletionResult(items))
    }

    private fun CompletionContext.prefix(): String {
        if (cursorPosition.line != wordRange.start.line) return ""
        val start = wordRange.start.column.coerceAtLeast(0)
        val end = cursorPosition.column.coerceAtLeast(start)
        if (start > lineText.length) return ""
        return lineText.substring(start, end.coerceAtMost(lineText.length))
    }
}

private class DemoDecorationProvider : DecorationProvider {
    override fun capabilities(): Set<DecorationType> = setOf(
        DecorationType.SYNTAX_HIGHLIGHT,
        DecorationType.INLAY_HINT,
        DecorationType.PHANTOM_TEXT,
        DecorationType.GUTTER_ICON,
        DecorationType.CODELENS,
        DecorationType.LINK,
        DecorationType.DIAGNOSTIC,
        DecorationType.DOCUMENT_HIGHLIGHT,
        DecorationType.FOLD_REGION,
        DecorationType.INDENT_GUIDE,
        DecorationType.BRACKET_GUIDE,
        DecorationType.FLOW_GUIDE,
        DecorationType.SEPARATOR_GUIDE,
    )

    override fun provideDecorations(context: DecorationContext, receiver: DecorationReceiver) {
        receiver.accept(
            DecorationResult(
                syntaxSpans = mapOf(
                    0 to listOf(StyleSpan(column = 0, length = 3, styleId = 1)),
                    1 to listOf(
                        StyleSpan(column = 4, length = 7, styleId = 1),
                        StyleSpan(column = 12, length = 26, styleId = 2),
                    ),
                ),
                syntaxSpansMode = DecorationApplyMode.REPLACE_RANGE,
                inlayHints = mapOf(0 to listOf(InlayHint(type = EditorInlayType.TEXT, column = 11, text = ": Unit"))),
                inlayHintsMode = DecorationApplyMode.REPLACE_RANGE,
                phantomTexts = mapOf(0 to listOf(PhantomText(column = 13, text = " // entry"))),
                phantomTextsMode = DecorationApplyMode.REPLACE_RANGE,
                gutterIcons = mapOf(0 to listOf(GutterIcon(1))),
                gutterIconsMode = DecorationApplyMode.REPLACE_RANGE,
                codeLensItems = mapOf(0 to listOf(CodeLensItem(column = 0, commandId = 1, text = "run"))),
                codeLensItemsMode = DecorationApplyMode.REPLACE_RANGE,
                links = mapOf(1 to listOf(LinkSpan(column = 12, length = 26, target = "https://github.com/lumkit/SweetEditorCompose"))),
                linksMode = DecorationApplyMode.REPLACE_RANGE,
                diagnostics = mapOf(
                    1 to listOf(Diagnostic(column = 4, length = 7, severity = EditorDiagnosticSeverity.WARNING)),
                ),
                diagnosticsMode = DecorationApplyMode.REPLACE_RANGE,
                documentHighlights = mapOf(
                    1 to listOf(DocumentHighlight(column = 4, length = 7, kind = EditorDocumentHighlightKind.READ)),
                ),
                documentHighlightsMode = DecorationApplyMode.REPLACE_RANGE,
                foldRegions = listOf(FoldRegion(startLine = 0, endLine = 81, collapsed = false)),
                foldRegionsMode = DecorationApplyMode.REPLACE_ALL,
                indentGuides = listOf(
                    IndentGuide(start = TextPosition(0, 4), end = TextPosition(80, 4)),
                ),
                indentGuidesMode = DecorationApplyMode.REPLACE_ALL,
                bracketGuides = listOf(
                    BracketGuide(
                        parent = TextPosition(0, 11),
                        end = TextPosition(81, 0),
                        children = listOf(TextPosition(1, 4)),
                    ),
                ),
                bracketGuidesMode = DecorationApplyMode.REPLACE_ALL,
                flowGuides = listOf(
                    FlowGuide(start = TextPosition(1, 4), end = TextPosition(2, 4)),
                ),
                flowGuidesMode = DecorationApplyMode.REPLACE_ALL,
                separatorGuides = listOf(
                    SeparatorGuide(line = 40, style = SeparatorStyle.SINGLE, count = 2, textEndColumn = 4),
                ),
                separatorGuidesMode = DecorationApplyMode.REPLACE_ALL,
            ),
        )
    }
}

private fun refreshStatus(controller: SweetEditorController, publish: (String) -> Unit) {
    val cursor = controller.getCursorRect()
    val visible = controller.getVisibleLineRange()
    val metrics = controller.getScrollMetrics()
    val selected = controller.getSelectedText()
    publish(
        buildString {
            append("sel=${selected.length}")
            if (cursor != null) {
                append(" cursor=(${cursor.x.toInt()},${cursor.y.toInt()})")
            }
            if (visible != null && !visible.isEmpty) {
                append(" lines=${visible.startLine + 1}-${visible.endLine + 1}")
            }
            if (metrics != null) {
                append(" scroll=${metrics.scrollY.toInt()} scale=${metrics.scale}")
            }
            val link = controller.getLinkTargetAt(1, 14)
            if (link.isNotEmpty()) {
                append(" link")
            }
            val language = controller.getLanguageConfiguration()?.languageId
            if (!language.isNullOrEmpty()) {
                append(" lang=$language")
            }
            val meta = controller.getMetadata() as? DemoFileMetadata
            if (meta != null) {
                append(" file=${meta.path}")
            }
            val search = controller.getSearchState()
            if (search != null && search.status != EditorSearchStatus.INACTIVE) {
                append(" find=${search.pattern} ${search.currentIndex + 1}/${search.matchCount}")
            }
        },
    )
}
