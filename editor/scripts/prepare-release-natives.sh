#!/usr/bin/env bash
# Build and install SweetEditor natives used by Maven artifacts into editor/natives/.
# Usage:
#   SWEETEDITOR_HOME=../SweetEditor editor/scripts/prepare-release-natives.sh [--all] [--host] [--macos-x86_64] [--ios] [--android] [--wasm] [--sync-prebuilt]
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
REPO="$(cd "$ROOT/.." && pwd)"
SE="${SWEETEDITOR_HOME:-}"
if [[ -z "$SE" && -d "$REPO/../SweetEditor" ]]; then
  SE="$(cd "$REPO/../SweetEditor" && pwd)"
fi
if [[ -z "$SE" ]]; then
  echo "SWEETEDITOR_HOME is required (or checkout SweetEditor next to this repo)" >&2
  exit 1
fi
export SWEETEDITOR_HOME="$SE"

DO_HOST=0
DO_MACOS_X64=0
DO_IOS=0
DO_ANDROID=0
DO_WASM=0
DO_SYNC=0
STRICT_OPTIONAL=0

if [[ $# -eq 0 ]]; then
  set -- --all
fi

while [[ $# -gt 0 ]]; do
  case "$1" in
    --all)
      DO_HOST=1
      DO_SYNC=1
      case "$(uname -s)" in
        Darwin) DO_MACOS_X64=1; DO_IOS=1 ;;
      esac
      DO_ANDROID=1
      DO_WASM=1
      ;;
    --host|--desktop) DO_HOST=1 ;;
    --macos-x86_64) DO_MACOS_X64=1 ;;
    --ios) DO_IOS=1 ;;
    --android) DO_ANDROID=1; STRICT_OPTIONAL=1 ;;
    --wasm) DO_WASM=1; STRICT_OPTIONAL=1 ;;
    --sync-prebuilt) DO_SYNC=1 ;;
    *)
      echo "Unknown option: $1" >&2
      echo "usage: $0 [--all] [--host] [--macos-x86_64] [--ios] [--android] [--wasm] [--sync-prebuilt]" >&2
      exit 1
      ;;
  esac
  shift
done

if [[ "$DO_SYNC" -eq 1 && -d "$SE/prebuilt" ]]; then
  echo "==> Syncing $SE/prebuilt into editor/natives (existing files kept if prebuilt is empty)"
  copy_if_present() {
    local src="$1"
    local dest="$2"
    local pattern="$3"
    [[ -d "$src" ]] || return 0
    mkdir -p "$dest"
    find "$src" -type f -name "$pattern" -exec cp -f {} "$dest/" \;
  }
  copy_if_present "$SE/prebuilt/android/arm64-v8a" "$ROOT/natives/android/arm64-v8a" "*.so"
  copy_if_present "$SE/prebuilt/android/x86_64" "$ROOT/natives/android/x86_64" "*.so"
  copy_if_present "$SE/prebuilt/macos/arm64" "$ROOT/natives/desktop/macos-aarch64" "*.dylib"
  copy_if_present "$SE/prebuilt/macos/x86_64" "$ROOT/natives/desktop/macos-x86_64" "*.dylib"
  copy_if_present "$SE/prebuilt/linux/x86_64" "$ROOT/natives/desktop/linux-x86_64" "*.so"
  copy_if_present "$SE/prebuilt/linux/aarch64" "$ROOT/natives/desktop/linux-aarch64" "*.so"
  copy_if_present "$SE/prebuilt/windows/x64" "$ROOT/natives/desktop/windows-x86_64" "*.dll"
  copy_if_present "$SE/prebuilt/ios/arm64" "$ROOT/natives/ios/arm64" "*.a"
  copy_if_present "$SE/prebuilt/ios/simulator-arm64" "$ROOT/natives/ios/simulator-arm64" "*.a"
  copy_if_present "$SE/prebuilt/wasm" "$ROOT/natives/web" "sweeteditor_c_abi.js"
  copy_if_present "$SE/prebuilt/wasm" "$ROOT/natives/web" "sweeteditor_c_abi.wasm"
  if [[ -d "$SE/include/sweeteditor" ]]; then
    mkdir -p "$ROOT/natives/include/sweeteditor"
    cp -R "$SE/include/sweeteditor/." "$ROOT/natives/include/sweeteditor/"
  fi
fi

if [[ "$DO_HOST" -eq 1 ]]; then
  echo "==> Host desktop Core + compose JNI"
  bash "$ROOT/scripts/build-host-core.sh" all
  bash "$ROOT/scripts/build-desktop-jni.sh" all
fi

if [[ "$DO_MACOS_X64" -eq 1 ]]; then
  if [[ "$(uname -s)" != "Darwin" ]]; then
    echo "Skipping --macos-x86_64 (not macOS)"
  else
    echo "==> Cross-compiling macOS x86_64 Core + compose JNI"
    export SWEETEDITOR_OSX_ARCH=x86_64
    export SWEETEDITOR_DESKTOP_FOLDER=macos-x86_64
    export SWEETEDITOR_JNI_BUILD="$ROOT/build/jni/desktop-macos-x86_64"
    export SWEETEDITOR_CORE_LIB="$ROOT/natives/desktop/macos-x86_64/libsweeteditor.dylib"
    bash "$ROOT/scripts/build-host-core.sh" all
    bash "$ROOT/scripts/build-desktop-jni.sh" all
    unset SWEETEDITOR_OSX_ARCH SWEETEDITOR_DESKTOP_FOLDER SWEETEDITOR_JNI_BUILD SWEETEDITOR_CORE_LIB
  fi
fi

if [[ "$DO_IOS" -eq 1 ]]; then
  if [[ "$(uname -s)" != "Darwin" ]]; then
    echo "Skipping --ios (not macOS)"
  else
    echo "==> iOS static archives"
    bash "$ROOT/scripts/build-ios-static-core.sh" arm64
    bash "$ROOT/scripts/build-ios-static-core.sh" simulator-arm64
  fi
fi

if [[ "$DO_ANDROID" -eq 1 ]]; then
  if bash "$ROOT/scripts/build-android-core.sh"; then
    :
  else
    if [[ "$STRICT_OPTIONAL" -eq 1 ]]; then
      exit 1
    fi
    echo "warning: Android natives skipped (NDK/CMake failed)" >&2
  fi
fi

if [[ "$DO_WASM" -eq 1 ]]; then
  if bash "$ROOT/scripts/build-web-c-abi.sh"; then
    :
  else
    if [[ "$STRICT_OPTIONAL" -eq 1 ]]; then
      exit 1
    fi
    echo "warning: Web C ABI skipped (Emscripten not available)" >&2
  fi
fi

echo "Natives ready under $ROOT/natives"
