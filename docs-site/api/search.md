# Search

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

`search` before ready is ignored. `getSearchState()` is `null` before ready.

Invalid regex sets `status` to `FAILED` and fills `errorMessage`.

Match backgrounds use theme `searchMatch*` / `searchCurrent*` (or `rangeEffects.searchMatch` / `searchCurrent`).
