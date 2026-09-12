# 语言与括号

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

Builder：

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

语言上的 `tabSize` / `insertSpaces` 非 null 时覆盖 `EditorSettings`。

## 手动配对与高亮

```kotlin
fun setBracketPairs(pairs: List<BracketPair>)
fun setAutoClosingPairs(pairs: List<BracketPair>)
fun setMatchedBrackets(openLine: Int, openColumn: Int, closeLine: Int, closeColumn: Int)
fun clearMatchedBrackets()
```

`setLanguageConfiguration` 会应用配置里的括号与自动闭合。语言服务算出光标处匹配后调用 `setMatchedBrackets`；主题的 `bracketHighlight*` / `rangeEffects.bracketMatch` 负责绘制。

配对按每个字符串的第一个 Unicode 码点编码。
