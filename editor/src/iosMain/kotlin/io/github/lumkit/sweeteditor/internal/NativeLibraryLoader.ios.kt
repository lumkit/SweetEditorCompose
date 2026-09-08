package io.github.lumkit.sweeteditor.internal

internal actual object NativeLibraryLoader {
    actual fun bundledLibraryResourcePath(): String? = null

    actual fun loadIfAvailable(): Boolean {
        // iOS links SweetEditor through cinterop into the consuming Kotlin/Native framework.
        return false
    }
}
