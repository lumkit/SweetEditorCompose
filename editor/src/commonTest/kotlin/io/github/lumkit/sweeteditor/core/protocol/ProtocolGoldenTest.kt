package io.github.lumkit.sweeteditor.core.protocol

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProtocolGoldenTest {
    @Test
    fun editorOptionsFullMatchesGoldenHex() {
        val encoded = CoreProtocol.encodeEditorOptions(
            EditorOptions(
                touchSlop = 11.5f,
                doubleTapTimeout = 321L,
                longPressMs = 654L,
                flingFriction = 4.25f,
                flingMinVelocity = 75.5f,
                flingMaxVelocity = 9000.25f,
                maxUndoStackSize = 1024L,
                keyChordTimeoutMs = 2500L,
                revealSelectionEndOnSelectAll = true,
            ),
        )
        assertEquals(
            "00 00 38 41 41 01 00 00 00 00 00 00 8e 02 00 00 00 00 00 00 00 00 88 40 00 00 97 42 00 a1 0c 46 00 04 00 00 00 00 00 00 c4 09 00 00 00 00 00 00 01",
            encoded.toHex(),
        )
    }

    @Test
    fun textChangeUtf8DecodesGoldenHex() {
        val decoded = CoreProtocol.decodeTextChange(
            parseHex("02 00 00 00 03 00 00 00 04 00 00 00 08 00 00 00 0c 00 00 00 68 65 6c 6c 6f 20 e4 b8 96 e7 95 8c"),
        )
        assertEquals(2, decoded.range.start.line)
        assertEquals(3, decoded.range.start.column)
        assertEquals(4, decoded.range.end.line)
        assertEquals(8, decoded.range.end.column)
        assertEquals("hello 世界", decoded.newText)
    }

    @Test
    fun editorActionResultNestedDecodesGoldenHex() {
        val result = CoreProtocol.decodeEditorActionResult(parseHex(EDITOR_ACTION_RESULT_NESTED_HEX))
        assertTrue(result.handled)
        assertTrue(result.needsRedraw)
        assertEquals(EditorActionSource.PROGRAMMATIC, result.source)
        assertEquals(TextChangeKind.REPLACEMENT, result.textChangeKind)
        assertEquals("abc", result.textChanges.single().newText)
        assertEquals(PointerCursorType.TEXT, result.pointerCursorBefore)
        assertEquals(PointerCursorType.HAND, result.pointerCursorAfter)
        assertEquals(7L, result.imeState.sessionId)
    }

    @Test
    fun editorRenderModelNestedDecodesGoldenHex() {
        val model = CoreProtocol.decodeEditorRenderModel(parseHex(EDITOR_RENDER_MODEL_NESTED_HEX))
        assertEquals(16f, model.splitX)
        assertEquals(1, model.lines.size)
        val line = model.lines.single()
        assertEquals(5, line.logicalLine)
        assertEquals(1, line.runs.size)
        assertEquals("hi", line.runs.single().text)
        assertEquals(5, model.cursor.textPosition.line)
        assertEquals(PointerCursorType.HAND, model.pointerCursorType)
    }
}

private const val EDITOR_ACTION_RESULT_NESTED_HEX =
    "01 00 00 00 01 00 00 00 02 00 00 00 02 00 00 00 01 00 00 00 00 00 00 00 01 00 00 00 00 00 00 00 01 00 00 00 00 00 00 00 01 00 00 00 02 00 00 00 00 00 00 00 03 00 00 00 01 00 00 00 01 00 00 00 02 00 00 00 01 00 00 00 05 00 00 00 03 00 00 00 61 62 63 01 00 00 00 02 00 00 00 01 00 00 00 05 00 00 00 00 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 02 00 00 00 01 00 00 00 05 00 00 00 00 00 00 3f 00 00 c0 3f 00 00 20 40 00 00 60 40 00 00 80 3f 00 00 a0 3f 01 00 00 00 02 00 00 00 00 00 00 00 00 00 00 00 07 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 02 00 00 00 00 00 00 00 05 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 02 00 00 00 00 00 00 00 05 00 00 00 00 00 00 00 01 00 00 00 05 00 00 00 00 00 20 41 00 00 a0 41 06 00 00 00 03 00 00 00 04 00 00 00 4d 00 00 00 00 ff 00 ff 03 00 00 00 02 00 00 00"

private const val EDITOR_RENDER_MODEL_NESTED_HEX =
    "00 00 80 41 01 00 00 00 00 00 80 3f 00 00 00 40 00 00 48 44 00 00 16 44 00 00 80 40 00 00 c0 41 01 00 00 00 01 00 00 00 05 00 00 00 00 00 00 00 00 00 00 40 00 00 90 41 01 00 00 00 00 00 00 00 00 00 20 42 00 00 90 41 02 00 00 00 68 69 ff ff ff ff 00 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 80 41 00 00 80 3f 00 00 00 40 01 00 00 00 00 00 00 00 01 00 00 00 01 00 00 00 06 00 00 00 44 33 22 11 88 77 66 55 05 00 00 00 02 00 00 00 00 00 60 42 00 00 90 41 00 00 a0 41 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 03 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 00 00 40 3f 01 00 00 00 00 80 45 44 00 00 00 00 00 00 20 41 00 00 16 44 00 80 45 44 00 00 20 42 00 00 20 41 00 00 f0 42 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 80 13 44 00 00 48 44 00 00 20 41 00 00 a0 41 00 80 13 44 00 00 f0 42 00 00 20 41 01 00 00 00 01 00 00 00 02 00 00 00"

private fun parseHex(hex: String): ByteArray {
    val parts = hex.split(Regex("\\s+")).filter { it.isNotEmpty() }
    return ByteArray(parts.size) { parts[it].toInt(16).toByte() }
}

private fun ByteArray.toHex(): String =
    joinToString(" ") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }
