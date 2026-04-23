#!/usr/bin/env bash
set -euo pipefail

export ANDROID_HOME="${ANDROID_HOME:-$HOME/android-sdk}"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/build-tools/36.0.0:$PATH"

OUT_DIR="$(pwd)/outputs"
KEYSTORE="$(pwd)/keystore/customrpc-release.keystore"
KEY_ALIAS="customrpc"
KEY_PASS="customrpc"

mkdir -p "$OUT_DIR"
rm -f "$OUT_DIR"/*.apk

echo "==> Building debug APK..."
./gradlew --no-daemon -Dorg.gradle.parallel=false assembleDebug

echo "==> Building release APK..."
./gradlew --no-daemon -Dorg.gradle.parallel=false assembleRelease

DEBUG_APK="app/build/outputs/apk/debug/app-debug.apk"
UNSIGNED_APK="app/build/outputs/apk/release/app-release-unsigned.apk"

[ -f "$DEBUG_APK" ] || { echo "Missing $DEBUG_APK"; exit 1; }
[ -f "$UNSIGNED_APK" ] || { echo "Missing $UNSIGNED_APK"; exit 1; }

cp "$DEBUG_APK"    "$OUT_DIR/app-debug.apk"
cp "$UNSIGNED_APK" "$OUT_DIR/app-release-unsigned.apk"

SIGNED_APK="$OUT_DIR/app-release-signed.apk"
cp "$UNSIGNED_APK" "$SIGNED_APK"

echo "==> Zip-aligning..."
ZIPALIGN="$ANDROID_HOME/build-tools/36.0.0/zipalign"
ALIGNED="$OUT_DIR/app-release-aligned.apk"
"$ZIPALIGN" -f -p 4 "$SIGNED_APK" "$ALIGNED"
mv "$ALIGNED" "$SIGNED_APK"

echo "==> Signing release APK with apksigner..."
apksigner sign \
  --ks "$KEYSTORE" \
  --ks-key-alias "$KEY_ALIAS" \
  --ks-pass "pass:$KEY_PASS" \
  --key-pass "pass:$KEY_PASS" \
  --v1-signing-enabled true \
  --v2-signing-enabled true \
  --v3-signing-enabled true \
  "$SIGNED_APK"

apksigner verify --print-certs "$SIGNED_APK" | head -n 6

echo ""
echo "==> Build complete. Output APKs:"
ls -lh "$OUT_DIR"/*.apk
