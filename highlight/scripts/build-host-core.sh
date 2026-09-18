#!/usr/bin/env bash
# Build the SweetLine C++ core for the current desktop host
# and copy the matching headers + shared library into highlight/natives/.
set -euo pipefail

MODE="${1:-build}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SL="${SWEETLINE_HOME:-}"
if [[ -z "$SL" ]]; then
  echo "SWEETLINE_HOME is required" >&2
  exit 1
fi

OS="$(uname -s)"
ARCH="$(uname -m)"
DEST_DIR=""
LIB_NAME=""
case "$OS" in
  Darwin)
    LIB_NAME="libsweetline.dylib"
    if [[ "${SWEETLINE_OSX_ARCH:-}" == "x86_64" || "$ARCH" == "x86_64" ]]; then
      DEST_DIR="$ROOT/natives/desktop/macos-x86_64"
    else
      DEST_DIR="$ROOT/natives/desktop/macos-aarch64"
    fi
    ;;
  Linux)
    LIB_NAME="libsweetline.so"
    if [[ "$ARCH" == "aarch64" || "$ARCH" == "arm64" ]]; then
      DEST_DIR="$ROOT/natives/desktop/linux-aarch64"
    else
      DEST_DIR="$ROOT/natives/desktop/linux-x86_64"
    fi
    ;;
  MINGW*|MSYS*|CYGWIN*)
    LIB_NAME="sweetline.dll"
    DEST_DIR="$ROOT/natives/desktop/windows-x86_64"
    ;;
  *)
    echo "Unsupported host OS: $OS" >&2
    exit 1
    ;;
esac

BUILD="$SL/build/compose-host"
if [[ -n "${SWEETLINE_OSX_ARCH:-}" ]]; then
  BUILD="$SL/build/compose-host-$SWEETLINE_OSX_ARCH"
fi
INCLUDE_SRC="$SL/include/sweetline"
INCLUDE_DST="$ROOT/natives/include/sweetline"

SDK_DIR="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"
if [[ -z "$SDK_DIR" && -f "$ROOT/../local.properties" ]]; then
  SDK_DIR="$(grep -E '^sdk.dir=' "$ROOT/../local.properties" | cut -d= -f2- | tr -d '\r')"
fi

CMAKE="$(command -v cmake || true)"
NINJA="$(command -v ninja || true)"
if [[ -z "$CMAKE" && -n "$SDK_DIR" ]]; then
  CMAKE="$(ls -1d "$SDK_DIR"/cmake/*/bin/cmake 2>/dev/null | tail -1 || true)"
fi
if [[ -z "$NINJA" && -n "$CMAKE" ]]; then
  NINJA="$(dirname "$CMAKE")/ninja"
fi
if [[ ! -x "$CMAKE" ]]; then
  echo "cmake not found; install CMake or the Android SDK cmake package" >&2
  exit 1
fi

mkdir -p "$BUILD" "$DEST_DIR" "$INCLUDE_DST"

GEN_ARGS=(
  "$CMAKE" -S "$SL" -B "$BUILD"
  -DCMAKE_BUILD_TYPE=Release
  -DSWEETLINE_BUILD_TESTS=OFF
  -DSWEETLINE_BUILD_SHARED=ON
  -DSWEETLINE_BUILD_STATIC=OFF
  -DSWEETLINE_BUILD_WASM_EMBIND=OFF
  -DSWEETLINE_BUILD_ANDROID_JNI=OFF
  -DSWEETLINE_BUILD_APPLE_FRAMEWORK=OFF
)
if [[ -n "${SWEETLINE_OSX_ARCH:-}" ]]; then
  GEN_ARGS+=("-DCMAKE_OSX_ARCHITECTURES=$SWEETLINE_OSX_ARCH")
fi
if [[ "$OS" == Darwin ]]; then
  # shellcheck source=../../scripts/macos-cmake-sysroot.sh
  source "$(cd "$(dirname "$0")/../.." && pwd)/scripts/macos-cmake-sysroot.sh"
  macos_cmake_apply_sysroot "$BUILD"
fi
if [[ -x "${NINJA:-}" ]]; then
  GEN_ARGS+=(-G Ninja "-DCMAKE_MAKE_PROGRAM=$NINJA")
fi

if [[ "$MODE" == "configure" || "$MODE" == "all" || ! -f "$BUILD/CMakeCache.txt" || "$OS" == Darwin ]]; then
  "${GEN_ARGS[@]}"
fi
if [[ "$MODE" != "configure" ]]; then
  "$CMAKE" --build "$BUILD" --target sweetline --config Release
fi

MINGW_PREFIX=""
if [[ "$OS" == MINGW* || "$OS" == MSYS* || "$OS" == CYGWIN* ]]; then
  MINGW_PREFIX="lib"
fi

CORE=""
for candidate in \
  "$BUILD/lib/$LIB_NAME" \
  "$BUILD/lib/Release/$LIB_NAME" \
  "$BUILD/bin/$LIB_NAME" \
  "$BUILD/bin/Release/$LIB_NAME" \
  "$BUILD/lib/${MINGW_PREFIX}$LIB_NAME" \
  "$BUILD/bin/${MINGW_PREFIX}$LIB_NAME"
do
  if [[ -f "$candidate" ]]; then
    CORE="$candidate"
    break
  fi
done
if [[ -z "$CORE" ]]; then
  CORE="$(find "$BUILD" -type f -name "$LIB_NAME" -o -name "${MINGW_PREFIX}$LIB_NAME" 2>/dev/null | head -n 1 || true)"
fi
if [[ -z "$CORE" || ! -f "$CORE" ]]; then
  echo "SweetLine core library $LIB_NAME not found under $BUILD" >&2
  exit 1
fi

mkdir -p "$DEST_DIR"
if [[ "$(basename "$CORE")" != "$LIB_NAME" ]]; then
  cp -f "$CORE" "$DEST_DIR/$LIB_NAME"
else
  cp -f "$CORE" "$DEST_DIR/"
fi
mkdir -p "$INCLUDE_DST"
cp -R "$INCLUDE_SRC/." "$INCLUDE_DST/"
echo "Installed $CORE -> $DEST_DIR/$LIB_NAME"
echo "Installed headers -> $INCLUDE_DST"
