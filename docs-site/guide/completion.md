# Completion

Register a `CompletionProvider`. The editor triggers on `Ctrl`/`⌘`+`Space` (default maps) and on characters you mark as triggers.

```kotlin
controller.addCompletionProvider(object : CompletionProvider {
    override fun isTriggerCharacter(ch: String) = ch == "."

    override fun provideCompletions(context: CompletionContext, receiver: CompletionReceiver) {
        if (receiver.isCancelled) return
        receiver.accept(
            CompletionResult(
                items = listOf(
                    CompletionItem(
                        label = "println",
                        kind = CompletionItemKind.FUNCTION,
                        insertText = "println(\$0)",
                        insertTextFormat = CompletionInsertTextFormat.SNIPPET,
                    ),
                ),
            ),
        )
    }
})
```

## How an item is applied

1. If `textEdit` is set, that range is replaced.
2. Otherwise `insertText` (or `label`) is inserted at the cursor.
3. `SNIPPET` format goes through `insertSnippet`.
4. `additionalTextEdits` are applied after the primary edit.

You can also push a list without a provider:

```kotlin
controller.showCompletionItems(items)
controller.applyCompletionItem(item)
controller.dismissCompletion()
controller.triggerCompletion()
```

## Custom row UI

```kotlin
controller.setCompletionItemRenderer { item, selected, theme ->
    // Compose row for one item
}
```

Pass `null` to restore the default renderer.

See [Completion API](/api/completion).
