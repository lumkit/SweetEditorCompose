package io.github.lumkit.sweeteditor.core

import io.github.lumkit.sweeteditor.internal.jni.NativeBridge
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DocumentJniTest {
    @Test
    fun utf8RoundTripAndInsertViaEditorRequiresMeasurerLater() {
        assertTrue(NativeBridge.isAvailable)
        val handle = NativeBridge.createDocumentFromUtf8("hello".encodeToByteArray())
        assertTrue(handle != 0L)
        try {
            assertEquals("hello", NativeBridge.getDocumentUtf8(handle).decodeToString())
        } finally {
            NativeBridge.freeDocument(handle)
        }
    }
}
