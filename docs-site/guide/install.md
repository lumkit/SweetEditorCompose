# Install

Add one dependency. Gradle resolves the platform variant.

```kotlin
implementation("io.github.lumkit:sweeteditor-compose:0.1.1")
```

## Host versions

The host **must** match this set (or a newer compatible pair):

| Piece | Version |
|---|---|
| Compose Multiplatform | 1.12.0 |
| Kotlin | 2.4.20 |
| Android `compileSdk` | 37 |

`targetSdk` and `minSdk` can stay lower. `minSdk` for the library is 24.

Older Compose Gradle plugins ship an older Skiko that is missing `Paragraph.nGetUnresolvedCodepointsCount`. JS and Wasm crash at runtime.

## What you get per platform

| Platform | Artifact | Native payload |
|---|---|---|
| Android | `sweeteditor-compose-android` (transitive `…-android-jni`) | `libsweeteditor.so` + `libsweeteditor_compose.so` per ABI |
| Desktop JVM | `sweeteditor-compose-jvm` | `/native/<os>-<arch>/` Core + compose JNI |
| iOS | `iosarm64` / `iosSimulatorArm64` | cinterop statically links `libsweeteditor.a` |
| Web | `js` / `wasm-js` | C ABI module under Compose resources |

You do not add an XCFramework, JNA, or extra native link flags.

## Repositories

Maven Central. Snapshots (if you use a `-SNAPSHOT` version) go to the Central Portal snapshots repository.

## Next

[Hello World](./quick-start.md)
