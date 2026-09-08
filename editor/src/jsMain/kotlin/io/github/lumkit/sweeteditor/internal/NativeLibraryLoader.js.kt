package io.github.lumkit.sweeteditor.internal

internal actual object NativeLibraryLoader {
    actual fun bundledLibraryResourcePath(): String? = NativeBundle.WASM_C_ABI_WASM

    actual fun loadIfAvailable(): Boolean {
        // Kotlin/JS loads the Emscripten C ABI module from bundled resources.
        return false
    }
}
