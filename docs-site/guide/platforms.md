# Platforms

## Android

- `minSdk` 24, `compileSdk` 37 (AAR metadata from Compose 1.12 and this library).
- JNI libraries ship inside the AAR. `consumer-rules.pro` on the KMP AAR and the JNI AAR keeps JNI entry points; the app does not add extra keep rules.
- Do not add a second copy of `libsweeteditor.so`.

## Desktop (JVM)

- JVM 11+.
- First load extracts Core + compose JNI from JAR resources into a per-user cache, then `System.load`.
- Keep rules ship in the JAR under `META-INF/proguard/` and `META-INF/com.android.tools/r8/`. Compose Desktop's ProGuard task does not scan dependency JARs; include those files if you enable release minification.

## iOS

- cinterop statically links `libsweeteditor.a` (and `libsweetline.a` for highlight) for `iosArm64` and `iosSimulatorArm64`.
- The host Xcode project must **not** add `-lsweeteditor`, `-lsweetline`, or `-liconv`. SweetLine embeds `LC_LINKER_OPTION -liconv`.
- Device and simulator slices are different archives. A device object in the simulator klib fails with “built for iOS while targeting iOS-simulator”.

## Web (JS and Wasm)

- The library injects `sweeteditor_web_abi.js` / `sweetline_web_abi.js` and loads `*_c_abi.{js,wasm}` from Compose resources (`files/`).
- Host `index.html` does **not** need a manual script tag (0.1.1+).
- `NativeBridge.isAvailable` becomes true after the C ABI module is ready. `SweetEditor` waits up to ~1800 frames, then shows a load error.
- Keep Compose 1.12.0 so Skiko exports `nGetUnresolvedCodepointsCount`.

## Shared constraints

All targets talk to the same C API. Line/column are 0-based. Colors are packed ARGB `Int` (`0xAARRGGBB`).
