package io.github.lumkit.sweeteditor.input

import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import android.text.Editable
import android.text.Selection
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextUtils
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.view.inputmethod.SurroundingText
import android.view.inputmethod.TextAttribute
import io.github.lumkit.sweeteditor.core.protocol.EditorActionResult
import io.github.lumkit.sweeteditor.core.protocol.ImeCommand
import io.github.lumkit.sweeteditor.core.protocol.ImeCommandKind
import io.github.lumkit.sweeteditor.core.protocol.ImeHostAction
import io.github.lumkit.sweeteditor.core.protocol.ImeResultCode
import io.github.lumkit.sweeteditor.core.protocol.ImeState
import io.github.lumkit.sweeteditor.core.protocol.ImeTextSource
import io.github.lumkit.sweeteditor.core.protocol.ImeTextUnit
import io.github.lumkit.sweeteditor.session.RememberedEditorSession

internal class ComposeEditorInputConnection(
    private val session: RememberedEditorSession,
    private val view: View,
) : BaseInputConnection(view, true), EditorImeAdapter {
    private val editable = SpannableStringBuilder()
    private val composingSpan = Any()
    private var sessionId = 0L
    private var windowStartUtf16 = 0L
    private var totalLengthUtf16 = 0L
    private var state = ImeState(
        resultCode = ImeResultCode.OK,
        sessionId = 0,
        stateRevision = 0,
        selection = noneImeSelection(),
        compositionRange = noneImeRange(),
    )

    init {
        Selection.setSelection(editable, 0)
        val started = session.beginImeSession()
        if (started != null && started.resultCode == ImeResultCode.OK && started.sessionId != 0L) {
            sessionId = started.sessionId
            session.bindImeAdapter(this)
        }
    }

    override fun closeConnection() {
        closeOwnedSession()
        super.closeConnection()
    }

    override fun getEditable(): Editable = editable

    fun configureEditorInfo(outAttrs: EditorInfo): Boolean {
        outAttrs.inputType = EditorInfo.TYPE_CLASS_TEXT or
            EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE or
            EditorInfo.TYPE_TEXT_FLAG_AUTO_CORRECT
        outAttrs.imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI or EditorInfo.IME_ACTION_NONE
        if (!syncFromCore()) {
            outAttrs.initialSelStart = 0
            outAttrs.initialSelEnd = 0
            return false
        }
        outAttrs.initialSelStart = safeInt(state.selection.anchorUtf16)
        outAttrs.initialSelEnd = safeInt(state.selection.activeUtf16)
        val active = clampEditableOffset(state.selection.activeUtf16 - windowStartUtf16)
        outAttrs.initialCapsMode = TextUtils.getCapsMode(
            editable,
            active,
            TextUtils.CAP_MODE_CHARACTERS or TextUtils.CAP_MODE_WORDS or TextUtils.CAP_MODE_SENTENCES,
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && editable.isNotEmpty()) {
            outAttrs.setInitialSurroundingSubText(editable, safeInt(windowStartUtf16))
        }
        return true
    }

    override fun setSelection(start: Int, end: Int): Boolean {
        if (!isActive()) return false
        val command = imeCommand(
            kind = ImeCommandKind.SET_SELECTION,
            selectionAfter = documentSelection(
                clampImeOffset(start.toLong(), totalLengthUtf16),
                clampImeOffset(end.toLong(), totalLengthUtf16),
            ),
        )
        return applyCommands(listOf(command))
    }

    override fun setComposingText(text: CharSequence?, newCursorPosition: Int): Boolean =
        replaceCurrentText(ImeCommandKind.UPDATE_COMPOSITION, text, newCursorPosition)

    override fun setComposingRegion(start: Int, end: Int): Boolean {
        if (!isActive()) return false
        val safeStart = clampImeOffset(minOf(start, end).toLong(), totalLengthUtf16)
        val safeEnd = clampImeOffset(maxOf(start, end).toLong(), totalLengthUtf16)
        val commands = ArrayList<ImeCommand>(2)
        if (hasComposition(state) || safeStart == safeEnd) {
            commands += imeCommand(ImeCommandKind.FINISH_COMPOSITION)
        }
        if (safeStart != safeEnd) {
            commands += imeCommand(
                kind = ImeCommandKind.BEGIN_COMPOSITION,
                targetRange = documentRange(safeStart, safeEnd),
            )
        }
        return applyCommands(commands)
    }

    override fun commitText(text: CharSequence?, newCursorPosition: Int): Boolean =
        replaceCurrentText(ImeCommandKind.COMMIT_TEXT, text, newCursorPosition)

    @TargetApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    override fun replaceText(
        start: Int,
        end: Int,
        text: CharSequence,
        newCursorPosition: Int,
        textAttribute: TextAttribute?,
    ): Boolean {
        if (start < 0 || end < 0 || !isActive()) return false
        val safeStart = clampImeOffset(minOf(start, end).toLong(), totalLengthUtf16)
        val safeEnd = clampImeOffset(maxOf(start, end).toLong(), totalLengthUtf16)
        val replacement = text.toString()
        val commands = ArrayList<ImeCommand>(2)
        if (hasComposition(state)) {
            commands += imeCommand(ImeCommandKind.FINISH_COMPOSITION)
        }
        commands += imeCommand(
            kind = ImeCommandKind.COMMIT_TEXT,
            targetRange = documentRange(safeStart, safeEnd),
            text = replacement,
            selectionAfter = collapsedDocumentSelection(
                cursorAfterReplacement(
                    safeStart,
                    safeEnd,
                    replacement.length,
                    newCursorPosition,
                    totalLengthUtf16,
                ),
            ),
        )
        return applyCommands(commands)
    }

    override fun finishComposingText(): Boolean {
        if (!isActive()) return false
        return applyCommands(listOf(imeCommand(ImeCommandKind.FINISH_COMPOSITION)))
    }

    override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean =
        deleteSurrounding(beforeLength, afterLength, ImeTextUnit.UTF16_CODE_UNIT)

    override fun deleteSurroundingTextInCodePoints(beforeLength: Int, afterLength: Int): Boolean =
        deleteSurrounding(beforeLength, afterLength, ImeTextUnit.UNICODE_CODE_POINT)

    override fun sendKeyEvent(event: KeyEvent): Boolean {
        if (!isActive()) return false
        if (event.action != KeyEvent.ACTION_DOWN) return true
        val mapped = mapAndroidImeKeyEvent(event) ?: return false
        session.handleKey(mapped.keyCode, mapped.text, mapped.modifiers)
        return true
    }

    @TargetApi(Build.VERSION_CODES.S)
    override fun getSurroundingText(beforeLength: Int, afterLength: Int, flags: Int): SurroundingText? {
        if (beforeLength < 0 || afterLength < 0 || !syncFromCore()) return null
        val local = super.getSurroundingText(beforeLength, afterLength, flags) ?: return null
        return SurroundingText(
            local.text,
            local.selectionStart,
            local.selectionEnd,
            safeInt(windowStartUtf16 + local.offset),
        )
    }

    override fun onEditorActionResult(result: EditorActionResult) {
        if (!isActive()) return
        if (result.imeHostAction != ImeHostAction.NONE) {
            closeLocalSession()
            executeHostAction(result.imeHostAction)
            return
        }
        if (result.imeState.resultCode == ImeResultCode.OK &&
            result.imeState.sessionId == sessionId &&
            needsImeNotification(result) &&
            syncMirror(result.imeState)
        ) {
            updateImeSelectionState()
        } else if (result.imeState.resultCode == ImeResultCode.SESSION_MISMATCH) {
            closeLocalSession()
            executeHostAction(ImeHostAction.RESTART_SESSION)
        }
    }

    override fun closeOwnedSession() {
        val id = sessionId
        closeLocalSession()
        if (session.isCurrentImeAdapter(this)) {
            session.bindImeAdapter(null)
        }
        if (id == 0L) return
        session.endImeSession(id)
    }

    private fun replaceCurrentText(
        kind: ImeCommandKind,
        text: CharSequence?,
        newCursorPosition: Int,
    ): Boolean {
        if (!isActive()) return false
        val replacement = text?.toString().orEmpty()
        val start: Long
        val end: Long
        if (hasComposition(state)) {
            start = state.compositionRange.startUtf16
            end = state.compositionRange.endUtf16
        } else {
            start = minOf(state.selection.anchorUtf16, state.selection.activeUtf16)
            end = maxOf(state.selection.anchorUtf16, state.selection.activeUtf16)
        }
        val command = imeCommand(
            kind = kind,
            text = replacement,
            selectionAfter = collapsedDocumentSelection(
                cursorAfterReplacement(
                    start,
                    end,
                    replacement.length,
                    newCursorPosition,
                    totalLengthUtf16,
                ),
            ),
        )
        return applyCommands(listOf(command))
    }

    private fun deleteSurrounding(beforeLength: Int, afterLength: Int, textUnit: ImeTextUnit): Boolean {
        if (beforeLength < 0 || afterLength < 0 || !isActive()) return false
        return applyCommands(
            listOf(
                imeCommand(
                    kind = ImeCommandKind.DELETE_SURROUNDING,
                    deleteBefore = beforeLength.toLong(),
                    deleteAfter = afterLength.toLong(),
                    textUnit = textUnit,
                ),
            ),
        )
    }

    private fun applyCommands(commands: List<ImeCommand>): Boolean {
        if (!isActive() || commands.isEmpty()) return false
        val result = session.applyImeCommands(sessionId, commands) ?: return false
        return result.handled && result.imeState.resultCode == ImeResultCode.OK
    }

    private fun syncFromCore(): Boolean {
        if (!isActive()) return false
        val next = session.getImeState(sessionId) ?: return false
        if (next.resultCode != ImeResultCode.OK || next.sessionId != sessionId) {
            closeLocalSession()
            return false
        }
        return syncMirror(next)
    }

    private fun syncMirror(next: ImeState): Boolean {
        var requiredStart = minOf(next.selection.anchorUtf16, next.selection.activeUtf16)
        var requiredEnd = maxOf(next.selection.anchorUtf16, next.selection.activeUtf16)
        if (hasComposition(next)) {
            requiredStart = minOf(requiredStart, next.compositionRange.startUtf16)
            requiredEnd = maxOf(requiredEnd, next.compositionRange.endUtf16)
        }
        val start = maxOf(0L, requiredStart - MAX_IME_TEXT_LENGTH / 2L)
        val length = maxOf(MAX_IME_TEXT_LENGTH.toLong(), requiredEnd - start)
        val context = session.getImeContext(sessionId, ImeTextSource.EDITING, start, length) ?: return false
        if (context.resultCode != ImeResultCode.OK) return false
        state = next
        windowStartUtf16 = context.sliceStartUtf16
        totalLengthUtf16 = context.totalLengthUtf16
        editable.removeSpan(composingSpan)
        editable.replace(0, editable.length, context.text)
        if (hasImeSelection(context.selection)) {
            Selection.setSelection(
                editable,
                clampEditableOffset(context.selection.anchorUtf16),
                clampEditableOffset(context.selection.activeUtf16),
            )
        } else {
            Selection.setSelection(editable, 0)
        }
        val composition = context.compositionRange
        if (hasImeRange(composition) && composition.startUtf16 != composition.endUtf16) {
            editable.setSpan(
                composingSpan,
                clampEditableOffset(composition.startUtf16),
                clampEditableOffset(composition.endUtf16),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE or Spanned.SPAN_COMPOSING,
            )
        }
        return true
    }

    private fun updateImeSelectionState() {
        if (!isActive()) return
        val manager = inputMethodManager() ?: return
        var compositionStart = -1
        var compositionEnd = -1
        if (hasComposition(state)) {
            compositionStart = safeInt(state.compositionRange.startUtf16)
            compositionEnd = safeInt(state.compositionRange.endUtf16)
        }
        manager.updateSelection(
            view,
            safeInt(state.selection.anchorUtf16),
            safeInt(state.selection.activeUtf16),
            compositionStart,
            compositionEnd,
        )
    }

    private fun closeLocalSession() {
        sessionId = 0
        editable.removeSpan(composingSpan)
        editable.clear()
        Selection.setSelection(editable, 0)
        windowStartUtf16 = 0
        totalLengthUtf16 = 0
        state = ImeState(
            resultCode = ImeResultCode.OK,
            sessionId = 0,
            stateRevision = 0,
            selection = noneImeSelection(),
            compositionRange = noneImeRange(),
        )
    }

    private fun executeHostAction(hostAction: ImeHostAction) {
        if (hostAction != ImeHostAction.CLOSE_SESSION && hostAction != ImeHostAction.RESTART_SESSION) {
            return
        }
        if (!session.isCurrentImeAdapter(this) || !view.hasFocus()) return
        inputMethodManager()?.restartInput(view)
    }

    private fun isActive(): Boolean = sessionId != 0L

    private fun inputMethodManager(): InputMethodManager? =
        view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager

    private fun clampEditableOffset(offset: Long): Int =
        safeInt(offset.coerceIn(0L, editable.length.toLong()))

    private fun needsImeNotification(result: EditorActionResult): Boolean =
        result.textChanges.isNotEmpty() ||
            result.cursorChanged ||
            result.selectionChanged ||
            result.compositionChanged

    private fun safeInt(value: Long): Int = value.coerceIn(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong()).toInt()

    private companion object {
        const val MAX_IME_TEXT_LENGTH = 32768
    }
}
