package io.github.lumkit.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
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
import io.github.lumkit.sweeteditor.SweetEditor
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
            "Open the editor, then go back. The native session is created only on the editor page and released when you leave.",
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
    DisposableEffect(controller) {
        onDispose { controller.dispose() }
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
        SweetEditor(
            modifier = Modifier.fillMaxSize(),
            controller = controller,
        )
    }
}
