# Inline suggestions

Ghost text at a caret, Copilot-style. You compute the suggestion; the editor draws it and reports accept / dismiss.

```kotlin
controller.setInlineSuggestionListener(object : InlineSuggestionListener {
    override fun onSuggestionAccepted(suggestion: InlineSuggestion) {
        // already inserted by the session
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
    // or
    controller.dismissInlineSuggestion()
}
```

`showInlineSuggestion` before ready is ignored. The bar colors come from `EditorTheme` (`inlineSuggestionBar*`).
