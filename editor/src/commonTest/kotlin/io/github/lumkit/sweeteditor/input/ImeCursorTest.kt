package io.github.lumkit.sweeteditor.input

import io.github.lumkit.sweeteditor.core.protocol.ImeCommandKind
import io.github.lumkit.sweeteditor.core.protocol.ImeResultCode
import io.github.lumkit.sweeteditor.core.protocol.ImeState
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

    @Test
    fun markedTextBeginsCompositionThenUpdates() {
        val commands = commandsForMarkedText(
            state = ImeState(
                resultCode = ImeResultCode.OK,
                sessionId = 1,
                stateRevision = 1,
                selection = documentSelection(2, 2),
                compositionRange = noneImeRange(),
            ),
            markedText = "ni",
            selectedLocation = 2,
            selectedLength = 0,
        )
        assertEquals(2, commands?.size)
        assertEquals(ImeCommandKind.BEGIN_COMPOSITION, commands!![0].kind)
        assertEquals(ImeCommandKind.UPDATE_COMPOSITION, commands[1].kind)
        assertEquals("ni", commands[1].text)
    }
}
