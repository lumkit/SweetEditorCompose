# Diff

```kotlin
data class DiffChange(
    val currentStartLine: Int,
    val currentLineCount: Int,
    val originalStartLine: Int,
    val removedLines: List<String> = emptyList(),
)
```

```kotlin
fun setDiffChanges(changes: List<DiffChange>)
fun computeDiff(originalText: String)
fun setBatchDiffLineSpans(layer: EditorSpanLayer, spansByOriginalLine: Map<Int, List<StyleSpan>>)
fun clearDiff()
```

`computeDiff` 让内核把当前缓冲区与 `originalText` 做 diff，并绘制增删行号区。

`setDiffChanges` 推入宿主算好的 hunk 列表。

`setBatchDiffLineSpans` 按**原始**（删除侧）行号给 token 上色。

主题：`diffAddedLineBackground`、`diffRemovedLineBackground`、`diffAddedGutterBackground`、`diffRemovedGutterBackground`。
