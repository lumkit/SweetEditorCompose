#!/usr/bin/env bash
# Build SweetLine WebAssembly C ABI (sweetline_c_abi.js/.wasm) into highlight/natives/web/.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SL="${SWEETLINE_HOME:-}"
if [[ -z "$SL" ]]; then
  echo "SWEETLINE_HOME is required" >&2
  exit 1
fi

if [[ -z "${EMSDK:-}" && -f "$HOME/emsdk/emsdk_env.sh" ]]; then
  EMSDK="$HOME/emsdk"
fi
if [[ -n "${EMSDK:-}" && -f "$EMSDK/emsdk_env.sh" ]]; then
  # shellcheck disable=SC1091
  source "$EMSDK/emsdk_env.sh"
fi

EMCMAKE="$(command -v emcmake || true)"
if [[ -z "$EMCMAKE" && -n "${EMSDK:-}" && -x "$EMSDK/upstream/emscripten/emcmake" ]]; then
  EMCMAKE="$EMSDK/upstream/emscripten/emcmake"
fi
if [[ -z "$EMCMAKE" ]]; then
  echo "emcmake not found. Activate the Emscripten SDK first." >&2
  exit 1
fi

SDK_DIR="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"
if [[ -z "$SDK_DIR" && -f "$ROOT/../local.properties" ]]; then
  SDK_DIR="$(grep -E '^sdk.dir=' "$ROOT/../local.properties" | cut -d= -f2- | tr -d '\r')"
fi
if [[ -n "$SDK_DIR" ]]; then
  CMAKE_BIN="$(ls -1d "$SDK_DIR"/cmake/*/bin 2>/dev/null | tail -1 || true)"
  if [[ -n "${CMAKE_BIN:-}" ]]; then
    export PATH="$CMAKE_BIN:$PATH"
  fi
fi

CMAKE_GEN=()
if command -v ninja >/dev/null 2>&1; then
  CMAKE_GEN=(-G Ninja)
elif [[ -n "$SDK_DIR" ]]; then
  NINJA="$(ls -1d "$SDK_DIR"/cmake/*/bin/ninja 2>/dev/null | tail -1 || true)"
  if [[ -x "${NINJA:-}" ]]; then
    CMAKE_GEN=(-G Ninja "-DCMAKE_MAKE_PROGRAM=$NINJA")
  fi
fi

BUILD="${SWEETLINE_WASM_BUILD:-$ROOT/build/web-c-abi}"
DEST="$ROOT/natives/web"
INCLUDE_SRC="$SL/include/sweetline"
INCLUDE_DST="$ROOT/natives/include/sweetline"
mkdir -p "$BUILD" "$DEST" "$INCLUDE_DST"

"$EMCMAKE" cmake -S "$ROOT/src/web-c-abi" -B "$BUILD" "${CMAKE_GEN[@]}" \
  -DCMAKE_BUILD_TYPE=Release \
  "-DSWEETLINE_HOME=$SL"

cmake --build "$BUILD" --target sweetline_c_abi --config Release

JS="$BUILD/sweetline_c_abi.js"
WASM="$BUILD/sweetline_c_abi.wasm"
if [[ ! -f "$JS" || ! -f "$WASM" ]]; then
  echo "sweetline_c_abi.{js,wasm} not found under $BUILD" >&2
  exit 1
fi
cp -f "$JS" "$DEST/sweetline_c_abi.js"
cp -f "$WASM" "$DEST/sweetline_c_abi.wasm"
cp -R "$INCLUDE_SRC/." "$INCLUDE_DST/"
echo "Installed web C ABI -> $DEST"
