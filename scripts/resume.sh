#!/usr/bin/env bash
# COLD-START RESUME REPORT for a fresh Claude session. Also wired to the SessionStart hook.
#   resume.sh [--hook]
# With --hook it ALSO recovers work: a dirty tree left by an interrupted session is saved as a
# wip commit and pushed; a clean, behind-only branch is fast-forwarded. Nothing is ever discarded.
set -u
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT" || exit 0
# shellcheck source=scripts/lib.sh
. "$ROOT/scripts/lib.sh"

HOOK=0; [ "${1:-}" = "--hook" ] && HOOK=1
if [ "$HOOK" = 1 ] && [ ! -t 0 ]; then cat >/dev/null 2>&1; fi
git rev-parse --is-inside-work-tree >/dev/null 2>&1 || { echo "not a git repo"; exit 0; }

BR="$(kst_branch)"
echo "=== KESTREL RESUME REPORT  $(kst_utc) ==="
echo "branch: $BR"

# 1) recover uncommitted work from an interrupted session
if [ -n "$(git status --porcelain 2>/dev/null)" ]; then
  echo "UNCOMMITTED CHANGES found (interrupted session?):"
  git status --short | head -30
  if [ "$HOOK" = 1 ] && kst_guard_branch "$BR"; then
    bash "$ROOT/scripts/autosave.sh" --force "recovered at session start" >/dev/null 2>&1
    echo "-> saved as a wip commit (and pushed if possible)"
  else
    echo "-> run: bash scripts/autosave.sh --force"
  fi
fi

# 2) sync with origin
if git ls-remote --exit-code --heads origin "$BR" >/dev/null 2>&1; then
  git fetch -q origin "$BR" 2>/dev/null
  ahead=$(git rev-list --count "origin/$BR..HEAD" 2>/dev/null || echo "?")
  behind=$(git rev-list --count "HEAD..origin/$BR" 2>/dev/null || echo "?")
  echo "remote: origin/$BR  ahead=$ahead behind=$behind"
  if [ "$behind" != "0" ] && [ "$behind" != "?" ]; then
    if [ "$ahead" = "0" ] && [ -z "$(git status --porcelain 2>/dev/null)" ]; then
      git merge -q --ff-only "origin/$BR" && echo "fast-forwarded to origin/$BR"
    else
      echo "WARNING: diverged from origin/$BR - reconcile with: git pull --rebase origin $BR"
    fi
  fi
  if [ "$ahead" != "0" ] && [ "$ahead" != "?" ]; then echo "NOTE: $ahead unpushed commit(s) - push: git push -u origin $BR"; fi
else
  echo "remote: origin has no '$BR' yet (created on first push)"
fi

# 3) the handoff
echo "--- last commits"; git log --oneline -8 2>/dev/null
echo "--- docs/handoff/STATE.md :: RESUME HERE"
if [ -f docs/handoff/STATE.md ]; then
  awk '/<!-- RESUME:BEGIN -->/{f=1;next} /<!-- RESUME:END -->/{f=0} f' docs/handoff/STATE.md
  awk '/<!-- CHECKPOINT:BEGIN -->/{f=1;next} /<!-- CHECKPOINT:END -->/{f=0} f' docs/handoff/STATE.md
else
  echo "(docs/handoff/STATE.md missing!)"
fi

# 4) toolchain sanity (one line each)
echo "--- toolchain"
echo "java:    $(java -version 2>&1 | grep -m1 -i version || echo MISSING)"
echo "sdk:     ${ANDROID_HOME:-unset} platforms=[$(ls "${ANDROID_HOME:-/nonexistent}/platforms" 2>/dev/null | tr '\n' ' ')]"
echo "gradle:  $( [ -x ./gradlew ] && echo ./gradlew || (command -v gradle || echo MISSING) )"
echo "tip:     full detail -> docs/handoff/STATE.md ; protocol -> docs/handoff/PROTOCOL.md"
exit 0
