#!/usr/bin/env bash
# Merge an LC_LINKER_OPTION object into libsweetline.a so Xcode auto-links iconv.
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
ARCHIVE="$ROOT/natives/ios/$TARGET/libsweetline.a"
SRC="$ROOT/src/nativeInterop/iconv_autolink.c"
if [[ ! -f "$ARCHIVE" ]]; then
  echo "missing $ARCHIVE" >&2
  exit 1
fi

if nm -g "$ARCHIVE" 2>/dev/null | grep -q 'sweetline_iconv_autolink'; then
  echo "iconv autolink already in $ARCHIVE"
  exit 0
fi

SDK_PATH="$(xcrun --sdk "$SDK" --show-sdk-path)"
DEPLOY="${SWEETLINE_IOS_DEPLOYMENT_TARGET:-14.0}"
MINVER_FLAG="-miphoneos-version-min=$DEPLOY"
if [[ "$SDK" == "iphonesimulator" ]]; then
  MINVER_FLAG="-mios-simulator-version-min=$DEPLOY"
fi
OBJ="$(mktemp -t sweetline-iconv-autolink).o"
xcrun --sdk "$SDK" clang -c "$SRC" -o "$OBJ" \
  -arch "$ARCH" \
  -isysroot "$SDK_PATH" \
  "$MINVER_FLAG"

NEW="$(mktemp -t sweetline-with-iconv).a"
xcrun libtool -static -o "$NEW" "$ARCHIVE" "$OBJ"
rm -f "$OBJ"
mv -f "$NEW" "$ARCHIVE"
echo "Merged iconv autolink into $ARCHIVE"
