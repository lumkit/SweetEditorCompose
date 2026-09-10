#!/usr/bin/env bash
set -euo pipefail

MODE="${1:-build}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SRC="$ROOT/src/jni"
BUILD="$ROOT/build/jni/desktop"
INCLUDE="$ROOT/natives/include"
CORE="$ROOT/natives/desktop/macos-aarch64/libsweeteditor.dylib"
OS="$(uname -s)"
ARCH="$(uname -m)"
if [[ "$OS" == "Darwin" && "$ARCH" == "x86_64" ]]; then
  CORE="$ROOT/natives/desktop/macos-x86_64/libsweeteditor.dylib"
elif [[ "$OS" == "Linux" && "$ARCH" == "x86_64" ]]; then
  CORE="$ROOT/natives/desktop/linux-x86_64/libsweeteditor.so"
elif [[ "$OS" == "Linux" && "$ARCH" == "aarch64" ]]; then
  CORE="$ROOT/natives/desktop/linux-aarch64/libsweeteditor.so"
fi

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
  echo "SweetEditor core library missing: $CORE" >&2
  exit 1
fi

export JAVA_HOME="${JAVA_HOME:-$(/usr/libexec/java_home 2>/dev/null || true)}"
mkdir -p "$BUILD"
GEN_ARGS=("$CMAKE" -S "$SRC" -B "$BUILD" -DCMAKE_BUILD_TYPE=Release
  "-DSWEETEDITOR_INCLUDE_DIR=$INCLUDE"
  "-DSWEETEDITOR_CORE_LIB=$CORE")
if [[ -x "${NINJA:-}" ]]; then
  GEN_ARGS+=(-G Ninja "-DCMAKE_MAKE_PROGRAM=$NINJA")
fi

if [[ "$MODE" == "configure" || "$MODE" == "all" ]]; then
  "${GEN_ARGS[@]}"
fi
if [[ "$MODE" != "configure" ]]; then
  "$CMAKE" --build "$BUILD" --config Release
fi
