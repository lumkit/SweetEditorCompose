# Decoration providers

A `DecorationProvider` computes decorations from the visible range and recent text changes. The session merges results onto the editor.

```kotlin
val provider = object : DecorationProvider {
    override fun capabilities() = setOf(DecorationType.SYNTAX_HIGHLIGHT)

    override fun provideDecorations(context: DecorationContext, receiver: DecorationReceiver) {
        if (receiver.isCancelled) return
        val spans = mapOf(
            context.visibleLineRange.startLine to listOf(
                StyleSpan(column = 0, length = 3, styleId = 1),
            ),
        )
        receiver.accept(
            DecorationResult(
                syntaxSpans = spans,
                syntaxSpansMode = DecorationApplyMode.REPLACE_RANGE,
            ),
        )
    }
}

controller.addDecorationProvider(provider)
```

## Context

`DecorationContext` includes:

- `visibleLineRange` — lines currently on screen (plus overscan from settings)
- `totalLineCount`
- `textChanges` — last dispatched edits (may be empty on a refresh)
- `languageConfiguration` / `editorMetadata` if you set them

## Apply modes

| Mode | Meaning |
|---|---|
| `MERGE` | Combine with existing items of that type |
| `REPLACE_ALL` | Drop every item of that type, then apply |
| `REPLACE_RANGE` | Replace items that fall in the provided range / lines |

Each field on `DecorationResult` has its own `*Mode` (default `MERGE`). Leave a field `null` to skip that type.

## Refresh

```kotlin
controller.requestDecorationRefresh()
controller.removeDecorationProvider(provider)
```

Direct setter APIs (`setLineSpans`, `setLineDiagnostics`, …) still work. Providers are the scalable path when decorations depend on viewport or language services.

See [Overlays](/api/overlays) for the payload types.
