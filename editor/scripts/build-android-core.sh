#!/usr/bin/env bash
# CMake-build SweetEditor shared libraries for Android ABIs into editor/natives/android/<abi>/.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SE="${SWEETEDITOR_HOME:-}"
if [[ -z "$SE" ]]; then
  echo "SWEETEDITOR_HOME is required" >&2
  exit 1
fi

ABIS=("arm64-v8a" "x86_64")
if [[ $# -gt 0 ]]; then
  ABIS=("$@")
fi

SDK_DIR="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"
if [[ -z "$SDK_DIR" && -f "$ROOT/../local.properties" ]]; then
  SDK_DIR="$(grep -E '^sdk.dir=' "$ROOT/../local.properties" | cut -d= -f2- | tr -d '\r')"
fi

NDK="${ANDROID_NDK:-${ANDROID_NDK_HOME:-}}"
if [[ -z "$NDK" && -n "$SDK_DIR" ]]; then
  NDK="$(ls -1d "$SDK_DIR"/ndk/* 2>/dev/null | tail -1 || true)"
fi
if [[ -z "$NDK" || ! -d "$NDK" ]]; then
  echo "ANDROID_NDK is not set and no NDK was found under the Android SDK" >&2
  exit 1
fi

CMAKE="$(command -v cmake || true)"
NINJA="$(command -v ninja || true)"
if [[ -z "$CMAKE" && -n "$SDK_DIR" ]]; then
  CMAKE="$(ls -1d "$SDK_DIR"/cmake/*/bin/cmake 2>/dev/null | tail -1 || true)"
fi
if [[ -z "$NINJA" && -n "$CMAKE" ]]; then
  NINJA="$(dirname "$CMAKE")/ninja"
fi
if [[ ! -x "${CMAKE:-}" ]]; then
  echo "cmake not found" >&2
  exit 1
fi

INCLUDE_SRC="$SE/include/sweeteditor"
INCLUDE_DST="$ROOT/natives/include/sweeteditor"
mkdir -p "$INCLUDE_DST"
cp -R "$INCLUDE_SRC/." "$INCLUDE_DST/"

TOOLCHAIN="$NDK/build/cmake/android.toolchain.cmake"
for ABI in "${ABIS[@]}"; do
  BUILD="$SE/build/compose-android-$ABI"
  DEST="$ROOT/natives/android/$ABI"
  mkdir -p "$BUILD" "$DEST"
  GEN_ARGS=(
    "$CMAKE" -S "$SE" -B "$BUILD"
    -DCMAKE_BUILD_TYPE=Release
    -DCMAKE_TOOLCHAIN_FILE="$TOOLCHAIN"
    -DANDROID_ABI="$ABI"
    -DCMAKE_ANDROID_ARCH_ABI="$ABI"
    -DANDROID_NDK="$NDK"
    -DCMAKE_ANDROID_NDK="$NDK"
    -DANDROID_PLATFORM=android-24
    -DSWEETEDITOR_BUILD_TESTS=OFF
    -DSWEETEDITOR_BUILD_SHARED=ON
    -DSWEETEDITOR_BUILD_STATIC=OFF
    -DSWEETEDITOR_BUILD_ANDROID_JNI=OFF
    -DSWEETEDITOR_BUILD_WASM_C_ABI=OFF
    -DSWEETEDITOR_BUILD_WASM_EMBIND=OFF
  )
  if [[ -x "${NINJA:-}" ]]; then
    GEN_ARGS+=(-G Ninja "-DCMAKE_MAKE_PROGRAM=$NINJA")
  fi
  "${GEN_ARGS[@]}"
  "$CMAKE" --build "$BUILD" --target sweeteditor --config Release
  SO=""
  for candidate in \
    "$BUILD/lib/libsweeteditor.so" \
    "$BUILD/lib/Release/libsweeteditor.so"
  do
    if [[ -f "$candidate" ]]; then
      SO="$candidate"
      break
    fi
  done
  if [[ -z "$SO" ]]; then
    SO="$(find "$BUILD" -type f -name "libsweeteditor.so" | head -n 1 || true)"
  fi
  if [[ -z "$SO" || ! -f "$SO" ]]; then
    echo "libsweeteditor.so not found for $ABI under $BUILD" >&2
    exit 1
  fi
  cp -f "$SO" "$DEST/libsweeteditor.so"
  echo "Installed $SO -> $DEST/libsweeteditor.so"
done
