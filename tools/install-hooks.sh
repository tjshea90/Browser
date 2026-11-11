#!/usr/bin/env bash
# install-hooks.sh — put the checkpoint hooks where Claude Code will actually
# read them, WITHOUT destroying anything else that lives in that file, and
# WITHOUT ever touching a directory that isn't this repo.
#
# WHY THIS IS A MERGE, NOT A `cp`
# --------------------------------
# The naive version of this step is:
#
#     cp tools/session-root-hooks.json /home/user/.claude/settings.json
#
# That works, and it is also an unconditional overwrite of the one file a
# user's own Claude Code settings live in — permissions, env, statusLine,
# and hooks belonging to OTHER projects in the same container (this account
# runs other repos, Launcher and Portfolio, side by
# side with this one). Every session that ran that `cp` would silently
# delete whatever those repos had written there. Nothing warns, because a
# clobbered settings file looks exactly like a settings file.
#
# So this merges instead: our hook entries are tagged with a marker, and
# only entries carrying that marker are ever touched. Everything else in the
# file — including any hooks a sibling repo installed for itself — is
# preserved byte-for-byte through a JSON round-trip.
#
# WHY EVERY INSTALLED COMMAND IS AN ABSOLUTE PATH INTO THIS REPO ONLY
# ------------------------------------------------------------------
# The owner runs several repos side by side (Launcher, Portfolio, Browser...). A session that has more than one of them
# attached gets ONE shared session root. A glob here would let a session commit and push inside a sibling repo as a
# side effect of Kestrel's infrastructure, so every command below is this repo's own absolute path, the marker is
# unique to Kestrel ("kestrel-checkpoint-hooks"), and the installer only ever replaces entries carrying it.
#
# WHEN THE REPO IS THE SESSION ROOT (the normal single-repo case: CLAUDE_PROJECT_DIR == this repo) Claude Code reads the
# committed .claude/settings.json directly, so there is nothing to merge and nothing is written anywhere else.
#
# It is idempotent — running it when it is already current changes nothing
# and writes nothing — so it is safe to call from other tools (ckpt.sh and
# resume.sh both do, so a session that forgets to run this by hand still
# gets the safety net the moment it checkpoints or resumes).
#
#   bash tools/install-hooks.sh           # install or repair
#   bash tools/install-hooks.sh --check   # 0 = current, 1 = needs installing
#   bash tools/install-hooks.sh --quiet   # only speak if something changed
#
set -uo pipefail
D="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"; cd "$D" || exit 1

MODE="install"; QUIET=0
for a in "$@"; do
  case "$a" in
    --check) MODE="check" ;;
    --quiet) QUIET=1 ;;
  esac
done

# WHERE DOES CLAUDE CODE READ HOOKS FROM HERE?
# NOT this repo's own .claude/settings.json, whenever this repo sits as a
# subdirectory of a session root that Claude Code resolves for hooks
# purposes (confirmed on this account's other repos, running in the same
# kind of container: Claude Code on the web, several repos under one root).
# CLAUDE_PROJECT_DIR is unset in that case and the harness resolves ONE
# project root for the session — which is the directory CONTAINING the
# repo. $HOME is a different thing entirely (the harness keeps its own
# hooks there), so do not use it.
#
# CLAUDE_HOOK_SETTINGS overrides the target and CLAUDE_REPO_ROOT overrides
# the root used to LOCATE it — tools/test_handoff.sh sets both to temp paths
# so the tests can never touch the real settings file. Substitution of the
# placeholder in the template always uses THIS repo's own path ($D), never
# the container root — see the header above for why.
ROOT="${CLAUDE_REPO_ROOT:-$(dirname "$D")}"
TARGET="${CLAUDE_HOOK_SETTINGS:-$ROOT/.claude/settings.json}"
# The repo IS the Claude Code project root: its committed .claude/settings.json is what is read. Healthy = it names our
# resume and autosave hooks (portable ${CLAUDE_PROJECT_DIR} form or absolute form, either counts). Only a damaged file is repaired.
if [ -z "${CLAUDE_HOOK_SETTINGS:-}" ] && [ -n "${CLAUDE_PROJECT_DIR:-}" ] && [ "$(cd "$CLAUDE_PROJECT_DIR" 2>/dev/null && pwd)" = "$D" ]; then
  TARGET="$D/.claude/settings.json"
  if grep -q 'tools/resume.sh' "$TARGET" 2>/dev/null && grep -q 'tools/autosave.sh' "$TARGET" 2>/dev/null; then
    [ "$MODE" = "check" ] && exit 0
    [ "$QUIET" -eq 1 ] || echo "  OK    this repo is the session root: its .claude/settings.json is read directly"
    exit 0
  fi
fi
TEMPLATE="$D/tools/session-root-hooks.json"

[ -f "$TEMPLATE" ] || { echo "  FAIL  missing $TEMPLATE — cannot install hooks."; exit 1; }

if ! command -v python3 >/dev/null 2>&1; then
  # Fallback only when there is nothing to lose. Refusing is correct here: a
  # blind cp over an existing settings file is the exact failure this
  # script was written to remove, and doing it "just this once" is how it
  # comes back.
  if [ ! -f "$TARGET" ]; then
    mkdir -p "$(dirname "$TARGET")"
    sed "s|__KESTREL_TOOLS__|$D/tools|g" "$TEMPLATE" > "$TARGET"
    echo "  OK    hooks installed at $TARGET (no python3 — plain copy, file did not exist)"
    exit 0
  fi
  echo "  FAIL  no python3, and $TARGET already exists. Merge it by hand rather"
  echo "        than overwriting — it may hold settings that are not ours."
  exit 1
fi

mkdir -p "$(dirname "$TARGET")"

CLAUDE_INSTALL_MODE="$MODE" CLAUDE_INSTALL_QUIET="$QUIET" \
CLAUDE_INSTALL_TARGET="$TARGET" CLAUDE_INSTALL_TEMPLATE="$TEMPLATE" \
CLAUDE_INSTALL_TOOLS="$D/tools" python3 <<'PY'
import json, os, sys, time, shutil

target   = os.environ["CLAUDE_INSTALL_TARGET"]
template = os.environ["CLAUDE_INSTALL_TEMPLATE"]
toolsdir = os.environ["CLAUDE_INSTALL_TOOLS"]
mode     = os.environ["CLAUDE_INSTALL_MODE"]
quiet    = os.environ["CLAUDE_INSTALL_QUIET"] == "1"

# The marker is the ONLY thing that identifies an entry as ours. Unlike an
# earlier, more literally-ported version of this script, there is no generic
# path-based fallback match — this repo's hooks are absolute paths into
# ITS OWN tools/ directory and nothing else, so there is nothing generic to
# collide with a sibling repo's separately-installed, differently-pathed
# hooks in the first place.
MARKER = "kestrel-checkpoint-hooks"

with open(template) as f:
    wanted = json.load(f)
wanted_hooks = wanted.get("hooks", {})

# Bake this repo's own real tools/ path into the commands. The template
# ships a placeholder so it never hard-codes a specific container layout.
def subst(obj):
    if isinstance(obj, str):
        return obj.replace("__KESTREL_TOOLS__", toolsdir)
    if isinstance(obj, list):
        return [subst(x) for x in obj]
    if isinstance(obj, dict):
        return {k: subst(v) for k, v in obj.items()}
    return obj
wanted_hooks = subst(wanted_hooks)

existing = {}
damaged = False
if os.path.exists(target):
    try:
        with open(target) as f:
            existing = json.load(f)
        if not isinstance(existing, dict):
            existing, damaged = {}, True
    except Exception:
        # Unparseable. Treat as damaged rather than as empty: it is still
        # backed up below, so nothing is lost either way.
        existing, damaged = {}, True

merged = json.loads(json.dumps(existing))   # deep copy
hooks = merged.setdefault("hooks", {})
if not isinstance(hooks, dict):
    hooks, damaged = {}, True
    merged["hooks"] = hooks

def is_ours(entry):
    for h in (entry or {}).get("hooks", []) or []:
        if MARKER in str(h.get("command", "")):
            return True
    return False

for event, entries in wanted_hooks.items():
    kept = [e for e in hooks.get(event, []) or [] if not is_ours(e)]
    hooks[event] = kept + entries

# Drop an event key we emptied out entirely (we removed our old entry and
# the template no longer defines one) so stale events don't linger forever.
for event in list(hooks.keys()):
    if event not in wanted_hooks and isinstance(hooks[event], list):
        left = [e for e in hooks[event] if not is_ours(e)]
        if left:
            hooks[event] = left
        else:
            del hooks[event]

# ENV: the template may carry an "env" object (Kestrel's does not); keys in it are merged, everything else in "env" is preserved untouched.
wanted_env = wanted.get("env", {}) or {}
if wanted_env:
    env = merged.get("env")
    if not isinstance(env, dict):
        env = {}
        merged["env"] = env
    for key, val in wanted_env.items():
        cur = env.get(key)
        if key == "CLAUDE_AUTOCOMPACT_PCT_OVERRIDE" and cur is not None:
            try:
                if float(cur) <= float(val):
                    continue
            except ValueError:
                pass
        env[key] = val

# An old `cp`-based install could have left the template's own _comment in
# the user's settings file, where it is meaningless. Remove it when it is
# unambiguously ours; never touch a _comment that is not.
c = merged.get("_comment")
if isinstance(c, str) and ("session-root-hooks.json" in c or "install-hooks.sh" in c) and "kestrel" in c.lower():
    del merged["_comment"]

new_text = json.dumps(merged, indent=2) + "\n"
old_text = None
if os.path.exists(target):
    try:
        with open(target) as f:
            old_text = f.read()
    except Exception:
        pass

if old_text == new_text and not damaged:
    if mode == "check":
        sys.exit(0)
    if not quiet:
        print("  OK    checkpoint hooks already current at %s" % target)
    sys.exit(0)

if mode == "check":
    sys.exit(1)

# Back up anything we are about to change, but only when it actually held
# something of its own — a backup per session of a file we wrote ourselves
# is just litter.
if old_text is not None and (existing.get("hooks") or [k for k in existing if k != "hooks"] or damaged):
    stamp = time.strftime("%Y%m%dT%H%M%SZ", time.gmtime())
    try:
        shutil.copy2(target, "%s.bak-%s" % (target, stamp))
    except Exception:
        pass

tmp = target + ".tmp"
with open(tmp, "w") as f:
    f.write(new_text)
os.replace(tmp, target)   # atomic: a killed session never leaves a half file

kept_keys = sorted(k for k in merged if k not in ("hooks", "env"))
note = " (preserved: %s)" % ", ".join(kept_keys) if kept_keys else ""
if damaged:
    print("  WARN  %s was unreadable or malformed — rebuilt it (a .bak- copy was kept)%s" % (target, note))
else:
    print("  OK    checkpoint hooks installed at %s%s" % (target, note))
PY
exit $?
