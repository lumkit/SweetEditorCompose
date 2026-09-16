package io.github.lumkit.sweeteditor.highlight.internal

internal actual object NativeLibraryLoader {
    actual fun bundledLibraryResourcePath(): String? = null

    actual fun loadIfAvailable(): Boolean = true

    actual fun loadComposeJni() = Unit
}
