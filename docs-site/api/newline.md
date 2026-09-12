# New-line actions

When the user inserts a newline, providers can supply extra text (auto-close braces, language-specific prefixes).

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

Return `null` to skip. `AutoIndentMode.KEEP_INDENT` still applies independently via settings.
