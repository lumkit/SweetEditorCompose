package io.github.lumkit.sweeteditor.core.protocol

import io.github.lumkit.sweeteditor.CodeLensItem
import io.github.lumkit.sweeteditor.Diagnostic
import io.github.lumkit.sweeteditor.DocumentHighlight
import io.github.lumkit.sweeteditor.EditorTextStyle
import io.github.lumkit.sweeteditor.GutterIcon
import io.github.lumkit.sweeteditor.InlayHint
import io.github.lumkit.sweeteditor.LinkSpan
import io.github.lumkit.sweeteditor.PhantomText
import io.github.lumkit.sweeteditor.StyleSpan

internal fun encodeRegisterBatchTextStylesPayload(styles: Map<Int, EditorTextStyle>): ByteArray {
    val writer = ProtocolWriter()
    val ordered = styles.toSortedMap()
    writer.writeI32(ordered.size)
    for ((id, style) in ordered) {
        writer.writeU32(id)
        writer.writeRaw(CoreProtocol.encodeTextStyle(style.toProtocol()))
    }
    return writer.toByteArray()
}

internal fun encodeSetLineSpansPayload(line: Int, layer: Int, spans: List<StyleSpan>): ByteArray {
    val writer = ProtocolWriter()
    writer.writeU32(line)
    writer.writeI32(layer)
    writer.writeItems(spans) { CoreProtocol.encodeStyleSpan(it.toProtocol()) }
    return writer.toByteArray()
}

internal fun encodeSetBatchLineSpansPayload(layer: Int, spansByLine: Map<Int, List<StyleSpan>>): ByteArray {
    val writer = ProtocolWriter()
    writer.writeI32(layer)
    writer.writeLineMap(spansByLine) { CoreProtocol.encodeStyleSpan(it.toProtocol()) }
    return writer.toByteArray()
}

internal fun encodeSetLineInlayHintsPayload(line: Int, hints: List<InlayHint>): ByteArray {
    val writer = ProtocolWriter()
    writer.writeU32(line)
    writer.writeItems(hints) { CoreProtocol.encodeInlayHint(it.toProtocol()) }
    return writer.toByteArray()
}

internal fun encodeSetBatchLineInlayHintsPayload(hintsByLine: Map<Int, List<InlayHint>>): ByteArray {
    val writer = ProtocolWriter()
    writer.writeLineMap(hintsByLine) { CoreProtocol.encodeInlayHint(it.toProtocol()) }
    return writer.toByteArray()
}

internal fun encodeSetLinePhantomTextsPayload(line: Int, phantoms: List<PhantomText>): ByteArray {
    val writer = ProtocolWriter()
    writer.writeU32(line)
    writer.writeItems(phantoms) { CoreProtocol.encodePhantomText(it.toProtocol()) }
    return writer.toByteArray()
}

internal fun encodeSetBatchLinePhantomTextsPayload(phantomsByLine: Map<Int, List<PhantomText>>): ByteArray {
    val writer = ProtocolWriter()
    writer.writeLineMap(phantomsByLine) { CoreProtocol.encodePhantomText(it.toProtocol()) }
    return writer.toByteArray()
}

internal fun encodeSetLineGutterIconsPayload(line: Int, icons: List<GutterIcon>): ByteArray {
    val writer = ProtocolWriter()
    writer.writeU32(line)
    writer.writeItems(icons) { CoreProtocol.encodeGutterIcon(ProtocolGutterIcon(it.iconId)) }
    return writer.toByteArray()
}

internal fun encodeSetBatchLineGutterIconsPayload(iconsByLine: Map<Int, List<GutterIcon>>): ByteArray {
    val writer = ProtocolWriter()
    writer.writeLineMap(iconsByLine) { CoreProtocol.encodeGutterIcon(ProtocolGutterIcon(it.iconId)) }
    return writer.toByteArray()
}

internal fun encodeSetLineCodeLensPayload(line: Int, items: List<CodeLensItem>): ByteArray {
    val writer = ProtocolWriter()
    writer.writeU32(line)
    writer.writeItems(items) { CoreProtocol.encodeCodeLensItem(it.toProtocol()) }
    return writer.toByteArray()
}

internal fun encodeSetBatchLineCodeLensPayload(itemsByLine: Map<Int, List<CodeLensItem>>): ByteArray {
    val writer = ProtocolWriter()
    writer.writeLineMap(itemsByLine) { CoreProtocol.encodeCodeLensItem(it.toProtocol()) }
    return writer.toByteArray()
}

internal fun encodeSetLineLinksPayload(line: Int, links: List<LinkSpan>): ByteArray {
    val writer = ProtocolWriter()
    writer.writeU32(line)
    writer.writeItems(links) { CoreProtocol.encodeLinkSpan(it.toProtocol()) }
    return writer.toByteArray()
}

internal fun encodeSetBatchLineLinksPayload(linksByLine: Map<Int, List<LinkSpan>>): ByteArray {
    val writer = ProtocolWriter()
    writer.writeLineMap(linksByLine) { CoreProtocol.encodeLinkSpan(it.toProtocol()) }
    return writer.toByteArray()
}

internal fun encodeSetLineDiagnosticsPayload(line: Int, items: List<Diagnostic>): ByteArray {
    val writer = ProtocolWriter()
    writer.writeU32(line)
    writer.writeItems(items) { CoreProtocol.encodeDiagnostic(it.toProtocol()) }
    return writer.toByteArray()
}

internal fun encodeSetBatchLineDiagnosticsPayload(itemsByLine: Map<Int, List<Diagnostic>>): ByteArray {
    val writer = ProtocolWriter()
    writer.writeLineMap(itemsByLine) { CoreProtocol.encodeDiagnostic(it.toProtocol()) }
    return writer.toByteArray()
}

internal fun encodeSetLineDocumentHighlightsPayload(line: Int, items: List<DocumentHighlight>): ByteArray {
    val writer = ProtocolWriter()
    writer.writeU32(line)
    writer.writeItems(items) { CoreProtocol.encodeDocumentHighlight(it.toProtocol()) }
    return writer.toByteArray()
}

internal fun encodeSetBatchLineDocumentHighlightsPayload(itemsByLine: Map<Int, List<DocumentHighlight>>): ByteArray {
    val writer = ProtocolWriter()
    writer.writeLineMap(itemsByLine) { CoreProtocol.encodeDocumentHighlight(it.toProtocol()) }
    return writer.toByteArray()
}

internal fun encodeSetFoldRegionsPayload(regions: List<io.github.lumkit.sweeteditor.FoldRegion>): ByteArray {
    val writer = ProtocolWriter()
    writer.writeItems(regions) {
        CoreProtocol.encodeFoldRegion(ProtocolFoldRegion(it.startLine, it.endLine, it.collapsed))
    }
    return writer.toByteArray()
}

private typealias ProtocolGutterIcon = io.github.lumkit.sweeteditor.core.protocol.GutterIcon
private typealias ProtocolFoldRegion = io.github.lumkit.sweeteditor.core.protocol.FoldRegion

private fun StyleSpan.toProtocol() = io.github.lumkit.sweeteditor.core.protocol.StyleSpan(column, length, styleId)

private fun EditorTextStyle.toProtocol() =
    io.github.lumkit.sweeteditor.core.protocol.TextStyle(color, backgroundColor, fontStyle)

private fun InlayHint.toProtocol() = io.github.lumkit.sweeteditor.core.protocol.InlayHint(
    `type` = InlayType.fromValue(type.value),
    column = column,
    intValue = intValue,
    text = text,
)

private fun PhantomText.toProtocol() = io.github.lumkit.sweeteditor.core.protocol.PhantomText(column, text)

private fun CodeLensItem.toProtocol() =
    io.github.lumkit.sweeteditor.core.protocol.CodeLensItem(column, commandId, text)

private fun LinkSpan.toProtocol() = io.github.lumkit.sweeteditor.core.protocol.LinkSpan(column, length, target)

private fun Diagnostic.toProtocol() = io.github.lumkit.sweeteditor.core.protocol.Diagnostic(
    column = column,
    length = length,
    severity = DiagnosticSeverity.fromValue(severity.value),
)

private fun DocumentHighlight.toProtocol() = io.github.lumkit.sweeteditor.core.protocol.DocumentHighlight(
    column = column,
    length = length,
    kind = DocumentHighlightKind.fromValue(kind.value),
)

private fun <T> ProtocolWriter.writeItems(items: List<T>, encode: (T) -> ByteArray) {
    writeI32(items.size)
    for (item in items) {
        writeRaw(encode(item))
    }
}

private fun <T> ProtocolWriter.writeLineMap(itemsByLine: Map<Int, List<T>>, encode: (T) -> ByteArray) {
    val ordered = itemsByLine.toSortedMap()
    writeI32(ordered.size)
    for ((line, items) in ordered) {
        writeU32(line)
        writeItems(items, encode)
    }
}
