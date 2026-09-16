# SweetEditor Compose

A Compose Multiplatform code editor backed by the [SweetEditor](https://github.com/FinalScave/SweetEditor) C++ core.

Targets **Android**, **iOS**, **Desktop (JVM)**, **Web (JS + Wasm)** — one API, one dependency.

## Install

```kotlin
implementation("io.github.lumkit:sweeteditor-compose:0.1.3")
```

The host must use **Compose Multiplatform 1.12.0**, **Kotlin 2.4.20**, and Android **compileSdk 37** (or newer matching that set). `targetSdk` / `minSdk` can stay lower. Older Compose Gradle plugins ship an older Skiko that is missing `Paragraph.nGetUnresolvedCodepointsCount`, which crashes JS and Wasm at runtime.

Gradle resolves the platform variant automatically:

| Platform | Artifact | Native payload |
|---|---|---|
| Android | `sweeteditor-compose-android` (+ transitive `…-android-jni`) | `libsweeteditor.so` + `libsweeteditor_compose.so` per ABI |
| Desktop JVM | `sweeteditor-compose-jvm` | `/native/<os>-<arch>/` Core + compose JNI |
| iOS | `sweeteditor-compose-iosarm64` / `iosSimulatorArm64` | cinterop statically links `libsweeteditor.a` |
| Web | `sweeteditor-compose-js` / `wasm-js` | `/native/web/` C ABI module |

No XCFramework, no JNA, no manual native linking — the artifacts are self-contained.

## Quick start

```kotlin
import io.github.lumkit.sweeteditor.SweetEditor
import io.github.lumkit.sweeteditor.SweetEditorController
import io.github.lumkit.sweeteditor.theme.EditorTheme

@Composable
fun CodeScreen() {
    val controller = remember { SweetEditorController(initialText = "fun main() { }") }

    SweetEditor(
        modifier = Modifier.fillMaxSize(),
        controller = controller,
        theme = EditorTheme(),
    )
}
```

`SweetEditorController` is the host command entry point. The session and
native `EditorCore` live inside the `SweetEditor` composable — the controller
only forwards commands and never holds a native handle.

```kotlin
controller.whenReady {
    controller.loadDocument("print('hello')")
    controller.insertText("world")
    controller.applyTheme(EditorTheme(dark = true))
    controller.canUndo()
    controller.setSelection(0, 0, 0, 5)
}
```

## Features

- Syntax highlighting, bracket matching, auto-closing pairs
- Search & replace, diff rendering, fold regions
- IME composition (Android, iOS, Desktop, Web)
- Snippet insertion with linked editing (tab stops)
- Completion popup with `textEdit` / `insertText` / snippet semantics
- Copilot-style inline suggestions
- Mobile selection menu, desktop context menu, right-click menu
- Custom decorations, inlay hints, CodeLens, gutter click events
- Theme, settings, and key map as composable inputs
- ProGuard / R8 consumer rules bundled (Android AAR + JVM JAR)

## Platform notes

**Android** — minSdk 24, compileSdk 37 (required by Compose 1.12 AAR metadata).
The AAR ships `consumer-rules.pro` keeping JNI entry points; no extra keep
rules needed.

**Desktop** — JVM 11+. The JAR extracts Core + compose JNI to a per-user
cache on first load. JNI keep rules ship in the JAR under
`META-INF/proguard/` and `META-INF/com.android.tools/r8/`. Android R8
merges them automatically. Compose Desktop's built-in ProGuard task does
**not** scan dependency JARs; if you enable release minification, add those
`.pro` files to `configurationFiles` (this repo's `desktopApp` harvests them
from the runtime classpath).

**iOS** — cinterop statically links `libsweeteditor.a` / `libsweetline.a`.
The SweetLine archive embeds `LC_LINKER_OPTION -liconv`, so the host Xcode
project must **not** add `-liconv` or `-lsweeteditor`.

**Web** — JS and Wasm load the C ABI module automatically (Compose resources
under `/composeResources/…/files/`). Host `index.html` does not need a
manual `<script src="sweeteditor_web_abi.js">`. Use Compose Multiplatform
1.12.0 so Skiko matches the library.

## Building from source

The C++ core lives in a sibling checkout of
[FinalScave/SweetEditor](https://github.com/FinalScave/SweetEditor):

```
SweetEditor/            # C++ core (sibling)
SweetEditorCompose/     # this repo
```

Build native libraries for the current host:

```bash
./editor/scripts/prepare-release-natives.sh --host          # current OS+arch
./editor/scripts/prepare-release-natives.sh --macos-x86_64   # macOS cross-arch
./editor/scripts/prepare-release-natives.sh --ios            # iOS static archives
./editor/scripts/prepare-release-natives.sh --android        # Android .so (needs NDK)
./editor/scripts/prepare-release-natives.sh --wasm          # Wasm C ABI (needs emsdk)
```

Then:

```bash
./gradlew :editor:publishToMavenLocal :editor-android-jni:publishToMavenLocal
```

## Documentation

- [Usage site](https://lumkit.github.io/SweetEditorCompose/) — install, Hello World, and public API (EN / 中文)
- [PUBLISHING.md](./PUBLISHING.md) — Maven coordinates, native packaging, Central Portal setup
- [COMPLIANCE.md](./COMPLIANCE.md) — Host API coverage vs. the SweetEditor integration standard

## License

[GNU Affero General Public License v3.0](./LICENSE)

[中文文档](./README.zh.md)
