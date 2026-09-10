package io.github.lumkit.sweeteditor.input

import io.github.lumkit.sweeteditor.core.protocol.CaretAffinity
import io.github.lumkit.sweeteditor.core.protocol.ImeCommand
import io.github.lumkit.sweeteditor.core.protocol.ImeCommandKind
import io.github.lumkit.sweeteditor.core.protocol.ImeCoordinateSpace
import io.github.lumkit.sweeteditor.core.protocol.ImeOffsetRange
import io.github.lumkit.sweeteditor.core.protocol.ImeSelection
import io.github.lumkit.sweeteditor.core.protocol.ImeState
import io.github.lumkit.sweeteditor.core.protocol.ImeTextUnit

internal fun noneImeRange(): ImeOffsetRange =
    ImeOffsetRange(ImeCoordinateSpace.DOCUMENT, -1, -1)

internal fun noneImeSelection(): ImeSelection =
    ImeSelection(ImeCoordinateSpace.DOCUMENT, -1, -1, CaretAffinity.DOWNSTREAM)

internal fun documentRange(startUtf16: Long, endUtf16: Long): ImeOffsetRange =
    ImeOffsetRange(ImeCoordinateSpace.DOCUMENT, startUtf16, endUtf16)

internal fun documentSelection(anchorUtf16: Long, activeUtf16: Long): ImeSelection =
    ImeSelection(ImeCoordinateSpace.DOCUMENT, anchorUtf16, activeUtf16, CaretAffinity.DOWNSTREAM)

internal fun collapsedDocumentSelection(offsetUtf16: Long): ImeSelection =
    documentSelection(offsetUtf16, offsetUtf16)

internal fun imeCommand(
    kind: ImeCommandKind,
    targetRange: ImeOffsetRange = noneImeRange(),
    selectionAfter: ImeSelection = noneImeSelection(),
    text: String = "",
    deleteBefore: Long = 0,
    deleteAfter: Long = 0,
    textUnit: ImeTextUnit = ImeTextUnit.UTF16_CODE_UNIT,
): ImeCommand = ImeCommand(
    kind = kind,
    targetRange = targetRange,
    selectionAfter = selectionAfter,
    text = text,
    deleteBefore = deleteBefore,
    deleteAfter = deleteAfter,
    textUnit = textUnit,
)

internal fun hasImeRange(range: ImeOffsetRange?): Boolean =
    range != null && range.startUtf16 >= 0 && range.endUtf16 >= 0

internal fun hasImeSelection(selection: ImeSelection?): Boolean =
    selection != null && selection.anchorUtf16 >= 0 && selection.activeUtf16 >= 0

internal fun hasComposition(state: ImeState?): Boolean =
    state != null && hasImeRange(state.compositionRange)

internal fun clampImeOffset(offset: Long, length: Long): Long =
    offset.coerceIn(0L, length.coerceAtLeast(0L))

internal fun cursorAfterReplacement(
    targetStart: Long,
    targetEnd: Long,
    replacementLength: Int,
    newCursorPosition: Int,
    oldTotalLength: Long,
): Long {
    val newTotalLength = oldTotalLength - (targetEnd - targetStart) + replacementLength
    val cursor = if (newCursorPosition > 0) {
        targetStart + replacementLength + newCursorPosition - 1L
    } else {
        targetStart + newCursorPosition
    }
    return clampImeOffset(cursor, newTotalLength)
}
