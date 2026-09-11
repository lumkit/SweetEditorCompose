package io.github.lumkit.sweeteditor.internal

internal actual object NativeLibraryLoader {
    actual fun bundledLibraryResourcePath(): String? = NativeBundle.WASM_C_ABI_WASM

    actual fun loadIfAvailable(): Boolean = true

    actual fun loadComposeJni() {
        throw IllegalStateException("Web target does not use JNI")
    }
}
