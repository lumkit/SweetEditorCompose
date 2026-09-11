package io.github.lumkit.sweeteditor.selection

import io.github.lumkit.sweeteditor.SelectionMenuContext
import io.github.lumkit.sweeteditor.SelectionMenuItem
import io.github.lumkit.sweeteditor.SelectionMenuItemProvider
import io.github.lumkit.sweeteditor.core.protocol.AnimationFlag
import io.github.lumkit.sweeteditor.core.protocol.EditorActionResult
import io.github.lumkit.sweeteditor.core.protocol.InteractionFlag

internal enum class SelectionMenuLifecycle {
    HIDDEN,
    PENDING_SHOW,
    VISIBLE,
    SUSPENDED,
}

internal data class SelectionMenuSignal(
    val textChanged: Boolean,
    val selectionChanged: Boolean,
    val hasSelectionAfter: Boolean,
    val blocked: Boolean,
    val scrollChanged: Boolean,
)

internal fun EditorActionResult.toSelectionMenuSignal(): SelectionMenuSignal {
    val viewportMotion =
        (animationFlags and (AnimationFlag.EDGE_SCROLL or AnimationFlag.FLING)) != 0
    return SelectionMenuSignal(
        textChanged = textChanges.isNotEmpty(),
        selectionChanged = selectionChanged,
        hasSelectionAfter = hasSelectionAfter,
        blocked = interactionFlags != InteractionFlag.NONE || viewportMotion,
        scrollChanged = scrollChanged,
    )
}

internal class SelectionMenuController(
    private val enabled: Boolean,
    private val buildContext: (Boolean) -> SelectionMenuContext,
) {
    var provider: SelectionMenuItemProvider? = null
    var lifecycle: SelectionMenuLifecycle = SelectionMenuLifecycle.HIDDEN
        private set
    var showToken: Int = 0
        private set
    private var hasSelection: Boolean = false
    private var blocked: Boolean = false

    fun onEditorSignal(signal: SelectionMenuSignal) {
        if (!enabled) {
            hide()
            return
        }
        hasSelection = signal.hasSelectionAfter
        blocked = signal.blocked
        if (signal.textChanged || !hasSelection) {
            hide()
            return
        }
        val wantsShow = signal.selectionChanged
        if (blocked) {
            if (wantsShow || lifecycle != SelectionMenuLifecycle.HIDDEN) {
                suspendMenu()
            }
            return
        }
        if (wantsShow || lifecycle == SelectionMenuLifecycle.SUSPENDED) {
            scheduleShow()
        }
    }

    fun consumePendingShow(token: Int): List<SelectionMenuItem>? {
        if (!enabled || token != showToken || lifecycle != SelectionMenuLifecycle.PENDING_SHOW) {
            return null
        }
        if (!hasSelection) {
            hide()
            return null
        }
        if (blocked) {
            suspendMenu()
            return null
        }
        val items = buildItems(hasSelection)
        if (items.isEmpty()) {
            hide()
            return null
        }
        lifecycle = SelectionMenuLifecycle.VISIBLE
        return items
    }

    fun hide() {
        lifecycle = SelectionMenuLifecycle.HIDDEN
        showToken += 1
    }

    fun dispose() {
        hide()
        provider = null
    }

    private fun scheduleShow() {
        if (!enabled || !hasSelection) {
            hide()
            return
        }
        if (blocked) {
            suspendMenu()
            return
        }
        lifecycle = SelectionMenuLifecycle.PENDING_SHOW
        showToken += 1
    }

    private fun suspendMenu() {
        lifecycle = SelectionMenuLifecycle.SUSPENDED
        showToken += 1
    }

    private fun buildItems(hasSelection: Boolean): List<SelectionMenuItem> {
        val custom = provider
        if (custom != null) {
            return custom.provideMenuItems(buildContext(hasSelection))
        }
        return defaultItems(hasSelection)
    }

    companion object {
        const val SHOW_DELAY_MS: Long = 100

        fun defaultItems(hasSelection: Boolean): List<SelectionMenuItem> = listOf(
            SelectionMenuItem(
                id = SelectionMenuItem.ACTION_CUT,
                label = "Cut",
                enabled = hasSelection,
            ),
            SelectionMenuItem(
                id = SelectionMenuItem.ACTION_COPY,
                label = "Copy",
                enabled = hasSelection,
            ),
            SelectionMenuItem(id = SelectionMenuItem.ACTION_PASTE, label = "Paste"),
            SelectionMenuItem(id = SelectionMenuItem.ACTION_SELECT_ALL, label = "Select All"),
        )
    }
}
