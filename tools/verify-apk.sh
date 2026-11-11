#!/usr/bin/env bash
# verify-apk.sh - is this APK the one the tablet can install over what is already there, and does it look sane?
#
# Building an APK proves it compiles. It does NOT prove it is installable over the copy on the tablet: that depends entirely
# on the signing certificate, and a wrong one gives a perfectly valid APK that Android refuses as an update (the owner would
# have to uninstall first and lose the browser's data). So the certificate is checked on the ARTIFACT, not just on the keystore
# that went in. The pin is signing/CERT_SHA256 (the certificate of every APK delivered so far); changing it is a deliberate act.
#
#   bash tools/verify-apk.sh <web|gecko> [path/to.apk]
#
# Also checks (cheap, from the APK itself): package name, versionCode/versionName equal what Gradle says, not debuggable,
# no Google Mobile Services anywhere in the manifest (the tablet has none and a GMS dependency must never be added), size budget.
set -uo pipefail
D="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"; cd "$D" || exit 1
. tools/appinfo.sh
APP="${1:-}"; app_valid "$APP" || { echo "usage: bash tools/verify-apk.sh <web|gecko> [apk]"; exit 2; }
APK="${2:-$(app_apk "$APP")}"
[ -f "$APK" ] || { echo "  FAIL  no APK at $APK"; exit 1; }
FAILS=0
bad(){ echo "  !!    $*"; FAILS=$((FAILS+1)); }
ok(){ echo "  OK    $*"; }

norm(){ tr -d ': \r' | tr 'A-Z' 'a-z'; }   # apksigner and keytool disagree on case and colons
WANT="$(norm < signing/CERT_SHA256 2>/dev/null | head -c 64)"
[ -n "$WANT" ] || { echo "  FAIL  signing/CERT_SHA256 is missing: nothing to verify the certificate against."; exit 1; }

BT="$(ls -d "${ANDROID_HOME:-/opt/android-sdk}"/build-tools/* 2>/dev/null | sort -V | tail -1)"
GOT=""
if [ -n "$BT" ] && [ -x "$BT/apksigner" ]; then
  OUT="$("$BT/apksigner" verify --print-certs "$APK" 2>&1)" || bad "apksigner verify FAILED: $(echo "$OUT" | head -2 | tr '\n' ' ')"
  GOT="$(echo "$OUT" | grep -i 'certificate SHA-256 digest' | head -1 | sed 's/.*digest: *//' | norm)"
fi
if [ -z "$GOT" ] && command -v keytool >/dev/null 2>&1; then
  CERT="$(unzip -l "$APK" 2>/dev/null | grep -oE 'META-INF/[^ ]*\.(RSA|EC|DSA)' | head -1)"
  [ -n "$CERT" ] && GOT="$(unzip -p "$APK" "$CERT" 2>/dev/null | keytool -printcert 2>/dev/null | sed -n 's/.*SHA256: *//p' | head -1 | norm)"
fi
if [ -z "$GOT" ]; then bad "could not read a signature from $APK - treat it as unsigned"
elif [ "$GOT" = "$WANT" ]; then ok "certificate is the pinned one ($(echo "$GOT" | cut -c1-16)...): installs over the copy on the tablet"
else bad "WRONG CERTIFICATE: it will NOT update the tablet in place. expected $WANT got $GOT"; fi

if [ -n "$BT" ] && [ -x "$BT/aapt2" ]; then
  BADGE="$("$BT/aapt2" dump badging "$APK" 2>/dev/null)"
  PKG="$(echo "$BADGE" | sed -n "s/^package: name='\([^']*\)'.*/\1/p")"
  VC="$(echo "$BADGE" | sed -n "s/^package:.* versionCode='\([^']*\)'.*/\1/p")"
  VN="$(echo "$BADGE" | sed -n "s/^package:.* versionName='\([^']*\)'.*/\1/p")"
  case "$APP" in web) WPKG="app.kestrel.browser" ;; gecko) WPKG="app.kestrel.browser.gecko" ;; esac
  [ "$PKG" = "$WPKG" ] && ok "package $PKG" || bad "package is '$PKG', expected '$WPKG'"
  [ "$VC" = "$(app_vcode "$APP")" ] && [ "$VN" = "$(app_vname "$APP")" ] && ok "version $VN (code $VC) equals the Gradle file" || bad "APK says $VN/$VC, Gradle says $(app_vname "$APP")/$(app_vcode "$APP")"
  echo "$BADGE" | grep -q "application-debuggable" && bad "APK is debuggable" || ok "not debuggable"
  # A GMS DEPENDENCY (service, receiver, provider, uses-library, meta-data...) is forbidden. The one allowed mention is the
  # <queries><package .../> entry the About page uses to DETECT whether Play services exist (an element named `package`).
  GMS="$("$BT/aapt2" dump xmltree "$APK" --file AndroidManifest.xml 2>/dev/null | awk '/^ *E: /{el=$2} /com\.google\.android\.gms/ && el!="package"{print el": "$0}' | head -3)"
  if [ -n "$GMS" ]; then bad "manifest depends on Google Mobile Services (the tablet has none; never add GMS): $GMS"; else ok "no Google Mobile Services dependency in the manifest"; fi
else
  echo "  note  no aapt2 here - package/version/manifest checks skipped"
fi

KB=$(( $(stat -c %s "$APK") / 1024 ))
case "$APP" in web) BUDGET="${KESTREL_APK_BUDGET_KB:-6000}" ;; gecko) BUDGET="${KESTREL_GECKO_BUDGET_KB:-130000}" ;; esac
[ "$KB" -le "$BUDGET" ] && ok "size ${KB} KB (budget ${BUDGET} KB)" || bad "size ${KB} KB exceeds the budget ${BUDGET} KB"

[ "$FAILS" -eq 0 ] && { echo "  OK    $(basename "$APK") passed all checks"; exit 0; }
echo "  $FAILS check(s) failed - do not publish or install $(basename "$APK")."
exit 1
