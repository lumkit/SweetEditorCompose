# 键位

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

传给 `SweetEditor(keyMap = …)` 或 `controller.setKeyMap`。Composable 上为 `null` 时用 `defaultKeyMap()`。

## 预设

共同部分：方向键、Home/End、Page Up/Down、Shift 选区、Backspace/Delete/Tab/Enter、全选、撤销、剪贴板、触发补全（`Ctrl`/`⌘`+Space）。

差异：

| | VS Code / Sublime | JetBrains |
|---|---|---|
| 重做 | Ctrl/⌘+Shift+Z、Ctrl/⌘+Y | Ctrl/⌘+Shift+Z |
| 删行 | Ctrl/⌘+Shift+K | Ctrl/⌘+Y |
| 复制行 | Alt+Shift+Up/Down（VS Code） | Ctrl/⌘+D（向下） |
| 移动行 | Alt+Up/Down（VS Code）；Ctrl+Shift+Up/Down（Sublime） | Alt+Shift+Up/Down |
| 下方插行 | Ctrl/⌘+Enter | Shift+Enter |
| 上方插行 | Ctrl/⌘+Shift+Enter | Ctrl/⌥+Enter（或 ⌘+⌥+Enter） |

## 自定义命令

```kotlin
val map = EditorKeyMap.vscode()
map.registerCommand(
    EditorKeyBinding(first = EditorKeyChord(modifiers = /* ctrl/meta 位 */, keyCode = /* 键 */), command = 0),
) { _, controller ->
    controller.triggerCompletion()
}
```

`command = 0` 会分配自定义 id。`registerCommand` 返回该 id。

和弦整数编码与原生键表一致。建议从预设起步，用 `snapshotBindings()`，不要手编原始码。
