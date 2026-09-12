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

`computeDiff` asks the core to diff the current buffer against `originalText` and paint added / removed gutters.

`setDiffChanges` pushes a host-computed hunk list.

`setBatchDiffLineSpans` colors tokens on the **original** (removed) side by original line index.

Theme: `diffAddedLineBackground`, `diffRemovedLineBackground`, `diffAddedGutterBackground`, `diffRemovedGutterBackground`.
