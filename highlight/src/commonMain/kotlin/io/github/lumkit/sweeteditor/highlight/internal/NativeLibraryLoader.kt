package io.github.lumkit.sweeteditor.highlight.internal

internal expect object NativeLibraryLoader {
    fun bundledLibraryResourcePath(): String?

    fun loadIfAvailable(): Boolean

    fun loadComposeJni()
}
