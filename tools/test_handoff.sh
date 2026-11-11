#!/usr/bin/env bash
# test_handoff.sh - hermetic self-test of the handoff/checkpoint/release tooling itself (~10 seconds).
# Runs as a fast check inside ckpt.sh, as a ship.sh gate, and as the first gate of the android.yml workflow. It builds a throwaway
# clone with a fake remote and never touches the real settings file or the real remote.
#   bash tools/test_handoff.sh        # exit 0 = all green
set -uo pipefail
D="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
T="$(mktemp -d)"; trap 'rm -rf "$T"' EXIT
PASS=0; FAIL=0
ok(){ PASS=$((PASS+1)); }
bad(){ FAIL=$((FAIL+1)); echo "  FAIL  $1"; }
check(){ if eval "$2" >/dev/null 2>&1; then ok; else bad "$1"; fi; }
unset CLAUDE_PROJECT_DIR KESTREL_ALLOW_MAIN

# ---------------------------------------------------------------- workflow minute rules (the owner's rule on all repos: minutes are scarce)
for W in "$D"/.github/workflows/*.yml; do
  [ -f "$W" ] || continue
  # No `push:` trigger that is not tags-only (autosave pushes after every tool call).
  if python3 -I - "$W" <<'PY'
import sys, re
t = open(sys.argv[1]).read()
m = re.search(r'^on:\n((?:[ \t]+.*\n|\n)+)', t, re.M)
block = m.group(1) if m else ''
bad = False
lines = block.splitlines()
for i, l in enumerate(lines):
    if re.match(r'^  push:\s*$', l):
        nxt = lines[i+1] if i+1 < len(lines) else ''
        if 'branches' in nxt or 'paths' in nxt or not nxt.strip().startswith('tags'):
            bad = True
    if re.match(r'^  push:\s*\{', l):
        bad = True
sys.exit(1 if bad else 0)
PY
  then ok; else bad "$(basename "$W") has a non-tag push trigger (would burn Actions minutes)"; fi
done

# A workflow file that does not parse silently LOSES its triggers (GitHub then says "Workflow does not have 'workflow_dispatch' trigger"): a colon inside a step name does it.
if python3 -I -c "import yaml" >/dev/null 2>&1; then
  for W in "$D"/.github/workflows/*.yml; do
    [ -f "$W" ] || continue
    if python3 -I - "$W" <<'PY'
import sys, yaml
d = yaml.safe_load(open(sys.argv[1]))
on = d.get(True, d.get('on')) or {}
name = sys.argv[1].rsplit('/', 1)[-1]
if name == 'android.yml':
    wd = on.get('workflow_dispatch') if isinstance(on, dict) else None
    if not isinstance(wd, dict) or 'app' not in (wd.get('inputs') or {}) or 'full_build' not in (wd.get('inputs') or {}):
        sys.exit(1)
sys.exit(0)
PY
    then ok; else bad "$(basename "$W") does not parse as YAML or lost its workflow_dispatch inputs (a colon inside a step name does that)"; fi
  done
fi
check "android.yml publishes with 'gh release create --target' (a tag PUSH from the sandbox is refused with 403)" 'grep -q "gh release create" "$D/.github/workflows/android.yml" && grep -q -e "--target" "$D/.github/workflows/android.yml"'
check "android.yml can write the Release (permissions: contents: write)" 'grep -q "contents: write" "$D/.github/workflows/android.yml"'

# ---------------------------------------------------------------- the repo's own wiring
check "every tools/*.sh and ship.sh passes bash -n" 'for f in "$D"/tools/*.sh "$D"/ship.sh "$D"/bootstrap.sh; do bash -n "$f" || exit 1; done'
check ".claude/settings.json is valid JSON and every hook command points at an existing tools/ script" 'python3 -I - "$D" <<PY
import json, re, sys, os
d = sys.argv[1]
s = json.load(open(os.path.join(d, ".claude/settings.json")))
n = 0
for ev in s["hooks"].values():
    for e in ev:
        for h in e["hooks"]:
            m = re.search(r"tools/([A-Za-z_.-]+\.sh)", h["command"])
            assert m and os.path.isfile(os.path.join(d, "tools", m.group(1))), h["command"]
            n += 1
assert n >= 6, n
PY'
check "a PostToolUse hook with matcher * (every tool, MCP included) is registered in settings and template" 'python3 -c "import json,sys; [sys.exit(0 if any(e.get(\"matcher\")==\"*\" and \"posttool.sh\" in json.dumps(e) for e in json.load(open(p))[\"hooks\"][\"PostToolUse\"]) else 1) for p in sys.argv[1:]]" "$D/.claude/settings.json" "$D/tools/session-root-hooks.json"'
check "the hook template is valid JSON with the Kestrel marker on every command" 'python3 -I -c "
import json,sys
t=json.load(open(sys.argv[1]))
cmds=[h[\"command\"] for ev in t[\"hooks\"].values() for e in ev for h in e[\"hooks\"]]
assert cmds and all(\"kestrel-checkpoint-hooks\" in c for c in cmds)
" "$D/tools/session-root-hooks.json"'
check "the built-in extension's manifest version equals 1.0.<Kestrel G versionCode> (else an updated background.js never reaches existing installs)" 'bash "$D/tools/sync-ext-version.sh" --check'
check "signing/CERT_SHA256 pins a 64-hex certificate" 'grep -Eq "^[0-9a-f]{64}$" "$D/signing/CERT_SHA256"'
check "tools/MIRROR_MAIN is on or off" 'grep -Eq "^(on|off)$" "$D/tools/MIRROR_MAIN"'
check "HANDOFF.md, BRIEF.md, TASKS.md, BUILDLOG.md and CLAUDE.md exist" '[ -f "$D/HANDOFF.md" ] && [ -f "$D/BRIEF.md" ] && [ -f "$D/TASKS.md" ] && [ -f "$D/BUILDLOG.md" ] && [ -f "$D/CLAUDE.md" ]'
check "both apps are readable through appinfo.sh (version, tag, asset)" '[ -n "$(bash "$D/tools/appinfo.sh" web tag)" ] && [ -n "$(bash "$D/tools/appinfo.sh" gecko asset)" ] && [ "$(bash "$D/tools/appinfo.sh" of-tag kestrel-g-v1.0)" = gecko ] && [ "$(bash "$D/tools/appinfo.sh" of-tag kestrel-v1.0)" = web ] && ! bash "$D/tools/appinfo.sh" of-tag v1.0'
check "BUILDLOG.md lines have the format the gates read (| tag | app | code N | time | note)" '! grep -E "^\| " "$D/BUILDLOG.md" | grep -vqE "^\| kestrel(-g)?-v[^ |]+ \| (web|gecko) \| code [0-9]+ \| "'

# ---------------------------------------------------------------- fixture repo + fake remote
git init -q --bare "$T/remote.git"
git clone -q "$T/remote.git" "$T/work" 2>/dev/null
mkdir -p "$T/work/tools" "$T/work/app" "$T/work/geckoapp" "$T/work/signing"
cp "$D"/tools/*.sh "$D"/tools/session-root-hooks.json "$D"/tools/MIRROR_MAIN "$T/work/tools/" 2>/dev/null
echo off > "$T/work/tools/MIRROR_MAIN"
cp "$D"/ship.sh "$D"/signing/CERT_SHA256 "$T/work/" 2>/dev/null
cp "$D/.gitattributes" "$T/work/.gitattributes" 2>/dev/null
mv "$T/work/CERT_SHA256" "$T/work/signing/CERT_SHA256"
cd "$T/work" || exit 1
rm -f tools/test_handoff.sh   # the fixture must not recurse into this test via ckpt.sh
git config user.email t@t; git config user.name t
printf '        versionCode = 1\n        versionName = "0.1.0"\n' > app/build.gradle.kts
printf '        versionCode = 100\n        versionName = "0.2.0-gecko-alpha1"\n' > geckoapp/build.gradle.kts
printf '# BUILDLOG\n' > BUILDLOG.md
printf '# TASKS\n- [ ] first task\n' > TASKS.md
git checkout -q -b sess1
export CLAUDE_HOOK_SETTINGS="$T/settings.json" CLAUDE_REPO_ROOT="$T"   # never the real file

# ---------------------------------------------------------------- hooks installer
echo '{"permissions":{"allow":["Bash"]},"hooks":{"Stop":[{"hooks":[{"type":"command","command":"echo other # launcher-checkpoint-hooks"}]}]}}' > "$CLAUDE_HOOK_SETTINGS"
check "install-hooks reports MISSING before install" '! bash tools/install-hooks.sh --check'
check "install-hooks installs"                      'bash tools/install-hooks.sh --quiet && bash tools/install-hooks.sh --check'
check "install-hooks is idempotent (no change)"     'B=$(cat "$CLAUDE_HOOK_SETTINGS"); bash tools/install-hooks.sh --quiet; [ "$B" = "$(cat "$CLAUDE_HOOK_SETTINGS")" ]'
check "install-hooks keeps permissions + a sibling repo's hook" 'python3 -I -c "
import json,os
d=json.load(open(os.environ[\"CLAUDE_HOOK_SETTINGS\"]))
assert d[\"permissions\"][\"allow\"]==[\"Bash\"]
assert any(\"launcher-checkpoint-hooks\" in h[\"command\"] for e in d[\"hooks\"][\"Stop\"] for h in e[\"hooks\"])
assert any(\"kestrel-checkpoint-hooks\" in h[\"command\"] for e in d[\"hooks\"][\"Stop\"] for h in e[\"hooks\"])
"'
check "hooks point only at this repo's tools/" 'python3 -I -c "
import json,os
d=json.load(open(os.environ[\"CLAUDE_HOOK_SETTINGS\"]))
cmds=[h[\"command\"] for ev in d[\"hooks\"].values() for e in ev for h in e[\"hooks\"] if \"kestrel-checkpoint-hooks\" in h[\"command\"]]
assert cmds and all(\"$PWD/tools/\" in c and \"*\" not in c for c in cmds), cmds
"'
# the single-repo case: the repo IS the session root, its committed settings are read directly and nothing else is written
mkdir -p .claude; cp "$D/.claude/settings.json" .claude/settings.json
check "install-hooks: when the repo is the session root it only checks the committed settings (writes nothing elsewhere)" 'S0=$(cat "$CLAUDE_HOOK_SETTINGS"); unset CLAUDE_HOOK_SETTINGS; CLAUDE_PROJECT_DIR="$PWD" bash tools/install-hooks.sh --check && CLAUDE_PROJECT_DIR="$PWD" bash tools/install-hooks.sh --quiet; [ "$S0" = "$(cat "$T/settings.json")" ]'
rm -rf .claude

# ---------------------------------------------------------------- secret scan + path guard
printf '%s\n' "token=ghp_$(printf 'a%.0s' $(seq 1 36))" > leak.txt
git add -A >/dev/null 2>&1
check "secretscan blocks a GitHub token" '! bash tools/secretscan.sh'
git reset -q; rm -f leak.txt
mkdir -p signing/dev; echo pub > signing/dev/kestrel-dev.jks; echo priv > my-private.jks; truncate -s 91M huge.bin
git add -A >/dev/null 2>&1
bash tools/secretscan.sh >/dev/null 2>&1
check "path guard unstages a private keystore and a 91 MB file but keeps the public dev key" '[ -n "$(git diff --cached --name-only | grep -x signing/dev/kestrel-dev.jks)" ] && [ -z "$(git diff --cached --name-only | grep -e my-private.jks -e huge.bin)" ]'
git reset -q; rm -rf signing/dev my-private.jks huge.bin

# ---------------------------------------------------------------- autosave + push
echo hi > a.txt
check "autosave commits and pushes"  'bash tools/autosave.sh --text >/dev/null; git log -1 --format=%s | grep -q "^auto-checkpoint:" && [ "$(bash tools/unpushed.sh)" = 0 ]'
check "branch reached the remote"    'git ls-remote --heads origin sess1 | grep -q sess1'
check "main is NOT touched while MIRROR_MAIN=off" '! git ls-remote --heads origin main | grep -q main'
printf 'ghp_%s\n' "$(printf 'b%.0s' $(seq 1 36))" > leak2.txt
check "autosave refuses a credential"  'bash tools/autosave.sh --text | grep -q "AUTOSAVE BLOCKED"; ! git log --format=%s | head -1 | grep -q leak; ! git ls-tree -r HEAD --name-only | grep -q leak2'
rm -f leak2.txt
# a merge with unresolved conflicts is never auto-committed (`git add -A` would mark the files resolved and publish the conflict markers)
CONF_BR="$(git rev-parse --abbrev-ref HEAD)"
git checkout -q -b conf_other; echo other > conf.txt; git add conf.txt; git commit -q -m "conf other"; git checkout -q "$CONF_BR"
echo mine > conf.txt; git add conf.txt; git commit -q -m "conf mine"
git merge conf_other >/dev/null 2>&1
CONF_HEAD="$(git rev-parse HEAD)"
check "autosave does NOT commit a merge with unresolved conflicts" 'bash tools/autosave.sh --text | grep -q "AUTOSAVE PAUSED" && [ "$(git rev-parse HEAD)" = "$CONF_HEAD" ] && [ -n "$(git diff --name-only --diff-filter=U)" ]'
CONF_CK="$(bash tools/ckpt.sh "x" "y" 2>&1)"   # (captured first: ckpt exits 1 here and this script runs with pipefail)
check "ckpt does NOT commit a merge with unresolved conflicts either" 'echo "$CONF_CK" | grep -q "ckpt NOT committed" && [ "$(git rev-parse HEAD)" = "$CONF_HEAD" ] && [ -n "$(git diff --name-only --diff-filter=U)" ]'
echo resolved > conf.txt; git add -A >/dev/null 2>&1; git commit -q --no-edit >/dev/null 2>&1
echo more > a2.txt
check "autosave saves again once the conflict is resolved and committed" 'bash tools/autosave.sh --text >/dev/null; git log -1 --format=%s | grep -q "^auto-checkpoint:" && [ "$(bash tools/unpushed.sh)" = 0 ]'
# a session that is (wrongly) on main never pushes it by itself
git checkout -q -b main; echo m > m.txt; git add -A; git commit -q -m "on main"
check "push.sh never pushes main/master from a session" 'bash tools/push.sh; ! git ls-remote --heads origin main | grep -q main'
git checkout -q sess1; git branch -q -D main

# ---------------------------------------------------------------- ckpt numbering + CHECKPOINT.md
bash tools/ckpt.sh "did one" "do two" >/dev/null 2>&1
N1="$(sed -n '1s/^# CHECKPOINT \([0-9]*\).*/\1/p' CHECKPOINT.md)"
check "ckpt writes CHECKPOINT.md with did/next" '[ -n "$N1" ] && grep -q "did one" CHECKPOINT.md && grep -q "do two" CHECKPOINT.md'
bash tools/ckpt.sh "did two" "do three" >/dev/null 2>&1
N2="$(sed -n '1s/^# CHECKPOINT \([0-9]*\).*/\1/p' CHECKPOINT.md)"
check "ckpt numbers only go up"  '[ "${N2:-0}" -gt "${N1:-99}" ]'
check "ckpt pushed everything"   '[ "$(bash tools/unpushed.sh)" = 0 ]'

# ---------------------------------------------------------------- resume: briefing, interruption, stranded branch
OUT="$(bash tools/resume.sh --text 2>&1)"
check "resume prints CHECKPOINT + open task" 'printf "%s" "$OUT" | grep -q "do three" && printf "%s" "$OUT" | grep -q "first task"'
check "resume reports a clean state (no interruption warning)" '! printf "%s" "$OUT" | grep -q "INTERRUPTED MID-CHANGE"'
echo wip > b.txt; bash tools/autosave.sh --text >/dev/null
OUT="$(bash tools/resume.sh --text 2>&1)"
check "resume flags an interrupted session"  'printf "%s" "$OUT" | grep -q "INTERRUPTED MID-CHANGE"'
bash tools/ckpt.sh "finished wip" "next" >/dev/null 2>&1
git checkout -q -b sess2 2>/dev/null; echo more > c.txt; bash tools/ckpt.sh "other session work" "continue" >/dev/null 2>&1
git checkout -q sess1 2>/dev/null
OUT="$(bash tools/resume.sh --text 2>&1)"
check "resume MERGES another session's checkpointed work (sync), keeps its card, and lists its lane" 'printf "%s" "$OUT" | grep -q "merged sess2" && [ -f c.txt ] && [ -f .checkpoints/sess2.md ] && printf "%s" "$OUT" | grep -q "lane sess2"'
check "resume briefing fits the 10,000-char hook cap (with a long INBOX + TASKS + CHECKPOINT)" '
  for i in $(seq 1 80); do echo "- [ ] open task number $i with a reasonably long description of what must be done" >> TASKS.md; done
  head -c 6000 /dev/zero | tr "\0" "x" >> INBOX.md
  bash tools/ckpt.sh "$(head -c 7000 /dev/zero | tr "\0" "d")" "$(head -c 3000 /dev/zero | tr "\0" "n")" >/dev/null 2>&1
  [ "$(bash tools/resume.sh --text | wc -c)" -lt 10000 ]'

# ---------------------------------------------------------------- many sessions and accounts: cards, merge driver, union files, conflicts, claims, cold clone
check "ckpt keeps this session's own card in .checkpoints/" '[ -f .checkpoints/sess1.md ] && grep -q "^# CHECKPOINT" .checkpoints/sess1.md'
git checkout -q -b sess3; echo "from sess3" >> INBOX.md; bash tools/ckpt.sh "sess3 work" "sess3 next" >/dev/null 2>&1
git checkout -q sess1; echo "from sess1" >> INBOX.md; bash tools/ckpt.sh "sess1 later" "sess1 next" >/dev/null 2>&1
check "parallel sessions merge with NO conflict: INBOX keeps both lines, CHECKPOINT.md is the newest card, both cards survive, all pushed" 'grep -q "from sess1" INBOX.md && grep -q "from sess3" INBOX.md && ! grep -q "<<<<<<<" INBOX.md CHECKPOINT.md && grep -q "sess1 later" CHECKPOINT.md && [ -f .checkpoints/sess3.md ] && grep -q "sess3 next" .checkpoints/sess3.md && [ -z "$(git status --porcelain)" ] && [ "$(bash tools/unpushed.sh)" = 0 ]'
printf -- '- [ ] Z1 claim test item\n- [ ] Z2 second item\n' >> TASKS.md; bash tools/autosave.sh --text >/dev/null 2>&1
check "claim marks an item [~] with branch and time, release gives it back, done ticks it with evidence" 'bash tools/claim.sh Z1 >/dev/null && grep -Eq "^- \[~\] Z1 claim test item \{sess1 [0-9T:-]+Z\}$" TASKS.md && bash tools/claim.sh Z1 --release >/dev/null && grep -q "^- \[ \] Z1 claim test item$" TASKS.md && bash tools/claim.sh Z1 --done "proved by run 7" >/dev/null && grep -q "^- \[x\] Z1 claim test item - proved by run 7$" TASKS.md'
git push -q origin sess1:refs/heads/sess9 >/dev/null 2>&1
sed -i "s/^- \[ \] Z2 second item$/- [~] Z2 second item {sess9 2026-11-11T00:00Z}/" TASKS.md
check "an item claimed by a LIVE other session is refused; --force takes it over" '! bash tools/claim.sh Z2 >/dev/null 2>&1 && bash tools/claim.sh Z2 --force >/dev/null 2>&1 && grep -q "Z2 second item {sess1 " TASKS.md'
OUT="$(bash tools/resume.sh --text 2>&1)"
check "resume lists the claimed items" 'printf "%s" "$OUT" | grep -q "CLAIMED by sessions"'
git checkout -q -b sess4 sess1; echo "sess4 version" > a.txt; bash tools/ckpt.sh "sess4 edits a.txt" "n" >/dev/null 2>&1
git checkout -q sess1; echo "sess1 version" > a.txt; bash tools/ckpt.sh "sess1 edits a.txt" "n" >/dev/null 2>&1
check "a real conflict is reported, the merge is aborted and nothing is lost" 'OUTS="$(bash tools/sync.sh 2>&1)"; printf "%s" "$OUTS" | grep -q "SYNC CONFLICT" && [ -z "$(git status --porcelain)" ] && [ "$(cat a.txt)" = "sess1 version" ] && [ -z "$(git diff --name-only --diff-filter=U)" ]'
git merge -q -s ours --no-edit origin/sess4 >/dev/null 2>&1; bash tools/push.sh
git clone -q -b sess1 "$T/remote.git" "$T/cold" 2>/dev/null
NEXTWORD="$(sed -n '/^## Do this next/{n;p}' CHECKPOINT.md | cut -c1-12)"
check "a COLD clone with no context is told the exact next step" 'COUT="$(cd "$T/cold" && bash tools/resume.sh --text 2>&1)"; printf "%s" "$COUT" | grep -q "RESUMING KESTREL" && printf "%s" "$COUT" | grep -q "Do this next" && printf "%s" "$COUT" | grep -qF "$NEXTWORD"'

# ---------------------------------------------------------------- inbox capture
printf '{"prompt":"please build X, my key is ghp_%s ok"}' "$(printf 'c%.0s' $(seq 1 36))" | bash tools/capture_inbox.sh
check "inbox captures the message, redacts the key, and commits it" 'grep -q "please build X" INBOX.md && grep -q "REDACTED-KEY" INBOX.md && ! grep -q "ghp_c" INBOX.md && [ "$(bash tools/unpushed.sh)" = 0 ]'

# ---------------------------------------------------------------- outbox capture
OB_T="$T/ob.jsonl"
printf '%s\n' '{"type":"user","message":{"role":"user","content":"hi"}}' "{\"type\":\"assistant\",\"message\":{\"role\":\"assistant\",\"content\":[{\"type\":\"text\",\"text\":\"Finding: the answer is 42, my key is ghp_$(printf 'c%.0s' $(seq 1 36))\"}]}}" > "$OB_T"
printf '{"hook_event_name":"Stop","transcript_path":"%s"}' "$OB_T" | bash tools/autosave.sh >/dev/null 2>&1
check "outbox captures Claude's last reply on Stop, quoted, key redacted, and commits it" 'grep -q "^> Finding: the answer is 42" OUTBOX.md && grep -q "REDACTED-KEY" OUTBOX.md && ! grep -q "ghp_c" OUTBOX.md && [ -z "$(git status --porcelain OUTBOX.md)" ]'
printf '{"hook_event_name":"Stop","transcript_path":"%s"}' "$OB_T" | bash tools/capture_outbox.sh
check "outbox does not repeat an identical reply" '[ "$(grep -c "^## 20" OUTBOX.md)" = 1 ]'
printf '{"hook_event_name":"PostToolUse","transcript_path":"%s"}' "$OB_T" | bash tools/autosave.sh >/dev/null 2>&1
check "outbox: a tool-call hook adds nothing for the same short reply" '[ "$(grep -c "^## 20" OUTBOX.md)" = 1 ]'
printf '%s\n' "{\"type\":\"assistant\",\"message\":{\"role\":\"assistant\",\"content\":[{\"type\":\"text\",\"text\":\"Short narration\"}]}}" > "$OB_T"
printf '{"hook_event_name":"PostToolUse","transcript_path":"%s"}' "$OB_T" | bash tools/autosave.sh >/dev/null 2>&1
check "outbox: a tool-call hook skips short narration (under 200 characters)" '[ "$(grep -c "^## 20" OUTBOX.md)" = 1 ]'
printf '%s\n' "{\"type\":\"assistant\",\"message\":{\"role\":\"assistant\",\"content\":[{\"type\":\"text\",\"text\":\"Finding two, mid-turn: $(printf 'z%.0s' $(seq 1 260))\"}]}}" > "$OB_T"
printf '{"hook_event_name":"PostToolUse","transcript_path":"%s"}' "$OB_T" | bash tools/autosave.sh >/dev/null 2>&1
check "outbox: a tool-call hook keeps substantial text, so a chat that dies mid-turn still leaves its findings" '[ "$(grep -c "^## 20" OUTBOX.md)" = 2 ]'

# ---------------------------------------------------------------- release recording + the ship gate (two apps, one BUILDLOG)
check "record-release refuses a tag that is not Kestrel's"            '! bash tools/record-release.sh v0.1.0 "x"'
check "record-release refuses a tag whose version is not the Gradle one" '! bash tools/record-release.sh kestrel-v9.9.9 "x"'
check "record-release writes the web line and pushes it"              'bash tools/record-release.sh kestrel-v0.1.0 "first web" && grep -q "^| kestrel-v0.1.0 | web | code 1 | " BUILDLOG.md && [ "$(bash tools/unpushed.sh)" = 0 ]'
check "record-release is idempotent"                                  'bash tools/record-release.sh kestrel-v0.1.0 "again" | grep -q "already in BUILDLOG" && [ "$(grep -c "^| kestrel-v0.1.0 |" BUILDLOG.md)" = 1 ]'
check "record-release writes the gecko line with its own code"         'bash tools/record-release.sh kestrel-g-v0.2.0-gecko-alpha1 "first gecko" && grep -q "^| kestrel-g-v0.2.0-gecko-alpha1 | gecko | code 100 | " BUILDLOG.md'
check "the gates read each app separately (web code 1, gecko code 100)" '. tools/appinfo.sh; [ "$(app_last_code web)" = 1 ] && [ "$(app_last_code gecko)" = 100 ]'
SHIPOUT="$(bash ship.sh web "x" 2>&1)"
check "ship refuses a non-increased web versionCode" 'printf "%s" "$SHIPOUT" | grep -q "not higher" && ! printf "%s" "$SHIPOUT" | grep -q "ready for GitHub"'
SHIPOUT="$(bash ship.sh gecko "x" 2>&1)"
check "ship refuses a non-increased gecko versionCode" 'printf "%s" "$SHIPOUT" | grep -q "not higher" && ! printf "%s" "$SHIPOUT" | grep -q "ready for GitHub"'
check "ship refuses an unknown app name" '! bash ship.sh desktop "x" >/dev/null 2>&1'
OUT="$(bash tools/resume.sh --text 2>&1)"
check "resume shows the recorded codes of both apps" 'printf "%s" "$OUT" | grep -q "web code 1, gecko code 100"'

echo "handoff self-test: $PASS passed, $FAIL failed"
[ "$FAIL" -eq 0 ]
