#!/usr/bin/env bash
# check-gecko-prefs.sh - every preference named in geckoapp/src/main/assets/gecko-prefs.txt must EXIST in the bundled GeckoView.
# A pref that the engine no longer knows is a silent no-op (16 popular names are already gone in GeckoView 160), so CI fails the Kestrel G build instead.
# Needs the GeckoView AAR in the Gradle cache (any geckoapp build puts it there). Reads the engine's strings only; runs no engine code.
#   bash tools/check-gecko-prefs.sh            exit 0 = all names found
set -uo pipefail
D="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"; cd "$D" || exit 1
AAR="$(find "${GRADLE_USER_HOME:-$HOME/.gradle}/caches/modules-2/files-2.1/org.mozilla.geckoview" -name 'geckoview-armeabi-v7a-*.aar' 2>/dev/null | head -1)"
[ -n "$AAR" ] || { echo "  note  no GeckoView AAR in the Gradle cache yet - build :geckoapp first"; exit 2; }
W="$(mktemp -d)"; trap 'rm -rf "$W"' EXIT
unzip -q -o "$AAR" 'jni/armeabi-v7a/libxul.so' 'assets/omni.ja' -d "$W/aar" || { echo "  FAIL  could not unpack $AAR"; exit 1; }
mkdir -p "$W/omni" && unzip -q -o "$W/aar/assets/omni.ja" -d "$W/omni" 2>/dev/null
MISSING=0; N=0
while IFS= read -r line; do
  case "$line" in ''|'#'*) continue ;; esac
  name="$(printf '%s' "${line%%=*}" | tr -d ' ')"
  case "$name" in kestrel.*) continue ;; esac
  N=$((N+1))
  if grep -a -q -F -- "$name" "$W/aar/jni/armeabi-v7a/libxul.so" || grep -r -a -q -F -- "$name" "$W/omni" 2>/dev/null; then :; else echo "  MISSING  $name (not in this GeckoView: remove it from gecko-prefs.txt)"; MISSING=$((MISSING+1)); fi
done < geckoapp/src/main/assets/gecko-prefs.txt
# the names the Kotlin code sets or clears LIVE (setGeckoPref / clearGeckoUserPref / the video rule's tri()) are held to the same rule
while IFS= read -r name; do
  [ -n "$name" ] || continue
  N=$((N+1))
  if grep -a -q -F -- "$name" "$W/aar/jni/armeabi-v7a/libxul.so" || grep -r -a -q -F -- "$name" "$W/omni" 2>/dev/null; then :; else echo "  MISSING  $name (set live from the Kotlin code, but not in this GeckoView)"; MISSING=$((MISSING+1)); fi
done < <(grep -rhoE '(setGeckoPref|clearGeckoUserPref|tri)\("[a-z0-9_.-]+"' geckoapp/src/main/kotlin | sed -E 's/^[A-Za-z]+\("//; s/"$//' | sort -u)
echo "  checked $N pref names against $(basename "$AAR"): $MISSING missing"
[ "$MISSING" -eq 0 ]
