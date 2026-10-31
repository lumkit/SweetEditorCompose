# Contributing to SweetEditor Compose

This guide outlines how to set up the development environment, build from source, run tests, and submit pull requests.

## Prerequisites

* **JDK 21**: Java Development Kit 21 (Temurin or Zulu recommended).
* **Android SDK**: `compileSdk 37` is required by Compose Multiplatform 1.12. Set the `ANDROID_HOME` environment variable or define `sdk.dir` in `local.properties`.
* **CMake (>= 3.22) & Ninja**: Required to compile the native C++ core (`SweetEditor` and `SweetLine`) for the host platform during tests and desktop runs.
  * Ubuntu / Debian: `sudo apt-get install cmake ninja-build`
  * macOS: `brew install cmake ninja`
  * Windows: Visual Studio C++ tools or `winget install Kitware.CMake Ninja-build.Ninja`
* **Node.js (>= 22)**: Only required when working on `docs-site`.

## Repository Structure

* `editor/`: Main Compose Multiplatform code editor component (`SweetEditor`, `SweetEditorController`, IME, renderers).
  * `editor/native/`: Vendored SweetEditor C++ core.
  * `editor/natives/`: Prebuilt native libraries per platform.
* `editor-android-jni/`: Android AAR packaging for editor JNI binaries.
* `highlight/`: Syntax highlighting integration using SweetLine.
  * `highlight/SweetLine/`: Git submodule referencing the SweetLine C++ core.
  * `highlight/src/commonMain/composeResources/files/syntaxes/`: Built-in syntax JSON definitions.
* `highlight-android-jni/`: Android AAR packaging for highlight JNI binaries.
* `shared/`: Cross-platform demo UI and test harness.
* `desktopApp/`, `androidApp/`, `iosApp/`, `webApp/`: Sample applications.
* `docs-site/`: VitePress documentation website.

## Setup and Submodules

SweetLine is tracked as a git submodule under `highlight/SweetLine`.

Clone with submodules:
```bash
git clone --recurse-submodules https://github.com/lumkit/SweetEditorCompose.git
cd SweetEditorCompose
```

If already cloned without submodules:
```bash
git submodule update --init --recursive
```

Configure `local.properties` if `ANDROID_HOME` is not set globally:
```properties
sdk.dir=/path/to/android/sdk
```

## Running Tests

Run the test suite across core modules:
```bash
./gradlew :editor:jvmTest :highlight:jvmTest :shared:jvmTest
```

## Running Sample Apps

* Desktop: `./gradlew :desktopApp:run`
* Android: `./gradlew :androidApp:assembleDebug`
* Web (Wasm): `./gradlew :webApp:wasmJsBrowserDevelopmentRun`

## Documentation Site

The documentation is built with VitePress:
```bash
cd docs-site
npm install
npm run docs:dev
```

To verify the production build:
```bash
npm run docs:build
```

## Pull Request Guidelines

1. Follow [Conventional Commits](https://www.conventionalcommits.org/) (`feat:`, `fix:`, `chore:`, `ci:`, `docs:`, `perf:`).
2. Keep pull requests focused on a single concern.
3. Ensure all tests pass locally before opening a pull request.
