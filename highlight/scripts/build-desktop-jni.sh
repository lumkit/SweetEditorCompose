#!/usr/bin/env bash
# Configure/build libsweetline_compose for the current desktop host
# and install it next to Core under highlight/natives/desktop/<os>-<arch>/.
set -euo pipefail

MODE="${1:-build}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SRC="$ROOT/src/jni"
BUILD="${SWEETLINE_JNI_BUILD:-$ROOT/build/jni/desktop}"
INCLUDE="$ROOT/natives/include"

OS="$(uname -s)"
ARCH="$(uname -m)"
DEST_FOLDER=""
CORE=""
JNI_NAME=""

case "$OS" in
  Darwin)
    DEST_FOLDER="macos-aarch64"
    JNI_NAME="libsweetline_compose.dylib"
    CORE="$ROOT/natives/desktop/macos-aarch64/libsweetline.dylib"
    if [[ "${SWEETLINE_OSX_ARCH:-}" == "x86_64" || "$ARCH" == "x86_64" ]]; then
      DEST_FOLDER="macos-x86_64"
      CORE="$ROOT/natives/desktop/macos-x86_64/libsweetline.dylib"
    fi
    ;;
  Linux)
    JNI_NAME="libsweetline_compose.so"
    if [[ "$ARCH" == "aarch64" || "$ARCH" == "arm64" ]]; then
      DEST_FOLDER="linux-aarch64"
      CORE="$ROOT/natives/desktop/linux-aarch64/libsweetline.so"
    else
      DEST_FOLDER="linux-x86_64"
      CORE="$ROOT/natives/desktop/linux-x86_64/libsweetline.so"
    fi
    ;;
  MINGW*|MSYS*|CYGWIN*)
    DEST_FOLDER="windows-x86_64"
    JNI_NAME="sweetline_compose.dll"
    CORE="$ROOT/natives/desktop/windows-x86_64/sweetline.dll"
    ;;
  *)
    echo "Unsupported host OS: $OS" >&2
    exit 1
    ;;
esac

if [[ -n "${SWEETLINE_CORE_LIB:-}" ]]; then
  CORE="$SWEETLINE_CORE_LIB"
fi
if [[ -n "${SWEETLINE_DESKTOP_FOLDER:-}" ]]; then
  DEST_FOLDER="$SWEETLINE_DESKTOP_FOLDER"
fi
INSTALL_DIR="${SWEETLINE_JNI_INSTALL:-$ROOT/natives/desktop/$DEST_FOLDER}"

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
if [[ ! -f "$CORE" ]]; then
  echo "SweetLine core library missing: $CORE" >&2
  exit 1
fi

if [[ -z "${JAVA_HOME:-}" ]]; then
  if [[ -x /usr/libexec/java_home ]]; then
    export JAVA_HOME="$(/usr/libexec/java_home)"
  fi
fi

mkdir -p "$BUILD"
GEN_ARGS=("$CMAKE" -S "$SRC" -B "$BUILD" -DCMAKE_BUILD_TYPE=Release
  "-DSWEETLINE_INCLUDE_DIR=$INCLUDE"
  "-DSWEETLINE_CORE_LIB=$CORE")
if [[ -n "${SWEETLINE_OSX_ARCH:-}" ]]; then
  GEN_ARGS+=("-DCMAKE_OSX_ARCHITECTURES=$SWEETLINE_OSX_ARCH")
fi
if [[ -x "${NINJA:-}" ]]; then
  GEN_ARGS+=(-G Ninja "-DCMAKE_MAKE_PROGRAM=$NINJA")
fi

if [[ "$MODE" == "configure" || "$MODE" == "all" ]]; then
  "${GEN_ARGS[@]}"
fi
if [[ "$MODE" != "configure" ]]; then
  "$CMAKE" --build "$BUILD" --config Release
  MINGW_PREFIX=""
  if [[ "$OS" == MINGW* || "$OS" == MSYS* || "$OS" == CYGWIN* ]]; then
    MINGW_PREFIX="lib"
  fi
  BUILT=""
  for candidate in \
    "$BUILD/$JNI_NAME" \
    "$BUILD/Release/$JNI_NAME" \
    "$BUILD/lib/$JNI_NAME" \
    "$BUILD/lib/Release/$JNI_NAME" \
    "$BUILD/${MINGW_PREFIX}$JNI_NAME" \
    "$BUILD/lib/${MINGW_PREFIX}$JNI_NAME"
  do
    if [[ -f "$candidate" ]]; then
      BUILT="$candidate"
      break
    fi
  done
  if [[ -z "$BUILT" ]]; then
    BUILT="$(find "$BUILD" -type f \( -name "$JNI_NAME" -o -name "${MINGW_PREFIX}$JNI_NAME" \) 2>/dev/null | head -n 1 || true)"
  fi
  if [[ -z "$BUILT" || ! -f "$BUILT" ]]; then
    echo "Compose JNI $JNI_NAME not found under $BUILD" >&2
    exit 1
  fi
  mkdir -p "$INSTALL_DIR"
  cp -f "$BUILT" "$INSTALL_DIR/$JNI_NAME"
  echo "Installed $BUILT -> $INSTALL_DIR/$JNI_NAME"
fi
