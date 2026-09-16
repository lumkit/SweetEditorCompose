package io.github.lumkit.editor

import androidx.compose.ui.uikit.OnFocusBehavior
import androidx.compose.ui.window.ComposeUIViewController

fun MainViewController() = ComposeUIViewController(
    configure = {
        // A full-screen editor must not pan the scene when the software keyboard opens.
        onFocusBehavior = OnFocusBehavior.DoNothing
    },
) {
    App()
}
