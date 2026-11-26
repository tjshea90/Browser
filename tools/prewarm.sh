#!/bin/sh
# tools/prewarm.sh - fills the Gradle, dependency and Robolectric caches of a FRESH container, so the first test, lint or build of a
# session does not download them (and does not meet a download limit in the middle of the work). Run by the owner's cloud-environment
# setup script in every new container, or by hand: sh tools/prewarm.sh. Nothing depends on it: every part is separate and a failure
# only prints a line, so a session always starts. Safe to run again. It writes only below build/, .gradle/ and the user caches.
cd "$(dirname "$0")/.." || exit 1
if [ -z "${ANDROID_HOME:-}" ]; then
  for c in /opt/android-sdk "$HOME/android-sdk"; do
    [ -d "$c/platforms" ] && ANDROID_HOME="$c" && break
  done
fi
: "${ANDROID_HOME:=/opt/android-sdk}"
export ANDROID_HOME
bad=0
part() { # part <what it warms> <gradle arguments>
  name=$1; shift
  echo "== $name"
  timeout 1500 sh ./gradlew --no-daemon -q --console=plain "$@" || { echo "warm-up part failed: $name"; bad=1; }
}
part "the Robolectric tests (Android 10 files)" :app:testDebugUnitTest --tests 'app.kestrel.browser.diag.CpuMeterTest'
part "the GeckoView engine libraries" :geckoapp:compileDebugKotlin
part "lint" :app:lintDebug
echo "warm-up done, problems: $bad"
exit $bad
