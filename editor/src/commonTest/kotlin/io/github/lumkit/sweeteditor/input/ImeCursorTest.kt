package io.github.lumkit.sweeteditor.input

import kotlin.test.Test
import kotlin.test.assertEquals

class ImeCursorTest {
    @Test
    fun positiveCursorPositionIsRelativeToReplacementEnd() {
        assertEquals(5, cursorAfterReplacement(2, 4, 3, 1, 10))
        assertEquals(6, cursorAfterReplacement(2, 4, 3, 2, 10))
    }

    @Test
    fun nonPositiveCursorPositionIsRelativeToReplacementStart() {
        assertEquals(2, cursorAfterReplacement(2, 4, 3, 0, 10))
        assertEquals(1, cursorAfterReplacement(2, 4, 3, -1, 10))
    }

    @Test
    fun cursorPositionIsClampedToResultingText() {
        assertEquals(0, cursorAfterReplacement(2, 4, 3, -10, 10))
        assertEquals(11, cursorAfterReplacement(2, 4, 3, 100, 10))
    }
}
