package io.github.lumkit.sweeteditor.input

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import java.awt.AWTEvent
import java.awt.Toolkit
import java.awt.Window
import java.awt.event.AWTEventListener
import java.awt.event.WindowEvent
import java.beans.PropertyChangeListener

@Composable
internal actual fun rememberScreenDensity(composeDensity: Float): Float {
    var scale by remember { mutableFloatStateOf(composeDensity) }
    DisposableEffect(composeDensity) {
        fun apply(next: Float) {
            if (next > 0f) scale = next
        }
        fun read(window: Window?): Float {
            val value = window?.graphicsConfiguration?.defaultTransform?.scaleX?.toFloat() ?: composeDensity
            return if (value > 0f) value else composeDensity
        }
        val listener = PropertyChangeListener { event ->
            if (event.propertyName != "graphicsConfiguration") return@PropertyChangeListener
            val window = event.source as? Window ?: return@PropertyChangeListener
            apply(read(window))
        }
        val windows = Window.getWindows().toList()
        windows.forEach { window ->
            window.addPropertyChangeListener("graphicsConfiguration", listener)
        }
        apply(read(windows.firstOrNull { it.isFocused } ?: windows.firstOrNull { it.isShowing }))
        val awtListener = AWTEventListener { event ->
            val window = event.source as? Window ?: return@AWTEventListener
            if (event.id == WindowEvent.WINDOW_OPENED || event.id == WindowEvent.WINDOW_GAINED_FOCUS) {
                window.removePropertyChangeListener("graphicsConfiguration", listener)
                window.addPropertyChangeListener("graphicsConfiguration", listener)
                apply(read(window))
            }
        }
        Toolkit.getDefaultToolkit().addAWTEventListener(awtListener, AWTEvent.WINDOW_EVENT_MASK)
        onDispose {
            windows.forEach { it.removePropertyChangeListener("graphicsConfiguration", listener) }
            Window.getWindows().forEach { it.removePropertyChangeListener("graphicsConfiguration", listener) }
            Toolkit.getDefaultToolkit().removeAWTEventListener(awtListener)
        }
    }
    return scale
}
