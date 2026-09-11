package io.github.lumkit.sweeteditor.core

import io.github.lumkit.sweeteditor.internal.jni.NativeBridge
import kotlin.test.Test
import kotlin.test.assertTrue

class SessionLifecycleJniTest {
    @Test
    fun createAndFreeEditorTwentyTimes() {
        assertTrue(NativeBridge.isAvailable)
        val host = HostTextMeasurer(charWidth = 8f, ascent = 12f, descent = 4f)
        repeat(20) {
            val document = Document.fromUtf8("hello #$it")
            val editor = EditorCore.create(host)
            try {
                val result = editor.setDocument(document)
                assertTrue(result != null)
                editor.setViewport(400, 300)
            } finally {
                editor.close()
                document.close()
            }
        }
    }
}
