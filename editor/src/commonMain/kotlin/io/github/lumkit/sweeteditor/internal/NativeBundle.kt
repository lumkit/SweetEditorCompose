package io.github.lumkit.sweeteditor.internal

/**
 * Layout of SweetEditor native artifacts that ship with this module.
 *
 * These paths are packaged into platform-specific Maven artifacts:
 * - Android AAR: `jniLibs/<abi>/libsweeteditor.so`
 * - Desktop JVM JAR: `/native/<os>-<arch>/<lib>`
 * - Web JS/Wasm: `/native/web/sweeteditor_c_abi.{js,wasm}`
 * - iOS klib: cinterop against `libsweeteditor` plus public C headers
 */
internal object NativeBundle {
    const val LIBRARY_NAME: String = "sweeteditor"
    const val RESOURCE_ROOT: String = "native"
    const val WASM_C_ABI_JS: String = "native/web/sweeteditor_c_abi.js"
    const val WASM_C_ABI_WASM: String = "native/web/sweeteditor_c_abi.wasm"
}
