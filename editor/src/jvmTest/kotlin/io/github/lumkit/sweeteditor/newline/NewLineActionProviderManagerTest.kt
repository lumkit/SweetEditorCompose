package io.github.lumkit.sweeteditor.newline

import io.github.lumkit.sweeteditor.EditorMetadata
import io.github.lumkit.sweeteditor.LanguageConfiguration
import io.github.lumkit.sweeteditor.NewLineAction
import io.github.lumkit.sweeteditor.TextPosition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NewLineActionProviderManagerTest {
    @Test
    fun firstNonNullActionWins() {
        val manager = NewLineActionProviderManager(FakeNewLineHost())
        manager.addProvider { null }
        manager.addProvider { NewLineAction("\n    ") }
        manager.addProvider { NewLineAction("\nignored") }
        assertEquals("\n    ", manager.provideNewLineAction()?.text)
    }

    @Test
    fun allNullFallsBackToDefault() {
        val manager = NewLineActionProviderManager(FakeNewLineHost())
        manager.addProvider { null }
        assertNull(manager.provideNewLineAction())
    }

    @Test
    fun emptyTextIsSkipped() {
        val manager = NewLineActionProviderManager(FakeNewLineHost())
        manager.addProvider { NewLineAction("") }
        manager.addProvider { NewLineAction("\n") }
        assertEquals("\n", manager.provideNewLineAction()?.text)
    }
}

private class FakeNewLineHost : NewLineHost {
    override fun isDisposed(): Boolean = false
    override fun cursorPosition(): TextPosition = TextPosition(0, 3)
    override fun lineText(line: Int): String = "fun"
    override fun languageConfiguration(): LanguageConfiguration? = null
    override fun editorMetadata(): EditorMetadata? = null
}
