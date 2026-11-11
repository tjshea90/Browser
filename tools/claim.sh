#!/usr/bin/env bash
# claim.sh - tell the other sessions what you are working on, so two Claude sessions/accounts never do the same TASKS.md item.
#
#   bash tools/claim.sh A3                      mark  - [ ] A3 ...  as  - [~] A3 ... {branch time}   (committed and pushed at once)
#   bash tools/claim.sh A3 --release            give it back  ([~] -> [ ])
#   bash tools/claim.sh A3 --done "evidence"    tick it  ([x]) with the test / run id that proves it
# Rules: claim an item BEFORE starting anything that takes more than one checkpoint. An item claimed by ANOTHER branch whose newest commit is
# younger than 90 minutes is refused (that session is alive); an older claim is dead and is simply taken over (add --force to override).
# tools/resume.sh lists every [~] line at session start.
set -uo pipefail
D="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"; cd "$D" || exit 1
ID="${1:-}"; MODE="claim"; EVID=""; FORCE=0
[ -n "$ID" ] || { echo "usage: bash tools/claim.sh <ID> [--release | --done \"evidence\"] [--force]"; exit 2; }
shift
while [ $# -gt 0 ]; do
  case "$1" in
    --release) MODE="release" ;;
    --done) MODE="done"; EVID="${2:-}"; shift ;;
    --force) FORCE=1 ;;
  esac
  shift
done
BR="$(git branch --show-current 2>/dev/null || echo unknown)"; NOW="$(date -u +%Y-%m-%dT%H:%MZ)"
export CLAIM_ID="$ID" CLAIM_MODE="$MODE" CLAIM_EVID="$EVID" CLAIM_BR="$BR" CLAIM_NOW="$NOW" CLAIM_FORCE="$FORCE"

RES="$(python3 -I - <<'PY'
import os, re, subprocess, sys, time
i, mode, evid, br, now, force = (os.environ[k] for k in ("CLAIM_ID", "CLAIM_MODE", "CLAIM_EVID", "CLAIM_BR", "CLAIM_NOW", "CLAIM_FORCE"))
path = "TASKS.md"
lines = open(path, encoding="utf-8").read().split("\n")
pat = re.compile(r"^(\s*- )\[( |~|x)\] (" + re.escape(i) + r")(\s.*)?$")
claim_re = re.compile(r"\s*\{([^{}\s]+) (\d{4}-\d\d-\d\dT\d\d:\d\dZ)\}\s*$")
for n, ln in enumerate(lines):
    m = pat.match(ln)
    if not m: continue
    pre, state, ident, rest = m.group(1), m.group(2), m.group(3), m.group(4) or ""
    cm = claim_re.search(rest)
    if mode == "claim":
        if state == "x": print("DONE-ALREADY"); sys.exit(0)
        if state == "~" and cm and cm.group(1) != br and force != "1":
            other = cm.group(1)
            try:
                age = time.time() - int(subprocess.run(["git", "log", "-1", "--format=%ct", "origin/" + other], capture_output=True, text=True).stdout.strip() or "0")
            except Exception:
                age = 1e9
            if age < 5400: print("TAKEN %s %d" % (other, age // 60)); sys.exit(0)
        rest = claim_re.sub("", rest)
        lines[n] = "%s[~] %s%s {%s %s}" % (pre, ident, rest, br, now)
    elif mode == "release":
        lines[n] = "%s[ ] %s%s" % (pre, ident, claim_re.sub("", rest))
    else:
        lines[n] = "%s[x] %s%s%s" % (pre, ident, claim_re.sub("", rest), (" - " + evid) if evid else "")
    open(path, "w", encoding="utf-8").write("\n".join(lines))
    print("OK"); sys.exit(0)
print("NOTFOUND")
PY
)"
case "$RES" in
  OK) echo "  OK    $ID: $MODE recorded in TASKS.md"; git add TASKS.md >/dev/null 2>&1; bash tools/autosave.sh --text </dev/null >/dev/null 2>&1 || true ;;
  TAKEN*) set -- $RES; echo "  REFUSED  $ID is claimed by branch $2, whose newest commit is $3 min old (alive). Pick another item, or use --force."; exit 1 ;;
  DONE-ALREADY) echo "  note  $ID is already ticked [x]"; ;;
  *) echo "  FAIL  no open item '$ID' in TASKS.md (lines look like: - [ ] $ID text)"; exit 1 ;;
esac
