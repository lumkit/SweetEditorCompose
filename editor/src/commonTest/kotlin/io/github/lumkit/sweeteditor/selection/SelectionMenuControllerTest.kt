package io.github.lumkit.sweeteditor.selection

import io.github.lumkit.sweeteditor.SelectionMenuContext
import io.github.lumkit.sweeteditor.SelectionMenuItem
import io.github.lumkit.sweeteditor.SelectionMenuItemProvider
import io.github.lumkit.sweeteditor.TextPosition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SelectionMenuControllerTest {
    @Test
    fun selectionChangeSchedulesShowAndDelayPresentsDefaultItems() {
        val controller = SelectionMenuController(enabled = true) { hasSelection ->
            SelectionMenuContext(hasSelection, TextPosition(0, 1))
        }
        controller.onEditorSignal(showSignal(hasSelection = true))
        assertEquals(SelectionMenuLifecycle.PENDING_SHOW, controller.lifecycle)
        val items = controller.consumePendingShow(controller.showToken)
        assertEquals(SelectionMenuLifecycle.VISIBLE, controller.lifecycle)
        assertEquals(
            listOf("cut", "copy", "paste", "select_all"),
            items?.map { it.id },
        )
        assertEquals(true, items?.first { it.id == SelectionMenuItem.ACTION_CUT }?.enabled)
    }

    @Test
    fun selectionChangeWithRangeEnablesCutAndCopy() {
        val controller = SelectionMenuController(enabled = true) { hasSelection ->
            SelectionMenuContext(hasSelection, TextPosition(0, 4), selectedText = "abcd")
        }
        controller.onEditorSignal(showSignal(hasSelection = true))
        val items = controller.consumePendingShow(controller.showToken)
        assertEquals(true, items?.first { it.id == SelectionMenuItem.ACTION_CUT }?.enabled)
        assertEquals(true, items?.first { it.id == SelectionMenuItem.ACTION_COPY }?.enabled)
    }

    @Test
    fun emptyProviderSkipsMenu() {
        val controller = SelectionMenuController(enabled = true) { hasSelection ->
            SelectionMenuContext(hasSelection, TextPosition(0, 0))
        }
        controller.provider = SelectionMenuItemProvider { emptyList() }
        controller.onEditorSignal(showSignal(hasSelection = true))
        assertNull(controller.consumePendingShow(controller.showToken))
        assertEquals(SelectionMenuLifecycle.HIDDEN, controller.lifecycle)
    }

    @Test
    fun customProviderReplacesDefaults() {
        val controller = SelectionMenuController(enabled = true) { hasSelection ->
            SelectionMenuContext(hasSelection, TextPosition(1, 0), selectedText = "x")
        }
        controller.provider = SelectionMenuItemProvider { context ->
            listOf(SelectionMenuItem(id = "wrap", label = "Wrap", enabled = context.hasSelection))
        }
        controller.onEditorSignal(showSignal(hasSelection = true))
        val items = controller.consumePendingShow(controller.showToken)
        assertEquals(listOf("wrap"), items?.map { it.id })
        assertTrue(items?.single()?.enabled == true)
    }

    @Test
    fun interactionSuspendsThenResumesWhenIdle() {
        val controller = SelectionMenuController(enabled = true) { hasSelection ->
            SelectionMenuContext(hasSelection, TextPosition(0, 2))
        }
        controller.onEditorSignal(showSignal(hasSelection = true))
        controller.consumePendingShow(controller.showToken)
        controller.onEditorSignal(showSignal(hasSelection = true, blocked = true, selectionChanged = false))
        assertEquals(SelectionMenuLifecycle.SUSPENDED, controller.lifecycle)
        controller.onEditorSignal(
            SelectionMenuSignal(
                textChanged = false,
                selectionChanged = false,
                hasSelectionAfter = true,
                blocked = false,
                scrollChanged = false,
            ),
        )
        assertEquals(SelectionMenuLifecycle.PENDING_SHOW, controller.lifecycle)
        assertEquals(4, controller.consumePendingShow(controller.showToken)?.size)
    }

    @Test
    fun textChangeOrEmptySelectionHides() {
        val controller = SelectionMenuController(enabled = true) { hasSelection ->
            SelectionMenuContext(hasSelection, TextPosition(0, 0))
        }
        controller.onEditorSignal(showSignal(hasSelection = true))
        controller.consumePendingShow(controller.showToken)
        controller.onEditorSignal(
            SelectionMenuSignal(
                textChanged = true,
                selectionChanged = true,
                hasSelectionAfter = true,
                blocked = false,
                scrollChanged = false,
            ),
        )
        assertEquals(SelectionMenuLifecycle.HIDDEN, controller.lifecycle)
        controller.onEditorSignal(showSignal(hasSelection = true))
        controller.consumePendingShow(controller.showToken)
        controller.onEditorSignal(showSignal(hasSelection = false, selectionChanged = true))
        assertEquals(SelectionMenuLifecycle.HIDDEN, controller.lifecycle)
    }

    @Test
    fun disabledNeverShows() {
        val controller = SelectionMenuController(enabled = false) { hasSelection ->
            SelectionMenuContext(hasSelection, TextPosition(0, 0))
        }
        controller.onEditorSignal(showSignal(hasSelection = true))
        assertEquals(SelectionMenuLifecycle.HIDDEN, controller.lifecycle)
        assertNull(controller.consumePendingShow(controller.showToken))
    }
}

private fun showSignal(
    hasSelection: Boolean = false,
    blocked: Boolean = false,
    selectionChanged: Boolean = true,
) = SelectionMenuSignal(
    textChanged = false,
    selectionChanged = selectionChanged,
    hasSelectionAfter = hasSelection,
    blocked = blocked,
    scrollChanged = false,
)
