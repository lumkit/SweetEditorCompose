#!/usr/bin/env bash
# Build the SweetEditor C++ core for the current desktop host (README cmake flow)
# and copy the matching headers + shared library into editor/natives/.
set -euo pipefail

MODE="${1:-build}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SE="${SWEETEDITOR_HOME:-}"
if [[ -z "$SE" ]]; then
  echo "SWEETEDITOR_HOME is required" >&2
  exit 1
fi

OS="$(uname -s)"
ARCH="$(uname -m)"
DEST_DIR=""
LIB_NAME=""
case "$OS" in
  Darwin)
    LIB_NAME="libsweeteditor.dylib"
    if [[ "${SWEETEDITOR_OSX_ARCH:-}" == "x86_64" || "$ARCH" == "x86_64" ]]; then
      DEST_DIR="$ROOT/natives/desktop/macos-x86_64"
    else
      DEST_DIR="$ROOT/natives/desktop/macos-aarch64"
    fi
    ;;
  Linux)
    LIB_NAME="libsweeteditor.so"
    if [[ "$ARCH" == "aarch64" || "$ARCH" == "arm64" ]]; then
      DEST_DIR="$ROOT/natives/desktop/linux-aarch64"
    else
      DEST_DIR="$ROOT/natives/desktop/linux-x86_64"
    fi
    ;;
  MINGW*|MSYS*|CYGWIN*)
    LIB_NAME="sweeteditor.dll"
    DEST_DIR="$ROOT/natives/desktop/windows-x86_64"
    ;;
  *)
    echo "Unsupported host OS: $OS" >&2
    exit 1
    ;;
esac

BUILD="$SE/build/compose-host"
if [[ -n "${SWEETEDITOR_OSX_ARCH:-}" ]]; then
  BUILD="$SE/build/compose-host-$SWEETEDITOR_OSX_ARCH"
fi
INCLUDE_SRC="$SE/include/sweeteditor"
INCLUDE_DST="$ROOT/natives/include/sweeteditor"

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
  "$CMAKE" -S "$SE" -B "$BUILD"
  -DCMAKE_BUILD_TYPE=Release
  -DSWEETEDITOR_BUILD_TESTS=OFF
  -DSWEETEDITOR_BUILD_SHARED=ON
  -DSWEETEDITOR_BUILD_WASM_C_ABI=OFF
  -DSWEETEDITOR_BUILD_WASM_EMBIND=OFF
  -DSWEETEDITOR_BUILD_ANDROID_JNI=OFF
)
if [[ -n "${SWEETEDITOR_OSX_ARCH:-}" ]]; then
  GEN_ARGS+=("-DCMAKE_OSX_ARCHITECTURES=$SWEETEDITOR_OSX_ARCH")
fi
if [[ -x "${NINJA:-}" ]]; then
  GEN_ARGS+=(-G Ninja "-DCMAKE_MAKE_PROGRAM=$NINJA")
fi

if [[ "$MODE" == "configure" || "$MODE" == "all" || ! -f "$BUILD/CMakeCache.txt" ]]; then
  "${GEN_ARGS[@]}"
fi
if [[ "$MODE" != "configure" ]]; then
  "$CMAKE" --build "$BUILD" --target sweeteditor --config Release
fi

CORE=""
for candidate in \
  "$BUILD/lib/$LIB_NAME" \
  "$BUILD/lib/Release/$LIB_NAME" \
  "$BUILD/bin/$LIB_NAME" \
  "$BUILD/bin/Release/$LIB_NAME"
do
  if [[ -f "$candidate" ]]; then
    CORE="$candidate"
    break
  fi
done
if [[ -z "$CORE" ]]; then
  CORE="$(find "$BUILD" -type f -name "$LIB_NAME" | head -n 1 || true)"
fi
if [[ -z "$CORE" || ! -f "$CORE" ]]; then
  echo "SweetEditor core library $LIB_NAME not found under $BUILD" >&2
  exit 1
fi

cp -f "$CORE" "$DEST_DIR/"
mkdir -p "$INCLUDE_DST"
cp -R "$INCLUDE_SRC/." "$INCLUDE_DST/"
echo "Installed $CORE -> $DEST_DIR/$LIB_NAME"
echo "Installed headers -> $INCLUDE_DST"
