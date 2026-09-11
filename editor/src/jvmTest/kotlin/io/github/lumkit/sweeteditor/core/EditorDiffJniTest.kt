package io.github.lumkit.sweeteditor.core

import io.github.lumkit.sweeteditor.DiffChange
import io.github.lumkit.sweeteditor.EditorTheme
import io.github.lumkit.sweeteditor.core.protocol.CoreProtocol
import io.github.lumkit.sweeteditor.core.protocol.VisualLineKind
import io.github.lumkit.sweeteditor.internal.jni.NativeBridge
import io.github.lumkit.sweeteditor.toRenderColors
import kotlin.test.Test
import kotlin.test.assertTrue

class EditorDiffJniTest {
    @Test
    fun computeDiffMarksAddedAndRemovedLines() {
        assertTrue(NativeBridge.isAvailable)
        val host = HostTextMeasurer(charWidth = 8f, ascent = 12f, descent = 4f)
        val document = Document.fromUtf8("line1\nline2\nline3\n")
        val editor = EditorCore.create(host)
        try {
            assertTrue(editor.setDocument(document)?.handled == true)
            assertTrue(editor.setViewport(400, 300)?.handled == true)
            assertTrue(editor.onFontMetricsChanged()?.handled == true)
            assertTrue(applyRenderColors(editor))
            assertTrue(editor.computeDiff("line1\nOLD\nline3\n")?.handled == true)

            val model = editor.buildRenderModel()
            assertTrue(model != null)
            assertTrue(model.lines.any { it.kind == VisualLineKind.REMOVED })
            assertTrue(model.lines.any { it.kind == VisualLineKind.CONTENT && it.lineBackgroundColor != 0 })

            assertTrue(editor.clearDiff()?.handled == true)
            val cleared = editor.buildRenderModel()
            assertTrue(cleared != null)
            assertTrue(cleared.lines.none { it.kind == VisualLineKind.REMOVED })
            assertTrue(cleared.lines.none { it.lineBackgroundColor != 0 })
        } finally {
            editor.close()
            document.close()
        }
    }

    @Test
    fun setDiffChangesIsHandled() {
        assertTrue(NativeBridge.isAvailable)
        val host = HostTextMeasurer(charWidth = 8f, ascent = 12f, descent = 4f)
        val document = Document.fromUtf8("line1\nline2\n")
        val editor = EditorCore.create(host)
        try {
            assertTrue(editor.setDocument(document)?.handled == true)
            assertTrue(editor.setViewport(400, 300)?.handled == true)
            assertTrue(
                editor.setDiffChanges(
                    listOf(
                        DiffChange(
                            currentStartLine = 1,
                            currentLineCount = 1,
                            originalStartLine = 1,
                            removedLines = listOf("OLD"),
                        ),
                    ),
                )?.handled == true,
            )
        } finally {
            editor.close()
            document.close()
        }
    }

    private fun applyRenderColors(editor: EditorCore): Boolean =
        editor.setEditorRenderColors(
            CoreProtocol.encodeEditorRenderColors(EditorTheme().toRenderColors()),
        )?.handled == true
}