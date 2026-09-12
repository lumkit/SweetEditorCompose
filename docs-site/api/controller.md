# Controller

```kotlin
@Stable
class SweetEditorController(initialText: String = "")
```

`initialText` is used until `loadDocument` runs, and as the fallback for `getDocument()` before ready.

## Ready

| API | Behavior |
|---|---|
| `isReady` | `true` after the composable created the native editor |
| `whenReady { }` | Runs now if ready; otherwise queued until attach + create |
| `events` | `EditorEventBus` — see [Events](./events.md) |
| `dispose()` | Clears listeners and ready callbacks. Does **not** `free_editor` |

Commands issued while `session` is null or not ready are no-ops (`session?.…`). Getters return defaults:

- `getDocument()` → `EditorDocument(initialText)`
- `getTotalLineCount()` → `1`
- `canUndo()` / `canRedo()` → `false`
- most other getters → `null` or empty string

## One editor

`attach` throws if the same controller is already bound to a different session. Recomposition of the same `SweetEditor` is fine.

## Configuration overrides

These keep a value even across detach/reattach of the same controller instance:

- `applyTheme` / `getTheme`
- `setSettings` / `getSettings`
- `setKeyMap` / `getKeyMap`
- `setLanguageConfiguration` / `getLanguageConfiguration`
- `setMetadata` / `getMetadata`
- `setEditorIconProvider` / `getEditorIconProvider`

`EditorMetadata` is a marker interface you implement. `EditorIconProvider.getIcon(iconId)` returns an `ImageBitmap?` for gutter / inlay icons.

## Event helpers

`onTextChanged`, `onCursorChanged`, `onSelectionChanged`, `onScrollChanged`, `onScaleChanged`, `onDocumentLoaded`, `onFoldToggle`, `onGutterIconClick`, `onInlayHintClick`, `onCodeLensClick`, `onLongPress`, `onDoubleTap` each return an unsubscribe `() -> Unit`.
