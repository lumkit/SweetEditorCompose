package io.github.lumkit.sweeteditor

enum class ContextMenuTriggerKind {
    LONG_PRESS,
    RIGHT_CLICK,
}

enum class HitTargetType {
    NONE,
    INLAY_HINT_TEXT,
    INLAY_HINT_ICON,
    INLAY_HINT_COLOR,
    CODELENS,
    LINK,
    GUTTER_ICON,
    FOLD_GUTTER,
    FOLD_PLACEHOLDER,
}

data class HitTarget(
    val type: HitTargetType,
    val line: Int = 0,
    val column: Int = 0,
    val iconId: Int = 0,
    val colorValue: Int = 0,
)

data class EditorPoint(
    val x: Float,
    val y: Float,
)

data class ContextMenuItem(
    val id: String,
    val label: String,
    val secondaryLabel: String? = null,
    val enabled: Boolean = true,
) {
    companion object {
        const val ACTION_OPEN_LINK: String = "open_link"
        const val ACTION_COPY_LINK: String = "copy_link"
        const val ACTION_CUT: String = "cut"
        const val ACTION_COPY: String = "copy"
        const val ACTION_PASTE: String = "paste"
        const val ACTION_SELECT_ALL: String = "select_all"
    }
}

data class ContextMenuSection(
    val items: List<ContextMenuItem>,
)

data class ContextMenuRequest(
    val triggerKind: ContextMenuTriggerKind,
    val cursorPosition: TextPosition,
    val locationInEditor: EditorPoint,
    val hasSelection: Boolean,
    val selection: TextRange? = null,
    val hitTarget: HitTarget,
    val linkTarget: String = "",
)

fun interface ContextMenuItemProvider {
    fun provideMenuItems(request: ContextMenuRequest): List<ContextMenuSection>
}

data class ContextMenuEvent(
    val cursorPosition: TextPosition,
    val locationInEditor: EditorPoint,
) : EditorEvent

data class ContextMenuItemClickEvent(
    val item: ContextMenuItem,
    val request: ContextMenuRequest,
) : EditorEvent

data class LinkClickEvent(
    val line: Int,
    val column: Int,
    val target: String,
    val locationInEditor: EditorPoint,
) : EditorEvent
