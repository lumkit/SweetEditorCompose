package io.github.lumkit.sweeteditor.core

import io.github.lumkit.sweeteditor.internal.jni.NativeBridge
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EditorQueryJniTest {
    @Test
    fun cursorRectVisibleRangeAndScrollMetricsAfterLayout() {
        assertTrue(NativeBridge.isAvailable)
        val host = HostTextMeasurer(charWidth = 8f, ascent = 12f, descent = 4f)
        val document = Document.fromUtf8("hello\nworld\n!")
        val editor = EditorCore.create(host)
        try {
            assertTrue(editor.setDocument(document)?.handled == true)
            assertTrue(editor.setViewport(400, 300)?.handled == true)
            editor.buildRenderModel()

            val cursor = editor.getCursorRect()
            assertTrue(cursor.height > 0f)

            val origin = editor.getPositionRect(0, 0)
            assertTrue(origin.height > 0f)

            val visible = editor.getVisibleLineRange()
            assertEquals(0, visible.startLine)
            assertTrue(!visible.isEmpty)
            assertTrue(visible.endLine >= visible.startLine)

            val metrics = editor.getScrollMetrics()
            assertTrue(metrics != null)
            assertEquals(400f, metrics!!.viewportWidth)
            assertEquals(300f, metrics.viewportHeight)
            assertTrue(metrics.scale > 0f)
        } finally {
            editor.close()
            document.close()
        }
    }
}
