package io.github.lumkit.sweeteditor.input

import androidx.compose.ui.Modifier
import androidx.compose.ui.node.ModifierNodeElement
import io.github.lumkit.sweeteditor.session.RememberedEditorSession
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGRectMake
import platform.UIKit.UIApplication
import platform.UIKit.UIView
import platform.UIKit.UIWindow

internal actual fun Modifier.editorIme(session: RememberedEditorSession): Modifier =
    this.then(EditorImeElement(session))

private data class EditorImeElement(
    val session: RememberedEditorSession,
) : ModifierNodeElement<EditorImeNode>() {
    override fun create(): EditorImeNode = EditorImeNode(session)

    override fun update(node: EditorImeNode) {
        node.bindSession(session)
    }
}

@OptIn(ExperimentalForeignApi::class)
private class EditorImeNode(
    session: RememberedEditorSession,
) : Modifier.Node() {
    var session: RememberedEditorSession = session
        private set
    private var inputView: ComposeIosTextInputView? = null
    private val onTap: () -> Unit = { showKeyboard() }

    fun bindSession(next: RememberedEditorSession) {
        if (session === next) return
        if (isAttached) {
            session.imeTapHandler = null
        }
        hideKeyboard()
        session = next
        if (isAttached) {
            session.imeTapHandler = onTap
        }
    }

    override fun onAttach() {
        session.imeTapHandler = onTap
    }

    override fun onDetach() {
        if (session.imeTapHandler === onTap) {
            session.imeTapHandler = null
        }
        hideKeyboard()
        super.onDetach()
    }

    private fun showKeyboard() {
        val host = keyWindow() ?: return
        val view = inputView ?: ComposeIosTextInputView(session).also { inputView = it }
        view.session = session
        if (view.superview == null) {
            view.setHidden(true)
            view.userInteractionEnabled = false
            view.setFrame(CGRectMake(0.0, 0.0, 1.0, 1.0))
            host.addSubview(view)
        }
        if (!view.isFirstResponder) {
            view.becomeFirstResponder()
        }
    }

    private fun hideKeyboard() {
        val view = inputView ?: return
        if (view.isFirstResponder) {
            view.resignFirstResponder()
        }
        view.removeFromSuperview()
        inputView = null
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun keyWindow(): UIView? {
    val app = UIApplication.sharedApplication
    val windows = app.windows
    for (window in windows) {
        val uiWindow = window as? UIWindow ?: continue
        if (uiWindow.isKeyWindow()) {
            return uiWindow
        }
    }
    return app.keyWindow
}
