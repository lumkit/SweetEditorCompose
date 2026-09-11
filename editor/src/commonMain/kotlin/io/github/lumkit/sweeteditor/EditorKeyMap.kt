package io.github.lumkit.sweeteditor

import io.github.lumkit.sweeteditor.core.protocol.EditorBuiltinCommand
import io.github.lumkit.sweeteditor.core.protocol.KeyBinding
import io.github.lumkit.sweeteditor.core.protocol.KeyChord
import io.github.lumkit.sweeteditor.core.protocol.KeyCode
import io.github.lumkit.sweeteditor.core.protocol.KeyModifier

data class EditorKeyChord(
    val modifiers: Int,
    val keyCode: Int,
)

data class EditorKeyBinding(
    val first: EditorKeyChord,
    val second: EditorKeyChord = EditorKeyChord(KeyModifier.NONE, KeyCode.NONE),
    val command: Int,
)

fun interface EditorShortcutHandler {
    fun onShortcut(binding: EditorKeyBinding, controller: SweetEditorController)
}

class EditorKeyMap {
    private val bindings = mutableListOf<EditorKeyBinding>()
    private val commands = mutableMapOf<Int, EditorShortcutHandler>()
    private var nextCustomId = EditorBuiltinCommand.TRIGGER_COMPLETION.value + 1
    internal var revision: Int = 0
        private set

    fun addBinding(binding: EditorKeyBinding) {
        bindings.removeAll { it.first == binding.first && it.second == binding.second }
        bindings += binding
        revision++
    }

    fun removeBinding(binding: EditorKeyBinding) {
        if (bindings.removeAll { it == binding }) {
            revision++
        }
    }

    fun registerCommand(binding: EditorKeyBinding, handler: EditorShortcutHandler): Int {
        var commandId = binding.command
        var resolved = binding
        if (commandId == EditorBuiltinCommand.NONE.value) {
            commandId = nextCustomId++
            resolved = binding.copy(command = commandId)
        } else if (commandId >= nextCustomId) {
            nextCustomId = commandId + 1
        }
        commands[commandId] = handler
        addBinding(resolved)
        return commandId
    }

    fun handlerFor(commandId: Int): EditorShortcutHandler? = commands[commandId]

    fun snapshotBindings(): List<EditorKeyBinding> = bindings.toList()

    internal fun toProtocolBindings(): List<KeyBinding> = bindings.map { binding ->
        KeyBinding(
            first = KeyChord(binding.first.modifiers, binding.first.keyCode),
            second = KeyChord(binding.second.modifiers, binding.second.keyCode),
            command = binding.command,
        )
    }

    companion object {
        fun defaultKeyMap(): EditorKeyMap = vscode()

        fun vscode(): EditorKeyMap = EditorKeyMap().apply {
            addCommonBindings()
            bind(KeyModifier.CTRL or KeyModifier.SHIFT, KeyCode.Z, EditorBuiltinCommand.REDO)
            bind(KeyModifier.META or KeyModifier.SHIFT, KeyCode.Z, EditorBuiltinCommand.REDO)
            bind(KeyModifier.CTRL, KeyCode.Y, EditorBuiltinCommand.REDO)
            bind(KeyModifier.META, KeyCode.Y, EditorBuiltinCommand.REDO)
            bind(KeyModifier.CTRL, KeyCode.ENTER, EditorBuiltinCommand.INSERT_LINE_BELOW)
            bind(KeyModifier.META, KeyCode.ENTER, EditorBuiltinCommand.INSERT_LINE_BELOW)
            bind(KeyModifier.CTRL or KeyModifier.SHIFT, KeyCode.ENTER, EditorBuiltinCommand.INSERT_LINE_ABOVE)
            bind(KeyModifier.META or KeyModifier.SHIFT, KeyCode.ENTER, EditorBuiltinCommand.INSERT_LINE_ABOVE)
            bind(KeyModifier.ALT, KeyCode.UP, EditorBuiltinCommand.MOVE_LINE_UP)
            bind(KeyModifier.ALT, KeyCode.DOWN, EditorBuiltinCommand.MOVE_LINE_DOWN)
            bind(KeyModifier.ALT or KeyModifier.SHIFT, KeyCode.UP, EditorBuiltinCommand.COPY_LINE_UP)
            bind(KeyModifier.ALT or KeyModifier.SHIFT, KeyCode.DOWN, EditorBuiltinCommand.COPY_LINE_DOWN)
            bind(KeyModifier.CTRL or KeyModifier.SHIFT, KeyCode.K, EditorBuiltinCommand.DELETE_LINE)
            bind(KeyModifier.META or KeyModifier.SHIFT, KeyCode.K, EditorBuiltinCommand.DELETE_LINE)
        }

        fun jetbrains(): EditorKeyMap = EditorKeyMap().apply {
            addCommonBindings()
            bind(KeyModifier.CTRL or KeyModifier.SHIFT, KeyCode.Z, EditorBuiltinCommand.REDO)
            bind(KeyModifier.META or KeyModifier.SHIFT, KeyCode.Z, EditorBuiltinCommand.REDO)
            bind(KeyModifier.CTRL, KeyCode.Y, EditorBuiltinCommand.DELETE_LINE)
            bind(KeyModifier.META, KeyCode.Y, EditorBuiltinCommand.DELETE_LINE)
            bind(KeyModifier.CTRL, KeyCode.D, EditorBuiltinCommand.COPY_LINE_DOWN)
            bind(KeyModifier.META, KeyCode.D, EditorBuiltinCommand.COPY_LINE_DOWN)
            bind(KeyModifier.SHIFT, KeyCode.ENTER, EditorBuiltinCommand.INSERT_LINE_BELOW)
            bind(KeyModifier.CTRL or KeyModifier.ALT, KeyCode.ENTER, EditorBuiltinCommand.INSERT_LINE_ABOVE)
            bind(KeyModifier.META or KeyModifier.ALT, KeyCode.ENTER, EditorBuiltinCommand.INSERT_LINE_ABOVE)
            bind(KeyModifier.ALT or KeyModifier.SHIFT, KeyCode.UP, EditorBuiltinCommand.MOVE_LINE_UP)
            bind(KeyModifier.ALT or KeyModifier.SHIFT, KeyCode.DOWN, EditorBuiltinCommand.MOVE_LINE_DOWN)
        }

        fun sublime(): EditorKeyMap = EditorKeyMap().apply {
            addCommonBindings()
            bind(KeyModifier.CTRL or KeyModifier.SHIFT, KeyCode.Z, EditorBuiltinCommand.REDO)
            bind(KeyModifier.META or KeyModifier.SHIFT, KeyCode.Z, EditorBuiltinCommand.REDO)
            bind(KeyModifier.CTRL, KeyCode.Y, EditorBuiltinCommand.REDO)
            bind(KeyModifier.META, KeyCode.Y, EditorBuiltinCommand.REDO)
            bind(KeyModifier.CTRL, KeyCode.ENTER, EditorBuiltinCommand.INSERT_LINE_BELOW)
            bind(KeyModifier.META, KeyCode.ENTER, EditorBuiltinCommand.INSERT_LINE_BELOW)
            bind(KeyModifier.CTRL or KeyModifier.SHIFT, KeyCode.ENTER, EditorBuiltinCommand.INSERT_LINE_ABOVE)
            bind(KeyModifier.META or KeyModifier.SHIFT, KeyCode.ENTER, EditorBuiltinCommand.INSERT_LINE_ABOVE)
            bind(KeyModifier.CTRL or KeyModifier.SHIFT, KeyCode.UP, EditorBuiltinCommand.MOVE_LINE_UP)
            bind(KeyModifier.CTRL or KeyModifier.SHIFT, KeyCode.DOWN, EditorBuiltinCommand.MOVE_LINE_DOWN)
            bind(KeyModifier.CTRL or KeyModifier.SHIFT, KeyCode.K, EditorBuiltinCommand.DELETE_LINE)
            bind(KeyModifier.META or KeyModifier.SHIFT, KeyCode.K, EditorBuiltinCommand.DELETE_LINE)
        }
    }
}

private fun EditorKeyMap.bind(modifiers: Int, keyCode: Int, command: EditorBuiltinCommand) {
    addBinding(EditorKeyBinding(EditorKeyChord(modifiers, keyCode), command = command.value))
}

private fun EditorKeyMap.addCommonBindings() {
    bind(KeyModifier.NONE, KeyCode.LEFT, EditorBuiltinCommand.CURSOR_LEFT)
    bind(KeyModifier.NONE, KeyCode.RIGHT, EditorBuiltinCommand.CURSOR_RIGHT)
    bind(KeyModifier.NONE, KeyCode.UP, EditorBuiltinCommand.CURSOR_UP)
    bind(KeyModifier.NONE, KeyCode.DOWN, EditorBuiltinCommand.CURSOR_DOWN)
    bind(KeyModifier.NONE, KeyCode.HOME, EditorBuiltinCommand.CURSOR_LINE_START)
    bind(KeyModifier.NONE, KeyCode.END, EditorBuiltinCommand.CURSOR_LINE_END)
    bind(KeyModifier.NONE, KeyCode.PAGE_UP, EditorBuiltinCommand.CURSOR_PAGE_UP)
    bind(KeyModifier.NONE, KeyCode.PAGE_DOWN, EditorBuiltinCommand.CURSOR_PAGE_DOWN)

    bind(KeyModifier.SHIFT, KeyCode.LEFT, EditorBuiltinCommand.SELECT_LEFT)
    bind(KeyModifier.SHIFT, KeyCode.RIGHT, EditorBuiltinCommand.SELECT_RIGHT)
    bind(KeyModifier.SHIFT, KeyCode.UP, EditorBuiltinCommand.SELECT_UP)
    bind(KeyModifier.SHIFT, KeyCode.DOWN, EditorBuiltinCommand.SELECT_DOWN)
    bind(KeyModifier.SHIFT, KeyCode.HOME, EditorBuiltinCommand.SELECT_LINE_START)
    bind(KeyModifier.SHIFT, KeyCode.END, EditorBuiltinCommand.SELECT_LINE_END)
    bind(KeyModifier.SHIFT, KeyCode.PAGE_UP, EditorBuiltinCommand.SELECT_PAGE_UP)
    bind(KeyModifier.SHIFT, KeyCode.PAGE_DOWN, EditorBuiltinCommand.SELECT_PAGE_DOWN)

    bind(KeyModifier.NONE, KeyCode.BACKSPACE, EditorBuiltinCommand.BACKSPACE)
    bind(KeyModifier.NONE, KeyCode.DELETE_KEY, EditorBuiltinCommand.DELETE_FORWARD)
    bind(KeyModifier.NONE, KeyCode.TAB, EditorBuiltinCommand.INSERT_TAB)
    bind(KeyModifier.NONE, KeyCode.ENTER, EditorBuiltinCommand.INSERT_NEWLINE)

    bind(KeyModifier.CTRL, KeyCode.A, EditorBuiltinCommand.SELECT_ALL)
    bind(KeyModifier.META, KeyCode.A, EditorBuiltinCommand.SELECT_ALL)
    bind(KeyModifier.CTRL, KeyCode.Z, EditorBuiltinCommand.UNDO)
    bind(KeyModifier.META, KeyCode.Z, EditorBuiltinCommand.UNDO)

    registerClipboard(KeyModifier.CTRL, KeyCode.C, EditorBuiltinCommand.COPY) { it.copy() }
    registerClipboard(KeyModifier.META, KeyCode.C, EditorBuiltinCommand.COPY) { it.copy() }
    registerClipboard(KeyModifier.CTRL, KeyCode.V, EditorBuiltinCommand.PASTE) { it.paste() }
    registerClipboard(KeyModifier.META, KeyCode.V, EditorBuiltinCommand.PASTE) { it.paste() }
    registerClipboard(KeyModifier.CTRL, KeyCode.X, EditorBuiltinCommand.CUT) { it.cut() }
    registerClipboard(KeyModifier.META, KeyCode.X, EditorBuiltinCommand.CUT) { it.cut() }

    bind(KeyModifier.CTRL, KeyCode.SPACE, EditorBuiltinCommand.TRIGGER_COMPLETION)
    bind(KeyModifier.META, KeyCode.SPACE, EditorBuiltinCommand.TRIGGER_COMPLETION)
}

private fun EditorKeyMap.registerClipboard(
    modifiers: Int,
    keyCode: Int,
    command: EditorBuiltinCommand,
    action: (SweetEditorController) -> Unit,
) {
    registerCommand(
        EditorKeyBinding(EditorKeyChord(modifiers, keyCode), command = command.value),
    ) { _, controller -> action(controller) }
}
