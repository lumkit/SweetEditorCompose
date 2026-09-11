package io.github.lumkit.sweeteditor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import io.github.lumkit.sweeteditor.completion.EditorCompletionPopup
import io.github.lumkit.sweeteditor.selection.EditorSelectionMenuPopup
import io.github.lumkit.sweeteditor.selection.SelectionMenuController
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import io.github.lumkit.sweeteditor.core.HostTextMeasurer
import io.github.lumkit.sweeteditor.core.protocol.EventType
import io.github.lumkit.sweeteditor.core.protocol.PointF
import io.github.lumkit.sweeteditor.core.protocol.PointerCursorType
import io.github.lumkit.sweeteditor.input.coreWheelDelta
import io.github.lumkit.sweeteditor.input.editorHostScale
import io.github.lumkit.sweeteditor.input.editorIme
import io.github.lumkit.sweeteditor.input.rememberEditorClipboard
import io.github.lumkit.sweeteditor.input.encodeGesture
import io.github.lumkit.sweeteditor.input.mapKeyEvent
import io.github.lumkit.sweeteditor.input.mapPointerGesture
import io.github.lumkit.sweeteditor.input.pointerModifiers
import io.github.lumkit.sweeteditor.input.wheelModifiersForCore
import io.github.lumkit.sweeteditor.render.drawEditor
import io.github.lumkit.sweeteditor.render.toComposeColor
import io.github.lumkit.sweeteditor.internal.jni.NativeBridge
import io.github.lumkit.sweeteditor.session.RememberedEditorSession
import androidx.compose.foundation.text.BasicText
import kotlinx.coroutines.delay

@Composable
fun SweetEditor(
    modifier: Modifier = Modifier,
    controller: SweetEditorController,
    theme: EditorTheme = EditorTheme(),
    settings: EditorSettings = EditorSettings(),
    keyMap: EditorKeyMap? = null,
) {
    val density = LocalDensity.current
    val densityValue = density.density
    val fontScale = density.fontScale
    val layoutDirection = LocalLayoutDirection.current
    val fontFamilyResolver = LocalFontFamilyResolver.current
    val textMeasurer = remember(fontFamilyResolver, densityValue, fontScale, layoutDirection) {
        TextMeasurer(
            defaultFontFamilyResolver = fontFamilyResolver,
            defaultDensity = Density(densityValue, fontScale),
            defaultLayoutDirection = layoutDirection,
        )
    }
    val hostMeasurer = remember(controller) {
        HostTextMeasurer(
            textMeasurer,
            TextStyle(
                color = theme.textColor.toComposeColor(),
                fontFamily = theme.fontFamily,
                fontSize = (settings.fontSizeSp * settings.scale).sp,
            ),
        )
    }
    val session = remember(controller) {
        RememberedEditorSession(controller, controller.initialText, hostMeasurer)
    }
    val textStyle = remember(theme, settings.fontSizeSp, session.visualScale, densityValue, fontScale) {
        TextStyle(
            color = theme.textColor.toComposeColor(),
            fontFamily = theme.fontFamily,
            fontSize = (settings.fontSizeSp * session.visualScale).sp,
        )
    }
    val clipboard = rememberEditorClipboard()
    val resolvedKeyMap = keyMap ?: remember { EditorKeyMap.defaultKeyMap() }
    val fontMetricsChanged = hostMeasurer.bind(textMeasurer, textStyle, densityValue, fontScale)
    SideEffect {
        session.bindClipboard(clipboard)
        session.applyKeyMap(resolvedKeyMap)
        session.applyAppearance(theme, settings)
        if (fontMetricsChanged) {
            session.notifyFontMetricsChanged()
        }
    }

    val focusRequester = remember { FocusRequester() }
    val fontAscent = -hostMeasurer.fontAscent()
    val fontDescent = hostMeasurer.fontDescent()
    val pointerIcon = when (session.pointerCursor) {
        PointerCursorType.HAND -> PointerIcon.Hand
        PointerCursorType.TEXT -> PointerIcon.Text
        PointerCursorType.DEFAULT -> PointerIcon.Default
    }

    DisposableEffect(session, focusRequester) {
        session.onTap = { runCatching { focusRequester.requestFocus() } }
        onDispose { session.onTap = null }
    }
    LaunchedEffect(session) {
        var frames = 0
        while (!NativeBridge.isAvailable) {
            withFrameNanos { }
            frames += 1
            if (frames > 1_800) {
                session.markLoadError("SweetEditor native core failed to load")
                return@LaunchedEffect
            }
        }
        session.onRemembered()
    }

    LaunchedEffect(settings.readOnly) {
        if (!settings.readOnly) {
            runCatching { focusRequester.requestFocus() }
        }
    }

    LaunchedEffect(session.wantsAnimation) {
        while (session.wantsAnimation) {
            withFrameNanos {
                session.tickAnimations()
            }
        }
    }

    LaunchedEffect(session.selectionMenuShowToken) {
        val token = session.selectionMenuShowToken
        if (token == 0) return@LaunchedEffect
        delay(SelectionMenuController.SHOW_DELAY_MS)
        session.presentSelectionMenu(token)
    }

    val error = session.loadError
    if (error != null) {
        Box(modifier.background(theme.backgroundColor.toComposeColor())) {
            BasicText(
                text = error,
                style = TextStyle(
                    color = Color(0xFFFF6B6B),
                    fontFamily = FontFamily.Monospace,
                ),
            )
        }
        return
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .background(theme.backgroundColor.toComposeColor()),
    ) {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { size -> session.setViewport(size.width, size.height) }
            .pointerHoverIcon(pointerIcon)
            .editorHostScale(session)
            .editorIme(session, settings.readOnly)
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                val mapped = mapKeyEvent(event) ?: return@onPreviewKeyEvent false
                if (mapped.pointerModifiersOnly) {
                    session.updatePointerModifiers(mapped.modifiers)
                    return@onPreviewKeyEvent false
                }
                session.handleKey(mapped.keyCode, mapped.text, mapped.modifiers)
                true
            }
            .pointerInput(session) {
                var previousPressedCount = 0
                var lastPoint = PointF(0f, 0f)
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: continue
                        val modifiers = pointerModifiers(event)
                        val isMouse = event.changes.any {
                            it.type == PointerType.Mouse || it.type == PointerType.Stylus
                        }
                        val pressedPoints = event.changes
                            .filter { it.pressed }
                            .map { PointF(it.position.x, it.position.y) }
                        lastPoint = PointF(change.position.x, change.position.y)
                        session.notePointer(lastPoint, hovering = event.type != PointerEventType.Exit)
                        if (event.type == PointerEventType.Press) {
                            runCatching { focusRequester.requestFocus() }
                        }
                        if (event.type == PointerEventType.Scroll) {
                            val point = PointF(change.position.x, change.position.y)
                            val (wheelX, wheelY) = coreWheelDelta(change.scrollDelta)
                            val wheelModifiers = wheelModifiersForCore(modifiers)
                            session.handleGesture(
                                encodeGesture(EventType.DIRECT_GESTURE_BEGIN, listOf(point), wheelModifiers),
                            )
                            session.handleGesture(
                                encodeGesture(
                                    type = EventType.MOUSE_WHEEL,
                                    points = listOf(point),
                                    modifiers = wheelModifiers,
                                    wheelDeltaX = wheelX,
                                    wheelDeltaY = wheelY,
                                ),
                            )
                            session.handleGesture(
                                encodeGesture(EventType.DIRECT_GESTURE_END, listOf(point), wheelModifiers),
                            )
                            event.changes.forEach { it.consume() }
                            continue
                        }
                        if (event.type == PointerEventType.Exit && previousPressedCount > 0) {
                            val endType = if (isMouse) EventType.MOUSE_UP else EventType.TOUCH_UP
                            session.handleGesture(encodeGesture(endType, listOf(lastPoint), modifiers))
                            previousPressedCount = 0
                            event.changes.forEach { it.consume() }
                            continue
                        }
                        val mapped = mapPointerGesture(
                            eventType = event.type,
                            isMouse = isMouse,
                            pressedPoints = pressedPoints,
                            fallbackPoint = lastPoint,
                            previousPressedCount = previousPressedCount,
                        )
                        previousPressedCount = pressedPoints.size
                        if (mapped == null) continue
                        session.handleGesture(
                            encodeGesture(
                                type = mapped.type,
                                points = mapped.points,
                                modifiers = modifiers,
                            ),
                        )
                        event.changes.forEach { it.consume() }
                    }
                }
            },
    ) {
        val model = session.renderModel
        if (model != null) {
            drawEditor(model, textMeasurer, textStyle, fontAscent, fontDescent, theme, session.iconProvider)
        }
    }
    EditorCompletionPopup(
        items = session.completionItems,
        selectedIndex = session.completionSelectedIndex,
        anchor = session.completionAnchor,
        theme = theme,
        onSelect = { session.selectCompletionIndex(it) },
        onConfirm = session::applyCompletionItem,
        onDismiss = session::dismissCompletion,
    )
    EditorSelectionMenuPopup(
        items = session.selectionMenuItems,
        anchor = session.selectionMenuAnchor,
        theme = theme,
        onItemClick = session::onSelectionMenuItemClick,
        onDismiss = session::hideSelectionMenu,
    )
    }
}

@Composable
fun rememberSweetEditorController(
    initialText: String = "",
): SweetEditorController {
    return remember {
        SweetEditorController(initialText)
    }
}
