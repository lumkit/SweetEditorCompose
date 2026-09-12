# Caret, selection, scroll

## Positions

```kotlin
data class TextPosition(val line: Int, val column: Int)
data class TextRange(val start: TextPosition, val end: TextPosition)
```

Line and column are **0-based**.

```kotlin
enum class ScrollBehavior { GOTO_TOP, GOTO_CENTER, GOTO_BOTTOM }
```

## Caret and selection

```kotlin
fun getCursorPosition(): TextPosition?
fun setCursorPosition(line: Int, column: Int)
fun setCursorPosition(position: TextPosition)
fun getSelection(): TextRange?
fun setSelection(start: TextPosition, end: TextPosition)
fun getSelectedText(): String
fun getWordRangeAtCursor(): TextRange?
fun getWordAtCursor(): String
fun selectAll()
```

Before ready: getters are `null` / `""`. Setters no-op.

## Scroll and layout queries

```kotlin
fun gotoPosition(line: Int, column: Int)
fun scrollToLine(line: Int, behavior: ScrollBehavior = ScrollBehavior.GOTO_CENTER)
fun setScroll(scrollX: Float, scrollY: Float)
fun ensureCursorVisible()
fun getCursorRect(): EditorCursorRect?
fun getPositionRect(line: Int, column: Int): EditorCursorRect?
fun getVisibleLineRange(): VisibleLineRange?
fun getScrollMetrics(): EditorScrollMetrics?
fun getTotalLineCount(): Int
```

```kotlin
data class EditorCursorRect(val x: Float, val y: Float, val height: Float)
data class VisibleLineRange(val startLine: Int, val endLine: Int) // isEmpty if endLine < startLine
data class EditorScrollMetrics(
    val scale: Float,
    val scrollX: Float, val scrollY: Float,
    val maxScrollX: Float, val maxScrollY: Float,
    val contentWidth: Float, val contentHeight: Float,
    val viewportWidth: Float, val viewportHeight: Float,
    val textAreaX: Float, val textAreaWidth: Float,
    val canScrollX: Boolean, val canScrollY: Boolean,
)
```

`getTotalLineCount()` is `1` before ready.

## Clipboard

```kotlin
fun copy(): Boolean
fun copyToClipboard(): Boolean   // alias of copy()
fun cut(): Boolean
fun cutToClipboard(): Boolean    // alias of cut()
fun paste()
fun pasteFromClipboard()         // same as paste()
```

`copy` / `cut` return `false` if there is no session or nothing to copy.
