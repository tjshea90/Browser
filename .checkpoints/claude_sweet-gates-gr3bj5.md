# CHECKPOINT 2803 — read me first, then TASKS.md

**Written:** 2026-11-21T07:42:42Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/sweet-gates-gr3bj5` · **builds on:** `0f1128eb` (this checkpoint is the commit after it)

## Just done
LD2a-d built and tested: app LiveDiag (tables fed after switch+whitelist, facts(), settingsMap(), advice(), applyAdvice whitelisted, sleep() on OFF, upload re-check before PUT, v2 batch), Settings 'Suggested:' rows, BrowserActivity extra facts, digest v2 (SETTINGS TO CHANGE), LIVE_DIAG.md LD2 section. LiveDiagTest 20/20, core livediag 33/33, handoff self-test 74/74, :geckoapp compile green

## Do this next
LD2e: full gate (:core:test :app:testDebugUnitTest :geckoapp:compileDebugKotlin :app:lintDebug, run in background to scratchpad gate2.log, never two gradle at once), then versionCode 184 / 0.3.0-alpha83 + bash tools/sync-ext-version.sh + docs/handoff/ALPHA83_RELEASE_NOTE.txt (ASCII, no quotes; covers LD2 + alpha82's two scrub fixes), bash ship.sh gecko, ONE dispatch (alpha82 is NOT dispatched), tag loop, verify jobs + release, record-release identical note, then answer the owner

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md
     M research/LIVE_DIAG.md

## Last ten checkpoints
```
  9ebf9afd ckpt 2802: LD2 core written + tested (33 tests in core livediag): DiagStats (event hist
  6719ce3e ckpt 2801: gated kestrel-g-v0.3.0-alpha82 (gecko code 183) and pushed it: handoff self-
  f95e13ba ckpt 2800: LD2 written into TASKS.md in the owner's words (LD2a-e): smarter + more comp
  946369a8 ckpt 2799: alpha82 prepared: versionCode 183 / 0.3.0-alpha82, extension version synced,
  2d4c1e81 ckpt 2798: alpha81 SHIPPED and RECORDED: run 44878029773 green (check, test-js, test-jv
  dba49a28 ckpt 2797: LD1 privacy fix 2 (for alpha82): DiagScrub removes dotted-quad network addre
  db02a2b5 ckpt 2796: LD1 privacy fix (for alpha82): DiagScrub now drops the path after a BARE hos
  472a364c ckpt 2795: alpha81 DISPATCHED once: android.yml run 44878029773 (run_number 263, head 7
  7f084cff ckpt 2794: gated kestrel-g-v0.3.0-alpha81 (gecko code 182) and pushed it: handoff self-
  68f9d24e ckpt 2793: LD1e: full gate green (:core:test, :app:testDebugUnitTest, :geckoapp:compile
```

(3 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
