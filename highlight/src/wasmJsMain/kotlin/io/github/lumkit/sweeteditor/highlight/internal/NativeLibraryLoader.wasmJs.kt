package io.github.lumkit.sweeteditor.highlight.internal

internal actual object NativeLibraryLoader {
    actual fun bundledLibraryResourcePath(): String? = NativeBundle.WASM_C_ABI_WASM

    actual fun loadIfAvailable(): Boolean = false

    actual fun loadComposeJni() {
        throw IllegalStateException("Web target does not use JNI")
    }
}
