package io.github.lumkit.sweeteditor.internal

internal expect object NativeLibraryLoader {
    fun bundledLibraryResourcePath(): String?

    fun loadIfAvailable(): Boolean

    fun loadComposeJni()
}
