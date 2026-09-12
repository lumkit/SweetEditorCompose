# 补全 API

```kotlin
enum class CompletionTriggerKind { INVOKED, CHARACTER, RETRIGGER }
enum class CompletionItemKind { KEYWORD, FUNCTION, VARIABLE, CLASS, INTERFACE, MODULE, PROPERTY, SNIPPET, TEXT }
enum class CompletionInsertTextFormat { PLAIN_TEXT, SNIPPET }

data class CompletionItem(
    val label: String,
    val detail: String? = null,
    val insertText: String? = null,
    val insertTextFormat: CompletionInsertTextFormat = PLAIN_TEXT,
    val textEdit: EditorTextEdit? = null,
    val additionalTextEdits: List<EditorTextEdit> = emptyList(),
    val filterText: String? = null,
    val sortKey: String? = null,
    val kind: CompletionItemKind = TEXT,
)

data class CompletionResult(val items: List<CompletionItem>, val isIncomplete: Boolean = false)

data class CompletionContext(
    val triggerKind: CompletionTriggerKind,
    val triggerCharacter: String?,
    val cursorPosition: TextPosition,
    val lineText: String,
    val wordRange: TextRange,
    val languageConfiguration: LanguageConfiguration? = null,
    val editorMetadata: EditorMetadata? = null,
)
```

`CompletionItem.matchText` 为 `filterText ?: label`。

```kotlin
interface CompletionReceiver {
    fun accept(result: CompletionResult): Boolean
    val isCancelled: Boolean
}

interface CompletionProvider {
    fun isTriggerCharacter(ch: String): Boolean = false
    fun provideCompletions(context: CompletionContext, receiver: CompletionReceiver)
}
```

## Controller

```kotlin
fun addCompletionProvider(provider: CompletionProvider)
fun removeCompletionProvider(provider: CompletionProvider)
fun triggerCompletion()
fun showCompletionItems(items: List<CompletionItem>)
fun dismissCompletion()
fun applyCompletionItem(item: CompletionItem)
fun setCompletionItemRenderer(
    renderer: (@Composable (CompletionItem, Boolean, EditorTheme) -> Unit)?,
)
```

落地顺序：有 `textEdit` 则用之，否则在光标插入 `insertText`/`label`。`SNIPPET` 走 `insertSnippet`。然后 `additionalTextEdits`。

指南：[补全](/zh/guide/completion)。
