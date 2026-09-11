package io.github.lumkit.sweeteditor.core

import io.github.lumkit.sweeteditor.EditorKeyMap
import io.github.lumkit.sweeteditor.core.protocol.EditorBuiltinCommand
import io.github.lumkit.sweeteditor.core.protocol.KeyCode
import io.github.lumkit.sweeteditor.core.protocol.KeyModifier
import io.github.lumkit.sweeteditor.core.protocol.encodeSetKeyMapPayload
import io.github.lumkit.sweeteditor.internal.jni.NativeBridge
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EditorKeyMapJniTest {
    @Test
    fun vscodeAndJetbrainsMapCtrlYDifferently() {
        assertTrue(NativeBridge.isAvailable)
        val host = HostTextMeasurer(charWidth = 8f, ascent = 12f, descent = 4f)
        val document = Document.fromUtf8("one\ntwo\nthree")
        val editor = EditorCore.create(host)
        try {
            assertTrue(editor.setDocument(document)?.handled == true)
            assertTrue(editor.setViewport(400, 300)?.handled == true)

            assertTrue(
                editor.setKeyMap(encodeSetKeyMapPayload(EditorKeyMap.vscode().toProtocolBindings()))
                    ?.handled == true,
            )
            val vscodeY = editor.handleKeyEvent(KeyCode.Y, null, KeyModifier.CTRL)
            assertEquals(EditorBuiltinCommand.REDO.value, vscodeY?.command)

            assertTrue(
                editor.setKeyMap(encodeSetKeyMapPayload(EditorKeyMap.jetbrains().toProtocolBindings()))
                    ?.handled == true,
            )
            val jetbrainsY = editor.handleKeyEvent(KeyCode.Y, null, KeyModifier.CTRL)
            assertEquals(EditorBuiltinCommand.DELETE_LINE.value, jetbrainsY?.command)
            assertEquals("two\nthree", document.utf8Text())
        } finally {
            editor.close()
            document.close()
        }
    }
}
