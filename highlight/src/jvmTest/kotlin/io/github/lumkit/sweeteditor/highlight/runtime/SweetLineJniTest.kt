package io.github.lumkit.sweeteditor.highlight.runtime

import kotlin.test.Test
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class SweetLineJniTest {
    @Test
    fun createEngineWithTabSizeFourIsNonZero() {
        assertTrue(NativeBridge.isAvailable)
        val engine = NativeBridge.createEngine(4)
        try {
            assertNotEquals(0L, engine)
        } finally {
            NativeBridge.freeEngine(engine)
        }
    }
}
