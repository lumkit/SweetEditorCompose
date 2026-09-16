package io.github.lumkit.sweeteditor.highlight.internal

internal actual object NativeLibraryLoader {
    actual fun bundledLibraryResourcePath(): String? = null

    actual fun loadIfAvailable(): Boolean = false

    actual fun loadComposeJni() = Unit
}
