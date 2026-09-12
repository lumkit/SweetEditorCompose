# SweetEditor

```kotlin
@Composable
fun SweetEditor(
    modifier: Modifier = Modifier,
    controller: SweetEditorController,
    theme: EditorTheme = EditorTheme(),
    settings: EditorSettings = EditorSettings(),
    keyMap: EditorKeyMap? = null,
)
```

绘制编辑器、拥有 native session，并 `attach` `controller`。

| 参数 | 作用 |
|---|---|
| `modifier` | 尺寸、边距、裁剪。编辑器铺满盒子。 |
| `controller` | 命令与事件入口。一个实例对应一个 `SweetEditor`。 |
| `theme` | 颜色、字体、可选 `rangeEffects`。每次组合都会应用。 |
| `settings` | 换行、缩进、行号区、空白、装饰刷新。 |
| `keyMap` | `null` 使用 `EditorKeyMap.defaultKeyMap()`（即 `vscode()`）。 |

```kotlin
@Composable
fun rememberSweetEditorController(initialText: String = ""): SweetEditorController
```

等价于 `remember { SweetEditorController(initialText) }`。

## Composable 内的副作用

每帧（`SideEffect`）session 会：

- 绑定平台剪贴板
- 应用 `keyMap`（或默认）
- 应用 `theme` 与 `settings`
- 字体度量变化时通知内核

就绪后仍可用 `controller.applyTheme` / `setSettings` / `setKeyMap`；最后一次写入生效。

## 生命周期

离开组合会 detach Controller 并释放 native 编辑器。`controller.dispose()` **不会**释放 native。

## 加载界面

Web 上会等到 C ABI 就绪（最多约 1800 帧），超时显示加载错误。其它平台通常在首次组合即可就绪。
