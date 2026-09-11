package io.github.lumkit.sweeteditor.selection

import androidx.compose.ui.unit.IntOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SelectionMenuPlacementTest {
    @Test
    fun prefersAboveWhenThereIsRoom() {
        val offset = computeSelectionMenuOffset(
            viewportWidth = 400,
            viewportHeight = 400,
            anchor = SelectionMenuAnchor(80f, 120f, 160f, 148f),
            menuWidth = 200,
            menuHeight = 36,
            gap = 8,
            handleClearance = 32,
        )
        assertEquals(IntOffset(20, 76), offset)
    }

    @Test
    fun placesBelowWhenTopIsBlocked() {
        val offset = computeSelectionMenuOffset(
            viewportWidth = 400,
            viewportHeight = 400,
            anchor = SelectionMenuAnchor(40f, 4f, 120f, 32f),
            menuWidth = 200,
            menuHeight = 36,
            gap = 8,
            handleClearance = 32,
        )
        assertEquals(IntOffset(0, 72), offset)
    }

    @Test
    fun placesToTheRightWhenVerticalSpaceIsGone() {
        val offset = computeSelectionMenuOffset(
            viewportWidth = 400,
            viewportHeight = 80,
            anchor = SelectionMenuAnchor(10f, 4f, 80f, 76f),
            menuWidth = 120,
            menuHeight = 36,
            gap = 8,
            handleClearance = 32,
        )
        assertEquals(IntOffset(88, 22), offset)
    }

    @Test
    fun placesToTheLeftWhenRightOverflows() {
        val offset = computeSelectionMenuOffset(
            viewportWidth = 200,
            viewportHeight = 80,
            anchor = SelectionMenuAnchor(90f, 4f, 190f, 76f),
            menuWidth = 80,
            menuHeight = 36,
            gap = 8,
            handleClearance = 32,
        )
        assertEquals(IntOffset(2, 22), offset)
    }

    @Test
    fun clampedFallbackStaysInsideViewport() {
        val offset = computeSelectionMenuOffset(
            viewportWidth = 120,
            viewportHeight = 50,
            anchor = SelectionMenuAnchor(0f, 0f, 120f, 50f),
            menuWidth = 100,
            menuHeight = 36,
            gap = 8,
            handleClearance = 32,
        )
        assertTrue(offset.x in 0..20)
        assertTrue(offset.y in 0..14)
    }
}
