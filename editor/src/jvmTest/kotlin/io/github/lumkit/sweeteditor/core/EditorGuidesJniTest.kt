package io.github.lumkit.sweeteditor.core

import io.github.lumkit.sweeteditor.BracketGuide
import io.github.lumkit.sweeteditor.FlowGuide
import io.github.lumkit.sweeteditor.IndentGuide
import io.github.lumkit.sweeteditor.SeparatorGuide
import io.github.lumkit.sweeteditor.SeparatorStyle
import io.github.lumkit.sweeteditor.TextPosition
import io.github.lumkit.sweeteditor.core.protocol.GuideType
import io.github.lumkit.sweeteditor.internal.jni.NativeBridge
import kotlin.test.Test
import kotlin.test.assertTrue

class EditorGuidesJniTest {
    @Test
    fun structureGuidesAppearInRenderModel() {
        assertTrue(NativeBridge.isAvailable)
        val host = HostTextMeasurer(charWidth = 8f, ascent = 12f, descent = 4f)
        val document = Document.fromUtf8("fun main() {\n    a\n    b\n}\n")
        val editor = EditorCore.create(host)
        try {
            assertTrue(editor.setDocument(document)?.handled == true)
            assertTrue(editor.setViewport(400, 300)?.handled == true)

            assertTrue(
                editor.setIndentGuides(
                    listOf(IndentGuide(TextPosition(0, 4), TextPosition(2, 4))),
                )?.handled == true,
            )
            val indent = editor.buildRenderModel()
            assertTrue(indent != null)
            assertTrue(indent.guideSegments.any { it.type == GuideType.INDENT })

            assertTrue(
                editor.setBracketGuides(
                    listOf(
                        BracketGuide(
                            parent = TextPosition(0, 11),
                            end = TextPosition(3, 0),
                            children = listOf(TextPosition(1, 4)),
                        ),
                    ),
                )?.handled == true,
            )
            val bracket = editor.buildRenderModel()
            assertTrue(bracket != null)
            assertTrue(bracket.guideSegments.any { it.type == GuideType.BRACKET })

            assertTrue(
                editor.setFlowGuides(
                    listOf(FlowGuide(TextPosition(1, 4), TextPosition(2, 4))),
                )?.handled == true,
            )
            val flow = editor.buildRenderModel()
            assertTrue(flow != null)
            assertTrue(flow.guideSegments.any { it.type == GuideType.FLOW && it.arrowEnd })

            assertTrue(
                editor.setSeparatorGuides(
                    listOf(SeparatorGuide(line = 2, style = SeparatorStyle.SINGLE, count = 1, textEndColumn = 4)),
                )?.handled == true,
            )
            val separator = editor.buildRenderModel()
            assertTrue(separator != null)
            assertTrue(separator.guideSegments.any { it.type == GuideType.SEPARATOR })

            assertTrue(editor.clearGuides()?.handled == true)
            val cleared = editor.buildRenderModel()
            assertTrue(cleared != null)
            assertTrue(cleared.guideSegments.isEmpty())
        } finally {
            editor.close()
            document.close()
        }
    }
}
