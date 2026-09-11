package io.github.lumkit.sweeteditor.core

import io.github.lumkit.sweeteditor.FoldArrowMode
import io.github.lumkit.sweeteditor.FoldRegion
import io.github.lumkit.sweeteditor.core.protocol.FoldState
import io.github.lumkit.sweeteditor.internal.jni.NativeBridge
import kotlin.test.Test
import kotlin.test.assertTrue

class EditorFoldJniTest {
    @Test
    fun foldRegionsToggleAndHideInnerLines() {
        assertTrue(NativeBridge.isAvailable)
        val host = HostTextMeasurer(charWidth = 8f, ascent = 12f, descent = 4f)
        val document = Document.fromUtf8("fun main() {\n    a\n    b\n}\n")
        val editor = EditorCore.create(host)
        try {
            assertTrue(editor.setDocument(document)?.handled == true)
            assertTrue(editor.setViewport(400, 300)?.handled == true)
            assertTrue(editor.setFoldArrowMode(FoldArrowMode.ALWAYS.value)?.handled == true)
            assertTrue(editor.setFoldRegions(listOf(FoldRegion(0, 3, collapsed = false)))?.handled == true)

            val expanded = editor.buildRenderModel()
            assertTrue(expanded != null)
            assertTrue(expanded.foldMarkers.any { it.logicalLine == 0 && it.foldState == FoldState.EXPANDED })
            assertTrue(editor.isLineVisible(1))

            assertTrue(editor.foldAll()?.handled == true)
            assertTrue(!editor.isLineVisible(1))
            assertTrue(!editor.isLineVisible(2))
            assertTrue(editor.isLineVisible(0))
            val collapsed = editor.buildRenderModel()
            assertTrue(collapsed != null)
            assertTrue(collapsed.foldMarkers.any { it.logicalLine == 0 && it.foldState == FoldState.COLLAPSED })

            assertTrue(editor.unfoldAll()?.handled == true)
            assertTrue(editor.isLineVisible(1))
            assertTrue(editor.toggleFold(0)?.handled == true)
            assertTrue(!editor.isLineVisible(2))
            assertTrue(editor.unfoldAt(0)?.handled == true)
            assertTrue(editor.isLineVisible(2))
        } finally {
            editor.close()
            document.close()
        }
    }
}
