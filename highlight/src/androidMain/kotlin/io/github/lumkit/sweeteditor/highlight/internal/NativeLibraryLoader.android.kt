package io.github.lumkit.sweeteditor.highlight.internal

internal actual object NativeLibraryLoader {
    @Volatile
    private var loaded = false

    actual fun bundledLibraryResourcePath(): String? = null

    actual fun loadIfAvailable(): Boolean {
        return try {
            loadComposeJni()
            true
        } catch (_: UnsatisfiedLinkError) {
            false
        }
    }

    actual fun loadComposeJni() {
        synchronized(this) {
            if (loaded) return
            System.loadLibrary(NativeBundle.LIBRARY_NAME)
            System.loadLibrary(NativeBundle.COMPOSE_JNI_LIBRARY_NAME)
            loaded = true
        }
    }
}
