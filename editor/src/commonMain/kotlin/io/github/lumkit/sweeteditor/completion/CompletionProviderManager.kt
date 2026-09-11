package io.github.lumkit.sweeteditor.completion

import io.github.lumkit.sweeteditor.CompletionContext
import io.github.lumkit.sweeteditor.CompletionItem
import io.github.lumkit.sweeteditor.CompletionProvider
import io.github.lumkit.sweeteditor.CompletionReceiver
import io.github.lumkit.sweeteditor.CompletionResult
import io.github.lumkit.sweeteditor.CompletionTriggerKind
import io.github.lumkit.sweeteditor.EditorCursorRect
import io.github.lumkit.sweeteditor.EditorMetadata
import io.github.lumkit.sweeteditor.LanguageConfiguration
import io.github.lumkit.sweeteditor.TextPosition
import io.github.lumkit.sweeteditor.TextRange
import io.github.lumkit.sweeteditor.internal.runOnEditorThread

internal interface CompletionHost {
    fun isDisposed(): Boolean
    fun cursorPosition(): TextPosition?
    fun lineText(line: Int): String
    fun wordRangeAtCursor(): TextRange
    fun cursorRect(): EditorCursorRect?
    fun languageConfiguration(): LanguageConfiguration?
    fun editorMetadata(): EditorMetadata?
}

internal class CompletionProviderManager(
    private val host: CompletionHost,
) {
    private val providers = mutableListOf<CompletionProvider>()
    private val activeReceivers = mutableMapOf<CompletionProvider, ManagedReceiver>()
    private val mergedItems = mutableListOf<CompletionItem>()
    private var generation = 0
    private var disposed = false
    var listener: Listener? = null

    interface Listener {
        fun onCompletionItemsUpdated(items: List<CompletionItem>, anchor: EditorCursorRect?)
        fun onCompletionDismissed()
    }

    fun addProvider(provider: CompletionProvider) {
        runOnEditorThread {
            if (disposed || providers.contains(provider)) return@runOnEditorThread
            providers += provider
        }
    }

    fun removeProvider(provider: CompletionProvider) {
        runOnEditorThread {
            if (disposed) return@runOnEditorThread
            providers.remove(provider)
            activeReceivers.remove(provider)?.cancel()
        }
    }

    fun isTriggerCharacter(ch: String): Boolean {
        if (disposed) return false
        return providers.any { it.isTriggerCharacter(ch) }
    }

    fun trigger(kind: CompletionTriggerKind, triggerCharacter: String?) {
        runOnEditorThread { executeRefresh(kind, triggerCharacter) }
    }

    fun showItems(items: List<CompletionItem>) {
        runOnEditorThread {
            if (disposed) return@runOnEditorThread
            generation++
            cancelAllReceivers()
            mergedItems.clear()
            mergedItems += items
            publishItems()
        }
    }

    fun dismiss() {
        runOnEditorThread {
            if (disposed) return@runOnEditorThread
            generation++
            cancelAllReceivers()
            mergedItems.clear()
            listener?.onCompletionDismissed()
        }
    }

    fun close() {
        runOnEditorThread {
            if (disposed) return@runOnEditorThread
            disposed = true
            generation++
            cancelAllReceivers()
            providers.clear()
            mergedItems.clear()
            listener = null
        }
    }

    private fun executeRefresh(kind: CompletionTriggerKind, triggerCharacter: String?) {
        if (disposed || providers.isEmpty()) return
        val currentGen = ++generation
        cancelAllReceivers()
        mergedItems.clear()
        val context = buildContext(kind, triggerCharacter)
        if (context == null) {
            listener?.onCompletionDismissed()
            return
        }
        for (provider in providers) {
            val receiver = ManagedReceiver(currentGen)
            activeReceivers[provider] = receiver
            runCatching { provider.provideCompletions(context, receiver) }
        }
    }

    private fun buildContext(
        kind: CompletionTriggerKind,
        triggerCharacter: String?,
    ): CompletionContext? {
        if (host.isDisposed()) return null
        val cursor = host.cursorPosition() ?: return null
        return CompletionContext(
            triggerKind = kind,
            triggerCharacter = triggerCharacter,
            cursorPosition = cursor,
            lineText = host.lineText(cursor.line),
            wordRange = host.wordRangeAtCursor(),
            languageConfiguration = host.languageConfiguration(),
            editorMetadata = host.editorMetadata(),
        )
    }

    private fun onProviderResult(result: CompletionResult, receiverGeneration: Int) {
        if (disposed || receiverGeneration != generation) return
        mergedItems += result.items
        mergedItems.sortBy { it.sortKey ?: it.label }
        publishItems()
    }

    private fun publishItems() {
        if (mergedItems.isEmpty()) {
            listener?.onCompletionDismissed()
        } else {
            listener?.onCompletionItemsUpdated(mergedItems.toList(), host.cursorRect())
        }
    }

    private fun cancelAllReceivers() {
        activeReceivers.values.forEach { it.cancel() }
        activeReceivers.clear()
    }

    private inner class ManagedReceiver(
        private val receiverGeneration: Int,
    ) : CompletionReceiver {
        private var cancelled = false

        fun cancel() {
            cancelled = true
        }

        override val isCancelled: Boolean
            get() = disposed || cancelled || receiverGeneration != generation

        override fun accept(result: CompletionResult): Boolean {
            if (isCancelled) return false
            runOnEditorThread { onProviderResult(result, receiverGeneration) }
            return true
        }
    }
}
