package io.github.lumkit.sweeteditor.core

import io.github.lumkit.sweeteditor.EditorTheme
import io.github.lumkit.sweeteditor.core.protocol.CoreProtocol
import io.github.lumkit.sweeteditor.core.protocol.EditorBuiltinCommand
import io.github.lumkit.sweeteditor.core.protocol.KeyCode
import io.github.lumkit.sweeteditor.core.protocol.KeyModifier
import io.github.lumkit.sweeteditor.core.protocol.RangeEffectKind
import io.github.lumkit.sweeteditor.toRangeEffectStyles
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EditorSelectionRenderJniTest {
    @Test
    fun selectionEmitsHighlightAndHandles() {
        val theme = EditorTheme()
        val host = HostTextMeasurer(charWidth = 8f, ascent = 12f, descent = 4f)
        val document = Document.fromUtf8("")
        val editor = EditorCore.create(host)
        try {
            assertTrue(editor.setDocument(document)?.handled == true)
            assertTrue(editor.setViewport(400, 300)?.handled == true)
            assertTrue(editor.onFontMetricsChanged()?.handled == true)
            assertTrue(
                editor.setEditorRangeEffectStyles(
                    CoreProtocol.encodeEditorRangeEffectStyles(theme.toRangeEffectStyles()),
                )?.handled == true,
            )
            assertTrue(editor.insertText("hello")?.handled == true)

            val selectAll = editor.handleKeyEvent(KeyCode.A, null, KeyModifier.CTRL)
            assertTrue(selectAll != null && selectAll.handled)
            assertEquals(EditorBuiltinCommand.SELECT_ALL.value, selectAll.command)

            val model = editor.buildRenderModel()
            requireNotNull(model)
            val selection = model.rangeEffects.filter { it.kind == RangeEffectKind.SELECTION }
            assertTrue(selection.isNotEmpty(), "expected selection range effects")
            assertTrue(selection.any { it.rect.width > 0f && it.rect.height > 0f })
            assertTrue(selection.all { it.style.backgroundColor == theme.selectionColor })
            assertTrue(model.selectionStartHandle.visible)
            assertTrue(model.selectionEndHandle.visible)
            assertTrue(model.selectionStartHandle.height > 0f)
            assertTrue(model.selectionEndHandle.height > 0f)
        } finally {
            editor.close()
            document.close()
        }
    }
}
