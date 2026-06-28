#!/usr/bin/env bash
# Builds a distributable Omilator.dmg from the debug build.
#
# Compose Desktop's release build runs ProGuard which fails on LWJGL's
# spurious javax.annotation.Nullable references. The debug build skips
# ProGuard and produces a working .app — we just package that as a DMG.
#
# Usage: ./release-dmg.sh
set -euo pipefail

cd "$(dirname "$0")/.."

APP_DIR="app-desktop/build/compose/binaries/main/app/Omilator.app"
DMG_DIR="app-desktop/build/compose/binaries/main/dmg-staging"
DMG_OUT="app-desktop/build/compose/binaries/main/Omilator-1.0.0.dmg"

echo "[1/4] Building debug distribution (skips ProGuard)..."
JAVA_HOME="/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home" \
  ./gradlew :app-desktop:createDistributable --quiet

if [ ! -d "$APP_DIR" ]; then
  echo "ERROR: $APP_DIR not found"
  exit 1
fi

echo "[2/4] Staging .app for DMG..."
rm -rf "$DMG_DIR"
mkdir -p "$DMG_DIR"
cp -R "$APP_DIR" "$DMG_DIR/"
ln -s /Applications "$DMG_DIR/Applications"

echo "[3/4] Creating DMG..."
rm -f "$DMG_OUT"
hdiutil create -volname "Omilator" -srcfolder "$DMG_DIR" -ov -format UDZO "$DMG_OUT"

echo "[4/4] Cleaning up staging..."
rm -rf "$DMG_DIR"

echo ""
echo "Done: $DMG_OUT"
du -sh "$DMG_OUT"
