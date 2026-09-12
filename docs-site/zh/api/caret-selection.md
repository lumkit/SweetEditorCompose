# 光标、选区、滚动

## 位置

```kotlin
data class TextPosition(val line: Int, val column: Int)
data class TextRange(val start: TextPosition, val end: TextPosition)
```

行列均为 **0 起**。

```kotlin
enum class ScrollBehavior { GOTO_TOP, GOTO_CENTER, GOTO_BOTTOM }
```

## 光标与选区

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

Ready 前 getter 为 `null` / `""`，setter 空操作。

## 滚动与布局查询

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
data class VisibleLineRange(val startLine: Int, val endLine: Int) // endLine < startLine 时 isEmpty
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

Ready 前 `getTotalLineCount()` 为 `1`。

## 剪贴板

```kotlin
fun copy(): Boolean
fun copyToClipboard(): Boolean   // copy() 别名
fun cut(): Boolean
fun cutToClipboard(): Boolean    // cut() 别名
fun paste()
fun pasteFromClipboard()         // 同 paste()
```

无 session 或无可复制内容时，`copy` / `cut` 返回 `false`。
