#!/usr/bin/env bash
# check-profile-coverage.sh - LAB7: the baseline profile of the RELEASE build marks (nearly) every method of the app as HOT.
# Android 10 compiles an app while it installs (`speed-profile`) but ONLY the methods the profile marks hot (H); the lab's rules mark about 2.5k of the 15k, the catch-all rule `HL**;->**(**)**` at the end of
# geckoapp/src/main/baseline-prof.txt marks the rest (AGP expands it over every class, R8 rewrites it onto the final dex). This reads the profile R8 wrote for the release build and fails when the rule is gone or
# no longer expands (an Android Gradle plugin change, a rule file regenerated without it): the release would still build and install, it would just be interpreted again until the idle dexopt.
#   bash tools/check-profile-coverage.sh        (after ./gradlew :geckoapp:assembleRelease)
set -uo pipefail
D="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"; cd "$D" || exit 1
SRC=geckoapp/src/main/baseline-prof.txt
R8="${PROFILE_R8_FILE:-geckoapp/build/intermediates/r8_art_profile/release/minifyReleaseWithR8/baseline-prof.txt}"
MIN_HOT="${PROFILE_MIN_HOT:-3000}"        # the app really has about 15000; the lab's own rules alone give 2500
MIN_PCT="${PROFILE_MIN_HOT_PCT:-95}"
fail=0
if ! grep -qxF 'HL**;->**(**)**' "$SRC"; then echo "  FAIL  $SRC does not end with the catch-all rule HL**;->**(**)** (LAB7: only hot methods are compiled at install)"; fail=1; fi
if [ ! -f "$R8" ]; then echo "  FAIL  $R8 not found: build :geckoapp:assembleRelease first, or the Android Gradle plugin moved the file"; exit 1; fi
all=$(grep -c -- '->' "$R8")
hot=$(grep -cE '^[HSP]*H[HSP]*L[^>]*->' "$R8")
echo "  profile after R8: $hot of $all methods are hot"
if [ "$all" -lt "$MIN_HOT" ] || [ "$hot" -lt "$MIN_HOT" ]; then echo "  FAIL  only $hot hot of $all methods (expected at least $MIN_HOT)"; fail=1; fi
if [ "$all" -gt 0 ] && [ $((hot * 100)) -lt $((all * MIN_PCT)) ]; then echo "  FAIL  $hot of $all methods are hot, less than $MIN_PCT % (the catch-all rule did not expand?)"; fail=1; fi
if [ "$fail" -eq 0 ]; then echo "  OK    the release profile compiles the whole app at install ($hot of $all methods hot)"; fi
exit "$fail"
