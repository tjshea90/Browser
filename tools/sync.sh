#!/usr/bin/env bash
# sync.sh - bring OTHER sessions' work into this branch. Many Claude sessions and accounts share this repo: each gets its own branch, and
# a session that starts from a stale branch point (or one that was cut off by a usage cap) would otherwise never see the work of the others.
#
#   bash tools/sync.sh            merge what is safe, report the rest
#   bash tools/sync.sh --quiet    print only when something was merged or went wrong
# Called by resume.sh (session start) and ckpt.sh (every checkpoint). Never loses anything: a merge that conflicts is ABORTED and reported.
#
# WHAT IS MERGED, from every other remote branch that has commits this one lacks (oldest first):
#   - a branch whose newest commit is OLDER than 20 minutes is treated as cut off: its WHOLE tip is merged, including the half-finished change
#     its last autosave captured (that change is the work to resume - read `git log` and the diff it names);
#   - a branch whose newest commit is YOUNGER than 20 minutes may still be running: only its last DELIBERATE checkpoint (ckpt / ship / record
#     commit) is merged, never its in-flight autosaves.
# Append-only files (INBOX, OUTBOX, BUILDLOG, TASKS) merge with the built-in `union` driver (.gitattributes); CHECKPOINT.md uses
# tools/merge-newest.sh (the newest card wins; each session's own card survives in .checkpoints/). Anything else that conflicts aborts the merge.
set -uo pipefail
D="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"; cd "$D" || exit 0
[ -e .git ] || exit 0
QUIET=0; for a in "$@"; do [ "$a" = "--quiet" ] && QUIET=1; done
say() { printf '%s\n' "$*"; }
BR="$(git branch --show-current 2>/dev/null || true)"; [ -z "$BR" ] && exit 0

# the CHECKPOINT.md merge driver (config is per clone, so it is (re)defined on every run)
git config merge.newest.name "keep the newest checkpoint card" 2>/dev/null
git config merge.newest.driver 'bash tools/merge-newest.sh %O %A %B' 2>/dev/null

GD="$(git rev-parse --git-dir)"
if [ -e "$GD/MERGE_HEAD" ] || [ -d "$GD/rebase-merge" ] || [ -d "$GD/rebase-apply" ] || [ -n "$(git diff --name-only --diff-filter=U 2>/dev/null)" ]; then
  say "  sync skipped: a merge or rebase is in progress here - finish it first"; exit 0
fi
timeout 30 git fetch -q origin '+refs/heads/*:refs/remotes/origin/*' >/dev/null 2>&1 || { [ "$QUIET" -eq 1 ] || say "  sync: could not reach GitHub - not merging"; exit 0; }

# a merge needs a clean tree: save what is on disk first (autosave never fails and never loses anything)
if [ -n "$(git status --porcelain 2>/dev/null)" ]; then bash tools/autosave.sh --text </dev/null >/dev/null 2>&1 || true; fi
if [ -n "$(git status --porcelain 2>/dev/null)" ]; then say "  sync skipped: uncommitted files remain (see tools/secretscan.sh)"; exit 0; fi

NOW="$(date +%s)"; MERGED=0; ISSUES=0
for RB in $(git for-each-ref --sort=committerdate --format='%(refname:short)' refs/remotes/origin 2>/dev/null | grep -v -e '^origin$' -e '^origin/HEAD$' -e "^origin/${BR}\$"); do
  case "$RB" in origin/dependabot/*) continue ;; esac
  N="$(git rev-list --count "HEAD..$RB" 2>/dev/null || echo 0)"; [ "${N:-0}" -gt 0 ] || continue
  if ! git merge-base HEAD "$RB" >/dev/null 2>&1; then
    # a shallow clone may lack the common ancestor: deepen that one branch (bounded; some proxies refuse --unshallow)
    [ "$(git rev-parse --is-shallow-repository 2>/dev/null)" = true ] && timeout 60 git fetch -q --depth=1000 origin "${RB#origin/}" >/dev/null 2>&1
    git merge-base HEAD "$RB" >/dev/null 2>&1 || continue          # unrelated history (an old re-rooted branch): not session work
  fi
  AGE=$(( NOW - $(git log -1 --format=%ct "$RB" 2>/dev/null || echo "$NOW") )); MIN=$(( AGE / 60 ))
  TARGET="$RB"
  if [ "$AGE" -lt 1200 ]; then
    TARGET="$(git log -1 --format=%H --extended-regexp --grep='^(ckpt [0-9]+:|ship |record )' "HEAD..$RB" 2>/dev/null)"
    if [ -z "$TARGET" ]; then say "  sync: ${RB#origin/} is active ($MIN min ago) and has no new checkpoint yet - left alone"; continue; fi
  fi
  if git merge -q --no-edit -m "sync: merge ${RB#origin/} (auto)" "$TARGET" >/dev/null 2>&1; then
    MERGED=$((MERGED+1))
    if [ "$TARGET" = "$RB" ]; then say "  sync: MERGED ${RB#origin/} ($N commit(s), its newest $MIN min old$([ "$AGE" -ge 1200 ] && echo ' - cut off? read its CHECKPOINT/.checkpoints card and the diff of its last commits'))"
    else say "  sync: merged ${RB#origin/} up to its last checkpoint (it is active, $MIN min old)"; fi
  else
    FILES="$(git diff --name-only --diff-filter=U 2>/dev/null | head -6 | tr '\n' ' ')"
    git merge --abort >/dev/null 2>&1
    ISSUES=$((ISSUES+1))
    say "  !! SYNC CONFLICT with ${RB#origin/} in: ${FILES}- NOT merged, nothing lost. Look: git log --oneline HEAD..$RB | head ; merge by hand: git merge $RB ; fix the files ; git add -A ; git commit --no-edit"
  fi
done

[ "$MERGED" -gt 0 ] && { bash tools/push.sh || say "  WARN could not push the merge: git push origin HEAD"; }
if [ "$QUIET" -eq 0 ] && [ "$MERGED" -eq 0 ] && [ "$ISSUES" -eq 0 ]; then say "  sync: no other session has work this branch lacks"; fi
exit 0
