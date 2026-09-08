package io.github.lumkit.editor

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import io.github.lumkit.sweeteditor.SweetEditor
import io.github.lumkit.sweeteditor.rememberSweetEditorController

private val SampleSource = """
    fun main() {
        println("Hello, SweetEditor!")
    }
""".trimIndent()

@Composable
@Preview
fun App() {
    MaterialTheme {
        val controller = rememberSweetEditorController(SampleSource)
        SweetEditor(
            modifier = Modifier
                .fillMaxSize()
                .safeContentPadding(),
            controller = controller,
        )
    }
}
