#!/usr/bin/env bash
# private.sh - PRIV1: the owner's raw messages (INBOX.md), Claude's replies (OUTBOX.md) and the old handoff archives (.handoff/) live in the PRIVATE repo
# tjshea90/Kestrel-Private, NOT in this public one. Here they are symlinks into a clone of it, kept out of git by .git/info/exclude, so every script that
# writes "INBOX.md" works unchanged and `git add -A` can never publish them.
#   bash tools/private.sh setup   idempotent: exclude + links (run at session start by resume.sh and by every autosave)
#   bash tools/private.sh push    commit + push the private clone (run by autosave)
# The clone: $KESTREL_PRIVATE, else ../kestrel-private or ../Kestrel-Private (any letter case, see find_clone). Missing clone = LOUD warning, and the three paths stay plain local files that are still excluded
# from git (nothing leaks into the public repo, but nothing is saved either until the clone exists):
#   mcp__claude-code-remote__add_repo tjshea90/Kestrel-Private (access push), then git clone --depth 1 https://github.com/tjshea90/kestrel-private ../kestrel-private
D="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"; cd "$D" || exit 1
# LD3d: find the clone whatever its letter case. The cloud harness clones the repo as ../Kestrel-Private (capital K and P) and Linux is case sensitive, so a fixed
# "../kestrel-private" was never found there: INBOX/OUTBOX were not saved and diag_digest.py saw no diagnostics. Order: $KESTREL_PRIVATE, kestrel-private, Kestrel-Private, any case.
find_clone() {
  local parent n d b; parent="$(dirname "$D")"
  [ -n "$KESTREL_PRIVATE" ] && { echo "$KESTREL_PRIVATE"; return; }
  for n in kestrel-private Kestrel-Private; do [ -d "$parent/$n/.git" ] && { echo "$parent/$n"; return; }; done
  for d in "$parent"/*; do b="$(basename "$d")"; [ "${b,,}" = "kestrel-private" ] && [ -d "$d/.git" ] && { echo "$d"; return; }; done
  echo "$parent/kestrel-private"
}
P="$(find_clone)"
[ "${1:-}" = "path" ] && { echo "$P"; exit 0; }       # bash tools/private.sh path -> where the clone is (used by resume.sh and other tools)
PATHS="INBOX.md OUTBOX.md .handoff"
[ -f tools/PRIVATE_ON ] || exit 0          # the switch: the file is committed once the owner's private repo is in use

setup() {
  local ex; ex="$(git rev-parse --git-path info/exclude)"; mkdir -p "$(dirname "$ex")"
  for f in $PATHS; do grep -qxF "/$f" "$ex" 2>/dev/null || echo "/$f" >> "$ex"; done
  if [ ! -d "$P/.git" ]; then
    echo "PRIVATE REPO MISSING: $P - INBOX/OUTBOX/.handoff are NOT being saved anywhere (not in the public repo either). Fix: add_repo tjshea90/Kestrel-Private (push), then: git clone --depth 1 https://github.com/tjshea90/kestrel-private $P" >&2
    return 0
  fi
  for f in $PATHS; do
    if [ -L "$f" ]; then continue; fi
    if [ -e "$f" ]; then                       # a plain local file made while the clone was missing: keep its text in the private repo, then link
      if [ -d "$f" ]; then mkdir -p "$P/$f"; cp -an "$f/." "$P/$f/" 2>/dev/null; rm -rf "$f"
      else cat "$f" >> "$P/$f"; rm -f "$f"; fi
    fi
    [ -e "$P/$f" ] || { case "$f" in .handoff) mkdir -p "$P/$f" ;; *) : > "$P/$f" ;; esac; }
    ln -s "$P/$f" "$f"
  done
}

push() {
  [ -d "$P/.git" ] || return 0
  ( cd "$P" || exit 0
    git add -A >/dev/null 2>&1
    git diff --cached --quiet && exit 0
    git -c user.name=kestrel-autosave -c user.email=autosave@localhost commit -q -m "private autosave $(date -u +%Y-%m-%dT%H:%M:%SZ)" >/dev/null 2>&1
    git push -q origin HEAD >/dev/null 2>&1 || { git pull -q --rebase origin "$(git branch --show-current)" >/dev/null 2>&1; git push -q origin HEAD >/dev/null 2>&1 || echo "PRIVATE PUSH FAILED: messages are only in this container ($P)" >&2; }
  )
}

case "${1:-setup}" in setup) setup ;; push) push ;; esac
