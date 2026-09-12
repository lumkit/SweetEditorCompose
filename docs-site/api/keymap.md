# Key map

```kotlin
data class EditorKeyChord(val modifiers: Int, val keyCode: Int)
data class EditorKeyBinding(
    val first: EditorKeyChord,
    val second: EditorKeyChord = EditorKeyChord(0, 0),
    val command: Int,
)

fun interface EditorShortcutHandler {
    fun onShortcut(binding: EditorKeyBinding, controller: SweetEditorController)
}

class EditorKeyMap {
    fun addBinding(binding: EditorKeyBinding)
    fun removeBinding(binding: EditorKeyBinding)
    fun registerCommand(binding: EditorKeyBinding, handler: EditorShortcutHandler): Int
    fun handlerFor(commandId: Int): EditorShortcutHandler?
    fun snapshotBindings(): List<EditorKeyBinding>

    companion object {
        fun defaultKeyMap(): EditorKeyMap  // vscode()
        fun vscode(): EditorKeyMap
        fun jetbrains(): EditorKeyMap
        fun sublime(): EditorKeyMap
    }
}
```

Pass the map to `SweetEditor(keyMap = …)` or `controller.setKeyMap`. `null` on the composable uses `defaultKeyMap()`.

## Presets

All presets share: arrows, Home/End, Page Up/Down, Shift-select, Backspace/Delete/Tab/Enter, Select All, Undo, clipboard, Trigger Completion (`Ctrl`/`⌘`+Space).

Differences:

| | VS Code / Sublime | JetBrains |
|---|---|---|
| Redo | Ctrl/⌘+Shift+Z, Ctrl/⌘+Y | Ctrl/⌘+Shift+Z |
| Delete line | Ctrl/⌘+Shift+K | Ctrl/⌘+Y |
| Duplicate line | Alt+Shift+Up/Down (VS Code) | Ctrl/⌘+D (down) |
| Move line | Alt+Up/Down (VS Code); Ctrl+Shift+Up/Down (Sublime) | Alt+Shift+Up/Down |
| Insert line below | Ctrl/⌘+Enter | Shift+Enter |
| Insert line above | Ctrl/⌘+Shift+Enter | Ctrl/⌥+Enter (or ⌘+⌥+Enter) |

## Custom command

```kotlin
val map = EditorKeyMap.vscode()
map.registerCommand(
    EditorKeyBinding(first = EditorKeyChord(modifiers = /* ctrl/meta bits */, keyCode = /* key */), command = 0),
) { _, controller ->
    controller.triggerCompletion()
}
```

`command = 0` allocates a custom id. `registerCommand` returns that id.

Chord integer encodings match the native key table. Prefer starting from a preset and `snapshotBindings()` rather than inventing raw codes.
