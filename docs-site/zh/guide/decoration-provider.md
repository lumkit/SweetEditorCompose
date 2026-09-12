# 装饰 Provider

`DecorationProvider` 根据可见行范围和最近文本变更计算装饰，Session 再合并到编辑器。

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

## 上下文

`DecorationContext` 包含：

- `visibleLineRange` — 当前屏幕行（含设置里的 overscan）
- `totalLineCount`
- `textChanges` — 最近一次派发的编辑（刷新时可能为空）
- 若已设置：`languageConfiguration` / `editorMetadata`

## 应用模式

| 模式 | 含义 |
|---|---|
| `MERGE` | 与该类型已有项合并 |
| `REPLACE_ALL` | 先清掉该类型全部项再写入 |
| `REPLACE_RANGE` | 只替换落入给定范围/行的项 |

`DecorationResult` 每个字段都有独立的 `*Mode`（默认 `MERGE`）。字段为 `null` 表示跳过该类型。

## 刷新

```kotlin
controller.requestDecorationRefresh()
controller.removeDecorationProvider(provider)
```

直接 setter（`setLineSpans`、`setLineDiagnostics` …）仍然可用。装饰依赖视口或语言服务时，用 Provider 更合适。

载荷类型见 [叠加装饰](/zh/api/overlays)。
