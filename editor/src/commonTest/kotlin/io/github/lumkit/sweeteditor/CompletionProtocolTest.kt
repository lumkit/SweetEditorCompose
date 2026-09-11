package io.github.lumkit.sweeteditor

import kotlin.test.Test
import kotlin.test.assertEquals

class CompletionProtocolTest {
    @Test
    fun insertTextFormatUsesLspWireValues() {
        assertEquals(1, CompletionInsertTextFormat.PLAIN_TEXT.value)
        assertEquals(2, CompletionInsertTextFormat.SNIPPET.value)
    }
}
