#!/usr/bin/env bash
# merge-newest.sh - git MERGE DRIVER for CHECKPOINT.md: when two sessions both rewrote the card, keep the one that was WRITTEN last.
# Nothing is lost: every session also keeps its own card in .checkpoints/<branch>.md (tools/ckpt.sh), and those files never collide.
# Wired by .gitattributes (CHECKPOINT.md merge=newest) and `git config merge.newest.driver` (tools/sync.sh and tools/ckpt.sh set it on every run).
#   usage (by git): merge-newest.sh %O %A %B      %A is the file git keeps: this script overwrites it with the winner and exits 0.
A="${2:-}"; B="${3:-}"
[ -f "$A" ] && [ -f "$B" ] || exit 1
ts() { sed -n 's/^\*\*Written:\*\* \([0-9T:Z-]*\).*/\1/p' "$1" | head -1; }
ta="$(ts "$A")"; tb="$(ts "$B")"
if [ -n "$tb" ] && { [ -z "$ta" ] || [[ "$tb" > "$ta" ]]; }; then cp "$B" "$A"; fi
exit 0
