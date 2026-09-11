package io.github.lumkit.sweeteditor.core

import io.github.lumkit.sweeteditor.CurrentLineRenderMode
import io.github.lumkit.sweeteditor.WrapMode
import io.github.lumkit.sweeteditor.internal.jni.NativeBridge
import kotlin.test.Test
import kotlin.test.assertTrue

class EditorSettingsJniTest {
    @Test
    fun wrapTabScaleAndReadOnlyDispatch() {
        assertTrue(NativeBridge.isAvailable)
        val host = HostTextMeasurer(charWidth = 8f, ascent = 12f, descent = 4f)
        val document = Document.fromUtf8("fun main() {\n    println(1)\n}")
        val editor = EditorCore.create(host)
        try {
            assertTrue(editor.setDocument(document)?.handled == true)
            assertTrue(editor.setWrapMode(WrapMode.WORD_BREAK.value)?.handled == true)
            assertTrue(editor.setTabSize(2)?.handled == true)
            assertTrue(editor.setInsertSpaces(true)?.handled == true)
            assertTrue(editor.setLineSpacing(0f, 1.4f)?.handled == true)
            assertTrue(editor.setScale(1.25f)?.handled == true)
            assertTrue(editor.setReadOnly(true)?.handled == true)
            assertTrue(editor.setGutterVisible(false)?.handled == true)
            assertTrue(editor.setGutterSticky(true)?.handled == true)
            assertTrue(editor.setCurrentLineRenderMode(CurrentLineRenderMode.NONE.value)?.handled == true)
        } finally {
            editor.close()
            document.close()
        }
    }
}
