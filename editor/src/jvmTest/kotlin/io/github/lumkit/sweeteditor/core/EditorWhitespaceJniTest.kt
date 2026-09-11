package io.github.lumkit.sweeteditor.core

import io.github.lumkit.sweeteditor.Diagnostic
import io.github.lumkit.sweeteditor.EditorDiagnosticSeverity
import io.github.lumkit.sweeteditor.EditorRangeEffectStyle
import io.github.lumkit.sweeteditor.EditorRangeEffects
import io.github.lumkit.sweeteditor.EditorRangeUnderlineStyle
import io.github.lumkit.sweeteditor.EditorTheme
import io.github.lumkit.sweeteditor.WhitespaceRenderMode
import io.github.lumkit.sweeteditor.core.protocol.CoreProtocol
import io.github.lumkit.sweeteditor.core.protocol.RangeEffectKind
import io.github.lumkit.sweeteditor.core.protocol.RangeEffectUnderlineStyle
import io.github.lumkit.sweeteditor.core.protocol.VisualRunType
import io.github.lumkit.sweeteditor.internal.jni.NativeBridge
import io.github.lumkit.sweeteditor.toRangeEffectStyles
import io.github.lumkit.sweeteditor.toRenderColors
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EditorWhitespaceJniTest {
    @Test
    fun allWhitespaceAndLineBreaksAppearInRenderModel() {
        assertTrue(NativeBridge.isAvailable)
        val host = HostTextMeasurer(charWidth = 8f, ascent = 12f, descent = 4f)
        val document = Document.fromUtf8("a  b\tc\n")
        val editor = EditorCore.create(host)
        try {
            assertTrue(editor.setDocument(document)?.handled == true)
            assertTrue(editor.setViewport(400, 300)?.handled == true)
            assertTrue(editor.setRenderWhitespace(WhitespaceRenderMode.ALL.value)?.handled == true)
            assertTrue(editor.setRenderLineBreaks(true)?.handled == true)
            val model = editor.buildRenderModel()
            assertTrue(model != null)
            val types = model.lines.flatMap { line -> line.runs.map { it.type } }.toSet()
            assertTrue(VisualRunType.WHITESPACE in types)
            assertTrue(VisualRunType.TAB in types)
            assertTrue(VisualRunType.NEWLINE in types)
        } finally {
            editor.close()
            document.close()
        }
    }

    @Test
    fun customRangeEffectStylesReachTheRenderModel() {
        assertTrue(NativeBridge.isAvailable)
        val host = HostTextMeasurer(charWidth = 8f, ascent = 12f, descent = 4f)
        val document = Document.fromUtf8("hello")
        val editor = EditorCore.create(host)
        try {
            assertTrue(editor.setDocument(document)?.handled == true)
            assertTrue(editor.setViewport(400, 300)?.handled == true)
            val theme = EditorTheme(
                rangeEffects = EditorRangeEffects(
                    diagnosticError = EditorRangeEffectStyle(
                        underlineColor = 0xFFFF1122.toInt(),
                        underlineStyle = EditorRangeUnderlineStyle.SOLID,
                    ),
                ),
            )
            assertTrue(
                editor.setEditorRenderColors(CoreProtocol.encodeEditorRenderColors(theme.toRenderColors()))
                    ?.handled == true,
            )
            assertTrue(
                editor.setEditorRangeEffectStyles(
                    CoreProtocol.encodeEditorRangeEffectStyles(theme.toRangeEffectStyles()),
                )?.handled == true,
            )
            assertTrue(
                editor.setLineDiagnostics(
                    0,
                    listOf(Diagnostic(0, 5, EditorDiagnosticSeverity.ERROR)),
                )?.handled == true,
            )
            val model = editor.buildRenderModel()
            assertTrue(model != null)
            val errors = model.rangeEffects.filter { it.kind == RangeEffectKind.DIAGNOSTIC_ERROR }
            assertTrue(errors.isNotEmpty())
            assertTrue(errors.any { it.style.underlineColor == 0xFFFF1122.toInt() })
            assertTrue(errors.any { it.style.underlineStyle == RangeEffectUnderlineStyle.SOLID })
            val styles = theme.toRangeEffectStyles()
            assertEquals(theme.selectionColor, styles.selection.backgroundColor)
            assertEquals(RangeEffectUnderlineStyle.WAVY, styles.diagnosticWarning.underlineStyle)
        } finally {
            editor.close()
            document.close()
        }
    }
}
