#!/usr/bin/env bash
# capture_inbox.sh — UserPromptSubmit hook. RUNG 0 of the save ladder.
#
# Appends the message the owner just sent to INBOX.md, verbatim, then commits+pushes it via
# autosave.sh — BEFORE Claude reads a file or makes a plan. A usage cap does not wait for a
# good moment: a session can spend its whole budget researching a request, be cut off before
# writing it into TASKS.md, and no edit hook ever fires (research trips none of them). The next
# session would open cold with no sign the request existed.
# This closes that gap at the one point that can never be skipped: the message arriving.
#
# Not a substitute for TASKS.md (a raw message is not a plan) — it makes a forgotten TASKS.md
# write recoverable instead of fatal. resume.sh prints the INBOX tail every session.
#
# MUST NEVER BLOCK THE PROMPT: always exit 0, never emit "decision":"block".
# Key-shaped strings are redacted before they touch disk (this may be a public repo, and
# tools/secretscan.sh would otherwise refuse to commit and wedge every later checkpoint).
set -uo pipefail
D="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"; cd "$D" || exit 0
[ -e .git ] || exit 0

INPUT="$(cat)"
[ -z "$INPUT" ] && exit 0
command -v python3 >/dev/null 2>&1 || exit 0

PROMPT="$(printf '%s' "$INPUT" | python3 -I -c '
import json, re, sys
try:
    p = json.load(sys.stdin).get("prompt", "")
    p = p if isinstance(p, str) else ""
except Exception:
    p = ""
pats = [r"sk-ant-[A-Za-z0-9_-]{20,}", r"sk-[A-Za-z0-9_-]{32,}", r"gh[pousr]_[A-Za-z0-9]{30,}",
        r"github_pat_[A-Za-z0-9_]{30,}", r"AIza[0-9A-Za-z_-]{30,}", r"AKIA[0-9A-Z]{16}",
        r"xox[baprs]-[A-Za-z0-9-]{10,}",
        r"-----BEGIN [A-Z ]*PRIVATE KEY-----[\s\S]*?(-----END [A-Z ]*PRIVATE KEY-----|$)"]
for pat in pats:
    p = re.sub(pat, "[REDACTED-KEY]", p)
sys.stdout.write(p)
' 2>/dev/null)"
[ -z "$PROMPT" ] && exit 0

# The harness'"'"'s own notices arrive as prompts too; they are not the owner'"'"'s words.
case "$PROMPT" in "<task-notification>"*) exit 0 ;; esac

[ -f INBOX.md ] || printf '# INBOX — every message the owner sends, verbatim (written by tools/capture_inbox.sh)\n' > INBOX.md
{
  echo ""
  echo "## $(date -u +%Y-%m-%dT%H:%M:%SZ)"
  echo '```'
  printf '%s\n' "$PROMPT"
  echo '```'
} >> INBOX.md

bash tools/autosave.sh --text >/dev/null 2>&1 || true
exit 0
