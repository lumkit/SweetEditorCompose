# Syntax highlights

The library has no built-in lexer. You register styles, then push `StyleSpan`s onto a line and layer.

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

## Layers

| Layer | Typical use |
|---|---|
| `SYNTAX` | Keywords, strings, comments |
| `SEMANTIC` | LSP-style tokens |
| `OVERLAY` | Temporary emphasis |

Layers stack. Clear one layer without wiping the others:

```kotlin
controller.clearHighlights(EditorSpanLayer.OVERLAY)
```

`clearHighlights()` with no argument clears every span layer.

## Batch

Prefer batch APIs when many lines change:

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

Line and column are 0-based. `StyleSpan.length` is in code units of that line.

For viewport-driven updates, use a [DecorationProvider](./decoration-provider.md) instead of calling `setLineSpans` on every keystroke.

See also [Spans API](/api/decorations-spans).
