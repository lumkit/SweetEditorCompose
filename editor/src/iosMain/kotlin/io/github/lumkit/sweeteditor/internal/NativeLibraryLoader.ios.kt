package io.github.lumkit.sweeteditor.internal

internal actual object NativeLibraryLoader {
    actual fun bundledLibraryResourcePath(): String? = null

    actual fun loadIfAvailable(): Boolean = false

    actual fun loadComposeJni() {
        // iOS links libsweeteditor through cinterop; there is no sweeteditor_compose JNI library.
    }
}
