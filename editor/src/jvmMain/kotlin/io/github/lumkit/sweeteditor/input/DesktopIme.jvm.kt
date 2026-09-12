package io.github.lumkit.sweeteditor.input

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusEventModifierNode
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.PlatformTextInputMethodRequest
import androidx.compose.ui.platform.PlatformTextInputModifierNode
import androidx.compose.ui.platform.establishTextInputSession
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.CommitTextCommand
import androidx.compose.ui.text.input.DeleteSurroundingTextInCodePointsCommand
import androidx.compose.ui.text.input.EditCommand
import androidx.compose.ui.text.input.FinishComposingTextCommand
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.ImeOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.SetComposingRegionCommand
import androidx.compose.ui.text.input.SetComposingTextCommand
import androidx.compose.ui.text.input.SetSelectionCommand
import androidx.compose.ui.text.input.TextEditingScope
import androidx.compose.ui.text.input.TextEditorState
import androidx.compose.ui.text.input.TextFieldValue
import io.github.lumkit.sweeteditor.core.protocol.EditorActionResult
import io.github.lumkit.sweeteditor.core.protocol.ImeCommand
import io.github.lumkit.sweeteditor.core.protocol.ImeHostAction
import io.github.lumkit.sweeteditor.core.protocol.ImeResultCode
import io.github.lumkit.sweeteditor.core.protocol.ImeState
import io.github.lumkit.sweeteditor.core.protocol.ImeTextSource
import io.github.lumkit.sweeteditor.core.protocol.KeyCode
import io.github.lumkit.sweeteditor.session.RememberedEditorSession
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@OptIn(ExperimentalComposeUiApi::class)
internal actual fun Modifier.editorIme(session: RememberedEditorSession, readOnly: Boolean): Modifier =
    this.then(EditorImeElement(session, readOnly))

private data class EditorImeElement(
    val session: RememberedEditorSession,
    val readOnly: Boolean,
) : ModifierNodeElement<EditorImeNode>() {
    override fun create(): EditorImeNode = EditorImeNode(session, readOnly)

    override fun update(node: EditorImeNode) {
        node.bindSession(session)
        node.setReadOnly(readOnly)
    }
}

@OptIn(ExperimentalComposeUiApi::class)
private class EditorImeNode(
    session: RememberedEditorSession,
    private var readOnly: Boolean,
) : Modifier.Node(),
    PlatformTextInputModifierNode,
    FocusEventModifierNode,
    CompositionLocalConsumerModifierNode {
    var session: RememberedEditorSession = session
        private set
    private var inputJob: Job? = null
    private val adapter = DesktopComposeImeAdapter()
    private val onTap: () -> Unit = {
        startInput()
        currentValueOf(LocalSoftwareKeyboardController)?.show()
    }

    fun setReadOnly(value: Boolean) {
        if (readOnly == value) return
        readOnly = value
        if (value) stopInput()
    }

    fun bindSession(next: RememberedEditorSession) {
        if (session === next) return
        if (isAttached) {
            session.imeTapHandler = null
            adapter.unbind()
        }
        session = next
        if (isAttached) {
            session.imeTapHandler = onTap
        }
    }

    override fun onAttach() {
        session.imeTapHandler = onTap
    }

    override fun onFocusEvent(focusState: FocusState) {
        if (focusState.isFocused && !readOnly) {
            startInput()
        } else if (!focusState.isFocused) {
            stopInput()
        }
    }

    override fun onDetach() {
        if (session.imeTapHandler === onTap) {
            session.imeTapHandler = null
        }
        stopInput()
        super.onDetach()
    }

    private fun startInput() {
        if (!isAttached || readOnly) return
        if (inputJob?.isActive == true) return
        if (!adapter.bind(session)) return
        inputJob = coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                establishTextInputSession {
                    startInputMethod(adapter.request)
                }
            } finally {
                adapter.unbind()
                if (inputJob?.isActive != true) {
                    inputJob = null
                }
            }
        }
    }

    private fun stopInput() {
        inputJob?.cancel()
        inputJob = null
        adapter.unbind()
        currentValueOf(LocalSoftwareKeyboardController)?.hide()
    }
}

@OptIn(ExperimentalComposeUiApi::class)
private class DesktopComposeImeAdapter : EditorImeAdapter {
    private var session: RememberedEditorSession? = null
    private var sessionId = 0L
    private var windowStartUtf16 = 0L
    private var totalLengthUtf16 = 0L
    private var state = emptyState()
    var value by mutableStateOf(TextFieldValue())
        private set
    var focusedRect by mutableStateOf<Rect?>(null)
        private set

    val request: PlatformTextInputMethodRequest = DesktopEditorTextInputRequest(this)

    fun bind(session: RememberedEditorSession): Boolean {
        unbind()
        val started = session.beginImeSession()
        if (started == null || started.resultCode != ImeResultCode.OK || started.sessionId == 0L) {
            return false
        }
        this.session = session
        sessionId = started.sessionId
        session.bindImeAdapter(this)
        syncFromCore()
        return true
    }

    fun unbind() {
        val current = session ?: return
        val id = sessionId
        closeLocal()
        if (current.isCurrentImeAdapter(this)) {
            current.bindImeAdapter(null)
        }
        if (id != 0L) {
            current.endImeSession(id)
        }
    }

    override fun onEditorActionResult(result: EditorActionResult) {
        if (!isActive()) return
        if (result.imeHostAction != ImeHostAction.NONE) {
            closeOwnedSession()
            return
        }
        if (result.imeState.resultCode == ImeResultCode.OK &&
            result.imeState.sessionId == sessionId
        ) {
            syncMirror(result.imeState)
            refreshFocusedRect()
        } else if (result.imeState.resultCode == ImeResultCode.SESSION_MISMATCH) {
            closeOwnedSession()
        }
    }

    override fun closeOwnedSession() {
        unbind()
    }

    fun onEditCommand(commands: List<EditCommand>) {
        val current = session ?: return
        if (!isActive()) return
        val pending = ArrayList<ImeCommand>()
        fun flush() {
            if (pending.isEmpty()) return
            current.applyImeCommands(sessionId, pending.toList())
            pending.clear()
        }
        for (command in commands) {
            when (val mapped = mapComposeEditCommand(command, state, windowStartUtf16, totalLengthUtf16)) {
                is ComposeInputAction.Commands -> pending += mapped.commands
                ComposeInputAction.Backspace -> {
                    flush()
                    current.handleKey(KeyCode.BACKSPACE, null, 0)
                }
                null -> Unit
            }
        }
        flush()
    }

    fun editText(block: TextEditingScope.() -> Unit) {
        val collected = ArrayList<EditCommand>()
        collectingTextEditingScope(collected).block()
        onEditCommand(collected)
    }

    private fun syncFromCore(): Boolean {
        val current = session ?: return false
        if (!isActive()) return false
        val next = current.getImeState(sessionId) ?: return false
        if (next.resultCode != ImeResultCode.OK || next.sessionId != sessionId) {
            closeOwnedSession()
            return false
        }
        return syncMirror(next)
    }

    private fun syncMirror(next: ImeState): Boolean {
        val current = session ?: return false
        var requiredStart = minOf(next.selection.anchorUtf16, next.selection.activeUtf16)
        var requiredEnd = maxOf(next.selection.anchorUtf16, next.selection.activeUtf16)
        if (hasComposition(next)) {
            requiredStart = minOf(requiredStart, next.compositionRange.startUtf16)
            requiredEnd = maxOf(requiredEnd, next.compositionRange.endUtf16)
        }
        val start = maxOf(0L, requiredStart - MAX_IME_TEXT_LENGTH / 2L)
        val length = maxOf(MAX_IME_TEXT_LENGTH.toLong(), requiredEnd - start)
        val context = current.getImeContext(sessionId, ImeTextSource.EDITING, start, length) ?: return false
        if (context.resultCode != ImeResultCode.OK) return false
        state = next
        windowStartUtf16 = context.sliceStartUtf16
        totalLengthUtf16 = context.totalLengthUtf16
        val localLength = context.text.length
        val selection = TextRange(
            localImeOffset(context.selection.anchorUtf16, context.selection.coordinateSpace, context.sliceStartUtf16, localLength),
            localImeOffset(context.selection.activeUtf16, context.selection.coordinateSpace, context.sliceStartUtf16, localLength),
        )
        val composition = localImeRange(context.compositionRange, context.sliceStartUtf16, localLength)
            ?.let { TextRange(it.first, it.second) }
        value = TextFieldValue(text = context.text, selection = selection, composition = composition)
        refreshFocusedRect()
        return true
    }

    private fun refreshFocusedRect() {
        val cursor = session?.getCursorRect() ?: return
        focusedRect = Rect(cursor.x, cursor.y, cursor.x + 1f, cursor.y + cursor.height)
    }

    private fun closeLocal() {
        session = null
        sessionId = 0
        windowStartUtf16 = 0
        totalLengthUtf16 = 0
        state = emptyState()
        value = TextFieldValue()
        focusedRect = null
    }

    private fun isActive(): Boolean = session != null && sessionId != 0L

    private companion object {
        const val MAX_IME_TEXT_LENGTH = 32768
        fun emptyState(): ImeState = ImeState(
            resultCode = ImeResultCode.OK,
            sessionId = 0,
            stateRevision = 0,
            selection = noneImeSelection(),
            compositionRange = noneImeRange(),
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
private class DesktopEditorTextInputRequest(
    private val adapter: DesktopComposeImeAdapter,
) : PlatformTextInputMethodRequest {
    override val value: () -> TextFieldValue = { adapter.value }
    override val state: TextEditorState = object : TextEditorState {
        override val selection: TextRange get() = adapter.value.selection
        override val composition: TextRange? get() = adapter.value.composition
        override val length: Int get() = adapter.value.text.length
        override fun get(index: Int): Char = adapter.value.text[index]
        override fun subSequence(startIndex: Int, endIndex: Int): CharSequence =
            adapter.value.text.subSequence(startIndex, endIndex)
        override val text: String get() = adapter.value.text
    }
    override val imeOptions: ImeOptions = ImeOptions(
        singleLine = false,
        capitalization = KeyboardCapitalization.None,
        autoCorrect = false,
        keyboardType = KeyboardType.Text,
        imeAction = ImeAction.None,
    )
    override val onEditCommand: (List<EditCommand>) -> Unit = adapter::onEditCommand
    override val onImeAction: ((ImeAction) -> Unit)? = null
    override val textLayoutResult: () -> TextLayoutResult? = { null }
    override val focusedRectInRoot: () -> Rect? = { adapter.focusedRect }
    override val textFieldRectInRoot: () -> Rect? = { adapter.focusedRect }
    override val textClippingRectInRoot: () -> Rect? = { adapter.focusedRect }
    override val unclippedTextOffsetInRoot: () -> Offset? = {
        adapter.focusedRect?.let { Offset(it.left, it.top) }
    }
    override val editText: (TextEditingScope.() -> Unit) -> Unit = adapter::editText
}

@OptIn(ExperimentalComposeUiApi::class)
private fun collectingTextEditingScope(commands: MutableList<EditCommand>): TextEditingScope =
    object : TextEditingScope {
        override fun deleteSurroundingTextInCodePoints(lengthBeforeCursor: Int, lengthAfterCursor: Int) {
            commands += DeleteSurroundingTextInCodePointsCommand(lengthBeforeCursor, lengthAfterCursor)
        }

        override fun setSelection(start: Int, end: Int) {
            commands += SetSelectionCommand(start, end)
        }

        override fun commitText(text: CharSequence, newCursorPosition: Int) {
            commands += CommitTextCommand(text.toString(), newCursorPosition)
        }

        override fun setComposingRegion(start: Int, end: Int) {
            commands += SetComposingRegionCommand(start, end)
        }

        override fun setComposingText(text: CharSequence, newCursorPosition: Int) {
            commands += SetComposingTextCommand(text.toString(), newCursorPosition)
        }

        override fun finishComposingText() {
            commands += FinishComposingTextCommand()
        }
    }
