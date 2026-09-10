package io.github.lumkit.editor

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import io.github.lumkit.sweeteditor.SweetEditor
import io.github.lumkit.sweeteditor.rememberSweetEditorController

private val SampleSource = buildString {
    appendLine("fun main() {")
    repeat(80) { index ->
        appendLine("    println(\"Hello, SweetEditor! #$index\")")
    }
    appendLine("}")
}

@Composable
@Preview
fun App() {
    MaterialTheme {
        val controller = rememberSweetEditorController(SampleSource)
        SweetEditor(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding(),
            controller = controller,
        )
    }
}
