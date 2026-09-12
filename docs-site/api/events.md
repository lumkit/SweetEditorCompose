# Events

Subscribe with typed helpers or the bus.

```kotlin
val unsub = controller.onTextChanged { event ->
    // event.changes, event.kind, event.source
}
unsub()

controller.events.subscribe<CursorChangedEvent> { }
```

`dispose()` and leaving composition (via controller `dispose` only) clear the bus. The composable does not auto-clear `events` on detach.

## EditorEvent types

| Type | Fields |
|---|---|
| `TextChangedEvent` | `changes: List<TextChange>`, `kind`, `source` |
| `CursorChangedEvent` | `cursorPosition` |
| `SelectionChangedEvent` | `hasSelection`, `selection`, `cursorPosition` |
| `ScrollChangedEvent` | `scrollX`, `scrollY` |
| `ScaleChangedEvent` | `scale` |
| `DocumentLoadedEvent` | `lineCount` |
| `FoldToggleEvent` | `line` |
| `GutterIconClickEvent` | `line`, `column`, `iconId`, `locationInEditor` |
| `InlayHintClickEvent` | `line`, `column`, `locationInEditor` |
| `CodeLensClickEvent` | `line`, `column`, `locationInEditor` |
| `LongPressEvent` | `cursorPosition`, `locationInEditor` |
| `DoubleTapEvent` | `cursorPosition`, `locationInEditor` |
| `ContextMenuEvent` | `cursorPosition`, `locationInEditor` |
| `ContextMenuItemClickEvent` | `item`, `request` |
| `SelectionMenuItemClickEvent` | `itemId` |
| `LinkClickEvent` | `line`, `column`, `target`, `locationInEditor` |

`TextChange` is `range` + `newText`.

## Source and kind

`EditorActionSource`: `NONE`, `SETUP`, `PROGRAMMATIC`, `KEYBOARD`, `IME`, `GESTURE`, `ANIMATION`, `DECORATION`, `FOLDING`, `SEARCH`, `LINKED_EDITING`, `DIFF`.

`TextChangeKind`: `NONE`, `INSERTION`, `REPLACEMENT`, `DELETION`, `MOVE`, `UNDO`, `REDO`, `MIXED`.

Query APIs never return an action result. Mutations go through one session dispatch and then these events.
