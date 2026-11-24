# CHECKPOINT 3255 — read me first, then TASKS.md

**Written:** 2026-11-24T19:48:44Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/kind-brahmagupta-ae3yl8` · **builds on:** `2f1c9b8e` (this checkpoint is the commit after it)

## Just done
OPT1: core done + 25 tests green, bench pages + web test green, tools/optimizer_digest.py smoke-tested; Android side (BenchServer, Store, Apply, Guard, Restart trampoline, Runner, Activity, Settings row, lab suite opt) written in scratchpad optsrc/ (apply_opt.py, apply_opt2.py, apply_opt3.py) because the alpha106 ship gate is still running

## Do this next
WHEN bash ship.sh gecko (alpha106) has printed 'ready for GitHub to build': (1) dispatch ONCE app=gecko full_build=true with the note of docs/handoff/ALPHA106_RELEASE_NOTE.txt, poll the run, get_release_by_tag kestrel-g-v0.3.0-alpha106, record-release, tick FF1c. (2) THEN apply the optimizer integration: python3 -B -I <scratchpad>/optsrc/apply_opt.py, apply_opt2.py, apply_opt3.py (scratchpad is /tmp/claude-0/-home-user/*/scratchpad/optsrc; if it is gone the sources are described in research/OPTIMIZER.md section 8 and must be rewritten), compile :geckoapp, fix errors, run :core:test :app:testDebugUnitTest, web tests, check-gecko-prefs, ckpt, dispatch lab suite opt, then ship alpha107 (Auto-tune)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  59efade6 ckpt 3253: FF1: alpha106 prepared (code 207, GeckoView 161): lab smoke GREEN on 161 (ru
  29576308 ckpt 3230: FF1: GeckoView 161.0.20261118180411 + webcompat 161.0 pinned, clean compile,
  e7367846 ckpt 3222: wrote FF1 (new Firefox version into Kestrel, optimised for the hardware) and
  2e56492f ckpt 3221: alpha105 (code 206) SHIPPED: run 45352897624 green, Release kestrel-g-v0.3.0
  6fb30f29 ckpt 3219: alpha105 (code 206) gated (ckpt 3218, head 8b6eebf7) and dispatched ONCE: ru
  8b6eebf7 ckpt 3218: gated kestrel-g-v0.3.0-alpha105 (gecko code 206) and pushed it: handoff self
  c45aae47 ckpt 3217: PERF25: app unit tests green; bumped to alpha105 / code 206, release note do
  512c9e18 ckpt 3215: PERF25c: built the hardware drawing trial (core DrawTrial + DrawTrialTest + 
  6d20d18a ckpt 3203: wrote PERF25 (owner: Brave is smooth on the parlayapi site) into TASKS.md, n
  f3c4d330 ckpt 3202: alpha104 (code 205) SHIPPED: run 45346018174 green, Release kestrel-g-v0.3.0
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
