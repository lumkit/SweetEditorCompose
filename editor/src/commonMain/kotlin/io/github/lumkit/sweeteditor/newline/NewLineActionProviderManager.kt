package io.github.lumkit.sweeteditor.newline

import io.github.lumkit.sweeteditor.EditorMetadata
import io.github.lumkit.sweeteditor.LanguageConfiguration
import io.github.lumkit.sweeteditor.NewLineAction
import io.github.lumkit.sweeteditor.NewLineActionProvider
import io.github.lumkit.sweeteditor.NewLineContext
import io.github.lumkit.sweeteditor.TextPosition

internal interface NewLineHost {
    fun isDisposed(): Boolean
    fun cursorPosition(): TextPosition?
    fun lineText(line: Int): String
    fun languageConfiguration(): LanguageConfiguration?
    fun editorMetadata(): EditorMetadata?
}

internal class NewLineActionProviderManager(
    private val host: NewLineHost,
) {
    private val providers = mutableListOf<NewLineActionProvider>()
    private var disposed = false

    fun addProvider(provider: NewLineActionProvider) {
        if (disposed || providers.contains(provider)) return
        providers += provider
    }

    fun removeProvider(provider: NewLineActionProvider) {
        if (disposed) return
        providers.remove(provider)
    }

    fun provideNewLineAction(): NewLineAction? {
        if (disposed || host.isDisposed() || providers.isEmpty()) return null
        val cursor = host.cursorPosition() ?: return null
        val context = NewLineContext(
            lineNumber = cursor.line,
            column = cursor.column,
            lineText = host.lineText(cursor.line),
            languageConfiguration = host.languageConfiguration(),
            editorMetadata = host.editorMetadata(),
        )
        for (provider in providers) {
            val action = provider.provideNewLineAction(context) ?: continue
            if (action.text.isNotEmpty()) return action
        }
        return null
    }

    fun close() {
        disposed = true
        providers.clear()
    }
}
