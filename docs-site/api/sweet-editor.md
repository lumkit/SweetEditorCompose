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

Draws the editor, owns the native session, and attaches `controller`.

| Parameter | Role |
|---|---|
| `modifier` | Size, padding, clipping. The editor fills the box. |
| `controller` | Command and event entry. One instance per `SweetEditor`. |
| `theme` | Colors, font family, optional `rangeEffects`. Applied every composition. |
| `settings` | Wrap, indent, gutter, whitespace, decoration refresh. |
| `keyMap` | `null` uses `EditorKeyMap.defaultKeyMap()` (`vscode()`). |

```kotlin
@Composable
fun rememberSweetEditorController(initialText: String = ""): SweetEditorController
```

Equivalent to `remember { SweetEditorController(initialText) }`.

## Side effects inside the composable

Each frame (via `SideEffect`) the session:

- binds the platform clipboard
- applies `keyMap` (or the default)
- applies `theme` and `settings`
- notifies the core if font metrics changed

Imperative `controller.applyTheme` / `setSettings` / `setKeyMap` after ready still work; the last write wins.

## Lifetime

Leaving composition detaches the controller and frees the native editor. `controller.dispose()` does **not** free native memory.

## Loading UI

On Web, until the C ABI is ready, the composable waits (up to ~1800 frames) then shows a load error. Other platforms typically become ready on first composition.
