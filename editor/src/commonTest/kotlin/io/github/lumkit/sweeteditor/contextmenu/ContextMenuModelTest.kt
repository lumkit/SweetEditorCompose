package io.github.lumkit.sweeteditor.contextmenu

import io.github.lumkit.sweeteditor.ContextMenuItem
import io.github.lumkit.sweeteditor.ContextMenuItemProvider
import io.github.lumkit.sweeteditor.ContextMenuRequest
import io.github.lumkit.sweeteditor.ContextMenuTriggerKind
import io.github.lumkit.sweeteditor.EditorPoint
import io.github.lumkit.sweeteditor.HitTarget
import io.github.lumkit.sweeteditor.HitTargetType
import io.github.lumkit.sweeteditor.TextPosition
import io.github.lumkit.sweeteditor.TextRange
import io.github.lumkit.sweeteditor.core.protocol.GestureType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ContextMenuModelTest {
    @Test
    fun defaultMenuIncludesPasteAndSelectAll() {
        val sections = defaultContextMenuSections(request(hasSelection = false))
        assertEquals(listOf("paste", "select_all"), sections.single().items.map { it.id })
    }

    @Test
    fun selectionAddsCutAndCopySection() {
        val sections = defaultContextMenuSections(
            request(
                hasSelection = true,
                selection = TextRange(TextPosition(0, 0), TextPosition(0, 4)),
            ),
        )
        assertEquals(
            listOf(listOf("cut", "copy"), listOf("paste", "select_all")),
            sections.map { it.items.map { item -> item.id } },
        )
    }

    @Test
    fun linkTargetAddsOpenAndCopyLink() {
        val sections = defaultContextMenuSections(
            request(
                hitTarget = HitTarget(HitTargetType.LINK, line = 1, column = 2),
                linkTarget = "https://example.com",
            ),
        )
        assertEquals("open_link", sections.first().items.first().id)
        assertEquals("copy_link", sections.first().items[1].id)
    }

    @Test
    fun emptyProviderHidesMenu() {
        val provider = ContextMenuItemProvider { emptyList() }
        assertTrue(buildContextMenuSections(provider, request()).isEmpty())
    }

    @Test
    fun customProviderReplacesDefaults() {
        val provider = ContextMenuItemProvider {
            listOf(
                io.github.lumkit.sweeteditor.ContextMenuSection(
                    listOf(ContextMenuItem(id = "wrap", label = "Wrap")),
                ),
            )
        }
        val sections = buildContextMenuSections(provider, request(hasSelection = true))
        assertEquals(listOf("wrap"), sections.single().items.map { it.id })
    }

    @Test
    fun rightClickShowsAndTapDismisses() {
        assertEquals(ContextMenuSignal.SHOW, contextMenuSignal(enabled = true, GestureType.CONTEXT_MENU))
        assertEquals(ContextMenuSignal.DISMISS, contextMenuSignal(enabled = true, GestureType.TAP))
        assertEquals(ContextMenuSignal.IGNORE, contextMenuSignal(enabled = false, GestureType.CONTEXT_MENU))
    }
}

private fun request(
    hasSelection: Boolean = false,
    selection: TextRange? = null,
    hitTarget: HitTarget = HitTarget(HitTargetType.NONE),
    linkTarget: String = "",
) = ContextMenuRequest(
    triggerKind = ContextMenuTriggerKind.RIGHT_CLICK,
    cursorPosition = TextPosition(0, 0),
    locationInEditor = EditorPoint(12f, 24f),
    hasSelection = hasSelection,
    selection = selection,
    hitTarget = hitTarget,
    linkTarget = linkTarget,
)
