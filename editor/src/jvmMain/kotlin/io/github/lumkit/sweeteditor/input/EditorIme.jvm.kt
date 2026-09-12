package io.github.lumkit.sweeteditor.input

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import io.github.lumkit.sweeteditor.session.RememberedEditorSession
import java.awt.KeyboardFocusManager
import java.awt.Window
import java.lang.reflect.Proxy
import javax.swing.JComponent
import javax.swing.RootPaneContainer
import javax.swing.SwingUtilities

internal actual fun Modifier.editorHostScale(session: RememberedEditorSession): Modifier {
    if (!isMacOs) return this
    return composed {
        val listener = remember(session) { MacosMagnifyBridge(session) }
        DisposableEffect(session, listener) {
            SwingUtilities.invokeLater { listener.attach() }
            onDispose { SwingUtilities.invokeLater { listener.detach() } }
        }
        this
    }
}

private val isMacOs: Boolean =
    System.getProperty("os.name").orEmpty().contains("mac", ignoreCase = true)

/**
 * Compose Desktop does not deliver trackpad pinch as two TOUCH points.
 * On macOS the JDK still receives NSMagnify via hidden `com.apple.eawt.event`.
 */
private class MacosMagnifyBridge(
    private val session: RememberedEditorSession,
) {
    private val components = mutableListOf<JComponent>()
    private var listener: Any? = null

    fun attach(retries: Int = 8) {
        if (listener != null) return
        val created = createListener() ?: return
        val roots = linkedSetOf<JComponent>()
        focusedRootPane()?.let { roots += it }
        for (window in Window.getWindows()) {
            val root = (window as? RootPaneContainer)?.rootPane ?: continue
            roots += root
        }
        for (root in roots) {
            if (invokeGestureUtilities("addGestureListenerTo", root, created)) {
                components += root
            }
        }
        if (components.isEmpty()) {
            if (roots.isEmpty() && retries > 0) {
                SwingUtilities.invokeLater { attach(retries - 1) }
            }
            return
        }
        listener = created
    }

    fun detach() {
        val current = listener
        listener = null
        val attached = components.toList()
        components.clear()
        if (current != null) {
            attached.forEach { invokeGestureUtilities("removeGestureListenerFrom", it, current) }
        }
    }

    private fun createListener(): Any? {
        return try {
            val magType = Class.forName("com.apple.eawt.event.MagnificationListener")
            val phaseType = Class.forName("com.apple.eawt.event.GesturePhaseListener")
            Proxy.newProxyInstance(magType.classLoader, arrayOf(magType, phaseType)) { _, method, args ->
                when (method.name) {
                    "gestureBegan" -> session.beginHostScaleGesture()
                    "gestureEnded" -> session.endHostScaleGesture()
                    "magnify" -> {
                        val event = args?.firstOrNull() ?: return@newProxyInstance null
                        val magnification = event.javaClass.getMethod("getMagnification").invoke(event) as Double
                        val factor = magnificationToDirectScale(magnification)
                        if (factor != null) {
                            session.handleDirectScale(factor)
                        }
                        runCatching { event.javaClass.getMethod("consume").invoke(event) }
                    }
                }
                null
            }
        } catch (_: Throwable) {
            null
        }
    }

    private fun invokeGestureUtilities(method: String, root: JComponent, listener: Any): Boolean {
        return try {
            val utilities = Class.forName("com.apple.eawt.event.GestureUtilities")
            val gestureListener = Class.forName("com.apple.eawt.event.GestureListener")
            utilities.getMethod(method, JComponent::class.java, gestureListener).invoke(null, root, listener)
            true
        } catch (_: Throwable) {
            false
        }
    }

    private fun focusedRootPane(): JComponent? {
        val manager = KeyboardFocusManager.getCurrentKeyboardFocusManager()
        val window = manager.focusedWindow ?: manager.activeWindow
        val root = (window as? RootPaneContainer)?.rootPane
        return root ?: window?.let { SwingUtilities.getRootPane(it) }
    }
}
