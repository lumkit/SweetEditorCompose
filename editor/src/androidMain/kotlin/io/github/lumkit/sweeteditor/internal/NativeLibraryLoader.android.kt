package io.github.lumkit.sweeteditor.internal

internal actual object NativeLibraryLoader {
    actual fun bundledLibraryResourcePath(): String? = null

    actual fun loadIfAvailable(): Boolean {
        // Android loads `libsweeteditor.so` from the AAR jniLibs folder via System.loadLibrary
        // once the JNI / C ABI bridge is wired.
        return false
    }
}
