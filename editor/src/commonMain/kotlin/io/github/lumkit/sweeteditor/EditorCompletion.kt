package io.github.lumkit.sweeteditor

import io.github.lumkit.sweeteditor.core.protocol.ProtocolWriter
import io.github.lumkit.sweeteditor.core.protocol.TextEdit as CoreTextEdit
import io.github.lumkit.sweeteditor.core.protocol.TextPosition as CoreTextPosition
import io.github.lumkit.sweeteditor.core.protocol.TextRange as CoreTextRange
import io.github.lumkit.sweeteditor.core.protocol.CoreProtocol

enum class CompletionTriggerKind {
    INVOKED,
    CHARACTER,
    RETRIGGER,
}

enum class CompletionItemKind {
    KEYWORD,
    FUNCTION,
    VARIABLE,
    CLASS,
    INTERFACE,
    MODULE,
    PROPERTY,
    SNIPPET,
    TEXT,
}

enum class CompletionInsertTextFormat(val value: Int) {
    PLAIN_TEXT(1),
    SNIPPET(2),
}

data class EditorTextEdit(
    val range: TextRange,
    val newText: String,
)

data class CompletionItem(
    val label: String,
    val detail: String? = null,
    val insertText: String? = null,
    val insertTextFormat: CompletionInsertTextFormat = CompletionInsertTextFormat.PLAIN_TEXT,
    val textEdit: EditorTextEdit? = null,
    val additionalTextEdits: List<EditorTextEdit> = emptyList(),
    val filterText: String? = null,
    val sortKey: String? = null,
    val kind: CompletionItemKind = CompletionItemKind.TEXT,
) {
    val matchText: String get() = filterText ?: label
}

data class CompletionResult(
    val items: List<CompletionItem>,
    val isIncomplete: Boolean = false,
)

data class CompletionContext(
    val triggerKind: CompletionTriggerKind,
    val triggerCharacter: String?,
    val cursorPosition: TextPosition,
    val lineText: String,
    val wordRange: TextRange,
    val languageConfiguration: LanguageConfiguration? = null,
    val editorMetadata: EditorMetadata? = null,
)

interface CompletionReceiver {
    fun accept(result: CompletionResult): Boolean
    val isCancelled: Boolean
}

interface CompletionProvider {
    fun isTriggerCharacter(ch: String): Boolean = false
    fun provideCompletions(context: CompletionContext, receiver: CompletionReceiver)
}

internal fun encodeApplyTextEdits(edits: List<EditorTextEdit>): ByteArray {
    val writer = ProtocolWriter()
    writer.writeI32(edits.size)
    for (edit in edits) {
        writer.writeRaw(CoreProtocol.encodeTextEdit(edit.toProtocol()))
    }
    return writer.toByteArray()
}

internal fun EditorTextEdit.toProtocol(): CoreTextEdit =
    CoreTextEdit(
        range = CoreTextRange(
            start = CoreTextPosition(range.start.line, range.start.column),
            end = CoreTextPosition(range.end.line, range.end.column),
        ),
        newText = newText,
    )

internal fun TextRange.isEmptyRange(): Boolean =
    start.line == end.line && start.column == end.column
