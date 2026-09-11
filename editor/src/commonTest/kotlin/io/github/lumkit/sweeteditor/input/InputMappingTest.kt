package io.github.lumkit.sweeteditor.input

import androidx.compose.ui.input.pointer.PointerEventType
import io.github.lumkit.sweeteditor.core.protocol.EventType
import io.github.lumkit.sweeteditor.core.protocol.KeyModifier
import io.github.lumkit.sweeteditor.core.protocol.PointF
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InputMappingTest {
    @Test
    fun modifierKeysymsAreNotTypedCharacters() {
        assertFalse(isTypedCharacter(0xFFE1))
        assertFalse(isTypedCharacter(0xF704))
        assertFalse(isTypedCharacter(16))
        assertFalse(isTypedCharacter(0))
        assertTrue(isTypedCharacter('a'.code))
        assertTrue(isTypedCharacter('中'.code))
        assertTrue(isTypedCharacter('！'.code))
        assertNull(typedCharacterBytes(0xFFE1))
    }

    @Test
    fun secondFingerBecomesPointerDownAndPinchMoveKeepsBothPoints() {
        val first = PointF(10f, 20f)
        val second = PointF(40f, 80f)
        val down = mapPointerGesture(
            eventType = PointerEventType.Press,
            isMouse = false,
            pressedPoints = listOf(first, second),
            fallbackPoint = second,
            previousPressedCount = 1,
        )
        assertEquals(EventType.TOUCH_POINTER_DOWN, down?.type)
        assertEquals(2, down?.points?.size)

        val move = mapPointerGesture(
            eventType = PointerEventType.Move,
            isMouse = false,
            pressedPoints = listOf(first, second),
            fallbackPoint = second,
            previousPressedCount = 2,
        )
        assertEquals(EventType.TOUCH_MOVE, move?.type)
        assertEquals(2, move?.points?.size)
    }

    @Test
    fun mouseSecondaryPressIsContextMenuDown() {
        val point = PointF(8f, 16f)
        val mapped = mapPointerGesture(
            eventType = PointerEventType.Press,
            isMouse = true,
            pressedPoints = listOf(point),
            fallbackPoint = point,
            previousPressedCount = 0,
            isSecondaryButton = true,
        )
        assertEquals(EventType.MOUSE_RIGHT_DOWN, mapped?.type)
    }

    @Test
    fun macosMagnificationDeltaBecomesCoreDirectScale() {
        assertEquals(1.1f, magnificationToDirectScale(0.1)!!)
        assertEquals(0.9f, magnificationToDirectScale(-0.1)!!)
        assertNull(magnificationToDirectScale(0.0))
        assertNull(magnificationToDirectScale(Double.NaN))
    }

    @Test
    fun commandOrControlWheelAddsCtrlForCoreZoom() {
        assertEquals(KeyModifier.CTRL, wheelModifiersForCore(KeyModifier.CTRL))
        assertEquals(KeyModifier.META or KeyModifier.CTRL, wheelModifiersForCore(KeyModifier.META))
        assertEquals(0, wheelModifiersForCore(0))
    }
}
