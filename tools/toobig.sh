#!/usr/bin/env bash
# toobig.sh — fires when the conversation has grown enough to auto-compact.
#
# WHY THIS EXISTS
# ---------------
# Every turn resends the whole conversation, and prompt caching only makes
# that cheap while the cache is WARM. Come back to a big session hours later
# and the cache has expired: the entire conversation is re-read at full price
# before a single new word is written.
#
# Compaction is not the escape either. It REWRITES the earlier messages, which
# is itself a cold start on the rewritten portion, and it summarises away
# detail that CHECKPOINT.md holds losslessly on disk anyway.
#
# The cheap move is to stop and start a fresh session: tools/resume.sh rebuilds
# everything a session needs from GitHub in well under a hundred lines, versus
# re-reading a conversation that by this point is far larger than that.
#
# So: guarantee everything is saved, then say so plainly.
#
# 2026-11-09 CORRECTION: with a 1M window and compaction at ~392K the cheap move is to compact and carry on, not to abandon the session; the
# message below no longer says to start a new session. (The old advice also hid the real bug: a plugin vetoed every compaction.)
set -uo pipefail
D="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"; cd "$D" || exit 0

TEXT_MODE=0
for a in "$@"; do [ "$a" = "--text" ] && TEXT_MODE=1; done

bash tools/autosave.sh --text >/dev/null 2>&1 || true

# Was '@{u}'..HEAD, which fails outright on a Claude-Code-assigned
# `claude/<id>` branch (no upstream configured) and printed a literal "?" —
# so the one line that is supposed to tell you whether your work is safe read
# "WARNING: ? commit(s) are not pushed". tools/unpushed.sh resolves the remote
# branch the way push.sh does.
UNPUSHED="$(bash tools/unpushed.sh 2>/dev/null || echo '?')"
if [ "$UNPUSHED" = "0" ]; then
  STATE="Everything is committed and pushed to GitHub."
else
  STATE="WARNING: $UNPUSHED commit(s) are not pushed yet — run: git push origin HEAD"
fi

# Compaction is normal: the session CONTINUES and the briefing is re-injected by the SessionStart hook, so this hook never tells
# the owner to start a new session. It only guarantees that everything is committed and pushed first.
MSG="Auto-compacting now (the conversation is being summarised so the session can go on). $STATE  The briefing (CHECKPOINT.md, open tasks, rules) is re-read automatically afterwards, and CHECKPOINT.md / TASKS.md hold everything losslessly, so nothing is lost. If you want an even cleaner break, run  bash tools/ckpt.sh \"what I just did\" \"what comes next\"  first."
if [ "$TEXT_MODE" -eq 1 ]; then
  printf '%s\n' "$MSG"
else
  printf '%s' "$MSG" | python3 -c 'import json,sys; print(json.dumps({"systemMessage": sys.stdin.read()}))' 2>/dev/null \
    || printf '%s\n' "$MSG"
fi
exit 0
