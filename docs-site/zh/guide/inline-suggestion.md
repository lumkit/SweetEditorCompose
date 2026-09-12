# 内联建议

光标处的幽灵文本（Copilot 风格）。宿主算建议，编辑器绘制并回报接受 / 关闭。

```kotlin
controller.setInlineSuggestionListener(object : InlineSuggestionListener {
    override fun onSuggestionAccepted(suggestion: InlineSuggestion) {
        // session 已经插入文本
    }

    override fun onSuggestionDismissed(suggestion: InlineSuggestion) { }
})

controller.whenReady {
    controller.showInlineSuggestion(
        InlineSuggestion(line = 0, column = 4, text = "println(\"hi\")"),
    )
}

if (controller.isInlineSuggestionShowing()) {
    controller.acceptInlineSuggestion()
    // 或
    controller.dismissInlineSuggestion()
}
```

Ready 前的 `showInlineSuggestion` 会被忽略。条带颜色来自 `EditorTheme` 的 `inlineSuggestionBar*`。
