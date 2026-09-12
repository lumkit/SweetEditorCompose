# 换行动作

用户插入换行时，Provider 可以提供额外文本（自动闭合大括号、语言前缀等）。

```kotlin
data class NewLineAction(val text: String)

data class NewLineContext(
    val lineNumber: Int,
    val column: Int,
    val lineText: String,
    val languageConfiguration: LanguageConfiguration? = null,
    val editorMetadata: EditorMetadata? = null,
)

fun interface NewLineActionProvider {
    fun provideNewLineAction(context: NewLineContext): NewLineAction?
}
```

```kotlin
controller.addNewLineActionProvider { context ->
    if (context.lineText.trimEnd().endsWith("{")) {
        NewLineAction(text = "}")
    } else {
        null
    }
}
controller.removeNewLineActionProvider(provider)
```

返回 `null` 表示跳过。`AutoIndentMode.KEEP_INDENT` 仍由设置独立生效。
