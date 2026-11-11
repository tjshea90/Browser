#!/usr/bin/env bash
# Verification gate (run before L2/L3 checkpoints).
#   verify.sh quick   core tests + tools build + app compiles          (~20 s warm)
#   verify.sh full    quick + lintRelease + R8 release APK + APK audit  (~1-2 min warm)
# APK audit: size budget, minSdk/targetSdk, permission allow-list, signature, no native libs, no AndroidX, not debuggable.
# Env: KESTREL_APK_BUDGET_KB (default 6000)
set -u
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT" || exit 2
MODE="${1:-quick}"
FAILS=0
fail() { echo "FAIL: $*" >&2; FAILS=$((FAILS + 1)); }
ok()   { echo "ok:   $*"; }

run() { echo ">> $*"; "$@" --console=plain 2>&1 | grep -v '^Picked up' | grep -E 'BUILD|FAILED|e: |w: .*kt|error:|What went wrong|tests completed|Lint found|Could not' ; return "${PIPESTATUS[0]}"; }

GRADLE="./gradlew"; [ -x "$GRADLE" ] || GRADLE="gradle"

case "$MODE" in
  quick) TASKS=(:core:test :tools:build :app:compileDebugKotlin) ;;
  full)  TASKS=(:core:test :tools:build :app:lintRelease :app:assembleRelease) ;;
  *) echo "usage: verify.sh quick|full" >&2; exit 2 ;;
esac

if run "$GRADLE" "${TASKS[@]}"; then ok "gradle ${TASKS[*]}"; else fail "gradle ${TASKS[*]}"; fi

if [ "$MODE" = full ] && command -v node >/dev/null 2>&1 && [ -d web-tests ]; then
  # injected-script tests in real headless Chromium (Playwright is preinstalled globally in the sandbox)
  WLOG="$(mktemp)"
  if (cd web-tests && node --test --test-timeout=60000 >"$WLOG" 2>&1); then ok "web-tests: $(grep -E '^# pass' "$WLOG") in Chromium"; else fail "web-tests failed:"; grep -E '^not ok|^# fail' "$WLOG" | head; fi
  rm -f "$WLOG"
fi

if [ "$MODE" = full ]; then
  BT="$(ls -d "${ANDROID_HOME:-/opt/android-sdk}"/build-tools/* 2>/dev/null | sort -V | tail -1)"
  APK="app/build/outputs/apk/release/app-release.apk"
  BUDGET_KB="${KESTREL_APK_BUDGET_KB:-6000}"
  ALLOWED_PERMS="INTERNET ACCESS_NETWORK_STATE ACCESS_FINE_LOCATION ACCESS_COARSE_LOCATION CAMERA RECORD_AUDIO MODIFY_AUDIO_SETTINGS"
  if [ ! -f "$APK" ]; then fail "release APK missing at $APK"; else
    KB=$(( $(stat -c %s "$APK") / 1024 ))
    [ "$KB" -le "$BUDGET_KB" ] && ok "APK size ${KB} KB (budget ${BUDGET_KB} KB)" || fail "APK ${KB} KB exceeds budget ${BUDGET_KB} KB"
    BADGE="$("$BT/aapt2" dump badging "$APK" 2>/dev/null)"
    echo "$BADGE" | grep -q "^minSdkVersion:'29'" && ok "minSdk 29" || fail "minSdk is not 29"
    echo "$BADGE" | grep -q "^targetSdkVersion:'36'" && ok "targetSdk 36" || fail "targetSdk is not 36"
    for p in $(echo "$BADGE" | sed -n "s/^uses-permission: name='android.permission.\([A-Z_]*\)'.*/\1/p"); do
      case " $ALLOWED_PERMS " in *" $p "*) ;; *) fail "unexpected permission $p" ;; esac
    done
    ok "permissions: $(echo "$BADGE" | sed -n "s/^uses-permission: name='android.permission.\([A-Z_]*\)'.*/\1/p" | tr '\n' ' ')"
    "$BT/apksigner" verify "$APK" >/dev/null 2>&1 && ok "signature verifies" || fail "signature does not verify"
    [ "$(unzip -l "$APK" | grep -c '\.so$')" = 0 ] && ok "no native libraries" || fail "native libraries present"
    TMP="$(mktemp -d)"; unzip -q -o "$APK" 'classes*.dex' -d "$TMP" 2>/dev/null
    if cat "$TMP"/classes*.dex 2>/dev/null | grep -a -q 'Landroidx/'; then fail "AndroidX classes found in dex"; else ok "no AndroidX in dex"; fi
    rm -f "$TMP"/classes*.dex; rmdir "$TMP" 2>/dev/null
    "$BT/aapt2" dump xmltree "$APK" --file AndroidManifest.xml 2>/dev/null | grep -q 'debuggable.*=true\|debuggable.*=(type 0x12)0xffffffff' && fail "APK is debuggable" || ok "not debuggable"
  fi
fi

echo "=== verify $MODE: $([ "$FAILS" = 0 ] && echo PASS || echo "FAIL ($FAILS)") ==="
[ "$FAILS" = 0 ]
