# Guides

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

`clearGuides()` removes all four kinds. Colors: `guideColor`, `separatorLineColor`.

Providers can fill `indentGuides`, `bracketGuides`, `flowGuides`, `separatorGuides` on `DecorationResult`.
