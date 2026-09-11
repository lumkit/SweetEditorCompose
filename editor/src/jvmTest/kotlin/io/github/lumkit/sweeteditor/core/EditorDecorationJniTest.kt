package io.github.lumkit.sweeteditor.core

import io.github.lumkit.sweeteditor.CodeLensItem
import io.github.lumkit.sweeteditor.Diagnostic
import io.github.lumkit.sweeteditor.DocumentHighlight
import io.github.lumkit.sweeteditor.EditorDiagnosticSeverity
import io.github.lumkit.sweeteditor.EditorDocumentHighlightKind
import io.github.lumkit.sweeteditor.EditorFontStyle
import io.github.lumkit.sweeteditor.EditorInlayType
import io.github.lumkit.sweeteditor.EditorSpanLayer
import io.github.lumkit.sweeteditor.EditorTextStyle
import io.github.lumkit.sweeteditor.EditorTheme
import io.github.lumkit.sweeteditor.GutterIcon
import io.github.lumkit.sweeteditor.InlayHint
import io.github.lumkit.sweeteditor.LinkSpan
import io.github.lumkit.sweeteditor.PhantomText
import io.github.lumkit.sweeteditor.StyleSpan
import io.github.lumkit.sweeteditor.core.protocol.CoreProtocol
import io.github.lumkit.sweeteditor.core.protocol.RangeEffectKind
import io.github.lumkit.sweeteditor.core.protocol.RangeEffectUnderlineStyle
import io.github.lumkit.sweeteditor.internal.jni.NativeBridge
import io.github.lumkit.sweeteditor.toRangeEffectStyles
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EditorDecorationJniTest {
    @Test
    fun writeDecorationsAndQueryLink() {
        assertTrue(NativeBridge.isAvailable)
        val host = HostTextMeasurer(charWidth = 8f, ascent = 12f, descent = 4f)
        val document = Document.fromUtf8("hello world")
        val editor = EditorCore.create(host)
        try {
            assertTrue(editor.setDocument(document)?.handled == true)
            assertTrue(editor.setViewport(400, 300)?.handled == true)

            assertTrue(
                editor.registerTextStyle(1, color = 0xFFCC0000.toInt(), fontStyle = EditorFontStyle.BOLD)
                    ?.handled == true,
            )
            assertTrue(
                editor.registerBatchTextStyles(
                    mapOf(2 to EditorTextStyle(color = 0xFF0066CC.toInt())),
                )?.handled == true,
            )
            assertTrue(
                editor.setLineSpans(0, EditorSpanLayer.SYNTAX, listOf(StyleSpan(0, 5, 1)))?.handled == true,
            )
            assertTrue(
                editor.setLineInlayHints(
                    0,
                    listOf(InlayHint(EditorInlayType.TEXT, column = 5, text = ": String")),
                )?.handled == true,
            )
            assertTrue(
                editor.setLinePhantomTexts(0, listOf(PhantomText(11, " // note")))?.handled == true,
            )
            assertTrue(editor.setMaxGutterIcons(1)?.handled == true)
            assertTrue(editor.setLineGutterIcons(0, listOf(GutterIcon(7)))?.handled == true)
            assertTrue(
                editor.setLineCodeLens(0, listOf(CodeLensItem(0, 42, "run")))?.handled == true,
            )
            assertTrue(
                editor.setLineLinks(0, listOf(LinkSpan(6, 5, "https://example.com")))?.handled == true,
            )
            assertEquals("https://example.com", editor.getLinkTargetAt(0, 7))
            assertTrue(
                editor.setLineDiagnostics(
                    0,
                    listOf(Diagnostic(0, 5, EditorDiagnosticSeverity.ERROR)),
                )?.handled == true,
            )
            assertTrue(
                editor.setLineDocumentHighlights(
                    0,
                    listOf(DocumentHighlight(6, 5, EditorDocumentHighlightKind.READ)),
                )?.handled == true,
            )

            val theme = EditorTheme()
            assertTrue(
                editor.setEditorRangeEffectStyles(
                    CoreProtocol.encodeEditorRangeEffectStyles(theme.toRangeEffectStyles()),
                )?.handled == true,
            )
            val model = editor.buildRenderModel()
            assertTrue(model != null)
            assertTrue(model.gutterIcons.any { it.iconId == 7 && it.rect.width > 0f && it.rect.height > 0f })
            val diagnostics = model.rangeEffects.filter { it.kind == RangeEffectKind.DIAGNOSTIC_ERROR }
            assertTrue(diagnostics.isNotEmpty())
            assertTrue(diagnostics.any { it.style.underlineColor == theme.diagnosticErrorColor })
            assertTrue(diagnostics.any { it.style.underlineStyle == RangeEffectUnderlineStyle.WAVY })
            assertTrue(
                model.rangeEffects.any {
                    it.kind == RangeEffectKind.DOCUMENT_HIGHLIGHT_READ &&
                        it.style.backgroundColor == theme.documentHighlightReadBgColor
                },
            )

            assertTrue(editor.clearAllDecorations()?.handled == true)
            assertEquals("", editor.getLinkTargetAt(0, 7))
        } finally {
            editor.close()
            document.close()
        }
    }
}
