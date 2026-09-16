package io.github.lumkit.sweeteditor.highlight.internal

/**
 * Layout of SweetLine native artifacts packaged with this module.
 *
 * - Android AAR: `jniLibs/<abi>/libsweetline.so` plus compose JNI
 * - Desktop JVM JAR: `/native/<os>-<arch>/<lib>`
 * - Web JS/Wasm: `/native/web/sweetline_c_abi.{js,wasm}`
 * - iOS klib: cinterop against `libsweetline.a`
 */
internal object NativeBundle {
    const val LIBRARY_NAME: String = "sweetline"
    const val COMPOSE_JNI_LIBRARY_NAME: String = "sweetline_compose"
    const val RESOURCE_ROOT: String = "native"
    const val WASM_C_ABI_JS: String = "native/web/sweetline_c_abi.js"
    const val WASM_C_ABI_WASM: String = "native/web/sweetline_c_abi.wasm"
}
