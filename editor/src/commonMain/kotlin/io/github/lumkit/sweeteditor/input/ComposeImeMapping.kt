package io.github.lumkit.sweeteditor.input

import androidx.compose.ui.text.input.BackspaceCommand
import androidx.compose.ui.text.input.CommitTextCommand
import androidx.compose.ui.text.input.DeleteAllCommand
import androidx.compose.ui.text.input.DeleteSurroundingTextCommand
import androidx.compose.ui.text.input.DeleteSurroundingTextInCodePointsCommand
import androidx.compose.ui.text.input.EditCommand
import androidx.compose.ui.text.input.FinishComposingTextCommand
import androidx.compose.ui.text.input.MoveCursorCommand
import androidx.compose.ui.text.input.SetComposingRegionCommand
import androidx.compose.ui.text.input.SetComposingTextCommand
import androidx.compose.ui.text.input.SetSelectionCommand
import io.github.lumkit.sweeteditor.core.protocol.ImeCommand
import io.github.lumkit.sweeteditor.core.protocol.ImeCommandKind
import io.github.lumkit.sweeteditor.core.protocol.ImeCoordinateSpace
import io.github.lumkit.sweeteditor.core.protocol.ImeOffsetRange
import io.github.lumkit.sweeteditor.core.protocol.ImeState
import io.github.lumkit.sweeteditor.core.protocol.ImeTextUnit

internal sealed class ComposeInputAction {
    data class Commands(val commands: List<ImeCommand>) : ComposeInputAction()
    data object Backspace : ComposeInputAction()
}

internal fun mapComposeEditCommand(
    command: EditCommand,
    state: ImeState,
    windowStartUtf16: Long,
    totalLengthUtf16: Long,
): ComposeInputAction? {
    return when (command) {
        is CommitTextCommand -> ComposeInputAction.Commands(
            listOf(replaceCurrentText(state, ImeCommandKind.COMMIT_TEXT, command.text, command.newCursorPosition, totalLengthUtf16)),
        )
        is SetComposingTextCommand -> ComposeInputAction.Commands(
            listOf(replaceCurrentText(state, ImeCommandKind.UPDATE_COMPOSITION, command.text, command.newCursorPosition, totalLengthUtf16)),
        )
        is FinishComposingTextCommand -> ComposeInputAction.Commands(
            listOf(imeCommand(ImeCommandKind.FINISH_COMPOSITION)),
        )
        is SetComposingRegionCommand -> {
            val start = toDocument(minOf(command.start, command.end), windowStartUtf16, totalLengthUtf16)
            val end = toDocument(maxOf(command.start, command.end), windowStartUtf16, totalLengthUtf16)
            val commands = ArrayList<ImeCommand>(2)
            if (hasComposition(state) || start == end) {
                commands += imeCommand(ImeCommandKind.FINISH_COMPOSITION)
            }
            if (start != end) {
                commands += imeCommand(
                    kind = ImeCommandKind.BEGIN_COMPOSITION,
                    targetRange = documentRange(start, end),
                )
            }
            ComposeInputAction.Commands(commands)
        }
        is SetSelectionCommand -> ComposeInputAction.Commands(
            listOf(
                imeCommand(
                    kind = ImeCommandKind.SET_SELECTION,
                    selectionAfter = documentSelection(
                        toDocument(command.start, windowStartUtf16, totalLengthUtf16),
                        toDocument(command.end, windowStartUtf16, totalLengthUtf16),
                    ),
                ),
            ),
        )
        is DeleteSurroundingTextCommand -> ComposeInputAction.Commands(
            listOf(
                imeCommand(
                    kind = ImeCommandKind.DELETE_SURROUNDING,
                    deleteBefore = command.lengthBeforeCursor.toLong(),
                    deleteAfter = command.lengthAfterCursor.toLong(),
                    textUnit = ImeTextUnit.UTF16_CODE_UNIT,
                ),
            ),
        )
        is DeleteSurroundingTextInCodePointsCommand -> ComposeInputAction.Commands(
            listOf(
                imeCommand(
                    kind = ImeCommandKind.DELETE_SURROUNDING,
                    deleteBefore = command.lengthBeforeCursor.toLong(),
                    deleteAfter = command.lengthAfterCursor.toLong(),
                    textUnit = ImeTextUnit.UNICODE_CODE_POINT,
                ),
            ),
        )
        is DeleteAllCommand -> ComposeInputAction.Commands(
            listOf(
                imeCommand(
                    kind = ImeCommandKind.COMMIT_TEXT,
                    targetRange = documentRange(0, totalLengthUtf16),
                    text = "",
                    selectionAfter = collapsedDocumentSelection(0),
                ),
            ),
        )
        is MoveCursorCommand -> {
            val current = state.selection.activeUtf16
            val next = clampImeOffset(current + command.amount, totalLengthUtf16)
            ComposeInputAction.Commands(
                listOf(
                    imeCommand(
                        kind = ImeCommandKind.SET_SELECTION,
                        selectionAfter = collapsedDocumentSelection(next),
                    ),
                ),
            )
        }
        is BackspaceCommand -> ComposeInputAction.Backspace
        else -> null
    }
}

internal fun localImeOffset(offsetUtf16: Long, space: ImeCoordinateSpace, sliceStartUtf16: Long, localLength: Int): Int {
    val document = if (space == ImeCoordinateSpace.DOCUMENT) offsetUtf16 else offsetUtf16 + sliceStartUtf16
    return (document - sliceStartUtf16).coerceIn(0L, localLength.toLong()).toInt()
}

internal fun localImeRange(range: ImeOffsetRange, sliceStartUtf16: Long, localLength: Int): Pair<Int, Int>? {
    if (!hasImeRange(range) || range.startUtf16 == range.endUtf16) return null
    return localImeOffset(range.startUtf16, range.coordinateSpace, sliceStartUtf16, localLength) to
        localImeOffset(range.endUtf16, range.coordinateSpace, sliceStartUtf16, localLength)
}

private fun replaceCurrentText(
    state: ImeState,
    kind: ImeCommandKind,
    text: String,
    newCursorPosition: Int,
    totalLengthUtf16: Long,
): ImeCommand {
    val start: Long
    val end: Long
    if (hasComposition(state)) {
        start = state.compositionRange.startUtf16
        end = state.compositionRange.endUtf16
    } else {
        start = minOf(state.selection.anchorUtf16, state.selection.activeUtf16)
        end = maxOf(state.selection.anchorUtf16, state.selection.activeUtf16)
    }
    return imeCommand(
        kind = kind,
        text = text,
        selectionAfter = collapsedDocumentSelection(
            cursorAfterReplacement(start, end, text.length, newCursorPosition, totalLengthUtf16),
        ),
    )
}

private fun toDocument(localOffset: Int, windowStartUtf16: Long, totalLengthUtf16: Long): Long =
    clampImeOffset(windowStartUtf16 + localOffset.toLong(), totalLengthUtf16)
