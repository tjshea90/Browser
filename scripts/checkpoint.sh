#!/usr/bin/env bash
# L1/L2/L3 CHECKPOINT — refresh STATE.md checkpoint block, append journal line, commit, push (with
# 2/4/8/16s retries) and VERIFY that origin has the commit.
#   checkpoint.sh [--milestone NAME] [--release VERSION] [-t "Trailer: value"]... "summary"
#   L1 (default)  after each finished unit of work (~10-20 min)
#   L2 --milestone  also tags cp/<NAME>-<utc>; update STATE.md phase table BEFORE running
#   L3 --release    also tags v<VERSION>; run scripts/verify.sh full BEFORE running
set -u
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT" || exit 2
# shellcheck source=scripts/lib.sh
. "$ROOT/scripts/lib.sh"

MILESTONE=""; RELEASE=""; SUMMARY=""; TRAILERS=()
while [ $# -gt 0 ]; do
  case "$1" in
    --milestone) MILESTONE="$2"; shift 2 ;;
    --release)   RELEASE="$2"; shift 2 ;;
    -t)          TRAILERS+=("$2"); shift 2 ;;
    *)           SUMMARY="$1"; shift ;;
  esac
done
[ -z "$SUMMARY" ] && { echo "usage: checkpoint.sh [--milestone N] [--release V] [-t trailer] \"summary\"" >&2; exit 2; }

BR="$(kst_branch)"
kst_guard_branch "$BR" || { echo "[kestrel] refusing to checkpoint on '$BR' (set KESTREL_ALLOW_MAIN=1 to override)" >&2; exit 2; }
NOW="$(kst_utc)"
STATE="docs/handoff/STATE.md"; JOURNAL="docs/handoff/JOURNAL.md"
LEVEL="L1"; [ -n "$MILESTONE" ] && LEVEL="L2"; [ -n "$RELEASE" ] && LEVEL="L3"

# 1) refresh the checkpoint block in STATE.md (between markers)
if [ -f "$STATE" ] && grep -q '<!-- CHECKPOINT:BEGIN -->' "$STATE"; then
  tmp="$(mktemp)"
  awk -v now="$NOW" -v br="$BR" -v lvl="$LEVEL" -v sum="$SUMMARY" -v base="$(git rev-parse --short HEAD 2>/dev/null)" '
    /<!-- CHECKPOINT:BEGIN -->/ {print; print "- **Last checkpoint:** " now " (" lvl ") on `" br "` after " base; print "- **Summary:** " sum; skip=1; next}
    /<!-- CHECKPOINT:END -->/ {skip=0}
    !skip {print}
  ' "$STATE" > "$tmp" && cat "$tmp" > "$STATE"
  rm -f "$tmp"
fi

# 2) journal (append-only)
mkdir -p "$(dirname "$JOURNAL")"
printf -- '- %s [%s] %s\n' "$NOW" "$LEVEL" "$SUMMARY" >> "$JOURNAL"

# 3) commit
git add -A
kst_secret_guard
MSG="checkpoint: ${SUMMARY}"
[ -n "$MILESTONE" ] && MSG="checkpoint(milestone ${MILESTONE}): ${SUMMARY}"
[ -n "$RELEASE" ] && MSG="release v${RELEASE}: ${SUMMARY}"
if git diff --cached --quiet; then
  echo "[kestrel] nothing new to commit; verifying push state only"
else
  args=(-m "$MSG")
  if [ ${#TRAILERS[@]} -gt 0 ]; then
    args+=(-m "$(printf '%s\n' "${TRAILERS[@]}")")
  fi
  git commit -q "${args[@]}" || { echo "[kestrel] commit FAILED" >&2; exit 1; }
fi

# 4) tags (L2/L3)
TAG=""
if [ -n "$MILESTONE" ]; then TAG="cp/${MILESTONE}-$(date -u +%Y%m%d-%H%M%S)"; fi
if [ -n "$RELEASE" ]; then TAG="v${RELEASE}"; fi
[ -n "$TAG" ] && git tag -f "$TAG" >/dev/null 2>&1

# 5) push with retries, then verify the remote really has our commit
if kst_push "$BR" 5; then
  [ -n "$TAG" ] && { timeout 40 git push -q -f origin "refs/tags/$TAG" 2>/dev/null || echo "[kestrel] tag push failed (non-fatal)" >&2; }
  LOCAL="$(git rev-parse HEAD)"; REMOTE="$(kst_remote_sha "$BR")"
  if [ "$LOCAL" = "$REMOTE" ]; then
    echo "CHECKPOINT OK  [$LEVEL] $(git rev-parse --short HEAD) is on origin/$BR${TAG:+  tag=$TAG}"
    date +%s > "$(git rev-parse --git-dir)/kestrel-autosave.stamp"
    exit 0
  fi
  echo "CHECKPOINT WARN push returned OK but origin/$BR=$REMOTE != local=$LOCAL" >&2
  exit 1
fi
echo "CHECKPOINT LOCAL-ONLY  [$LEVEL] committed $(git rev-parse --short HEAD) but PUSH FAILED - retry: git push -u origin $BR" >&2
exit 1
