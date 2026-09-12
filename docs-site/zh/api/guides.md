# 参考线

```kotlin
data class IndentGuide(val start: TextPosition, val end: TextPosition)
data class BracketGuide(
    val parent: TextPosition,
    val end: TextPosition,
    val children: List<TextPosition> = emptyList(),
)
data class FlowGuide(val start: TextPosition, val end: TextPosition)

enum class SeparatorStyle { SINGLE, DOUBLE }
data class SeparatorGuide(
    val line: Int,
    val style: SeparatorStyle = SeparatorStyle.SINGLE,
    val count: Int = 1,
    val textEndColumn: Int = 0,
)
```

```kotlin
fun setIndentGuides(guides: List<IndentGuide>)
fun setBracketGuides(guides: List<BracketGuide>)
fun setFlowGuides(guides: List<FlowGuide>)
fun setSeparatorGuides(guides: List<SeparatorGuide>)
fun clearGuides()
```

`clearGuides()` 清掉全部四种。颜色：`guideColor`、`separatorLineColor`。

Provider 可在 `DecorationResult` 里填写 `indentGuides`、`bracketGuides`、`flowGuides`、`separatorGuides`。
