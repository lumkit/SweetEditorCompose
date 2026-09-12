# Spans

```kotlin
enum class EditorSpanLayer { SYNTAX, SEMANTIC, OVERLAY }

object EditorFontStyle {
    const val NORMAL = 0
    const val BOLD = 1
    const val ITALIC = 2
    const val STRIKETHROUGH = 4
}

data class StyleSpan(val column: Int, val length: Int, val styleId: Int)
data class EditorTextStyle(
    val color: Int,
    val backgroundColor: Int = 0,
    val fontStyle: Int = EditorFontStyle.NORMAL,
)
```

`fontStyle` bits can be OR-ed (`BOLD or ITALIC`).

## Controller

```kotlin
fun registerTextStyle(styleId: Int, color: Int, backgroundColor: Int = 0, fontStyle: Int = 0)
fun registerBatchTextStyles(styles: Map<Int, EditorTextStyle>)
fun setLineSpans(line: Int, layer: EditorSpanLayer, spans: List<StyleSpan>)
fun setBatchLineSpans(layer: EditorSpanLayer, spansByLine: Map<Int, List<StyleSpan>>)
fun clearLineSpans(line: Int, layer: EditorSpanLayer)
fun clearHighlights()
fun clearHighlights(layer: EditorSpanLayer)
```

Register a `styleId` before using it in a span. Re-registering the same id updates the look.

`clearHighlights()` without a layer clears SYNTAX, SEMANTIC, and OVERLAY.

See [Syntax highlights](/guide/highlights) for a walkthrough.
