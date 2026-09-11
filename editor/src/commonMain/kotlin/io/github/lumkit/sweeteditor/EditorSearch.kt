package io.github.lumkit.sweeteditor

import io.github.lumkit.sweeteditor.core.protocol.CoreProtocol
import io.github.lumkit.sweeteditor.core.protocol.ProtocolWriter
import io.github.lumkit.sweeteditor.core.protocol.SearchOptions as CoreSearchOptions
import io.github.lumkit.sweeteditor.core.protocol.SearchRequest
import io.github.lumkit.sweeteditor.core.protocol.SearchState as CoreSearchState
import io.github.lumkit.sweeteditor.core.protocol.SearchStatus as CoreSearchStatus

enum class EditorSearchStatus {
    INACTIVE,
    SEARCHING,
    READY,
    STALE,
    FAILED,
}

data class EditorSearchOptions(
    val caseSensitive: Boolean = false,
    val wholeWord: Boolean = false,
    val useRegex: Boolean = false,
    val wrapAround: Boolean = true,
    val maxMatches: Int = 10_000,
)

data class EditorSearchState(
    val status: EditorSearchStatus,
    val pattern: String,
    val options: EditorSearchOptions,
    val generation: Long,
    val matchCount: Int,
    val currentIndex: Int,
    val hasCurrentMatch: Boolean,
    val currentRange: TextRange,
    val errorMessage: String,
)

internal fun EditorSearchOptions.toProtocol(): CoreSearchOptions = CoreSearchOptions(
    caseSensitive = caseSensitive,
    wholeWord = wholeWord,
    useRegex = useRegex,
    wrapAround = wrapAround,
    maxMatches = maxMatches,
)

internal fun encodeSearchRequest(pattern: String, options: EditorSearchOptions): ByteArray =
    CoreProtocol.encodeSearchRequest(SearchRequest(pattern, options.toProtocol()))

internal fun encodeSearchReplacement(replacement: String): ByteArray {
    val writer = ProtocolWriter()
    writer.writeUtf8String(replacement)
    return writer.toByteArray()
}

internal fun CoreSearchState.toPublic(): EditorSearchState = EditorSearchState(
    status = status.toPublic(),
    pattern = pattern,
    options = EditorSearchOptions(
        caseSensitive = options.caseSensitive,
        wholeWord = options.wholeWord,
        useRegex = options.useRegex,
        wrapAround = options.wrapAround,
        maxMatches = options.maxMatches,
    ),
    generation = generation,
    matchCount = matchCount,
    currentIndex = currentIndex,
    hasCurrentMatch = hasCurrentMatch,
    currentRange = TextRange(
        start = TextPosition(currentRange.start.line, currentRange.start.column),
        end = TextPosition(currentRange.end.line, currentRange.end.column),
    ),
    errorMessage = errorMessage,
)

private fun CoreSearchStatus.toPublic(): EditorSearchStatus = when (this) {
    CoreSearchStatus.INACTIVE -> EditorSearchStatus.INACTIVE
    CoreSearchStatus.SEARCHING -> EditorSearchStatus.SEARCHING
    CoreSearchStatus.READY -> EditorSearchStatus.READY
    CoreSearchStatus.STALE -> EditorSearchStatus.STALE
    CoreSearchStatus.FAILED -> EditorSearchStatus.FAILED
}
