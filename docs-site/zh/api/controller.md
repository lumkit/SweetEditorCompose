# Controller

```kotlin
@Stable
class SweetEditorController(initialText: String = "")
```

在调用 `loadDocument` 之前使用 `initialText`；Ready 前 `getDocument()` 也回退到它。

## Ready

| API | 行为 |
|---|---|
| `isReady` | Composable 创建完 native 编辑器后为 `true` |
| `whenReady { }` | 已就绪立刻跑，否则排队到 attach 且创建完成 |
| `events` | `EditorEventBus`，见 [事件](./events.md) |
| `dispose()` | 清监听和 ready 回调。**不会** `free_editor` |

`session` 为空或未就绪时，命令是空操作（`session?.…`）。Getter 返回默认值：

- `getDocument()` → `EditorDocument(initialText)`
- `getTotalLineCount()` → `1`
- `canUndo()` / `canRedo()` → `false`
- 其它多数 getter → `null` 或空字符串

## 一个编辑器

若同一 Controller 已绑到另一个 session，`attach` 会抛错。同一个 `SweetEditor` 重组没问题。

## 配置覆盖

下列值在同一 Controller 实例 detach/reattach 后仍保留：

- `applyTheme` / `getTheme`
- `setSettings` / `getSettings`
- `setKeyMap` / `getKeyMap`
- `setLanguageConfiguration` / `getLanguageConfiguration`
- `setMetadata` / `getMetadata`
- `setEditorIconProvider` / `getEditorIconProvider`

`EditorMetadata` 是宿主实现的标记接口。`EditorIconProvider.getIcon(iconId)` 为行号区 / inlay 图标返回 `ImageBitmap?`。

## 事件助手

`onTextChanged`、`onCursorChanged`、`onSelectionChanged`、`onScrollChanged`、`onScaleChanged`、`onDocumentLoaded`、`onFoldToggle`、`onGutterIconClick`、`onInlayHintClick`、`onCodeLensClick`、`onLongPress`、`onDoubleTap` 都返回取消订阅的 `() -> Unit`。
