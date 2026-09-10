package io.github.lumkit.sweeteditor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
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
import io.github.lumkit.sweeteditor.input.editorIme
import io.github.lumkit.sweeteditor.input.encodeGesture
import io.github.lumkit.sweeteditor.input.mapKeyEvent
import io.github.lumkit.sweeteditor.input.mapPointerEventType
import io.github.lumkit.sweeteditor.input.pointerModifiers
import io.github.lumkit.sweeteditor.render.drawEditor
import io.github.lumkit.sweeteditor.session.RememberedEditorSession
import androidx.compose.foundation.text.BasicText

@Composable
fun SweetEditor(
    modifier: Modifier = Modifier,
    controller: SweetEditorController,
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
    val textStyle = remember(densityValue, fontScale) {
        TextStyle(
            color = Color(0xFFD4D4D4),
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp,
        )
    }
    val hostMeasurer = remember(controller) {
        HostTextMeasurer(textMeasurer, textStyle)
    }
    val session = remember(controller) {
        RememberedEditorSession(controller, controller.initialText, hostMeasurer)
    }
    val fontMetricsChanged = hostMeasurer.bind(textMeasurer, textStyle, densityValue, fontScale)
    SideEffect {
        if (fontMetricsChanged) {
            session.notifyFontMetricsChanged()
        }
    }

    val focusRequester = remember { FocusRequester() }
    val fontAscent = -hostMeasurer.fontAscent()
    val pointerIcon = when (session.pointerCursor) {
        PointerCursorType.HAND -> PointerIcon.Hand
        PointerCursorType.TEXT -> PointerIcon.Text
        PointerCursorType.DEFAULT -> PointerIcon.Default
    }

    DisposableEffect(session, focusRequester) {
        session.onTap = { runCatching { focusRequester.requestFocus() } }
        onDispose { session.onTap = null }
    }

    LaunchedEffect(session.wantsAnimation) {
        while (session.wantsAnimation) {
            withFrameNanos {
                session.tickAnimations()
            }
        }
    }

    val error = session.loadError
    if (error != null) {
        Box(modifier.background(Color(0xFF1E1E1E))) {
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

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E1E))
            .onSizeChanged { size -> session.setViewport(size.width, size.height) }
            .pointerHoverIcon(pointerIcon)
            .editorIme(session)
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                val mapped = mapKeyEvent(event) ?: return@onPreviewKeyEvent false
                session.handleKey(mapped.keyCode, mapped.text, mapped.modifiers)
                true
            }
            .pointerInput(session) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: continue
                        val modifiers = pointerModifiers(event)
                        if (event.type == PointerEventType.Press) {
                            change.consume()
                        }
                        if (event.type == PointerEventType.Scroll) {
                            val point = PointF(change.position.x, change.position.y)
                            val (wheelX, wheelY) = coreWheelDelta(change.scrollDelta)
                            session.handleGesture(
                                encodeGesture(EventType.DIRECT_GESTURE_BEGIN, listOf(point), modifiers),
                            )
                            session.handleGesture(
                                encodeGesture(
                                    type = EventType.MOUSE_WHEEL,
                                    points = listOf(point),
                                    modifiers = modifiers,
                                    wheelDeltaX = wheelX,
                                    wheelDeltaY = wheelY,
                                ),
                            )
                            session.handleGesture(
                                encodeGesture(EventType.DIRECT_GESTURE_END, listOf(point), modifiers),
                            )
                            change.consume()
                            continue
                        }
                        if (event.type == PointerEventType.Exit) {
                            session.handleGesture(
                                encodeGesture(
                                    EventType.MOUSE_MOVE,
                                    listOf(PointF(-1f, -1f)),
                                    modifiers,
                                ),
                            )
                            continue
                        }
                        val type = mapPointerEventType(event, change.type) ?: continue
                        session.handleGesture(
                            encodeGesture(
                                type = type,
                                points = listOf(PointF(change.position.x, change.position.y)),
                                modifiers = modifiers,
                            ),
                        )
                        change.consume()
                    }
                }
            },
    ) {
        val model = session.renderModel
        if (model != null) {
            drawEditor(model, textMeasurer, textStyle, fontAscent)
        }
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
