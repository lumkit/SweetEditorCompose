# 外观

把 `theme`、`settings` 传给 `SweetEditor`，或在就绪后调用 `applyTheme` / `setSettings`。

颜色是打包的 ARGB `Int`（`0xAARRGGBB`）。大于 `0x7FFFFFFF` 的字面量要 `.toInt()`。

## EditorSettings

| 字段 | 默认 | 说明 |
|---|---|---|
| `wrapMode` | `NONE` | `CHAR_BREAK`、`WORD_BREAK` |
| `tabSize` | `4` | |
| `insertSpaces` | `true` | |
| `lineSpacingAdd` | `0f` | 额外像素 |
| `lineSpacingMult` | `1.2f` | |
| `scale` | `1f` | 视觉缩放初值 |
| `fontSizeSp` | `14f` | |
| `readOnly` | `false` | |
| `gutterVisible` | `true` | |
| `gutterSticky` | 平台默认 | 粘性行号 |
| `currentLineRenderMode` | `BACKGROUND` | `BORDER`、`NONE` |
| `foldArrowMode` | `ALWAYS` | `AUTO`、`HIDDEN` |
| `renderWhitespace` | `NONE` | `BOUNDARY`、`SELECTION`、`TRAILING`、`ALL` |
| `renderLineBreaks` | `false` | |
| `autoIndentMode` | `KEEP_INDENT` | 或 `NONE` |
| `backspaceUnindent` | `true` | |
| `decorationOverscanViewportMultiplier` | `1f` | Provider 额外视口 |
| `decorationScrollRefreshMinIntervalMs` | `50` | 节流 |

## EditorTheme

`EditorTheme.dark()` 即默认构造。`EditorTheme.light()` 是浅色预设。

常见分组：

- 表面：`backgroundColor`、`textColor`、`cursorColor`、`currentLineColor`
- 行号区：`lineNumberColor`、`currentLineNumberColor`、`splitLineColor`、`gutterIconColor`
- 滚动条：`scrollbarTrackColor`、`scrollbarThumbColor`、`scrollbarThumbActiveColor`
- 选区 / 链接 / CodeLens / 搜索 / 诊断 / 高亮 / 联动编辑 / 括号
- Diff：`diffAdded*` / `diffRemoved*`
- 菜单与内联建议条
- `fontFamily`（默认 `FontFamily.Monospace`）
- `rangeEffects: EditorRangeEffects?` — 按区间覆盖

`EditorRangeEffectStyle` 含 `foregroundColor`、`backgroundColor`、`borderColor`、`underlineColor`、`underlineStyle`（`NONE`、`SOLID`、`DASHED`、`WAVY`）。

`rangeEffects` 为 null 时，主题颜色字段会自动映射（选区背景、波浪诊断、实线 IME 下划线等）。
