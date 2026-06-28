#!/usr/bin/env bash
# Downloads MoltenVK.xcframework + Vulkan headers and vendoring them into
# the iOS app. Required for HW-rendered cores (PSP/GameCube/Wii/Dreamcast).
#
# Idempotent — safe to re-run. Skips download if files already present.
#
# After running, the layout is:
#   iosApp/Frameworks/MoltenVK.xcframework       (linked + embedded by xcodebuild)
#   core-libretro/src/nativeInterop/cinterop/vulkan/*.h  (used by cinterop)
set -euo pipefail

cd "$(dirname "$0")/.."

MOLTENVK_VERSION="1.4.1"
TARBALL="/tmp/MoltenVK-all-${MOLTENVK_VERSION}.tar"
EXTRACT="/tmp/MoltenVK-all-${MOLTENVK_VERSION}"
FRAMEWORKS_DIR="iosApp/Frameworks"
HEADERS_DIR="core-libretro/src/nativeInterop/cinterop/vulkan"

if [ -d "${FRAMEWORKS_DIR}/MoltenVK.xcframework" ] && [ "$(ls ${HEADERS_DIR}/*.h 2>/dev/null | wc -l)" -gt 0 ]; then
  echo "MoltenVK already vendored. Skipping."
  exit 0
fi

echo "[1/4] Downloading MoltenVK v${MOLTENVK_VERSION}..."
if [ ! -f "$TARBALL" ]; then
  curl -fsSL "https://github.com/KhronosGroup/MoltenVK/releases/download/v${MOLTENVK_VERSION}/MoltenVK-all.tar" -o "$TARBALL"
fi

echo "[2/4] Extracting..."
rm -rf "$EXTRACT"
mkdir -p "$EXTRACT"
tar -xf "$TARBALL" -C "$EXTRACT"

echo "[3/4] Copying xcframework to ${FRAMEWORKS_DIR}/..."
mkdir -p "$FRAMEWORKS_DIR"
rm -rf "${FRAMEWORKS_DIR}/MoltenVK.xcframework"
cp -R "${EXTRACT}/MoltenVK/MoltenVK/dynamic/MoltenVK.xcframework" "${FRAMEWORKS_DIR}/"

echo "[4/4] Copying Vulkan headers to ${HEADERS_DIR}/..."
mkdir -p "$HEADERS_DIR"
rm -f "$HEADERS_DIR"/*.h
cp "${EXTRACT}/MoltenVK/MoltenVK/include/vulkan/"*.h "$HEADERS_DIR/"

echo ""
echo "Done. MoltenVK ${MOLTENVK_VERSION} vendored:"
du -sh "${FRAMEWORKS_DIR}/MoltenVK.xcframework"
echo "${HEADERS_DIR}/ contains $(ls ${HEADERS_DIR}/*.h | wc -l | tr -d ' ') headers"
