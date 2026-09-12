# 联动编辑

```kotlin
data class TabStopGroup(
    val index: Int,
    val ranges: List<TextRange>,
    val defaultText: String = "",
)
```

改一组里的一个区间会同步其它区间。常见来源：`insertSnippet` 的 tab stop。

```kotlin
fun startLinkedEditing(groups: List<TabStopGroup>)
fun isInLinkedEditing(): Boolean
fun linkedEditingNext()
fun linkedEditingPrev()
fun cancelLinkedEditing()
```

Ready 前 `isInLinkedEditing()` 为 `false`。

主题：`linkedEditingActiveColor`、`linkedEditingInactiveColor`（或 `rangeEffects.linkedEditing*`）。
