package io.github.lumkit.sweeteditor.input

import io.github.lumkit.sweeteditor.core.protocol.EditorActionResult
import io.github.lumkit.sweeteditor.core.protocol.ImeCommandKind
import io.github.lumkit.sweeteditor.core.protocol.ImeHostAction
import io.github.lumkit.sweeteditor.core.protocol.ImeResultCode
import io.github.lumkit.sweeteditor.core.protocol.ImeState
import io.github.lumkit.sweeteditor.core.protocol.ImeTextSource
import io.github.lumkit.sweeteditor.core.protocol.KeyCode
import io.github.lumkit.sweeteditor.session.RememberedEditorSession
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.cValue
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGPoint
import platform.CoreGraphics.CGRect
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGRectZero
import platform.Foundation.NSComparisonResult
import platform.Foundation.NSOrderedAscending
import platform.Foundation.NSOrderedDescending
import platform.Foundation.NSOrderedSame
import platform.Foundation.NSRange
import platform.UIKit.UIKeyInputProtocol
import platform.UIKit.UITextInputDelegateProtocol
import platform.UIKit.UITextInputProtocol
import platform.UIKit.UITextInputStringTokenizer
import platform.UIKit.UITextInputTokenizerProtocol
import platform.UIKit.UITextLayoutDirection
import platform.UIKit.UITextLayoutDirectionLeft
import platform.UIKit.UITextLayoutDirectionUp
import platform.UIKit.UITextPosition
import platform.UIKit.UITextRange
import platform.UIKit.UITextStorageDirection
import platform.UIKit.UIView
import platform.darwin.NSInteger
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

@OptIn(ExperimentalForeignApi::class)
internal class IosTextPosition(val utf16: Int) : UITextPosition()

@OptIn(ExperimentalForeignApi::class)
internal class IosTextRange(
    val startOffset: Int,
    val endOffset: Int,
) : UITextRange() {
    override fun isEmpty(): Boolean = startOffset == endOffset
    override fun start(): UITextPosition = IosTextPosition(startOffset)
    override fun end(): UITextPosition = IosTextPosition(endOffset)
}

@OptIn(ExperimentalForeignApi::class)
internal class ComposeIosTextInputView(
    var session: RememberedEditorSession,
) : UIView(cValue { CGRectZero }), UITextInputProtocol, UIKeyInputProtocol {
    val adapter: EditorImeAdapter = object : EditorImeAdapter {
        override fun onEditorActionResult(result: EditorActionResult) {
            handleEditorActionResult(result)
        }

        override fun closeOwnedSession() {
            closeImeSession()
        }
    }
    private var sessionId = 0L
    private var lifecycleVersion = 0L
    private var state = emptyImeState()
    private var inputDelegateRef: UITextInputDelegateProtocol? = null
    private val stringTokenizer = UITextInputStringTokenizer(this)

    override fun canBecomeFirstResponder(): Boolean = true

    override fun becomeFirstResponder(): Boolean {
        if (!beginIfNeeded()) return false
        if (super.becomeFirstResponder()) {
            return true
        }
        closeImeSession()
        return false
    }

    override fun resignFirstResponder(): Boolean {
        closeImeSession()
        return super.resignFirstResponder()
    }

    override fun hasText(): Boolean = documentLength() > 0

    override fun insertText(text: String) {
        applyCommands(commandsForCommitText(state, text))
    }

    override fun deleteBackward() {
        session.handleKey(KeyCode.BACKSPACE, null, 0)
    }

    override fun textInRange(range: UITextRange): String? {
        val offsets = offsets(range) ?: return null
        if (!isActive()) return null
        val context = session.getImeContext(
            sessionId,
            ImeTextSource.EDITING,
            offsets.first.toLong(),
            (offsets.second - offsets.first).toLong(),
        ) ?: return null
        if (context.resultCode != ImeResultCode.OK) return null
        return context.text
    }

    override fun replaceRange(range: UITextRange, withText: String) {
        val offsets = offsets(range) ?: return
        applyCommands(commandsForCommitText(state, withText, offsets.first.toLong(), offsets.second.toLong()))
    }

    override fun setMarkedText(markedText: String?, selectedRange: CValue<NSRange>) {
        selectedRange.useContents {
            val commands = commandsForMarkedText(
                state,
                markedText.orEmpty(),
                location.toInt(),
                length.toInt(),
            ) ?: return
            applyCommands(commands)
        }
    }

    override fun unmarkText() {
        if (!hasComposition(state)) return
        applyCommands(listOf(imeCommand(ImeCommandKind.FINISH_COMPOSITION)))
    }

    override fun selectedTextRange(): UITextRange? {
        if (!hasImeSelection(state.selection)) return null
        val start = minOf(state.selection.anchorUtf16, state.selection.activeUtf16).toInt()
        val end = maxOf(state.selection.anchorUtf16, state.selection.activeUtf16).toInt()
        return IosTextRange(start, end)
    }

    override fun setSelectedTextRange(selectedTextRange: UITextRange?) {
        val offsets = offsets(selectedTextRange) ?: return
        applyCommands(
            listOf(
                imeCommand(
                    kind = ImeCommandKind.SET_SELECTION,
                    selectionAfter = documentSelection(offsets.first.toLong(), offsets.second.toLong()),
                ),
            ),
        )
    }

    override fun markedTextRange(): UITextRange? {
        if (!hasComposition(state)) return null
        return IosTextRange(
            state.compositionRange.startUtf16.toInt(),
            state.compositionRange.endUtf16.toInt(),
        )
    }

    override fun markedTextStyle(): Map<Any?, *>? = null

    override fun setMarkedTextStyle(markedTextStyle: Map<Any?, *>?) = Unit

    override fun beginningOfDocument(): UITextPosition = IosTextPosition(0)

    override fun endOfDocument(): UITextPosition = IosTextPosition(documentLength())

    override fun textRangeFromPosition(fromPosition: UITextPosition, toPosition: UITextPosition): UITextRange? {
        val start = (fromPosition as? IosTextPosition)?.utf16 ?: return null
        val end = (toPosition as? IosTextPosition)?.utf16 ?: return null
        return IosTextRange(minOf(start, end), maxOf(start, end))
    }

    override fun positionFromPosition(position: UITextPosition, offset: NSInteger): UITextPosition? {
        val current = (position as? IosTextPosition)?.utf16 ?: return null
        val next = (current + offset.toInt()).coerceIn(0, documentLength())
        return IosTextPosition(next)
    }

    override fun positionFromPosition(
        position: UITextPosition,
        inDirection: UITextLayoutDirection,
        offset: NSInteger,
    ): UITextPosition? {
        val sign = if (inDirection == UITextLayoutDirectionLeft || inDirection == UITextLayoutDirectionUp) -1 else 1
        return positionFromPosition(position, offset * sign)
    }

    override fun comparePosition(position: UITextPosition, toPosition: UITextPosition): NSComparisonResult {
        val left = (position as? IosTextPosition)?.utf16 ?: return NSOrderedSame
        val right = (toPosition as? IosTextPosition)?.utf16 ?: return NSOrderedSame
        return when {
            left < right -> NSOrderedAscending
            left > right -> NSOrderedDescending
            else -> NSOrderedSame
        }
    }

    override fun offsetFromPosition(from: UITextPosition, toPosition: UITextPosition): NSInteger {
        val start = (from as? IosTextPosition)?.utf16 ?: return 0
        val end = (toPosition as? IosTextPosition)?.utf16 ?: return 0
        return (end - start).toLong()
    }

    override fun positionWithinRange(range: UITextRange, farthestInDirection: UITextLayoutDirection): UITextPosition? {
        return if (farthestInDirection == UITextLayoutDirectionLeft || farthestInDirection == UITextLayoutDirectionUp) {
            range.start()
        } else {
            range.end()
        }
    }

    override fun characterRangeByExtendingPosition(
        position: UITextPosition,
        inDirection: UITextLayoutDirection,
    ): UITextRange? {
        val current = (position as? IosTextPosition)?.utf16 ?: return null
        val length = documentLength()
        val start: Int
        val end: Int
        if (inDirection == UITextLayoutDirectionLeft || inDirection == UITextLayoutDirectionUp) {
            start = (current - 1).coerceAtLeast(0)
            end = current
        } else {
            start = current
            end = (current + 1).coerceAtMost(length)
        }
        return IosTextRange(start, end)
    }

    override fun baseWritingDirectionForPosition(
        position: UITextPosition,
        inDirection: UITextStorageDirection,
    ): Long = 0

    override fun setBaseWritingDirection(writingDirection: Long, forRange: UITextRange) = Unit

    override fun firstRectForRange(range: UITextRange): CValue<CGRect> = CGRectMake(0.0, 0.0, 1.0, 20.0)

    override fun caretRectForPosition(position: UITextPosition): CValue<CGRect> = CGRectMake(0.0, 0.0, 1.0, 20.0)

    override fun selectionRectsForRange(range: UITextRange): List<*> = emptyList<Any>()

    override fun closestPositionToPoint(point: CValue<CGPoint>): UITextPosition? {
        val start = minOf(state.selection.anchorUtf16, state.selection.activeUtf16)
        return IosTextPosition(if (start >= 0) start.toInt() else 0)
    }

    override fun closestPositionToPoint(point: CValue<CGPoint>, withinRange: UITextRange): UITextPosition? {
        val closest = (closestPositionToPoint(point) as? IosTextPosition)?.utf16 ?: return withinRange.start()
        val offsets = offsets(withinRange) ?: return withinRange.start()
        return IosTextPosition(closest.coerceIn(offsets.first, offsets.second))
    }

    override fun characterRangeAtPoint(point: CValue<CGPoint>): UITextRange? {
        val position = closestPositionToPoint(point) ?: return selectedTextRange()
        return textRangeFromPosition(position, position)
    }

    override fun tokenizer(): UITextInputTokenizerProtocol = stringTokenizer

    override fun inputDelegate(): UITextInputDelegateProtocol? = inputDelegateRef

    override fun setInputDelegate(inputDelegate: UITextInputDelegateProtocol?) {
        inputDelegateRef = inputDelegate
    }

    private fun handleEditorActionResult(result: EditorActionResult) {
        if (!isActive()) return
        when (result.imeHostAction) {
            ImeHostAction.CLOSE_SESSION -> {
                closeLocalSession()
                if (session.isCurrentImeAdapter(adapter)) {
                    session.bindImeAdapter(null)
                }
                if (isFirstResponder) {
                    resignFirstResponder()
                }
            }
            ImeHostAction.RESTART_SESSION -> restartWithoutEnding()
            else -> {
                if (result.imeState.resultCode == ImeResultCode.OK &&
                    result.imeState.sessionId == sessionId
                ) {
                    state = result.imeState
                    if (result.textChanges.isNotEmpty() || result.compositionChanged) {
                        inputDelegateRef?.textDidChange(this)
                    }
                    if (result.cursorChanged || result.selectionChanged) {
                        inputDelegateRef?.selectionDidChange(this)
                    }
                } else if (result.imeState.resultCode == ImeResultCode.SESSION_MISMATCH) {
                    restartWithoutEnding()
                }
            }
        }
    }

    private fun closeImeSession() {
        val id = sessionId
        closeLocalSession()
        if (session.isCurrentImeAdapter(adapter)) {
            session.bindImeAdapter(null)
        }
        if (id == 0L) return
        session.endImeSession(id)
    }

    private fun beginIfNeeded(): Boolean {
        if (isActive()) return true
        val started = session.beginImeSession()
        if (started == null || started.resultCode != ImeResultCode.OK || started.sessionId == 0L) {
            return false
        }
        sessionId = started.sessionId
        state = started
        session.bindImeAdapter(adapter)
        return true
    }

    private fun restartWithoutEnding() {
        closeLocalSession()
        lifecycleVersion += 1
        val version = lifecycleVersion
        dispatch_async(dispatch_get_main_queue()) {
            if (version != lifecycleVersion || !isFirstResponder) return@dispatch_async
            if (beginIfNeeded()) {
                inputDelegateRef?.textDidChange(this)
                inputDelegateRef?.selectionDidChange(this)
            }
        }
    }

    private fun applyCommands(commands: List<io.github.lumkit.sweeteditor.core.protocol.ImeCommand>): Boolean {
        if (!isActive() || commands.isEmpty()) return false
        val result = session.applyImeCommands(sessionId, commands) ?: return false
        return result.handled && result.imeState.resultCode == ImeResultCode.OK
    }

    private fun closeLocalSession() {
        sessionId = 0
        state = emptyImeState()
    }

    private fun isActive(): Boolean = sessionId != 0L

    private fun documentLength(): Int {
        if (!isActive()) return 0
        val context = session.getImeContext(sessionId, ImeTextSource.EDITING, 0, 0) ?: return 0
        return context.totalLengthUtf16.toInt().coerceAtLeast(0)
    }

    private fun offsets(range: UITextRange?): Pair<Int, Int>? {
        val ios = range as? IosTextRange ?: return null
        return ios.startOffset to ios.endOffset
    }

    private fun emptyImeState(): ImeState = ImeState(
        resultCode = ImeResultCode.OK,
        sessionId = 0,
        stateRevision = 0,
        selection = noneImeSelection(),
        compositionRange = noneImeRange(),
    )
}
