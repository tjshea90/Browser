#!/usr/bin/env bash
# ship.sh - cut a release. GITHUB BUILDS AND SIGNS IT; this prepares and queues that.
#
#   bash ship.sh gecko "what changed this release"     # Kestrel G (Firefox GeckoView), the modern-engine build
#   bash ship.sh web   "what changed this release"     # Kestrel (system WebView), the light fallback
#   bash ship.sh --local gecko "what changed"          # FALLBACK ONLY: build it here instead (Actions down / out of minutes)
#
# WHY GITHUB BUILDS IT
# --------------------
# The owner's rule on all his repos: "GitHub Actions should do all the app compiling and apk making. Claude codes the app,
# GitHub makes the apk." The mechanism:
#   1. Claude codes, tests and checkpoints as usual (tools/ckpt.sh).
#   2. `ship.sh <app> "note"` runs the cheap local gates first - a red suite must never spend GitHub's minutes - then pushes.
#   3. Claude dispatches .github/workflows/android.yml through the GitHub API (mcp__github__actions_run_trigger). That run
#      builds, SIGNS, verifies the certificate on the artifact it just produced, and publishes a GitHub Release with the APK.
#   4. Once that run is green, tools/record-release.sh writes the line into BUILDLOG.md.
# The APK is never committed: Releases hold the binaries, BUILDLOG.md is the durable record (and what the next versionCode is gated on).
set -uo pipefail
D="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"; cd "$D" || exit 1
. tools/appinfo.sh

LOCAL=0
if [ "${1:-}" = "--local" ]; then LOCAL=1; shift; fi
APP="${1:-}"; NOTE="${2:-}"
if ! app_valid "$APP" || [ -z "$NOTE" ]; then
  echo "usage: bash ship.sh [--local] <web|gecko> \"one-line change note\""; exit 1
fi

# COMMIT BEFORE BUILDING. A build against a dirty tree proves nothing about what is committed, and leaves the tree looking like the
# last session was interrupted when it was not.
if [ -e .git ] && [ -n "$(git status --porcelain 2>/dev/null)" ]; then
  bash tools/ckpt.sh "pre-ship: $NOTE" "ship.sh gates and releases this ($APP)" >/dev/null 2>&1
  echo "  OK    committed the working tree before releasing"
fi

echo "== ship gates ($APP) =="
# the built-in extension reinstalls only when its manifest version changes: keep it equal to 1.0.<versionCode> (committed with the rest below)
[ "$APP" = gecko ] && bash tools/sync-ext-version.sh

# ---- the shell tooling self-test (handoff, secret scan, workflow rules) ----
if [ -f tools/test_handoff.sh ]; then
  if ! bash tools/test_handoff.sh >.git/ship-handoff.log 2>&1; then
    echo "  FAIL  tools/test_handoff.sh is red - see .git/ship-handoff.log"; tail -15 .git/ship-handoff.log | sed 's/^/          /'
    exit 1
  fi
  echo "  OK    handoff self-test"
fi

# ---- the version Android will actually see ----
VCODE="$(app_vcode "$APP")"; VNAME="$(app_vname "$APP")"
[ -n "$VCODE" ] && [ -n "$VNAME" ] || { echo "  FAIL  could not read versionCode/versionName from $(app_gradle "$APP")"; exit 1; }
PREV_MAX="$(app_last_code "$APP")"
# STRICTLY HIGHER THAN EVERY CODE EVER SHIPPED for this app, or Android refuses the install as a downgrade/duplicate. Read from
# BUILDLOG.md - the workflow gates against exactly the same file.
if [ "$VCODE" -le "$PREV_MAX" ]; then
  echo "  FAIL  $APP versionCode $VCODE is not higher than the last shipped ($PREV_MAX)."
  echo "        Android will refuse to install this over what is on the tablet."
  echo "        Bump versionCode (and versionName) in $(app_gradle "$APP"), then ship again."
  exit 1
fi
TAG="$(app_tag "$APP")"
echo "  OK    $(app_title "$APP") $VNAME (code $VCODE) - higher than every previous ship; tag will be $TAG"

# ---- the unit suites must be green (run HERE even though the workflow runs them too: a red suite found on a runner has already cost minutes) ----
echo "  ..    running the unit suites (cold Gradle can take minutes)"
TASKS=(:core:test :app:testDebugUnitTest)
[ "$APP" = gecko ] && TASKS+=(:geckoapp:compileDebugKotlin)
if ! ./gradlew "${TASKS[@]}" --console=plain > .git/ship-test.log 2>&1; then
  echo "  FAIL  the suites are red. Not shipping this."
  grep -E 'FAILED|error:|e: ' .git/ship-test.log | head -20 | sed 's/^/          /'
  echo "        full log: .git/ship-test.log"
  exit 1
fi
echo "  OK    unit suites green (${TASKS[*]})"

# ------------------------------------------------------------------ local fallback
if [ "$LOCAL" -eq 1 ]; then
  echo "  ..    --local: building and signing HERE instead of on GitHub"
  if ! ./gradlew "$(app_module "$APP"):assembleRelease" --console=plain > .git/ship-build.log 2>&1; then
    echo "  FAIL  the release build did not succeed."; tail -30 .git/ship-build.log | sed 's/^/          /'; exit 1
  fi
  bash tools/verify-apk.sh "$APP" || { echo "  FAIL  the APK did not pass verification."; exit 1; }
  mkdir -p dist; cp "$(app_apk "$APP")" "dist/$(app_asset "$APP")"
  ( cd dist && sha256sum "$(app_asset "$APP")" > "$(app_asset "$APP").sha256" )
  echo "  OK    dist/$(app_asset "$APP") (NOT committed; deliver it as a file). Record it with tools/record-release.sh only if it was really published."
  exit 0
fi

# ------------------------------------------------------------------ the normal path
#
# WHY THIS DOES NOT PUSH A TAG: the cloud sandbox allows pushes to refs/heads/* and answers 403 to refs/tags/*. So the tag is created by
# GitHub instead: the workflow's publish step calls `gh release create --target <sha>`, which makes the tag server-side.
if ! bash tools/push.sh; then
  echo "  FAIL  could not push the commit. Nothing to build - a run would check out a commit GitHub does not have."
  exit 1
fi
echo "  OK    commit pushed"

BR="$(git branch --show-current 2>/dev/null || echo '<branch>')"
# LEAVE THE NEXT STEP ON DISK, NOT JUST ON SCREEN. A usage cap landing between this script and the dispatch would otherwise leave a gated,
# pushed, unreleased commit whose only instruction was printed to a terminal nobody reads again. The next session is told by resume.sh.
bash tools/ckpt.sh \
  "gated $TAG ($APP code $VCODE) and pushed it: handoff self-test, unit suites and the versionCode check passed here. NOT yet built - GitHub has not been asked." \
  "TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow_id android.yml, ref $BR, inputs {\"app\":\"$APP\",\"full_build\":\"true\",\"note\":\"$NOTE\"}.
When the run is green, confirm the Release shows the APK (mcp__github__get_release_by_tag $TAG), then:  bash tools/record-release.sh $TAG \"$NOTE\"
NEVER ship $TAG twice (check BUILDLOG.md and the Releases first)." >/dev/null 2>&1
echo "  OK    next step recorded in CHECKPOINT.md (survives an interruption here)"
echo
echo "== $TAG ($APP code $VCODE) is ready for GitHub to build =="
echo
echo "  NEXT, and Claude does this - it needs the GitHub API, not git:"
echo "    mcp__github__actions_run_trigger  method=run_workflow  workflow_id=android.yml  ref=$BR"
echo "    inputs={\"app\":\"$APP\",\"full_build\":\"true\",\"note\":\"$NOTE\"}"
echo
echo "  That run tests, builds, SIGNS, verifies the certificate on the APK it just produced, creates the tag $TAG and"
echo "  publishes it under Releases. WHEN IT IS GREEN, and not before:"
echo "    bash tools/record-release.sh $TAG \"$NOTE\""
echo "  BUILDLOG.md is what the next release is gated against, so a line in it must never describe a build that does not exist."
