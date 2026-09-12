# 折叠

```kotlin
data class FoldRegion(
    val startLine: Int,
    val endLine: Int,
    val collapsed: Boolean = false,
)
```

行号 0 起，闭区间。

```kotlin
fun setFoldRegions(regions: List<FoldRegion>)
fun toggleFold(line: Int)
fun foldAt(line: Int)
fun unfoldAt(line: Int)
fun foldAll()
fun unfoldAll()
fun isLineVisible(line: Int): Boolean
```

Ready 前 `isLineVisible` 为 `true`（尚无折叠）。

用户点折叠箭头时发 `FoldToggleEvent`。箭头显示由 `EditorSettings.foldArrowMode` 控制。

也可通过 `DecorationProvider` 的 `foldRegions` / `foldRegionsMode` 提供折叠区。
