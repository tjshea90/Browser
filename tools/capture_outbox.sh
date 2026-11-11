#!/usr/bin/env bash
# capture_outbox.sh - RUNG 0b of the save ladder, the mirror of capture_inbox.sh.
#
# capture_inbox.sh saves what the owner SAYS (every prompt, verbatim, before any work). Nothing saved what
# Claude TOLD the owner: findings, answers and decisions that only ever lived in the chat were lost when a
# session died ("Prompt is too long", a usage cap) unless Claude had also written them into TASKS.md
# by hand. This appends Claude's LAST reply of every turn to OUTBOX.md (quoted, key-redacted, long
# replies cut in the middle, the working file bounded: git history keeps everything).
#
# Called by autosave.sh on the Stop hook (autosave then commits and pushes it in the same step).
# Needs the hook JSON on stdin (transcript_path). resume.sh prints the OUTBOX tail every session.
#
# MUST NEVER BLOCK: always exit 0, no stdout (a hook's stdout is read as JSON).
set -uo pipefail
D="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"; cd "$D" || exit 0
[ -e .git ] || exit 0

INPUT="$(cat)"
[ -z "$INPUT" ] && exit 0
command -v python3 >/dev/null 2>&1 || exit 0

printf '%s' "$INPUT" | python3 -I -c '
import json, re, sys, os, hashlib, datetime
OUT = "OUTBOX.md"; MAXKEEP = 200000; CAP_HEAD = 3000; CAP_TAIL = 900
HEADER = "# OUTBOX - what Claude told the owner (the last reply of every turn), newest last. Written by tools/capture_outbox.sh on the Stop hook; key-shaped strings are redacted and long replies cut in the middle. The mirror of INBOX.md.\n"
try:
    d = json.load(sys.stdin)
    tp = d.get("transcript_path", "")
    if not tp or not os.path.isfile(tp):
        sys.exit(0)
    size = os.path.getsize(tp)
    with open(tp, "rb") as f:
        f.seek(max(0, size - 800000))
        raw = f.read().decode("utf-8", "replace")
    lines = raw.splitlines()
    if size > 800000:
        lines = lines[1:]
    text = ""
    for ln in reversed(lines):
        try:
            o = json.loads(ln)
        except Exception:
            continue
        if not isinstance(o, dict) or o.get("isSidechain"):
            continue
        m = o.get("message")
        if not isinstance(m, dict) or (o.get("type") != "assistant" and m.get("role") != "assistant"):
            continue
        c = m.get("content")
        if isinstance(c, list):
            t = "\n".join(b.get("text", "") for b in c if isinstance(b, dict) and b.get("type") == "text").strip()
        elif isinstance(c, str):
            t = c.strip()
        else:
            t = ""
        if t:
            text = t
            break
    if not text:
        sys.exit(0)
    # a tool-call hook (OUTBOX_MIN=200) records only SUBSTANTIAL new text, not the one-line narration between tool calls; the Stop hook records whatever the last reply was
    if len(text) < int(os.environ.get("OUTBOX_MIN", "0") or 0):
        sys.exit(0)
    pats = [r"sk-ant-[A-Za-z0-9_-]{20,}", r"sk-[A-Za-z0-9_-]{32,}", r"gh[pousr]_[A-Za-z0-9]{30,}",
            r"github_pat_[A-Za-z0-9_]{30,}", r"AIza[0-9A-Za-z_-]{30,}", r"AKIA[0-9A-Z]{16}",
            r"xox[baprs]-[A-Za-z0-9-]{10,}",
            r"-----BEGIN [A-Z ]*PRIVATE KEY-----[\s\S]*?(-----END [A-Z ]*PRIVATE KEY-----|$)"]
    for pat in pats:
        text = re.sub(pat, "[REDACTED-KEY]", text)
    if len(text) > CAP_HEAD + CAP_TAIL + 100:
        text = text[:CAP_HEAD] + "\n[... %d characters omitted ...]\n" % (len(text) - CAP_HEAD - CAP_TAIL) + text[-CAP_TAIL:]
    sha = hashlib.sha1(text.encode("utf-8")).hexdigest()[:12]
    prev = ""
    if os.path.isfile(OUT):
        with open(OUT, "rb") as f:
            f.seek(max(0, os.path.getsize(OUT) - 400))
            prev = f.read().decode("utf-8", "replace")
    if ("sha:" + sha) in prev:
        sys.exit(0)
    ts = datetime.datetime.utcnow().strftime("%Y-%m-%dT%H:%M:%SZ")
    body = "\n".join(("> " + l) if l.strip() else ">" for l in text.splitlines())
    entry = "\n## %s\n%s\n<!-- sha:%s -->\n" % (ts, body, sha)
    if not os.path.isfile(OUT):
        entry = HEADER + entry
    with open(OUT, "a", encoding="utf-8") as f:
        f.write(entry)
    if os.path.getsize(OUT) > MAXKEEP:
        with open(OUT, "r", encoding="utf-8", errors="replace") as f:
            whole = f.read()
        cut = whole[-int(MAXKEEP * 0.75):]
        i = cut.find("\n## ")
        cut = cut[i:] if i >= 0 else cut
        with open(OUT, "w", encoding="utf-8") as f:
            f.write(HEADER.rstrip("\n") + " (older entries: git history)\n" + cut)
except Exception:
    pass
' >/dev/null 2>&1
exit 0
