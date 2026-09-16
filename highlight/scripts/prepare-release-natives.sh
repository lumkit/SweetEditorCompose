#!/usr/bin/env bash
# Build and install SweetLine natives used by Maven artifacts into highlight/natives/.
# Usage:
#   SWEETLINE_HOME=../SweetLine highlight/scripts/prepare-release-natives.sh [--all] [--host] [--macos-x86_64] [--ios] [--android] [--wasm] [--sync-prebuilt]
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
REPO="$(cd "$ROOT/.." && pwd)"
SL="${SWEETLINE_HOME:-}"
if [[ -z "$SL" && -d "$REPO/../SweetLine" ]]; then
  SL="$(cd "$REPO/../SweetLine" && pwd)"
fi
if [[ -z "$SL" ]]; then
  echo "SWEETLINE_HOME is required (or checkout SweetLine next to this repo)" >&2
  exit 1
fi
export SWEETLINE_HOME="$SL"

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

if [[ "$DO_SYNC" -eq 1 && -d "$SL/prebuilt" ]]; then
  echo "==> Syncing $SL/prebuilt into highlight/natives (existing files kept if prebuilt is empty)"
  copy_if_present() {
    local src="$1"
    local dest="$2"
    local pattern="$3"
    [[ -d "$src" ]] || return 0
    mkdir -p "$dest"
    find "$src" -type f -name "$pattern" -exec cp -f {} "$dest/" \;
  }
  copy_if_present "$SL/prebuilt/android/arm64-v8a" "$ROOT/natives/android/arm64-v8a" "*.so"
  copy_if_present "$SL/prebuilt/android/x86_64" "$ROOT/natives/android/x86_64" "*.so"
  copy_if_present "$SL/prebuilt/macos/arm64" "$ROOT/natives/desktop/macos-aarch64" "*.dylib"
  copy_if_present "$SL/prebuilt/macos/x86_64" "$ROOT/natives/desktop/macos-x86_64" "*.dylib"
  copy_if_present "$SL/prebuilt/linux/x86_64" "$ROOT/natives/desktop/linux-x86_64" "*.so"
  copy_if_present "$SL/prebuilt/linux/aarch64" "$ROOT/natives/desktop/linux-aarch64" "*.so"
  copy_if_present "$SL/prebuilt/windows/x64" "$ROOT/natives/desktop/windows-x86_64" "*.dll"
  copy_if_present "$SL/prebuilt/ios/arm64" "$ROOT/natives/ios/arm64" "*.a"
  copy_if_present "$SL/prebuilt/ios/simulator-arm64" "$ROOT/natives/ios/simulator-arm64" "*.a"
  copy_if_present "$SL/prebuilt/wasm" "$ROOT/natives/web" "sweetline_c_abi.js"
  copy_if_present "$SL/prebuilt/wasm" "$ROOT/natives/web" "sweetline_c_abi.wasm"
  if [[ -d "$SL/include/sweetline" ]]; then
    mkdir -p "$ROOT/natives/include/sweetline"
    cp -R "$SL/include/sweetline/." "$ROOT/natives/include/sweetline/"
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
    export SWEETLINE_OSX_ARCH=x86_64
    export SWEETLINE_DESKTOP_FOLDER=macos-x86_64
    export SWEETLINE_JNI_BUILD="$ROOT/build/jni/desktop-macos-x86_64"
    export SWEETLINE_CORE_LIB="$ROOT/natives/desktop/macos-x86_64/libsweetline.dylib"
    bash "$ROOT/scripts/build-host-core.sh" all
    bash "$ROOT/scripts/build-desktop-jni.sh" all
    unset SWEETLINE_OSX_ARCH SWEETLINE_DESKTOP_FOLDER SWEETLINE_JNI_BUILD SWEETLINE_CORE_LIB
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
