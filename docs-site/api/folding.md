# Folding

```kotlin
data class FoldRegion(
    val startLine: Int,
    val endLine: Int,
    val collapsed: Boolean = false,
)
```

Lines are 0-based inclusive.

```kotlin
fun setFoldRegions(regions: List<FoldRegion>)
fun toggleFold(line: Int)
fun foldAt(line: Int)
fun unfoldAt(line: Int)
fun foldAll()
fun unfoldAll()
fun isLineVisible(line: Int): Boolean
```

`isLineVisible` is `true` before ready (no folds yet).

`FoldToggleEvent` is published when the user clicks a fold arrow. Arrow visibility follows `EditorSettings.foldArrowMode`.

You can also supply fold regions from a `DecorationProvider` (`foldRegions` / `foldRegionsMode`).
