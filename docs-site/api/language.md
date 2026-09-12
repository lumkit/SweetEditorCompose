# Language and brackets

```kotlin
data class BracketPair(val open: String, val close: String)

data class LanguageConfiguration(
    val languageId: String,
    val brackets: List<BracketPair>? = null,
    val autoClosingPairs: List<BracketPair>? = null,
    val tabSize: Int? = null,
    val insertSpaces: Boolean? = null,
)
```

Builder:

```kotlin
val kotlin = LanguageConfiguration.builder("kotlin")
    .addBracket("{", "}")
    .addBracket("(", ")")
    .addAutoClosingPair("\"", "\"")
    .setTabSize(4)
    .setInsertSpaces(true)
    .build()

controller.setLanguageConfiguration(kotlin)
```

`tabSize` / `insertSpaces` on the language override `EditorSettings` when non-null.

## Manual pairs and highlights

```kotlin
fun setBracketPairs(pairs: List<BracketPair>)
fun setAutoClosingPairs(pairs: List<BracketPair>)
fun setMatchedBrackets(openLine: Int, openColumn: Int, closeLine: Int, closeColumn: Int)
fun clearMatchedBrackets()
```

`setLanguageConfiguration` applies brackets and auto-closing pairs from the config. Use `setMatchedBrackets` when your language service computed the match at the caret; theme `bracketHighlight*` / `rangeEffects.bracketMatch` paint it.

Pairs are encoded as the first Unicode code point of each string.
