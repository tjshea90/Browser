#!/usr/bin/env bash
# posttool.sh - PostToolUse hook for EVERY tool (matcher "*"): the light layer around autosave.sh.
#
# WHY: autosave.sh is registered for Edit|Write|NotebookEdit|Bash only. Claude Code's matcher is a list of EXACT tool
# names, so every GitHub MCP call, Agent, Read, Grep and Glob would skip the whole save ladder: findings written between
# MCP calls (a CI triage, a research answer) never reached OUTBOX.md and died with the session. This runs after ANY
# other tool and does the cheap thing: capture Claude's words (200+ characters) into OUTBOX.md and, only if that wrote
# something, commit + push it through autosave.sh (no git work on a plain read).
# ALWAYS exits 0, never blocks, prints nothing (whatever a hook prints is read as hook JSON).
set -uo pipefail
D="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"; cd "$D" || exit 0
[ -e .git ] || exit 0
IN=""; [ ! -t 0 ] && IN="$(timeout 2 cat 2>/dev/null || true)"

TOOL="$(printf '%s' "$IN" | grep -o '"tool_name": *"[^"]*"' | head -1 | sed 's/.*: *"\(.*\)"/\1/')"
case "$TOOL" in
  Edit|Write|NotebookEdit|Bash|"") ;;         # autosave.sh handles these (and an unparsable payload is not worth guessing at)
  *)
    S0="$(stat -c %s OUTBOX.md 2>/dev/null || echo 0)"
    printf '%s' "$IN" | OUTBOX_MIN=200 bash tools/capture_outbox.sh >/dev/null 2>&1 || true
    S1="$(stat -c %s OUTBOX.md 2>/dev/null || echo 0)"
    [ "$S0" != "$S1" ] && { bash tools/autosave.sh --text </dev/null >/dev/null 2>&1 || true; }
    ;;
esac
exit 0
