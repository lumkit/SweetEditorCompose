package io.github.lumkit.sweeteditor.highlight

import io.github.lumkit.sweeteditor.SweetEditorController
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SweetLineHighlightBindTest {
    @Test
    fun secondBindThrows() {
        val highlight = SweetLineHighlight()
        val controller = SweetEditorController("int x;")
        val binding = highlight.bind(controller)
        try {
            assertFailsWith<IllegalStateException> {
                highlight.bind(controller)
            }
        } finally {
            binding.close()
            highlight.close()
        }
    }

    @Test
    fun closeIsIdempotent() {
        val highlight = SweetLineHighlight()
        val controller = SweetEditorController()
        val binding = highlight.bind(controller)
        binding.close()
        binding.close()
        highlight.close()
        highlight.close()
        assertTrue(true)
    }
}
