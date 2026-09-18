#!/usr/bin/env bash
# Resolve the macOS SDK on this machine (Xcode or CLT) and keep CMake in sync.
# Source from editor/highlight Darwin CMake wrappers; do not execute directly.
#
# Uses xcrun every run so the path is never baked to another Mac or an old
# Xcode (MacOSX26.5.sdk vs MacOSX27.sdk). Prefer the unversioned MacOSX.sdk
# symlink when present. Sets SDKROOT/CMAKE_OSX_SYSROOT and appends
# -DCMAKE_OSX_SYSROOT to GEN_ARGS. Wipes CMakeCache when the cached sysroot
# is missing or points at a different SDK. Sets MACOS_CMAKE_SYSROOT_CHANGED=1
# so callers reconfigure even if Gradle skipped the configure task.

macos_sdk_realpath() {
  local p="${1:-}"
  [[ -n "$p" && -d "$p" ]] || return 1
  (cd "$p" && pwd -P)
}

macos_resolve_sdk() {
  if ! command -v xcrun >/dev/null 2>&1; then
    echo "xcrun not found; install Xcode or Command Line Tools" >&2
    return 1
  fi
  local sdk
  sdk="$(xcrun --sdk macosx --show-sdk-path 2>/dev/null || true)"
  if [[ -z "$sdk" || ! -d "$sdk" ]]; then
    echo "Could not resolve macOS SDK (xcrun --sdk macosx --show-sdk-path)" >&2
    return 1
  fi
  local versionless
  versionless="$(dirname "$sdk")/MacOSX.sdk"
  if [[ -d "$versionless" ]]; then
    sdk="$versionless"
  fi
  printf '%s' "$sdk"
}

# Call after GEN_ARGS is created. Argument: CMake build directory.
macos_cmake_apply_sysroot() {
  local build_dir="${1:?cmake build dir required}"
  MACOS_CMAKE_SYSROOT_CHANGED=0
  local sdk
  sdk="$(macos_resolve_sdk)"

  # A stale CMAKE_OSX_SYSROOT in the environment is ignored by CMake 3.22
  # (missing directory) and then the compiler has no stdio.h.
  if [[ -n "${CMAKE_OSX_SYSROOT:-}" && ! -d "${CMAKE_OSX_SYSROOT}" ]]; then
    unset CMAKE_OSX_SYSROOT
  fi
  export SDKROOT="$sdk"
  export CMAKE_OSX_SYSROOT="$sdk"
  GEN_ARGS+=("-DCMAKE_OSX_SYSROOT=$sdk")

  if [[ ! -f "$build_dir/CMakeCache.txt" ]]; then
    return 0
  fi
  local cached cached_real current_real
  cached="$(sed -n 's/^CMAKE_OSX_SYSROOT:[^=]*=//p' "$build_dir/CMakeCache.txt" | head -1)"
  [[ -n "$cached" ]] || return 0
  current_real="$(macos_sdk_realpath "$sdk" || true)"
  if [[ ! -d "$cached" ]]; then
    echo "CMake cache sysroot is gone ($cached); reconfiguring with $sdk"
    rm -f "$build_dir/CMakeCache.txt"
    MACOS_CMAKE_SYSROOT_CHANGED=1
    return 0
  fi
  cached_real="$(macos_sdk_realpath "$cached" || true)"
  if [[ -n "$current_real" && -n "$cached_real" && "$current_real" != "$cached_real" ]]; then
    echo "CMake cache sysroot changed ($cached_real -> $current_real); reconfiguring"
    rm -f "$build_dir/CMakeCache.txt"
    MACOS_CMAKE_SYSROOT_CHANGED=1
  fi
}
