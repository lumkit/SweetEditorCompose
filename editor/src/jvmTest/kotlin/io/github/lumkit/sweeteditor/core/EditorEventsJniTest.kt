package io.github.lumkit.sweeteditor.core

import io.github.lumkit.sweeteditor.TextChangeKind
import io.github.lumkit.sweeteditor.TextChangedEvent
import io.github.lumkit.sweeteditor.collectStateEvents
import io.github.lumkit.sweeteditor.internal.jni.NativeBridge
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EditorEventsJniTest {
    @Test
    fun insertTextProducesTextChangedEventFromActionResult() {
        assertTrue(NativeBridge.isAvailable)
        val host = HostTextMeasurer(charWidth = 8f, ascent = 12f, descent = 4f)
        val document = Document.fromUtf8("")
        val editor = EditorCore.create(host)
        try {
            assertTrue(editor.setDocument(document)?.handled == true)
            assertTrue(editor.setViewport(400, 300)?.handled == true)
            val result = editor.insertText("hello")
            assertTrue(result?.handled == true)
            val events = collectStateEvents(result)
            val text = events.filterIsInstance<TextChangedEvent>().single()
            assertEquals(TextChangeKind.INSERTION, text.kind)
            assertEquals("hello", text.changes.single().newText)
            assertTrue(events.any { it is io.github.lumkit.sweeteditor.CursorChangedEvent })
        } finally {
            editor.close()
            document.close()
        }
    }
}
