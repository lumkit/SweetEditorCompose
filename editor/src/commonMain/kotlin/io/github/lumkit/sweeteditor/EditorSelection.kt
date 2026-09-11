package io.github.lumkit.sweeteditor

data class SelectionMenuItem(
    val id: String,
    val label: String,
    val enabled: Boolean = true,
    val iconId: Int? = null,
) {
    companion object {
        const val ACTION_CUT: String = "cut"
        const val ACTION_COPY: String = "copy"
        const val ACTION_DELETE: String = "delete"
        const val ACTION_PASTE: String = "paste"
        const val ACTION_SELECT_ALL: String = "select_all"
    }
}

data class SelectionMenuContext(
    val hasSelection: Boolean,
    val cursorPosition: TextPosition,
    val selection: TextRange? = null,
    val selectedText: String = "",
)

fun interface SelectionMenuItemProvider {
    fun provideMenuItems(context: SelectionMenuContext): List<SelectionMenuItem>
}

data class SelectionMenuItemClickEvent(
    val itemId: String,
) : EditorEvent
