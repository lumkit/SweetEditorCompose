# 事件

用类型化助手或 EventBus 订阅。

```kotlin
val unsub = controller.onTextChanged { event ->
    // event.changes, event.kind, event.source
}
unsub()

controller.events.subscribe<CursorChangedEvent> { }
```

`dispose()` 会清空 bus。Composable detach 时**不会**自动清 `events`（除非你调用 `dispose`）。

## EditorEvent 类型

| 类型 | 字段 |
|---|---|
| `TextChangedEvent` | `changes: List<TextChange>`、`kind`、`source` |
| `CursorChangedEvent` | `cursorPosition` |
| `SelectionChangedEvent` | `hasSelection`、`selection`、`cursorPosition` |
| `ScrollChangedEvent` | `scrollX`、`scrollY` |
| `ScaleChangedEvent` | `scale` |
| `DocumentLoadedEvent` | `lineCount` |
| `FoldToggleEvent` | `line` |
| `GutterIconClickEvent` | `line`、`column`、`iconId`、`locationInEditor` |
| `InlayHintClickEvent` | `line`、`column`、`locationInEditor` |
| `CodeLensClickEvent` | `line`、`column`、`locationInEditor` |
| `LongPressEvent` | `cursorPosition`、`locationInEditor` |
| `DoubleTapEvent` | `cursorPosition`、`locationInEditor` |
| `ContextMenuEvent` | `cursorPosition`、`locationInEditor` |
| `ContextMenuItemClickEvent` | `item`、`request` |
| `SelectionMenuItemClickEvent` | `itemId` |
| `LinkClickEvent` | `line`、`column`、`target`、`locationInEditor` |

`TextChange` 是 `range` + `newText`。

## 来源与种类

`EditorActionSource`：`NONE`、`SETUP`、`PROGRAMMATIC`、`KEYBOARD`、`IME`、`GESTURE`、`ANIMATION`、`DECORATION`、`FOLDING`、`SEARCH`、`LINKED_EDITING`、`DIFF`。

`TextChangeKind`：`NONE`、`INSERTION`、`REPLACEMENT`、`DELETION`、`MOVE`、`UNDO`、`REDO`、`MIXED`。

查询 API 不返回动作结果。变更走 session 唯一派发，再变成这些事件。
