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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.lumkit.sweeteditor.AutoIndentMode
import io.github.lumkit.sweeteditor.CodeLensItem
import io.github.lumkit.sweeteditor.CurrentLineRenderMode
import io.github.lumkit.sweeteditor.Diagnostic
import io.github.lumkit.sweeteditor.DocumentHighlight
import io.github.lumkit.sweeteditor.EditorDiagnosticSeverity
import io.github.lumkit.sweeteditor.EditorDocumentHighlightKind
import io.github.lumkit.sweeteditor.EditorFontStyle
import io.github.lumkit.sweeteditor.EditorInlayType
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
import io.github.lumkit.sweeteditor.SweetEditor
import io.github.lumkit.sweeteditor.SweetEditorController
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

private val LightEditorTheme = EditorTheme(
    backgroundColor = 0xFFFFFFFF.toInt(),
    textColor = 0xFF1E1E1E.toInt(),
    cursorColor = 0xFF1E1E1E.toInt(),
    currentLineColor = 0xFFF3F6FB.toInt(),
    lineNumberColor = 0xFF8A93A3.toInt(),
    currentLineNumberColor = 0xFF3D4F6F.toInt(),
    splitLineColor = 0x33202838,
    scrollbarTrackColor = 0x22000000,
    scrollbarThumbColor = 0x66858585,
    scrollbarThumbActiveColor = 0xFF7A7A7A.toInt(),
    selectionColor = 0x664C9AFF,
    linkColor = 0xFF0B67D3.toInt(),
)

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
    var scale by remember { mutableStateOf(1f) }
    var keyMapPreset by remember { mutableStateOf(KeyMapPreset.Vscode) }
    var status by remember { mutableStateOf("waiting for editor…") }

    val theme = if (darkTheme) EditorTheme() else LightEditorTheme
    val settings = EditorSettings(
        wrapMode = wrapMode,
        scale = scale,
        readOnly = readOnly,
        gutterVisible = gutterVisible,
        currentLineRenderMode = CurrentLineRenderMode.BACKGROUND,
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
            applySampleDecorations(controller)
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
            onApplyDecorations = { applySampleDecorations(controller) },
            onClearDecorations = controller::clearAllDecorations,
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
            modifier = Modifier.fillMaxSize(),
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
    onApplyDecorations: () -> Unit,
    onClearDecorations: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        DemoChipRow {
            FilterChip(selected = darkTheme, onClick = onToggleTheme, label = { Text(if (darkTheme) "Dark" else "Light") })
            FilterChip(selected = wrapMode != WrapMode.NONE, onClick = onCycleWrap, label = { Text("Wrap ${wrapMode.name}") })
            FilterChip(selected = gutterVisible, onClick = onToggleGutter, label = { Text("Gutter") })
            FilterChip(selected = readOnly, onClick = onToggleReadOnly, label = { Text("ReadOnly") })
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

private fun applySampleDecorations(controller: SweetEditorController) {
    controller.registerBatchTextStyles(
        mapOf(
            1 to EditorTextStyle(color = 0xFF569CD6.toInt(), fontStyle = EditorFontStyle.BOLD),
            2 to EditorTextStyle(color = 0xFFCE9178.toInt()),
        ),
    )
    controller.setLineSpans(
        line = 0,
        layer = EditorSpanLayer.SYNTAX,
        spans = listOf(StyleSpan(column = 0, length = 3, styleId = 1)),
    )
    controller.setBatchLineSpans(
        layer = EditorSpanLayer.SYNTAX,
        spansByLine = mapOf(
            1 to listOf(
                StyleSpan(column = 4, length = 7, styleId = 1),
                StyleSpan(column = 12, length = 26, styleId = 2),
            ),
        ),
    )
    controller.setLineInlayHints(
        0,
        listOf(InlayHint(type = EditorInlayType.TEXT, column = 11, text = ": Unit")),
    )
    controller.setLinePhantomTexts(0, listOf(PhantomText(column = 13, text = " // entry")))
    controller.setMaxGutterIcons(1)
    controller.setLineGutterIcons(0, listOf(GutterIcon(1)))
    controller.setLineCodeLens(0, listOf(CodeLensItem(column = 0, commandId = 1, text = "run")))
    controller.setLineLinks(1, listOf(LinkSpan(column = 12, length = 26, target = "https://github.com/lumkit/SweetEditorCompose")))
    controller.setLineDiagnostics(
        1,
        listOf(Diagnostic(column = 4, length = 7, severity = EditorDiagnosticSeverity.WARNING)),
    )
    controller.setLineDocumentHighlights(
        1,
        listOf(DocumentHighlight(column = 4, length = 7, kind = EditorDocumentHighlightKind.READ)),
    )
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
        },
    )
}
