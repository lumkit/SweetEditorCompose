# 语法高亮

库没有内置词法器。先注册样式，再按行、按层写入 `StyleSpan`。

```kotlin
controller.whenReady {
    controller.registerTextStyle(
        styleId = 1,
        color = 0xFFC586C0.toInt(),
        fontStyle = EditorFontStyle.BOLD,
    )
    controller.setLineSpans(
        line = 0,
        layer = EditorSpanLayer.SYNTAX,
        spans = listOf(StyleSpan(column = 0, length = 3, styleId = 1)),
    )
}
```

## 层

| 层 | 常见用途 |
|---|---|
| `SYNTAX` | 关键字、字符串、注释 |
| `SEMANTIC` | LSP 风格语义 token |
| `OVERLAY` | 临时强调 |

层会叠加。清一层不会清其它层：

```kotlin
controller.clearHighlights(EditorSpanLayer.OVERLAY)
```

无参 `clearHighlights()` 会清掉所有 span 层。

## 批量

多行变更请用批量 API：

```kotlin
controller.registerBatchTextStyles(
    mapOf(
        1 to EditorTextStyle(color = 0xFF569CD6.toInt()),
        2 to EditorTextStyle(color = 0xFF6A9955.toInt()),
    ),
)
controller.setBatchLineSpans(
    EditorSpanLayer.SYNTAX,
    mapOf(
        0 to listOf(StyleSpan(0, 3, 1)),
        1 to listOf(StyleSpan(0, 2, 2)),
    ),
)
```

行列从 0 起。`StyleSpan.length` 是该行的码元长度。

视口驱动更新请用 [DecorationProvider](./decoration-provider.md)，不要每个按键都全量 `setLineSpans`。

另见 [Span API](/zh/api/decorations-spans)。
