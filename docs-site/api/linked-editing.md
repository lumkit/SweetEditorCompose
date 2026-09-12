# Linked editing

```kotlin
data class TabStopGroup(
    val index: Int,
    val ranges: List<TextRange>,
    val defaultText: String = "",
)
```

Editing one range in a group updates the others. Typical source: snippet tab stops from `insertSnippet`.

```kotlin
fun startLinkedEditing(groups: List<TabStopGroup>)
fun isInLinkedEditing(): Boolean
fun linkedEditingNext()
fun linkedEditingPrev()
fun cancelLinkedEditing()
```

`isInLinkedEditing()` is `false` before ready.

Theme: `linkedEditingActiveColor`, `linkedEditingInactiveColor` (or `rangeEffects.linkedEditing*`).
