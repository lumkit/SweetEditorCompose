package io.github.lumkit.sweeteditor.core

import io.github.lumkit.sweeteditor.EditorTheme
import io.github.lumkit.sweeteditor.TabStopGroup
import io.github.lumkit.sweeteditor.TextPosition
import io.github.lumkit.sweeteditor.TextRange
import io.github.lumkit.sweeteditor.core.protocol.CoreProtocol
import io.github.lumkit.sweeteditor.core.protocol.RangeEffectKind
import io.github.lumkit.sweeteditor.internal.jni.NativeBridge
import io.github.lumkit.sweeteditor.toRangeEffectStyles
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EditorSnippetJniTest {
    @Test
    fun insertSnippetStartsLinkedEditing() {
        assertTrue(NativeBridge.isAvailable)
        val host = HostTextMeasurer(charWidth = 8f, ascent = 12f, descent = 4f)
        val document = Document.fromUtf8("")
        val editor = EditorCore.create(host)
        try {
            assertTrue(editor.setDocument(document)?.handled == true)
            assertTrue(editor.setViewport(400, 300)?.handled == true)
            assertTrue(editor.onFontMetricsChanged()?.handled == true)
            assertTrue(applyRangeStyles(editor))
            assertTrue(editor.insertSnippet("hello \${1:world}\$0")?.handled == true)
            assertEquals("hello world", document.utf8Text())
            assertTrue(editor.isInLinkedEditing())

            val model = editor.buildRenderModel()
            assertTrue(model != null)
            assertTrue(
                model.rangeEffects.any {
                    it.kind == RangeEffectKind.LINKED_EDITING_ACTIVE ||
                        it.kind == RangeEffectKind.LINKED_EDITING_INACTIVE
                },
            )

            assertTrue(editor.linkedEditingNext()?.handled == true)
            assertTrue(editor.cancelLinkedEditing()?.handled == true)
            assertTrue(!editor.isInLinkedEditing())
        } finally {
            editor.close()
            document.close()
        }
    }

    @Test
    fun startLinkedEditingSelectsExistingRange() {
        assertTrue(NativeBridge.isAvailable)
        val host = HostTextMeasurer(charWidth = 8f, ascent = 12f, descent = 4f)
        val document = Document.fromUtf8("hello world")
        val editor = EditorCore.create(host)
        try {
            assertTrue(editor.setDocument(document)?.handled == true)
            assertTrue(editor.setViewport(400, 300)?.handled == true)
            assertTrue(editor.onFontMetricsChanged()?.handled == true)
            assertTrue(applyRangeStyles(editor))
            val hello = TextRange(TextPosition(0, 0), TextPosition(0, 5))
            val world = TextRange(TextPosition(0, 6), TextPosition(0, 11))
            assertTrue(
                editor.startLinkedEditing(
                    listOf(
                        TabStopGroup(index = 1, ranges = listOf(hello), defaultText = "hello"),
                        TabStopGroup(index = 2, ranges = listOf(world), defaultText = "world"),
                    ),
                )?.handled == true,
            )
            assertTrue(editor.isInLinkedEditing())
            assertTrue(editor.linkedEditingNext()?.handled == true)
            assertTrue(editor.linkedEditingPrev()?.handled == true)
            assertTrue(editor.cancelLinkedEditing()?.handled == true)
            assertTrue(!editor.isInLinkedEditing())
        } finally {
            editor.close()
            document.close()
        }
    }

    private fun applyRangeStyles(editor: EditorCore): Boolean =
        editor.setEditorRangeEffectStyles(
            CoreProtocol.encodeEditorRangeEffectStyles(EditorTheme().toRangeEffectStyles()),
        )?.handled == true
}
