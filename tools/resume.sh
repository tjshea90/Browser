#!/usr/bin/env bash
# resume.sh - the cold-start handoff. Runs from the SessionStart hook (and by hand: `bash tools/resume.sh`, or `bash bootstrap.sh`,
# if a session did not get the briefing).
#
# A session can die at any point with no warning. The next one - another account, another device, hours later - opens this repo COLD.
# Everything it needs must already be on disk and must arrive without the owner explaining anything. This prints: whether the
# checkout is current with GitHub, whether the last session was cut off mid-step, any other branch holding unlanded work, a release
# GitHub built that nobody recorded, then CHECKPOINT.md, the OPEN items of TASKS.md, the INBOX/OUTBOX tails, and the rules.
#
# BUDGET: Claude Code caps hook output at 10,000 characters (more is cut to a 2,000-char preview - the briefing is silently lost).
# tools/test_handoff.sh checks the size. Keep this short; narrative belongs in CHECKPOINT.md / TASKS.md / HANDOFF.md / CLAUDE.md.
set -uo pipefail
D="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"; cd "$D" || exit 0
. tools/appinfo.sh 2>/dev/null || true

TEXT_MODE=0
for a in "$@"; do [ "$a" = "--text" ] && TEXT_MODE=1; done

# Self-repair the safety net (idempotent, silent when correct).
bash tools/install-hooks.sh --quiet >/dev/null 2>&1 || true

BRIEF="$(
  echo "=== RESUMING KESTREL BROWSER - light private browser for an Android 10 Go tablet (Kestrel = system WebView, Kestrel G = Firefox GeckoView). A previous session may have been cut off. ==="
  echo "=== Do NOT re-plan or re-read finished work: continue from 'Do this next' below. ==="
  echo

  if [ -e .git ]; then
    find .git -name '*.lock' -mmin +2 -delete 2>/dev/null || true
    timeout 25 git fetch -q --tags origin >/dev/null 2>&1 || echo "  NOTE  could not reach GitHub - working from the local checkout only."

    DIRTY="$(git status --porcelain 2>/dev/null)"
    CURBR="$(git branch --show-current 2>/dev/null || true)"
    UP="$(git rev-parse --abbrev-ref '@{u}' 2>/dev/null || true)"
    [ -z "$UP" ] && [ -n "$CURBR" ] && UP="origin/$CURBR"
    BEHIND=0; AHEAD=0
    if [ -n "$UP" ] && git rev-parse --verify -q "$UP" >/dev/null 2>&1; then
      BEHIND="$(git rev-list --count "HEAD..$UP" 2>/dev/null || echo 0)"
      AHEAD="$(git rev-list --count "$UP..HEAD" 2>/dev/null || echo 0)"
    elif [ -n "$CURBR" ]; then
      echo "  WARN  branch $CURBR is not on GitHub yet - nothing on it is backed up until the next checkpoint pushes it."
    fi
    # Fast-forward only, from a clean tree (a merge could conflict on a half-finished change).
    if [ -z "$DIRTY" ] && [ "$BEHIND" -gt 0 ] && [ "$AHEAD" -eq 0 ]; then
      timeout 25 git merge -q --ff-only "$UP" >/dev/null 2>&1 \
        && echo "  OK    pulled $BEHIND newer commit(s) from GitHub - checkout is current."
    elif [ "$BEHIND" -gt 0 ] && [ "$AHEAD" -gt 0 ]; then
      echo "  WARN  branch DIVERGED from GitHub ($AHEAD local, $BEHIND remote) - two sessions at once? Reconcile before working."
    elif [ "$BEHIND" -gt 0 ]; then
      echo "  WARN  $BEHIND commit(s) behind GitHub and the tree is dirty - reconcile before new work."
    fi
    [ "$AHEAD" -gt 0 ] && echo "  NOTE  $AHEAD commit(s) not yet pushed - 'git push origin HEAD'."

    # OTHER SESSIONS (many Claude sessions/accounts share this repo). tools/sync.sh merges every other branch that has commits this one lacks:
    # a cut-off session's WHOLE tip (including the half-finished change its last autosave captured), an active session's last deliberate
    # checkpoint only. A conflict is aborted and reported (nothing lost).
    bash tools/sync.sh 2>&1 | cut -c1-260
    # The parallel lanes: each session keeps its own card in .checkpoints/<branch>.md (newest first, last 3 days, at most 4).
    ME="$(printf '%s' "$CURBR" | tr -c 'A-Za-z0-9._\n-' '_')"
    for C in $(ls -t .checkpoints/*.md 2>/dev/null | head -6); do
      LB="$(basename "$C" .md)"; [ "$LB" = "$ME" ] && continue
      LW="$(sed -n 's/^\*\*Written:\*\* \([0-9T:Z-]*\).*/\1/p' "$C" | head -1)"
      LAGE=$(( ( $(date +%s) - $(date -d "${LW:-1970-01-01T00:00:00Z}" +%s 2>/dev/null || echo 0) ) / 60 ))
      [ "$LAGE" -gt 4320 ] && continue
      LNX="$(awk '/^## Do this next/{f=1;next} /^## /{f=0} f&&NF{print;exit}' "$C" | cut -c1-150)"
      echo "  lane $LB ($LAGE min ago): $LNX"
    done
    # claimed TASKS items: [~] ... {branch time}
    CL="$(grep -n '^\s*- \[~\]' TASKS.md 2>/dev/null | cut -c1-150)"
    [ -n "$CL" ] && { echo "  CLAIMED by sessions (a claim whose branch has no commit for 90 min is dead: take it over with tools/claim.sh <ID>):"; printf '%s\n' "$CL" | sed 's/^/    /'; }

    LASTAUTO="$(git log -1 --format=%cr --grep='^auto-checkpoint:' 2>/dev/null || true)"
    if [ -n "$LASTAUTO" ]; then
      echo "  OK    auto-checkpointing is live ($(git log --oneline --grep='^auto-checkpoint:' | wc -l | tr -d ' ') so far, latest $LASTAUTO)."
    else
      echo "  NOTE  no auto-checkpoint commits yet. If edits go by and none appear over a few tool calls,"
      echo "        the hooks are not firing: run 'bash tools/install-hooks.sh', say so, and use 'bash tools/ckpt.sh' by hand."
    fi

    # A release GitHub built that nobody recorded would let the next build reuse a versionCode already on the tablet
    # (Android then refuses the install). GitHub creates the tags (a push of one from here is refused), `git fetch --tags` brings them.
    for T in $(git tag -l 'kestrel-v*' 'kestrel-g-v*' 2>/dev/null | head -20); do
      grep -q "^| ${T} |" BUILDLOG.md 2>/dev/null || echo "  !!    $T WAS RELEASED BUT NEVER RECORDED - run:  bash tools/record-release.sh $T \"what it changed\""
    done

    # Cut off mid-change? Commits after the newest deliberate checkpoint = an unfinished step.
    LASTCKPT="$(git log -1 --format=%H --extended-regexp --grep='^(ckpt [0-9]+:|ship )' 2>/dev/null || true)"
    if [ -n "$LASTCKPT" ]; then
      SINCE="$(git rev-list --count "$LASTCKPT"..HEAD 2>/dev/null || echo 0)"
      if [ "$SINCE" -gt 0 ]; then
        echo
        echo "  !!    THE LAST SESSION WAS INTERRUPTED MID-CHANGE: $SINCE automatic checkpoint(s) after the last"
        echo "        deliberate one. CHECKPOINT.md below describes the state BEFORE them. Read the change first:"
        echo "          git diff $(git rev-parse --short "$LASTCKPT")..HEAD"
        git diff --stat "$LASTCKPT"..HEAD 2>/dev/null | tail -12 | sed 's/^/          /'
        echo "        Finish it, then checkpoint with tools/ckpt.sh before starting anything new."
      fi
    fi
    if [ -n "$DIRTY" ]; then
      echo
      echo "  !!    UNCOMMITTED WORK IS PRESENT (not even autosave got to it). 'git diff' is what was in flight:"
      printf '%s\n' "$DIRTY" | head -15 | sed 's/^/          /'
    fi
    echo
  fi

  echo "-- toolchain / builds --"
  command -v java >/dev/null 2>&1 && echo "  OK    java" || echo "  WARN  no java - gradlew cannot run"
  AH="${ANDROID_HOME:-/opt/android-sdk}"
  [ -d "$AH/platforms" ] && echo "  OK    android sdk: $(ls "$AH/platforms" | tr '\n' ' ')" || echo "  note  no android sdk here - APKs are built by GitHub Actions anyway (android.yml)"
  echo "  APKs: GitHub Actions builds + signs them, Releases hold them (tags kestrel-v<ver> / kestrel-g-v<ver>). No emulator/KVM in the cloud sandbox."
  echo "  last recorded: web code $(app_last_code web 2>/dev/null || echo ?), gecko code $(app_last_code gecko 2>/dev/null || echo ?) (BUILDLOG.md); now in Gradle: web $(app_vname web 2>/dev/null) ($(app_vcode web 2>/dev/null)), gecko $(app_vname gecko 2>/dev/null) ($(app_vcode gecko 2>/dev/null))"
  echo

  if [ -f CHECKPOINT.md ]; then
    echo "######## CHECKPOINT.md - where the last session stopped. START HERE. ########"
    if [ "$(wc -c < CHECKPOINT.md)" -gt 4800 ]; then head -c 4800 CHECKPOINT.md; echo; echo "  [... cut here to fit the hook cap: the full text is in CHECKPOINT.md]"; else cat CHECKPOINT.md; fi
    echo
  fi
  if [ -f TASKS.md ]; then
    echo "######## TASKS.md - the 18 NEWEST open items (line numbers; older ones: grep -n '\- \[ \]' TASKS.md | head -40) ########"
    grep -n '^\s*- \[ \]' TASKS.md | cut -c1-130 | tail -18     # newest last: new requests are appended
    echo "  ($(grep -c '^\s*- \[x\]' TASKS.md) done - not shown)"
    echo
  fi
  if [ -f INBOX.md ]; then
    echo "######## INBOX.md tail - the owner's latest raw messages; anything TASKS.md lacks is an unwritten request ########"
    tail -c 700 INBOX.md
    echo
  fi
  if [ -f OUTBOX.md ]; then   # what Claude LAST told the owner, saved by tools/capture_outbox.sh on every Stop
    echo "######## OUTBOX.md tail - what Claude last told the owner (answers, findings, decisions); the whole text is in the file ########"
    tail -c 550 OUTBOX.md | grep -v '^<!-- sha:'
    echo
  fi
  echo "######## RULES (full text: CLAUDE.md; NEW to this project with no context: HANDOFF.md, BRIEF.md) ########"
  cat <<'SHORT'
- Checkpoint after EVERY step:  bash tools/ckpt.sh "did" "next"   (no gate; "next" is what no diff can recover).
- New request from the owner -> TASKS.md as [ ] boxes in their words FIRST, ckpt, then code.
- GitHub Actions builds EVERY APK ("Claude codes, GitHub makes the APK"): bash ship.sh <web|gecko> "note", then dispatch android.yml
  (mcp__github__actions_run_trigger run_workflow, ref = this branch, inputs app + full_build=true), then bash tools/record-release.sh <tag> "note".
- Never commit a key (signing/dev is the one intentional PUBLIC dev key). Never curl api.github.com (a hook blocks it): use the GitHub MCP tools.
- Device/engine/hotspot rules and the architecture map: CLAUDE.md, BRIEF.md. No emulator/KVM here: say honestly what was and was not run.
- AUTO-RESUME: on a long job keep ONE heartbeat (id in the latest ckpt 'next'); a cut-off notice with a reset time -> send_later at reset+2 min.
SHORT
)"

if [ "$TEXT_MODE" -eq 1 ]; then
  printf '%s\n' "$BRIEF"
elif command -v python3 >/dev/null 2>&1; then
  printf '%s' "$BRIEF" | python3 -I -c '
import json, sys
print(json.dumps({"hookSpecificOutput": {"hookEventName": "SessionStart", "additionalContext": sys.stdin.read()}}))
' 2>/dev/null || printf '%s\n' "$BRIEF"
else
  printf '%s\n' "$BRIEF"
fi
exit 0
