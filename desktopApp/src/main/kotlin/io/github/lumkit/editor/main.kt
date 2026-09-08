package io.github.lumkit.editor

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "SweetEditor",
    ) {
        App()
    }
}