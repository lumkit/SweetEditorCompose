package io.github.lumkit.sweeteditor.core

import io.github.lumkit.sweeteditor.EditorTheme
import io.github.lumkit.sweeteditor.core.protocol.CoreProtocol
import io.github.lumkit.sweeteditor.core.protocol.RangeEffectKind
import io.github.lumkit.sweeteditor.internal.jni.NativeBridge
import io.github.lumkit.sweeteditor.toRangeEffectStyles
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EditorBracketJniTest {
    @Test
    fun insertOpenAutoClosesAndHighlightsMatch() {
        assertTrue(NativeBridge.isAvailable)
        val host = HostTextMeasurer(charWidth = 8f, ascent = 12f, descent = 4f)
        val document = Document.fromUtf8("")
        val editor = EditorCore.create(host)
        try {
            assertTrue(editor.setDocument(document)?.handled == true)
            assertTrue(editor.setViewport(400, 300)?.handled == true)
            assertTrue(editor.onFontMetricsChanged()?.handled == true)
            assertTrue(applyRangeStyles(editor))
            assertTrue(
                editor.setBracketPairs(intArrayOf('('.code), intArrayOf(')'.code))?.handled == true,
            )
            assertTrue(
                editor.setAutoClosingPairs(intArrayOf('('.code), intArrayOf(')'.code))?.handled == true,
            )
            assertTrue(editor.insertText("(")?.handled == true)
            assertEquals("()", document.utf8Text())

            val model = editor.buildRenderModel()
            assertTrue(model != null)
            assertEquals(2, model.rangeEffects.count { it.kind == RangeEffectKind.BRACKET_MATCH })
        } finally {
            editor.close()
            document.close()
        }
    }

    @Test
    fun externalMatchedBracketsOverrideScan() {
        assertTrue(NativeBridge.isAvailable)
        val host = HostTextMeasurer(charWidth = 8f, ascent = 12f, descent = 4f)
        val document = Document.fromUtf8("abcd")
        val editor = EditorCore.create(host)
        try {
            assertTrue(editor.setDocument(document)?.handled == true)
            assertTrue(editor.setViewport(400, 300)?.handled == true)
            assertTrue(editor.onFontMetricsChanged()?.handled == true)
            assertTrue(applyRangeStyles(editor))
            assertTrue(
                editor.setBracketPairs(intArrayOf('('.code), intArrayOf(')'.code))?.handled == true,
            )
            assertTrue(editor.setMatchedBrackets(0, 0, 0, 3)?.handled == true)
            val overridden = editor.buildRenderModel()
            assertTrue(overridden != null)
            assertEquals(2, overridden.rangeEffects.count { it.kind == RangeEffectKind.BRACKET_MATCH })

            assertTrue(editor.clearMatchedBrackets()?.handled == true)
            val cleared = editor.buildRenderModel()
            assertTrue(cleared != null)
            assertTrue(cleared.rangeEffects.none { it.kind == RangeEffectKind.BRACKET_MATCH })
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
