#!/usr/bin/env bash
# Build SweetLine as a static archive for one iOS ABI and install
# highlight/natives/ios/<abi>/libsweetline.a (publish layout).
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
SL="${SWEETLINE_HOME:-}"
if [[ -z "$SL" ]]; then
  echo "SWEETLINE_HOME is required" >&2
  exit 1
fi

DEST_DIR="$ROOT/natives/ios/$TARGET"
INCLUDE_SRC="$SL/include/sweetline"
INCLUDE_DST="$ROOT/natives/include/sweetline"
BUILD="$SL/build/compose-ios-$TARGET"
DEPLOY="${SWEETLINE_IOS_DEPLOYMENT_TARGET:-14.0}"

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

SDK_PATH="$(xcrun --sdk "$SDK" --show-sdk-path)"
if [[ -z "$SDK_PATH" || ! -d "$SDK_PATH" ]]; then
  echo "xcrun could not resolve SDK path for $SDK" >&2
  exit 1
fi
export SDKROOT="$SDK_PATH"
echo "Using $SDK sysroot: $SDK_PATH"

ONIG_CMAKE="$SL/3dparty/oniguruma/sweetline_3p.cmake"
if [[ -f "$ONIG_CMAKE" ]] && ! grep -q '\[Ss\]imulator' "$ONIG_CMAKE"; then
  python3 - "$ONIG_CMAKE" <<'PY'
from pathlib import Path
import sys
path = Path(sys.argv[1])
text = path.read_text()
old = 'CMAKE_OSX_SYSROOT MATCHES ".*simulator.*"'
new = 'CMAKE_OSX_SYSROOT MATCHES ".*[Ss]imulator.*"'
if old in text:
    path.write_text(text.replace(old, new, 1))
    print(f"Patched {path} for case-insensitive simulator sysroot")
PY
fi

mkdir -p "$BUILD" "$DEST_DIR" "$INCLUDE_DST"
GEN_ARGS=(
  "$CMAKE" -S "$SL" -B "$BUILD"
  -DCMAKE_SYSTEM_NAME=iOS
  -DCMAKE_OSX_SYSROOT="$SDK"
  -DCMAKE_OSX_ARCHITECTURES="$ARCH"
  -DCMAKE_OSX_DEPLOYMENT_TARGET="$DEPLOY"
  -DCMAKE_BUILD_TYPE=Release
  -DCMAKE_CXX_STANDARD=17
  -DSWEETLINE_BUILD_SHARED=OFF
  -DSWEETLINE_BUILD_STATIC=ON
  -DSWEETLINE_BUILD_TESTS=OFF
  -DSWEETLINE_BUILD_APPLE_FRAMEWORK=OFF
  -DSWEETLINE_BUILD_WASM_EMBIND=OFF
  -DSWEETLINE_BUILD_ANDROID_JNI=OFF
)
if [[ -x "${NINJA:-}" ]]; then
  GEN_ARGS+=(-G Ninja "-DCMAKE_MAKE_PROGRAM=$NINJA")
fi

"${GEN_ARGS[@]}" 2>&1 | tee "$BUILD/configure.log"
if [[ "$SDK" == "iphonesimulator" ]] && ! grep -q "lib/ios/simulator-${ARCH}" "$BUILD/configure.log"; then
  echo "Oniguruma did not select lib/ios/simulator-${ARCH} (see $BUILD/configure.log)" >&2
  grep -E "Add third-party library|onig" "$BUILD/configure.log" || true
  exit 1
fi
"$CMAKE" --build "$BUILD" --target sweetline_static --config Release

ARCHIVE=""
for candidate in \
  "$BUILD/lib/libsweetline_static.a" \
  "$BUILD/lib/Release/libsweetline_static.a" \
  "$BUILD/lib/Release-iphoneos/libsweetline_static.a" \
  "$BUILD/lib/Release-iphonesimulator/libsweetline_static.a"
do
  if [[ -f "$candidate" ]]; then
    ARCHIVE="$candidate"
    break
  fi
done
if [[ -z "$ARCHIVE" ]]; then
  ARCHIVE="$(find "$BUILD" -type f -name "libsweetline_static.a" | head -n 1 || true)"
fi
if [[ -z "$ARCHIVE" || ! -f "$ARCHIVE" ]]; then
  echo "libsweetline_static.a not found under $BUILD" >&2
  exit 1
fi

cp -f "$ARCHIVE" "$DEST_DIR/libsweetline.a"
mkdir -p "$INCLUDE_DST"
cp -R "$INCLUDE_SRC/." "$INCLUDE_DST/"

EXPECT_PLATFORM="IOS"
if [[ "$SDK" == "iphonesimulator" ]]; then
  EXPECT_PLATFORM="IOSSIMULATOR"
fi
TMP="$(mktemp -d)"
(cd "$TMP" && ar -x "$DEST_DIR/libsweetline.a")
OBJ="$(find "$TMP" -name '*.o' | head -n 1 || true)"
if [[ -z "$OBJ" || ! -f "$OBJ" ]]; then
  echo "Could not extract an object from $DEST_DIR/libsweetline.a to verify platform" >&2
  rm -rf "$TMP"
  exit 1
fi
PLATFORM="$(xcrun vtool -show-build "$OBJ" 2>/dev/null | awk '/platform/{print $2; exit}')"
rm -rf "$TMP"
if [[ "$PLATFORM" != "$EXPECT_PLATFORM" ]]; then
  echo "iOS archive platform mismatch: $DEST_DIR/libsweetline.a is $PLATFORM, expected $EXPECT_PLATFORM" >&2
  echo "A device slice in the simulator klib causes: linking object built for iOS while targeting iOS-simulator." >&2
  exit 1
fi

"$ROOT/scripts/merge-ios-iconv-autolink.sh" "$TARGET"
echo "Installed $ARCHIVE -> $DEST_DIR/libsweetline.a ($PLATFORM)"
