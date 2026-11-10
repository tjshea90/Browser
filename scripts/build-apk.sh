#!/usr/bin/env bash
# Build the R8-minified release APK and copy it to dist/kestrel-<versionName>.apk (+ .sha256).
#   build-apk.sh [--no-verify]
# Signed with signing/dev/ (PUBLIC dev key) unless keystore.properties exists. See docs/ARCHITECTURE.md section 8.
set -u
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT" || exit 2
if [ "${1:-}" != "--no-verify" ]; then bash scripts/verify.sh full || { echo "verify failed - not publishing an APK" >&2; exit 1; }
else ./gradlew :app:assembleRelease --console=plain 2>&1 | grep -E 'BUILD|FAILED' ; fi

APK="app/build/outputs/apk/release/app-release.apk"
[ -f "$APK" ] || { echo "missing $APK" >&2; exit 1; }
BT="$(ls -d "${ANDROID_HOME:-/opt/android-sdk}"/build-tools/* | sort -V | tail -1)"
VER="$("$BT/aapt2" dump badging "$APK" | sed -n "s/^package:.* versionName='\([^']*\)'.*/\1/p")"
mkdir -p dist
OUT="dist/kestrel-${VER}.apk"
cp "$APK" "$OUT"
( cd dist && sha256sum "kestrel-${VER}.apk" > "kestrel-${VER}.apk.sha256" )
echo "built $OUT ($(( $(stat -c %s "$OUT") / 1024 )) KB)"
cat "dist/kestrel-${VER}.apk.sha256"
"$BT/apksigner" verify --print-certs "$OUT" 2>&1 | grep -E 'DN|SHA-256' | head -2
