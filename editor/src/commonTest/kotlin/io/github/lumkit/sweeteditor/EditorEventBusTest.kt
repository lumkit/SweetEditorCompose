package io.github.lumkit.sweeteditor

import kotlin.test.Test
import kotlin.test.assertEquals

class EditorEventBusTest {
    @Test
    fun subscribeReceivesMatchingEventsAndCanUnsubscribe() {
        val bus = EditorEventBus()
        val seen = mutableListOf<String>()
        val cancel = bus.subscribe<ScrollChangedEvent> { seen += "${it.scrollX.toInt()},${it.scrollY.toInt()}" }
        bus.publish(ScrollChangedEvent(1f, 2f))
        bus.publish(ScaleChangedEvent(1.5f))
        cancel()
        bus.publish(ScrollChangedEvent(3f, 4f))
        assertEquals(listOf("1,2"), seen)
    }

    @Test
    fun newHostEventsAreSubscribeable() {
        val bus = EditorEventBus()
        var loaded = 0
        bus.subscribe<DocumentLoadedEvent> { loaded = it.lineCount }
        bus.publish(DocumentLoadedEvent(3))
        assertEquals(3, loaded)
    }
}
