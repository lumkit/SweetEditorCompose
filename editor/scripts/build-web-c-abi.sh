#!/usr/bin/env bash
# Build SweetEditor WebAssembly C ABI (sweeteditor_c_abi.js/.wasm) into editor/natives/web/.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SE="${SWEETEDITOR_HOME:-}"
if [[ -z "$SE" ]]; then
  echo "SWEETEDITOR_HOME is required" >&2
  exit 1
fi

EMCMAKE="$(command -v emcmake || true)"
if [[ -z "$EMCMAKE" && -n "${EMSDK:-}" && -x "$EMSDK/upstream/emscripten/emcmake" ]]; then
  EMCMAKE="$EMSDK/upstream/emscripten/emcmake"
fi
if [[ -z "$EMCMAKE" ]]; then
  echo "emcmake not found. Activate the Emscripten SDK first." >&2
  exit 1
fi

CMAKE_GEN=()
if command -v ninja >/dev/null 2>&1; then
  CMAKE_GEN=(-G Ninja)
fi

BUILD="$SE/build/compose-wasm"
DEST="$ROOT/natives/web"
INCLUDE_SRC="$SE/include/sweeteditor"
INCLUDE_DST="$ROOT/natives/include/sweeteditor"
mkdir -p "$BUILD" "$DEST" "$INCLUDE_DST"

"$EMCMAKE" cmake "$SE" -B "$BUILD" "${CMAKE_GEN[@]}" \
  -DCMAKE_BUILD_TYPE=Release \
  -DSWEETEDITOR_BUILD_TESTS=OFF \
  -DSWEETEDITOR_BUILD_STATIC=OFF \
  -DSWEETEDITOR_BUILD_ANDROID_JNI=OFF

cmake --build "$BUILD" --target sweeteditor_wasm_c_abi --config Release

JS="$(find "$BUILD" -type f -name "sweeteditor_c_abi.js" | head -n 1 || true)"
WASM="$(find "$BUILD" -type f -name "sweeteditor_c_abi.wasm" | head -n 1 || true)"
if [[ -z "$JS" || -z "$WASM" ]]; then
  echo "sweeteditor_c_abi.{js,wasm} not found under $BUILD" >&2
  exit 1
fi
cp -f "$JS" "$DEST/sweeteditor_c_abi.js"
cp -f "$WASM" "$DEST/sweeteditor_c_abi.wasm"
cp -R "$INCLUDE_SRC/." "$INCLUDE_DST/"
echo "Installed web C ABI -> $DEST"
