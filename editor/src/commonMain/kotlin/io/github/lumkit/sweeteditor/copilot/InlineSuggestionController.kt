package io.github.lumkit.sweeteditor.copilot

import io.github.lumkit.sweeteditor.EditorCursorRect
import io.github.lumkit.sweeteditor.InlineSuggestion
import io.github.lumkit.sweeteditor.InlineSuggestionListener
import io.github.lumkit.sweeteditor.core.protocol.KeyCode
import io.github.lumkit.sweeteditor.core.protocol.KeyModifier

internal interface InlineSuggestionHost {
    fun injectPhantom(suggestion: InlineSuggestion)
    fun clearPhantom()
    fun insertSuggestion(suggestion: InlineSuggestion)
    fun suggestionAnchor(suggestion: InlineSuggestion): EditorCursorRect?
}

internal class InlineSuggestionController(
    private val host: InlineSuggestionHost,
) {
    var listener: InlineSuggestionListener? = null
    var suggestion: InlineSuggestion? = null
        private set
    var overlay: EditorCursorRect? = null
        private set
    val isShowing: Boolean get() = suggestion != null

    private var suppressAutoDismiss = false
    private var disposed = false

    fun show(next: InlineSuggestion) {
        if (disposed) return
        if (suggestion != null) {
            clearQuietly()
        }
        suggestion = next
        host.injectPhantom(next)
        overlay = host.suggestionAnchor(next)
    }

    fun accept() {
        val current = suggestion ?: return
        withSuppressedAutoDismiss {
            host.clearPhantom()
            host.insertSuggestion(current)
            clearState()
        }
        if (!disposed) {
            listener?.onSuggestionAccepted(current)
        }
    }

    fun dismiss() {
        val current = suggestion ?: return
        withSuppressedAutoDismiss {
            host.clearPhantom()
            clearState()
        }
        if (!disposed) {
            listener?.onSuggestionDismissed(current)
        }
    }

    fun handleKey(keyCode: Int, modifiers: Int): Boolean {
        if (!isShowing || modifiers != KeyModifier.NONE) return false
        return when (keyCode) {
            KeyCode.TAB -> {
                accept()
                true
            }
            KeyCode.ESCAPE -> {
                dismiss()
                true
            }
            else -> false
        }
    }

    fun onTextChanged() = autoDismiss()

    fun onCursorChanged() = autoDismiss()

    fun onScrollChanged() {
        val current = suggestion ?: return
        overlay = host.suggestionAnchor(current)
    }

    fun dispose() {
        disposed = true
        listener = null
        withSuppressedAutoDismiss {
            if (suggestion != null) {
                host.clearPhantom()
            }
            clearState()
        }
    }

    private fun autoDismiss() {
        if (suppressAutoDismiss) return
        dismiss()
    }

    private fun clearQuietly() {
        withSuppressedAutoDismiss {
            host.clearPhantom()
            clearState()
        }
    }

    private fun clearState() {
        suggestion = null
        overlay = null
    }

    private inline fun withSuppressedAutoDismiss(block: () -> Unit) {
        suppressAutoDismiss = true
        try {
            block()
        } finally {
            suppressAutoDismiss = false
        }
    }
}
