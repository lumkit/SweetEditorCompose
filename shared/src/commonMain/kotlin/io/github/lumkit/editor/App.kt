package io.github.lumkit.editor

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.lumkit.editor.generated.resources.Res
import io.github.lumkit.sweeteditor.AutoIndentMode
import io.github.lumkit.sweeteditor.CompletionContext
import io.github.lumkit.sweeteditor.CompletionItem
import io.github.lumkit.sweeteditor.CompletionItemKind
import io.github.lumkit.sweeteditor.CompletionProvider
import io.github.lumkit.sweeteditor.CompletionReceiver
import io.github.lumkit.sweeteditor.CompletionResult
import io.github.lumkit.sweeteditor.CurrentLineRenderMode
import io.github.lumkit.sweeteditor.EditorKeyMap
import io.github.lumkit.sweeteditor.EditorMetadata
import io.github.lumkit.sweeteditor.EditorSearchStatus
import io.github.lumkit.sweeteditor.EditorSettings
import io.github.lumkit.sweeteditor.EditorTextEdit
import io.github.lumkit.sweeteditor.EditorTheme
import io.github.lumkit.sweeteditor.LanguageConfiguration
import io.github.lumkit.sweeteditor.SweetEditor
import io.github.lumkit.sweeteditor.SweetEditorController
import io.github.lumkit.sweeteditor.WhitespaceRenderMode
import io.github.lumkit.sweeteditor.WrapMode
import io.github.lumkit.sweeteditor.highlight.HighlightDocumentDescriptor
import io.github.lumkit.sweeteditor.highlight.HighlightFeatureFlags
import io.github.lumkit.sweeteditor.highlight.HighlightTheme
import io.github.lumkit.sweeteditor.highlight.SweetLineHighlight
import io.github.lumkit.sweeteditor.highlight.SweetLineHighlightConfig
import io.github.lumkit.sweeteditor.rememberSweetEditorController
import org.jetbrains.compose.resources.ExperimentalResourceApi

private enum class DemoSample(
    val fileName: String,
    val languageId: String,
    val syntaxName: String,
    val tabSize: Int,
) {
    Java("example.java", "java", "java", 4),
    Kotlin("example.kt", "kotlin", "kotlin", 4),
    Lua("example.lua", "lua", "lua", 2),
    Cpp("gc.cpp", "cpp", "cpp", 4),
    ;

    val descriptor: HighlightDocumentDescriptor
        get() = HighlightDocumentDescriptor(
            fileName = fileName,
            languageId = languageId,
            syntaxName = syntaxName,
        )
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
        DemoEditor()
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalResourceApi::class)
@Composable
private fun DemoEditor() {
    val controller = rememberSweetEditorController()
    var sample by remember { mutableStateOf(DemoSample.Kotlin) }
    var darkTheme by remember { mutableStateOf(true) }
    var wrapMode by remember { mutableStateOf(WrapMode.NONE) }
    var gutterVisible by remember { mutableStateOf(true) }
    var readOnly by remember { mutableStateOf(false) }
    var renderWhitespace by remember { mutableStateOf(WhitespaceRenderMode.NONE) }
    var renderLineBreaks by remember { mutableStateOf(false) }
    var scale by remember { mutableStateOf(1f) }
    var keyMapPreset by remember { mutableStateOf(KeyMapPreset.Vscode) }
    var status by remember { mutableStateOf("loading sample…") }
    var luaSyntaxJson by remember { mutableStateOf<String?>(null) }
    val completionProvider = remember { DemoCompletionProvider() }
    val highlight = remember {
        SweetLineHighlight(
            SweetLineHighlightConfig(
                document = DemoSample.Kotlin.descriptor,
                features = HighlightFeatureFlags(
                    syntaxHighlight = true,
                    indentGuides = false,
                    bracketGuides = false,
                    matchedBrackets = false,
                    rainbowBrackets = false,
                ),
            ),
        )
    }

    val theme = if (darkTheme) EditorTheme.dark() else EditorTheme.light()
    LaunchedEffect(darkTheme) {
        highlight.updateTheme(if (darkTheme) HighlightTheme.dark() else HighlightTheme.light())
    }
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
            controller.setMetadata(DemoFileMetadata(sample.fileName))
            controller.setLanguageConfiguration(languageConfiguration(sample))
            controller.addCompletionProvider(completionProvider)
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

    DisposableEffect(controller, highlight) {
        val binding = highlight.bind(controller)
        onDispose {
            binding.close()
        }
    }

    LaunchedEffect(sample) {
        if (sample == DemoSample.Lua && luaSyntaxJson == null) {
            luaSyntaxJson = Res.readBytes("files/syntaxes/lua.json").decodeToString()
        }
        val luaJson = luaSyntaxJson
        if (sample == DemoSample.Lua && luaJson != null) {
            highlight.registerSyntaxJson(luaJson)
        }
        highlight.updateDocument(sample.descriptor)
        val text = Res.readBytes("files/samples/${sample.fileName}").decodeToString()
        controller.setMetadata(DemoFileMetadata(sample.fileName))
        controller.setLanguageConfiguration(languageConfiguration(sample))
        controller.loadDocument(text)
        refreshStatus(controller) { status = it }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding(),
    ) {
        TopAppBar(title = { Text("SweetLine highlight") })
        DemoChipRow {
            DemoSample.entries.forEach { item ->
                FilterChip(
                    selected = sample == item,
                    onClick = { sample = item },
                    label = { Text(item.fileName) },
                )
            }
        }
        DemoChipRow {
            FilterChip(
                selected = darkTheme,
                onClick = { darkTheme = !darkTheme },
                label = { Text(if (darkTheme) "Dark" else "Light") },
            )
            FilterChip(
                selected = wrapMode != WrapMode.NONE,
                onClick = {
                    wrapMode = when (wrapMode) {
                        WrapMode.NONE -> WrapMode.WORD_BREAK
                        WrapMode.WORD_BREAK -> WrapMode.CHAR_BREAK
                        WrapMode.CHAR_BREAK -> WrapMode.NONE
                    }
                },
                label = { Text("Wrap ${wrapMode.name}") },
            )
            FilterChip(
                selected = gutterVisible,
                onClick = { gutterVisible = !gutterVisible },
                label = { Text("Gutter") },
            )
            FilterChip(selected = readOnly, onClick = { readOnly = !readOnly }, label = { Text("ReadOnly") })
            FilterChip(
                selected = renderWhitespace != WhitespaceRenderMode.NONE,
                onClick = {
                    renderWhitespace = when (renderWhitespace) {
                        WhitespaceRenderMode.NONE -> WhitespaceRenderMode.ALL
                        WhitespaceRenderMode.ALL -> WhitespaceRenderMode.BOUNDARY
                        WhitespaceRenderMode.BOUNDARY -> WhitespaceRenderMode.SELECTION
                        WhitespaceRenderMode.SELECTION -> WhitespaceRenderMode.TRAILING
                        WhitespaceRenderMode.TRAILING -> WhitespaceRenderMode.NONE
                    }
                },
                label = { Text("WS ${renderWhitespace.name}") },
            )
            FilterChip(
                selected = renderLineBreaks,
                onClick = { renderLineBreaks = !renderLineBreaks },
                label = { Text("¶") },
            )
            TextButton(onClick = { scale = (scale - 0.1f).coerceAtLeast(0.5f) }) { Text("A-") }
            TextButton(onClick = { scale = (scale + 0.1f).coerceAtMost(3f) }) { Text("A+") }
            KeyMapPreset.entries.forEach { preset ->
                FilterChip(
                    selected = keyMapPreset == preset,
                    onClick = { keyMapPreset = preset },
                    label = { Text(preset.name) },
                )
            }
        }
        DemoChipRow {
            TextButton(onClick = controller::undo) { Text("Undo") }
            TextButton(onClick = controller::redo) { Text("Redo") }
            TextButton(onClick = { controller.copy() }) { Text("Copy") }
            TextButton(onClick = { controller.cut() }) { Text("Cut") }
            TextButton(onClick = controller::paste) { Text("Paste") }
            TextButton(onClick = controller::moveLineUp) { Text("Line↑") }
            TextButton(onClick = controller::moveLineDown) { Text("Line↓") }
            TextButton(onClick = controller::copyLineUp) { Text("Dup↑") }
            TextButton(onClick = controller::copyLineDown) { Text("Dup↓") }
            TextButton(onClick = controller::deleteLine) { Text("Del line") }
            TextButton(onClick = controller::insertLineAbove) { Text("+above") }
            TextButton(onClick = controller::insertLineBelow) { Text("+below") }
            TextButton(
                onClick = {
                    controller.search("Hello")
                    refreshStatus(controller) { status = it }
                },
            ) { Text("Find Hello") }
            TextButton(
                onClick = {
                    controller.findPreviousSearchMatch()
                    refreshStatus(controller) { status = it }
                },
            ) { Text("Find↑") }
            TextButton(
                onClick = {
                    controller.findNextSearchMatch()
                    refreshStatus(controller) { status = it }
                },
            ) { Text("Find↓") }
            TextButton(
                onClick = {
                    controller.replaceCurrentSearchMatch("Hi")
                    refreshStatus(controller) { status = it }
                },
            ) { Text("Replace Hi") }
            TextButton(
                onClick = {
                    controller.replaceAllSearchMatches("Hi")
                    refreshStatus(controller) { status = it }
                },
            ) { Text("Replace all Hi") }
            TextButton(
                onClick = {
                    controller.clearSearch()
                    refreshStatus(controller) { status = it }
                },
            ) { Text("Clear find") }
            TextButton(onClick = controller::triggerCompletion) { Text("Complete") }
            TextButton(onClick = controller::foldAll) { Text("Fold all") }
            TextButton(onClick = controller::unfoldAll) { Text("Unfold") }
            TextButton(
                onClick = {
                    val line = controller.getCursorPosition()?.line ?: 0
                    controller.toggleFold(line)
                },
            ) { Text("Toggle fold") }
            TextButton(
                onClick = {
                    controller.computeDiff(controller.getDocument().text.replace("Hello", "Hi"))
                },
            ) { Text("Diff") }
            TextButton(onClick = controller::clearDiff) { Text("Clear diff") }
            TextButton(onClick = { controller.insertSnippet("println(\"\${1:Hello}, \${2:World}!\")\$0") }) { Text("Snippet") }
            TextButton(onClick = controller::linkedEditingNext) { Text("Link next") }
            TextButton(onClick = controller::linkedEditingPrev) { Text("Link prev") }
            TextButton(onClick = controller::cancelLinkedEditing) { Text("Cancel link") }
        }
        Text(
            text = status,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .horizontalScroll(rememberScrollState()),
            maxLines = 1,
        )
        SweetEditor(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .imePadding(),
            controller = controller,
            theme = theme,
            settings = settings,
            keyMap = keyMap,
        )
    }
}

@Composable
private fun DemoChipRow(content: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp)
            .focusProperties { canFocus = false },
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        content()
    }
}

private data class DemoFileMetadata(val path: String) : EditorMetadata

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

private fun languageConfiguration(sample: DemoSample): LanguageConfiguration =
    LanguageConfiguration.builder(sample.languageId)
        .addBracket("(", ")")
        .addBracket("{", "}")
        .addBracket("[", "]")
        .addAutoClosingPair("(", ")")
        .addAutoClosingPair("{", "}")
        .addAutoClosingPair("[", "]")
        .addAutoClosingPair("\"", "\"")
        .setTabSize(sample.tabSize)
        .setInsertSpaces(true)
        .build()

private fun refreshStatus(controller: SweetEditorController, publish: (String) -> Unit) {
    val cursor = controller.getCursorRect()
    val visible = controller.getVisibleLineRange()
    val metrics = controller.getScrollMetrics()
    publish(
        buildString {
            val meta = controller.getMetadata() as? DemoFileMetadata
            if (meta != null) {
                append(meta.path)
            }
            val language = controller.getLanguageConfiguration()?.languageId
            if (!language.isNullOrEmpty()) {
                append(" lang=$language")
            }
            if (cursor != null) {
                append(" cursor=(${cursor.x.toInt()},${cursor.y.toInt()})")
            }
            if (visible != null && !visible.isEmpty) {
                append(" lines=${visible.startLine + 1}-${visible.endLine + 1}")
            }
            if (metrics != null) {
                append(" scale=${metrics.scale}")
            }
            val search = controller.getSearchState()
            if (search != null && search.status != EditorSearchStatus.INACTIVE) {
                append(" find=${search.pattern} ${search.currentIndex + 1}/${search.matchCount}")
            }
        },
    )
}
