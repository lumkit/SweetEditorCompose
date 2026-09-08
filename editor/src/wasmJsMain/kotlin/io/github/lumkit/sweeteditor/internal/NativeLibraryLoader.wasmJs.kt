package io.github.lumkit.sweeteditor.internal

internal actual object NativeLibraryLoader {
    actual fun bundledLibraryResourcePath(): String? = NativeBundle.WASM_C_ABI_WASM

    actual fun loadIfAvailable(): Boolean {
        // Kotlin/Wasm consumes the same Emscripten C ABI module as JS.
        return false
    }
}
