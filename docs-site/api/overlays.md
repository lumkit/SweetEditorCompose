# Overlays

Line-based decorations. Each family has `setLine*`, `setBatchLine*`, and `clear*`. `clearAllDecorations()` clears all of them (plus spans).

Before ready, setters no-op. `getLinkTargetAt` returns `""`.

## Inlay

```kotlin
enum class EditorInlayType { TEXT, ICON, COLOR }
data class InlayHint(val type: EditorInlayType, val column: Int, val text: String = "", val intValue: Int = 0)

fun setLineInlayHints(line: Int, hints: List<InlayHint>)
fun setBatchLineInlayHints(hintsByLine: Map<Int, List<InlayHint>>)
fun clearInlayHints()
```

`ICON` / `COLOR` use `intValue` (`iconId` or packed color). Clicks: `onInlayHintClick`.

## Phantom

```kotlin
data class PhantomText(val column: Int, val text: String)
fun setLinePhantomTexts(line: Int, phantoms: List<PhantomText>)
fun setBatchLinePhantomTexts(phantomsByLine: Map<Int, List<PhantomText>>)
fun clearPhantomTexts()
```

## Gutter

```kotlin
data class GutterIcon(val iconId: Int)
fun setLineGutterIcons(line: Int, icons: List<GutterIcon>)
fun setBatchLineGutterIcons(iconsByLine: Map<Int, List<GutterIcon>>)
fun setMaxGutterIcons(count: Int)
fun clearGutterIcons()
```

Resolve bitmaps with `setEditorIconProvider`. Clicks: `onGutterIconClick`.

## CodeLens

```kotlin
data class CodeLensItem(val column: Int, val commandId: Int, val text: String)
fun setLineCodeLens(line: Int, items: List<CodeLensItem>)
fun setBatchLineCodeLens(itemsByLine: Map<Int, List<CodeLensItem>>)
fun clearCodeLens()
```

Clicks: `onCodeLensClick`. Colors: `codeLensColor` / `activeCodeLensColor`.

## Link

```kotlin
data class LinkSpan(val column: Int, val length: Int, val target: String)
fun setLineLinks(line: Int, links: List<LinkSpan>)
fun setBatchLineLinks(linksByLine: Map<Int, List<LinkSpan>>)
fun clearLinks()
fun getLinkTargetAt(line: Int, column: Int): String
```

Clicks: `onLinkClick`.

## Diagnostic

```kotlin
enum class EditorDiagnosticSeverity { ERROR, WARNING, INFO, HINT }
data class Diagnostic(val column: Int, val length: Int, val severity: EditorDiagnosticSeverity)
fun setLineDiagnostics(line: Int, items: List<Diagnostic>)
fun setBatchLineDiagnostics(itemsByLine: Map<Int, List<Diagnostic>>)
fun clearDiagnostics()
```

Default paint: wavy underline (hint is dashed) from theme diagnostic colors.

## Document highlight

```kotlin
enum class EditorDocumentHighlightKind { TEXT, READ, WRITE }
data class DocumentHighlight(val column: Int, val length: Int, val kind: EditorDocumentHighlightKind)
fun setLineDocumentHighlights(line: Int, items: List<DocumentHighlight>)
fun setBatchLineDocumentHighlights(itemsByLine: Map<Int, List<DocumentHighlight>>)
fun clearDocumentHighlights()
```

All of the above can also come from a [DecorationProvider](/guide/decoration-provider).
