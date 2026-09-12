# Span

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

`fontStyle` 可按位或（`BOLD or ITALIC`）。

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

先注册 `styleId` 再在 span 里使用。同一 id 再次注册会更新外观。

无参 `clearHighlights()` 会清掉 SYNTAX、SEMANTIC、OVERLAY。

走查见 [语法高亮](/zh/guide/highlights)。
