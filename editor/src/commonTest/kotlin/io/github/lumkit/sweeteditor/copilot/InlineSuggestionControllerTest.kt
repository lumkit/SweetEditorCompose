package io.github.lumkit.sweeteditor.copilot

import io.github.lumkit.sweeteditor.EditorCursorRect
import io.github.lumkit.sweeteditor.InlineSuggestion
import io.github.lumkit.sweeteditor.InlineSuggestionListener
import io.github.lumkit.sweeteditor.core.protocol.KeyCode
import io.github.lumkit.sweeteditor.core.protocol.KeyModifier
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InlineSuggestionControllerTest {
    @Test
    fun showInjectsPhantomAndAcceptInsertsOnce() {
        val host = RecordingHost()
        val controller = InlineSuggestionController(host)
        val events = RecordingListener()
        controller.listener = events
        val suggestion = InlineSuggestion(1, 4, "world")
        controller.show(suggestion)
        assertTrue(controller.isShowing)
        assertEquals(listOf("inject:1:4:world"), host.ops)
        controller.accept()
        assertFalse(controller.isShowing)
        assertEquals(listOf("inject:1:4:world", "clear", "insert:1:4:world"), host.ops)
        assertEquals(listOf("accepted:world"), events.events)
        controller.accept()
        assertEquals(listOf("accepted:world"), events.events)
    }

    @Test
    fun dismissNotifiesOnceAndReplacementIsQuiet() {
        val host = RecordingHost()
        val controller = InlineSuggestionController(host)
        val events = RecordingListener()
        controller.listener = events
        controller.show(InlineSuggestion(0, 0, "one"))
        controller.show(InlineSuggestion(0, 1, "two"))
        assertEquals(listOf("inject:0:0:one", "clear", "inject:0:1:two"), host.ops)
        assertTrue(events.events.isEmpty())
        controller.dismiss()
        assertEquals(listOf("dismissed:two"), events.events)
        controller.dismiss()
        assertEquals(listOf("dismissed:two"), events.events)
    }

    @Test
    fun textOrCursorChangeDismissesButScrollDoesNot() {
        val host = RecordingHost()
        val controller = InlineSuggestionController(host)
        val events = RecordingListener()
        controller.listener = events
        controller.show(InlineSuggestion(0, 0, "hint"))
        controller.onScrollChanged()
        assertTrue(controller.isShowing)
        assertEquals(2, host.anchorReads)
        controller.onTextChanged()
        assertFalse(controller.isShowing)
        assertEquals(listOf("dismissed:hint"), events.events)
        controller.show(InlineSuggestion(0, 0, "again"))
        controller.onCursorChanged()
        assertFalse(controller.isShowing)
    }

    @Test
    fun tabAcceptsAndEscapeDismisses() {
        val host = RecordingHost()
        val controller = InlineSuggestionController(host)
        val events = RecordingListener()
        controller.listener = events
        controller.show(InlineSuggestion(0, 0, "ok"))
        assertTrue(controller.handleKey(KeyCode.TAB, KeyModifier.NONE))
        assertEquals(listOf("accepted:ok"), events.events)
        controller.show(InlineSuggestion(0, 0, "no"))
        assertTrue(controller.handleKey(KeyCode.ESCAPE, KeyModifier.NONE))
        assertEquals(listOf("accepted:ok", "dismissed:no"), events.events)
        assertFalse(controller.handleKey(KeyCode.TAB, KeyModifier.NONE))
    }

    @Test
    fun disposeClearsWithoutCallback() {
        val host = RecordingHost()
        val controller = InlineSuggestionController(host)
        val events = RecordingListener()
        controller.listener = events
        controller.show(InlineSuggestion(0, 0, "gone"))
        controller.dispose()
        assertFalse(controller.isShowing)
        assertNull(controller.listener)
        assertTrue(events.events.isEmpty())
    }
}

private class RecordingHost : InlineSuggestionHost {
    val ops = mutableListOf<String>()
    var anchorReads = 0

    override fun injectPhantom(suggestion: InlineSuggestion) {
        ops += "inject:${suggestion.line}:${suggestion.column}:${suggestion.text}"
    }

    override fun clearPhantom() {
        ops += "clear"
    }

    override fun insertSuggestion(suggestion: InlineSuggestion) {
        ops += "insert:${suggestion.line}:${suggestion.column}:${suggestion.text}"
    }

    override fun suggestionAnchor(suggestion: InlineSuggestion): EditorCursorRect? {
        anchorReads += 1
        return EditorCursorRect(1f, 2f, 16f)
    }
}

private class RecordingListener : InlineSuggestionListener {
    val events = mutableListOf<String>()

    override fun onSuggestionAccepted(suggestion: InlineSuggestion) {
        events += "accepted:${suggestion.text}"
    }

    override fun onSuggestionDismissed(suggestion: InlineSuggestion) {
        events += "dismissed:${suggestion.text}"
    }
}
