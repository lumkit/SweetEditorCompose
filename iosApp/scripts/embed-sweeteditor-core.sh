#!/bin/sh
# Embed libsweeteditor.dylib into the app. Must run even when the IDE skips Gradle
# (OVERRIDE_KOTLIN_BUILD_IDE_SUPPORTED=YES).
set -eu

ARCH_DIR=simulator-arm64
if [ "${PLATFORM_NAME:-}" = "iphoneos" ]; then
  ARCH_DIR=arm64
fi

SRC="$SRCROOT/../editor/natives/ios/$ARCH_DIR/libsweeteditor.dylib"
if [ ! -f "$SRC" ]; then
  echo "error: missing SweetEditor core: $SRC" >&2
  exit 1
fi

DEST_DIR="${TARGET_BUILD_DIR}/${FRAMEWORKS_FOLDER_PATH}"
mkdir -p "$DEST_DIR"
cp -f "$SRC" "$DEST_DIR/libsweeteditor.dylib"
chmod +x "$DEST_DIR/libsweeteditor.dylib"
echo "Embedded $SRC -> $DEST_DIR/libsweeteditor.dylib"

if [ "${PLATFORM_NAME:-}" = "iphoneos" ] && [ -n "${EXPANDED_CODE_SIGN_IDENTITY:-}" ] && [ "$EXPANDED_CODE_SIGN_IDENTITY" != "-" ]; then
  codesign --force --sign "$EXPANDED_CODE_SIGN_IDENTITY" --timestamp=none \
    "$DEST_DIR/libsweeteditor.dylib"
fi
