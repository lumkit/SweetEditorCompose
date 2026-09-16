#!/usr/bin/env bash
# CMake-build SweetLine shared libraries for Android ABIs into highlight/natives/android/<abi>/.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SL="${SWEETLINE_HOME:-}"
if [[ -z "$SL" ]]; then
  echo "SWEETLINE_HOME is required" >&2
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

INCLUDE_SRC="$SL/include/sweetline"
INCLUDE_DST="$ROOT/natives/include/sweetline"
mkdir -p "$INCLUDE_DST"
cp -R "$INCLUDE_SRC/." "$INCLUDE_DST/"

TOOLCHAIN="$NDK/build/cmake/android.toolchain.cmake"
for ABI in "${ABIS[@]}"; do
  BUILD="$SL/build/compose-android-$ABI"
  DEST="$ROOT/natives/android/$ABI"
  mkdir -p "$BUILD" "$DEST"
  GEN_ARGS=(
    "$CMAKE" -S "$SL" -B "$BUILD"
    -DCMAKE_BUILD_TYPE=Release
    -DCMAKE_TOOLCHAIN_FILE="$TOOLCHAIN"
    -DANDROID_ABI="$ABI"
    -DCMAKE_ANDROID_ARCH_ABI="$ABI"
    -DANDROID_NDK="$NDK"
    -DCMAKE_ANDROID_NDK="$NDK"
    -DANDROID_PLATFORM=android-24
    -DSWEETLINE_BUILD_TESTS=OFF
    -DSWEETLINE_BUILD_SHARED=ON
    -DSWEETLINE_BUILD_STATIC=OFF
    -DSWEETLINE_BUILD_ANDROID_JNI=OFF
    -DSWEETLINE_BUILD_WASM_EMBIND=OFF
    -DSWEETLINE_BUILD_APPLE_FRAMEWORK=OFF
  )
  if [[ -x "${NINJA:-}" ]]; then
    GEN_ARGS+=(-G Ninja "-DCMAKE_MAKE_PROGRAM=$NINJA")
  fi
  "${GEN_ARGS[@]}"
  "$CMAKE" --build "$BUILD" --target sweetline --config Release
  SO=""
  for candidate in \
    "$BUILD/lib/libsweetline.so" \
    "$BUILD/lib/Release/libsweetline.so"
  do
    if [[ -f "$candidate" ]]; then
      SO="$candidate"
      break
    fi
  done
  if [[ -z "$SO" ]]; then
    SO="$(find "$BUILD" -type f -name "libsweetline.so" | head -n 1 || true)"
  fi
  if [[ -z "$SO" || ! -f "$SO" ]]; then
    echo "libsweetline.so not found for $ABI under $BUILD" >&2
    exit 1
  fi
  cp -f "$SO" "$DEST/libsweetline.so"
  echo "Installed $SO -> $DEST/libsweetline.so"
done
