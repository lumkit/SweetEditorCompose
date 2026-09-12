# Appearance

Pass `theme` and `settings` into `SweetEditor`, or call `applyTheme` / `setSettings` after ready.

Colors are packed ARGB `Int` (`0xAARRGGBB`). Use `.toInt()` on hex literals above `0x7FFFFFFF`.

## EditorSettings

| Field | Default | Notes |
|---|---|---|
| `wrapMode` | `NONE` | `CHAR_BREAK`, `WORD_BREAK` |
| `tabSize` | `4` | |
| `insertSpaces` | `true` | |
| `lineSpacingAdd` | `0f` | Extra pixels |
| `lineSpacingMult` | `1.2f` | |
| `scale` | `1f` | Visual zoom seed |
| `fontSizeSp` | `14f` | |
| `readOnly` | `false` | |
| `gutterVisible` | `true` | |
| `gutterSticky` | platform default | Sticky line numbers |
| `currentLineRenderMode` | `BACKGROUND` | `BORDER`, `NONE` |
| `foldArrowMode` | `ALWAYS` | `AUTO`, `HIDDEN` |
| `renderWhitespace` | `NONE` | `BOUNDARY`, `SELECTION`, `TRAILING`, `ALL` |
| `renderLineBreaks` | `false` | |
| `autoIndentMode` | `KEEP_INDENT` | or `NONE` |
| `backspaceUnindent` | `true` | |
| `decorationOverscanViewportMultiplier` | `1f` | Extra viewport for providers |
| `decorationScrollRefreshMinIntervalMs` | `50` | Throttle |

## EditorTheme

`EditorTheme.dark()` is the default constructor. `EditorTheme.light()` is a white-background preset.

Notable groups:

- Surface: `backgroundColor`, `textColor`, `cursorColor`, `currentLineColor`
- Gutter: `lineNumberColor`, `currentLineNumberColor`, `splitLineColor`, `gutterIconColor`
- Scrollbar: `scrollbarTrackColor`, `scrollbarThumbColor`, `scrollbarThumbActiveColor`
- Selection / links / CodeLens / search / diagnostics / highlights / linked-editing / brackets
- Diff: `diffAdded*` / `diffRemoved*`
- Menus and inline suggestion bar
- `fontFamily` (`FontFamily.Monospace` by default)
- `rangeEffects: EditorRangeEffects?` — optional per-range overrides

`EditorRangeEffectStyle` has `foregroundColor`, `backgroundColor`, `borderColor`, `underlineColor`, `underlineStyle` (`NONE`, `SOLID`, `DASHED`, `WAVY`).

If `rangeEffects` is null, the theme color fields are mapped automatically (selection background, wavy diagnostics, solid IME underline, …).
