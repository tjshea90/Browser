#!/usr/bin/env bash
# L0 AUTOSAVE — throttled WIP commit + push of the current branch.
# Safe to call from Claude Code hooks: it never fails the caller and never rewrites history.
#   autosave.sh [--hook] [--force] ["note"]
#     --hook   consume hook JSON on stdin
#     --force  ignore the throttle (used by Stop / PreCompact / SessionStart-recovery)
# Env: KESTREL_AUTOSAVE_SECS (default 120), KESTREL_ALLOW_MAIN=1 to permit main/master.
set -u
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT" || exit 0
# shellcheck source=scripts/lib.sh
. "$ROOT/scripts/lib.sh"

FORCE=0; HOOK=0; NOTE=""
for a in "$@"; do
  case "$a" in --force) FORCE=1 ;; --hook) HOOK=1 ;; *) NOTE="$a" ;; esac
done
if [ "$HOOK" = 1 ] && [ ! -t 0 ]; then cat >/dev/null 2>&1; fi

git rev-parse --is-inside-work-tree >/dev/null 2>&1 || exit 0
BR="$(kst_branch)"
kst_guard_branch "$BR" || exit 0
GD="$(git rev-parse --git-dir)"
STAMP="$GD/kestrel-autosave.stamp"; LOCK="$GD/kestrel-autosave.lock"; LOG="$GD/kestrel-autosave.log"
now=$(date +%s)

if [ "$FORCE" = 0 ] && [ -f "$STAMP" ]; then
  last=$(cat "$STAMP" 2>/dev/null || echo 0)
  if [ $((now - last)) -lt "${KESTREL_AUTOSAVE_SECS:-120}" ]; then exit 0; fi
fi

# single-flight lock (mkdir is atomic); steal it if older than 5 minutes
if ! mkdir "$LOCK" 2>/dev/null; then
  age=$(( now - $(stat -c %Y "$LOCK" 2>/dev/null || echo 0) ))
  if [ "$age" -lt 300 ]; then exit 0; fi
  rmdir "$LOCK" 2>/dev/null
  mkdir "$LOCK" 2>/dev/null || exit 0
fi
trap 'rmdir "$LOCK" 2>/dev/null' EXIT

# never autosave in the middle of a merge / rebase / cherry-pick
if [ -e "$GD/MERGE_HEAD" ] || [ -d "$GD/rebase-merge" ] || [ -d "$GD/rebase-apply" ] || [ -e "$GD/CHERRY_PICK_HEAD" ]; then exit 0; fi

if [ -n "$(git status --porcelain 2>/dev/null)" ]; then
  git add -A >>"$LOG" 2>&1
  kst_secret_guard
  if ! git diff --cached --quiet 2>/dev/null; then
    n=$(git diff --cached --name-only | wc -l | tr -d ' ')
    git commit -q -m "wip(autosave): $(kst_utc) - ${n} file(s)${NOTE:+ - $NOTE}" >>"$LOG" 2>&1 \
      || echo "[kestrel] autosave commit failed (see $LOG)" >&2
  fi
fi

# push only when there is something unpushed (no network call otherwise)
if [ "$(kst_unpushed "$BR")" != "0" ]; then
  if kst_push "$BR" "${KESTREL_AUTOSAVE_PUSH_ATTEMPTS:-2}"; then
    echo "[kestrel] autosaved + pushed origin/$BR @ $(git rev-parse --short HEAD)" >&2
  else
    echo "[kestrel] autosave committed locally; push failed (will retry next time)" >&2
  fi
fi
echo "$now" > "$STAMP"
exit 0
