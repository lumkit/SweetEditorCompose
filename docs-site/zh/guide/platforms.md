# 平台注意

## Android

- `minSdk` 24，`compileSdk` 37（Compose 1.12 与本库 AAR metadata 会检查）。
- JNI 打在 AAR 里。`consumer-rules.pro` 保留 JNI 入口。
- 不要再打一份 `libsweeteditor.so`。

## 桌面（JVM）

- JVM 11+。
- 首次加载把 JAR 资源里的 Core 与 compose JNI 解到用户缓存，再 `System.load`。
- Compose Desktop R8/ProGuard 会读 JAR 里的 `META-INF/com.android.tools/r8/`。

## iOS

- `iosArm64` / `iosSimulatorArm64` 由 cinterop 静态链 `libsweeteditor.a`。
- 宿主 Xcode 工程**不得**加 `-lsweeteditor`，也不得再链 SweetEditor 的 dylib / XCFramework。
- 真机与模拟器是两份归档。模拟器 klib 里若混进真机 object，链接会报 “built for iOS while targeting iOS-simulator”。

## Web（JS 与 Wasm）

- 库会注入 `sweeteditor_web_abi.js`，并从 Compose 资源 `files/` 加载 `sweeteditor_c_abi.{js,wasm}`。
- 宿主 `index.html` **不必**再手写 script（0.1.1+）。
- C ABI 就绪后 `NativeBridge.isAvailable` 为 true。`SweetEditor` 最多等约 1800 帧，超时显示加载错误。
- Compose 必须是 1.12.0，否则 Skiko 没有 `nGetUnresolvedCodepointsCount`。

## 共同约定

各端走同一套 C API。行列从 0 起。颜色是打包的 ARGB `Int`（`0xAARRGGBB`）。
