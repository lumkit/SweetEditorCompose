package io.github.lumkit.sweeteditor.core

import io.github.lumkit.sweeteditor.core.protocol.CoreProtocol
import io.github.lumkit.sweeteditor.internal.jni.NativeBridge
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

class RenderModelJniTest {
    @Test
    fun buildRenderModelDecodesWithoutHugeLists() {
        val host = HostTextMeasurer(charWidth = 8f, ascent = 12f, descent = 4f)
        val document = Document.fromUtf8("hello\nworld")
        val editor = EditorCore.create(host)
        try {
            editor.setDocument(document)
            editor.setViewport(800, 600)
            editor.onFontMetricsChanged()
            val payload = NativeBridge.editorBuildRenderModel(editor.handle)
            if (payload == null || payload.isEmpty()) {
                fail("render model payload is null or empty")
            }
            val hex = payload.joinToString(" ") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }
            val model = try {
                CoreProtocol.decodeEditorRenderModel(payload)
            } catch (error: Throwable) {
                fail("decode failed size=${payload.size} hex=$hex cause=$error")
            }
            assertTrue(model.lines.size in 1..64, "lines=${model.lines.size} size=${payload.size}")
            assertTrue(
                model.lines.all { it.runs.size < 10_000 },
                "run counts=${model.lines.map { it.runs.size }} size=${payload.size}",
            )
        } finally {
            editor.close()
            document.close()
        }
    }
}
