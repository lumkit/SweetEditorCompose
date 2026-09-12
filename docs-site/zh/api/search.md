# 搜索

```kotlin
data class EditorSearchOptions(
    val caseSensitive: Boolean = false,
    val wholeWord: Boolean = false,
    val useRegex: Boolean = false,
    val wrapAround: Boolean = true,
    val maxMatches: Int = 10_000,
)

enum class EditorSearchStatus { INACTIVE, SEARCHING, READY, STALE, FAILED }

data class EditorSearchState(
    val status: EditorSearchStatus,
    val pattern: String,
    val options: EditorSearchOptions,
    val generation: Long,
    val matchCount: Int,
    val currentIndex: Int,
    val hasCurrentMatch: Boolean,
    val currentRange: TextRange,
    val errorMessage: String,
)
```

## Controller

```kotlin
fun search(pattern: String, options: EditorSearchOptions = EditorSearchOptions())
fun findNextSearchMatch()
fun findPreviousSearchMatch()
fun replaceCurrentSearchMatch(replacement: String)
fun replaceAllSearchMatches(replacement: String)
fun clearSearch()
fun getSearchState(): EditorSearchState?
```

Ready 前 `search` 被忽略。Ready 前 `getSearchState()` 为 `null`。

非法正则会把 `status` 设为 `FAILED` 并填充 `errorMessage`。

匹配背景用主题的 `searchMatch*` / `searchCurrent*`（或 `rangeEffects.searchMatch` / `searchCurrent`）。
