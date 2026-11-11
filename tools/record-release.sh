#!/usr/bin/env bash
# record-release.sh - write a shipped version into BUILDLOG.md, AFTER GitHub built it.
#
# WHY THIS IS A SEPARATE STEP FROM ship.sh
# ----------------------------------------
# BUILDLOG.md is load-bearing, not documentation: it is the durable record of every versionCode that was ever shipped, and
# BOTH ship.sh and .github/workflows/android.yml read it to decide whether the next build could actually install over the
# last one on the tablet. A line in it is a claim that a release EXISTS.
#
# ship.sh does not build the APK - GitHub does - so when ship.sh finishes the release does not exist yet. Writing the line
# there would make BUILDLOG lie whenever the run failed. So: ship.sh gates and pushes, GitHub builds, and only once that run is
# GREEN (and the Release shows its APK) does this record it.
#
#   bash tools/record-release.sh kestrel-g-v0.2.0-gecko-alpha1 "what changed"
#   bash tools/record-release.sh kestrel-v0.1.1 "what changed"
set -uo pipefail
D="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"; cd "$D" || exit 1
. tools/appinfo.sh

TAG="${1:-}"; NOTE="${2:-}"
if [ -z "$TAG" ] || [ -z "$NOTE" ]; then
  echo "usage: bash tools/record-release.sh <kestrel-v0.1.1 | kestrel-g-v0.2.0-gecko-alpha1> \"what changed this release\""; exit 1
fi
APP="$(app_of_tag "$TAG")" || { echo "  FAIL  '$TAG' is not a Kestrel tag (kestrel-v<version> or kestrel-g-v<version>)"; exit 1; }
VNAME="${TAG#$(app_prefix "$APP")}"
GV="$(app_vname "$APP")"; VCODE="$(app_vcode "$APP")"
[ -n "$VCODE" ] || { echo "  FAIL  could not read versionCode from $(app_gradle "$APP")"; exit 1; }
# Guard: the code is read from the CURRENT Gradle file, so if the version was already bumped for the next release the line would be wrong.
[ "$GV" = "$VNAME" ] || { echo "  FAIL  $(app_gradle "$APP") is at version $GV, not $VNAME: record the release BEFORE bumping for the next one (or write the BUILDLOG line by hand with the real versionCode)"; exit 1; }

if grep -q "^| ${TAG} |" BUILDLOG.md 2>/dev/null; then
  echo "  ..    $TAG is already in BUILDLOG.md - nothing to record"; exit 0
fi

printf '| %s | %s | code %s | %s | %s\n' "$TAG" "$APP" "$VCODE" "$(date -u +%Y-%m-%dT%H:%MZ)" "$NOTE" >> BUILDLOG.md

git add BUILDLOG.md >/dev/null 2>&1
if ! bash tools/secretscan.sh; then
  git reset -q >/dev/null 2>&1
  echo "  FAIL  a credential is in the tree - nothing recorded."; exit 1
fi
git commit -q -m "record $TAG ($APP code $VCODE): $NOTE

Built and signed by GitHub Actions; this line is the durable record that the
release exists. The APK is under Releases.

Co-Authored-By: Claude <noreply@anthropic.com>" >/dev/null 2>&1

if bash tools/push.sh; then
  echo "  OK    recorded $TAG ($APP code $VCODE) in BUILDLOG.md and pushed"
else
  echo "  WARN  recorded locally but COULD NOT PUSH. Retry: git push origin HEAD"; exit 1
fi
