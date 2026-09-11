#!/usr/bin/env bash
# Build SweetEditor as a static archive for one iOS ABI and install
# editor/natives/ios/<abi>/libsweeteditor.a (publish layout).
set -euo pipefail

TARGET="${1:-}"
case "$TARGET" in
  simulator-arm64)
    SDK=iphonesimulator
    ARCH=arm64
    ;;
  arm64)
    SDK=iphoneos
    ARCH=arm64
    ;;
  *)
    echo "usage: $0 simulator-arm64|arm64" >&2
    exit 1
    ;;
esac

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SE="${SWEETEDITOR_HOME:-}"
if [[ -z "$SE" ]]; then
  echo "SWEETEDITOR_HOME is required" >&2
  exit 1
fi

DEST_DIR="$ROOT/natives/ios/$TARGET"
INCLUDE_SRC="$SE/include/sweeteditor"
INCLUDE_DST="$ROOT/natives/include/sweeteditor"
BUILD="$SE/build/compose-ios-$TARGET"
DEPLOY="${SWEETEDITOR_IOS_DEPLOYMENT_TARGET:-14.0}"

SDK_DIR="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"
if [[ -z "$SDK_DIR" && -f "$ROOT/../local.properties" ]]; then
  SDK_DIR="$(grep -E '^sdk.dir=' "$ROOT/../local.properties" | cut -d= -f2- | tr -d '\r')"
fi

CMAKE="$(command -v cmake || true)"
NINJA="$(command -v ninja || true)"
if [[ ! -x "${CMAKE:-}" ]]; then
  for candidate in \
    /opt/homebrew/bin/cmake \
    /usr/local/bin/cmake
  do
    if [[ -x "$candidate" ]]; then
      CMAKE="$candidate"
      break
    fi
  done
fi
if [[ ! -x "${CMAKE:-}" && -n "$SDK_DIR" ]]; then
  CMAKE="$(ls -1d "$SDK_DIR"/cmake/*/bin/cmake 2>/dev/null | tail -1 || true)"
fi
if [[ ! -x "${NINJA:-}" && -n "${CMAKE:-}" ]]; then
  NINJA="$(dirname "$CMAKE")/ninja"
fi
if [[ ! -x "${CMAKE:-}" ]]; then
  echo "cmake not found; install CMake or the Android SDK cmake package" >&2
  exit 1
fi

mkdir -p "$BUILD" "$DEST_DIR" "$INCLUDE_DST"

GEN_ARGS=(
  "$CMAKE" -S "$SE" -B "$BUILD"
  -DCMAKE_SYSTEM_NAME=iOS
  -DCMAKE_OSX_SYSROOT="$SDK"
  -DCMAKE_OSX_ARCHITECTURES="$ARCH"
  -DCMAKE_OSX_DEPLOYMENT_TARGET="$DEPLOY"
  -DCMAKE_BUILD_TYPE=Release
  -DCMAKE_CXX_STANDARD=17
  -DSWEETEDITOR_BUILD_SHARED=OFF
  -DSWEETEDITOR_BUILD_STATIC=ON
  -DSWEETEDITOR_BUILD_TESTS=OFF
  -DSWEETEDITOR_BUILD_APPLE_FRAMEWORK=OFF
  -DSWEETEDITOR_BUILD_WASM_C_ABI=OFF
  -DSWEETEDITOR_BUILD_WASM_EMBIND=OFF
  -DSWEETEDITOR_BUILD_ANDROID_JNI=OFF
)
if [[ -x "${NINJA:-}" ]]; then
  GEN_ARGS+=(-G Ninja "-DCMAKE_MAKE_PROGRAM=$NINJA")
fi

"${GEN_ARGS[@]}"
"$CMAKE" --build "$BUILD" --target sweeteditor_static --config Release

ARCHIVE=""
for candidate in \
  "$BUILD/lib/libsweeteditor_static.a" \
  "$BUILD/lib/Release/libsweeteditor_static.a" \
  "$BUILD/lib/Release-iphoneos/libsweeteditor_static.a" \
  "$BUILD/lib/Release-iphonesimulator/libsweeteditor_static.a"
do
  if [[ -f "$candidate" ]]; then
    ARCHIVE="$candidate"
    break
  fi
done
if [[ -z "$ARCHIVE" ]]; then
  ARCHIVE="$(find "$BUILD" -type f -name "libsweeteditor_static.a" | head -n 1 || true)"
fi
if [[ -z "$ARCHIVE" || ! -f "$ARCHIVE" ]]; then
  echo "libsweeteditor_static.a not found under $BUILD" >&2
  exit 1
fi

cp -f "$ARCHIVE" "$DEST_DIR/libsweeteditor.a"
mkdir -p "$INCLUDE_DST"
cp -R "$INCLUDE_SRC/." "$INCLUDE_DST/"
echo "Installed $ARCHIVE -> $DEST_DIR/libsweeteditor.a"
