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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CompletionProviderManagerTest {
    @Test
    fun triggerMergesAndSortsProviderItems() {
        val host = FakeCompletionHost()
        val manager = CompletionProviderManager(host)
        val seen = mutableListOf<List<CompletionItem>>()
        manager.listener = object : CompletionProviderManager.Listener {
            override fun onCompletionItemsUpdated(items: List<CompletionItem>, anchor: EditorCursorRect?) {
                seen.add(items)
            }

            override fun onCompletionDismissed() {
                seen.add(emptyList())
            }
        }
        manager.addProvider(StaticCompletionProvider(listOf(CompletionItem(label = "zeta"), CompletionItem(label = "alpha"))))
        manager.trigger(CompletionTriggerKind.INVOKED, null)

        assertTrue(seen.isNotEmpty())
        assertEquals(listOf("alpha", "zeta"), seen.last().map { it.label })
    }

    @Test
    fun dismissClearsItems() {
        val host = FakeCompletionHost()
        val manager = CompletionProviderManager(host)
        var dismissed = false
        manager.listener = object : CompletionProviderManager.Listener {
            override fun onCompletionItemsUpdated(items: List<CompletionItem>, anchor: EditorCursorRect?) = Unit
            override fun onCompletionDismissed() {
                dismissed = true
            }
        }
        manager.showItems(listOf(CompletionItem(label = "fun")))
        manager.dismiss()
        assertTrue(dismissed)
    }
}

private class StaticCompletionProvider(
    private val items: List<CompletionItem>,
) : CompletionProvider {
    override fun provideCompletions(context: CompletionContext, receiver: CompletionReceiver) {
        receiver.accept(CompletionResult(items))
    }
}

private class FakeCompletionHost : CompletionHost {
    override fun isDisposed(): Boolean = false
    override fun cursorPosition(): TextPosition = TextPosition(0, 0)
    override fun lineText(line: Int): String = "fun"
    override fun wordRangeAtCursor(): TextRange = TextRange(TextPosition(0, 0), TextPosition(0, 3))
    override fun cursorRect(): EditorCursorRect = EditorCursorRect(10f, 20f, 16f)
    override fun languageConfiguration(): LanguageConfiguration? = null
    override fun editorMetadata(): EditorMetadata? = null
}
