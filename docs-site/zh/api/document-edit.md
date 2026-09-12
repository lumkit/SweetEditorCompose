# 文档与编辑

## 加载与快照

```kotlin
fun loadDocument(text: String)
fun loadDocument(document: EditorDocument)
fun getDocument(): EditorDocument
```

`EditorDocument` 是 `data class EditorDocument(val text: String)` —— 文本快照，不是 native 句柄。

`loadDocument` 替换缓冲区并发送 `DocumentLoadedEvent`。Ready 前被忽略；此时 `getDocument()` 返回 `initialText`。

## 插入 / 替换 / 删除

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

`EditorTextEdit(range, newText)` 做区间替换。`applyTextEdits` 中的多条编辑作为一次动作。

`insertSnippet` 解析片段占位符（`$0`、`${1:name}` …），并可进入联动编辑 tab stop。

## 撤销 / 重做

```kotlin
fun undo()
fun redo()
fun canUndo(): Boolean
fun canRedo(): Boolean
```

Ready 前 `canUndo` / `canRedo` 为 `false`。

## 行操作

```kotlin
fun moveLineUp()
fun moveLineDown()
fun copyLineUp()
fun copyLineDown()
fun deleteLine()
fun insertLineAbove()
fun insertLineBelow()
```

作用于当前光标行（内核支持时也可作用于选区）。默认键位会绑定其中若干项，见 [键位](./keymap.md)。
