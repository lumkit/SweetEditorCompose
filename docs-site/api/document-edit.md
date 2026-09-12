# Document and edits

## Load and snapshot

```kotlin
fun loadDocument(text: String)
fun loadDocument(document: EditorDocument)
fun getDocument(): EditorDocument
```

`EditorDocument` is `data class EditorDocument(val text: String)` — a snapshot, not a native handle.

`loadDocument` replaces the buffer and emits `DocumentLoadedEvent`. Before ready it is ignored; `getDocument()` then returns `initialText`.

## Insert / replace / delete

```kotlin
fun insertText(text: String)
fun insertTextAt(line: Int, column: Int, text: String)
fun replaceText(start: TextPosition, end: TextPosition, text: String)
fun replaceText(startLine: Int, startColumn: Int, endLine: Int, endColumn: Int, text: String)
fun deleteText(start: TextPosition, end: TextPosition)
fun deleteText(startLine: Int, startColumn: Int, endLine: Int, endColumn: Int)
fun applyTextEdits(edits: List<EditorTextEdit>)
fun insertSnippet(template: String)
fun backspace()
```

`EditorTextEdit(range, newText)` applies a range replacement. Multiple edits in `applyTextEdits` are applied as one action.

`insertSnippet` interprets snippet placeholders (`$0`, `${1:name}`, …) and can start linked-editing tab stops.

## Undo / redo

```kotlin
fun undo()
fun redo()
fun canUndo(): Boolean
fun canRedo(): Boolean
```

`canUndo` / `canRedo` are `false` before ready.

## Line operations

```kotlin
fun moveLineUp()
fun moveLineDown()
fun copyLineUp()
fun copyLineDown()
fun deleteLine()
fun insertLineAbove()
fun insertLineBelow()
```

These operate on the current caret line (or selection, when the core supports it). Default key maps bind several of them — see [Key map](./keymap.md).
