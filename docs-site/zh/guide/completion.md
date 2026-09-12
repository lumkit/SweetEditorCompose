# 补全

注册 `CompletionProvider`。默认键位在 `Ctrl`/`⌘`+`Space` 触发，也可由你声明的触发字符触发。

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

## 条目如何落地

1. 有 `textEdit` 时替换该区间。
2. 否则在光标处插入 `insertText`（或 `label`）。
3. `SNIPPET` 走 `insertSnippet`。
4. `additionalTextEdits` 在主编辑之后应用。

也可以不经过 Provider 直接推列表：

```kotlin
controller.showCompletionItems(items)
controller.applyCompletionItem(item)
controller.dismissCompletion()
controller.triggerCompletion()
```

## 自定义行 UI

```kotlin
controller.setCompletionItemRenderer { item, selected, theme ->
    // 一条补全的 Compose 行
}
```

传 `null` 恢复默认渲染。

见 [补全 API](/zh/api/completion)。
