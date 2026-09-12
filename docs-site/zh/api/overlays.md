# 叠加装饰

按行装饰。每类都有 `setLine*`、`setBatchLine*`、`clear*`。`clearAllDecorations()` 会清掉它们以及 span。

Ready 前 setter 空操作。`getLinkTargetAt` 返回 `""`。

## Inlay

```kotlin
enum class EditorInlayType { TEXT, ICON, COLOR }
data class InlayHint(val type: EditorInlayType, val column: Int, val text: String = "", val intValue: Int = 0)

fun setLineInlayHints(line: Int, hints: List<InlayHint>)
fun setBatchLineInlayHints(hintsByLine: Map<Int, List<InlayHint>>)
fun clearInlayHints()
```

`ICON` / `COLOR` 用 `intValue`（`iconId` 或打包颜色）。点击：`onInlayHintClick`。

## Phantom

```kotlin
data class PhantomText(val column: Int, val text: String)
fun setLinePhantomTexts(line: Int, phantoms: List<PhantomText>)
fun setBatchLinePhantomTexts(phantomsByLine: Map<Int, List<PhantomText>>)
fun clearPhantomTexts()
```

## 行号区

```kotlin
data class GutterIcon(val iconId: Int)
fun setLineGutterIcons(line: Int, icons: List<GutterIcon>)
fun setBatchLineGutterIcons(iconsByLine: Map<Int, List<GutterIcon>>)
fun setMaxGutterIcons(count: Int)
fun clearGutterIcons()
```

用 `setEditorIconProvider` 解析位图。点击：`onGutterIconClick`。

## CodeLens

```kotlin
data class CodeLensItem(val column: Int, val commandId: Int, val text: String)
fun setLineCodeLens(line: Int, items: List<CodeLensItem>)
fun setBatchLineCodeLens(itemsByLine: Map<Int, List<CodeLensItem>>)
fun clearCodeLens()
```

点击：`onCodeLensClick`。颜色：`codeLensColor` / `activeCodeLensColor`。

## 链接

```kotlin
data class LinkSpan(val column: Int, val length: Int, val target: String)
fun setLineLinks(line: Int, links: List<LinkSpan>)
fun setBatchLineLinks(linksByLine: Map<Int, List<LinkSpan>>)
fun clearLinks()
fun getLinkTargetAt(line: Int, column: Int): String
```

点击：`onLinkClick`。

## 诊断

```kotlin
enum class EditorDiagnosticSeverity { ERROR, WARNING, INFO, HINT }
data class Diagnostic(val column: Int, val length: Int, val severity: EditorDiagnosticSeverity)
fun setLineDiagnostics(line: Int, items: List<Diagnostic>)
fun setBatchLineDiagnostics(itemsByLine: Map<Int, List<Diagnostic>>)
fun clearDiagnostics()
```

默认绘制：主题诊断色的波浪下划线（Hint 为虚线）。

## 文档高亮

```kotlin
enum class EditorDocumentHighlightKind { TEXT, READ, WRITE }
data class DocumentHighlight(val column: Int, val length: Int, val kind: EditorDocumentHighlightKind)
fun setLineDocumentHighlights(line: Int, items: List<DocumentHighlight>)
fun setBatchLineDocumentHighlights(itemsByLine: Map<Int, List<DocumentHighlight>>)
fun clearDocumentHighlights()
```

以上也可由 [DecorationProvider](/zh/guide/decoration-provider) 提供。
