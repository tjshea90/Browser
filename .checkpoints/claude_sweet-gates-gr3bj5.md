# CHECKPOINT 2796 — read me first, then TASKS.md

**Written:** 2026-11-21T07:00:25Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/sweet-gates-gr3bj5` · **builds on:** `60eb562e` (this checkpoint is the commit after it)

## Just done
LD1 privacy fix (for alpha82): DiagScrub now drops the path after a BARE host (media frame-gone writes 'host/path' without https://, up to 40 chars of path would have been kept); new test aBareHostWithAPathKeepsOnlyTheHost, core livediag 14/14. Code NOT in alpha81 (run 44878029773 was dispatched from 7f084cff before this edit).

## Do this next
1) WAIT for run 44878029773 (alpha81): background loop bx08z5pgk waits for tag kestrel-g-v0.3.0-alpha81; then verify jobs + release APK, bash tools/record-release.sh kestrel-g-v0.3.0-alpha81 with the identical note from docs/handoff/ALPHA81_RELEASE_NOTE.txt. 2) ONLY AFTER alpha81 is recorded: gate (:core:test :app:testDebugUnitTest :geckoapp:compileDebugKotlin), bump versionCode 183 / 0.3.0-alpha82 + sync-ext-version, ALPHA82_RELEASE_NOTE.txt = 'alpha82: privacy fix for the live diagnostics: a site address written without https:// in front keeps only the site name', ship.sh gecko, ONE dispatch. 3) answer the owner

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  472a364c ckpt 2795: alpha81 DISPATCHED once: android.yml run 44878029773 (run_number 263, head 7
  7f084cff ckpt 2794: gated kestrel-g-v0.3.0-alpha81 (gecko code 182) and pushed it: handoff self-
  68f9d24e ckpt 2793: LD1e: full gate green (:core:test, :app:testDebugUnitTest, :geckoapp:compile
  a6c87bc8 ckpt 2792: LD1d done: tools/diag_digest.py (crashes/kills, slow sites by host, playback
  2a79bf47 ckpt 2791: LD1c tested: LiveDiagTest 13/13 (switch off = nothing kept, switch survives 
  2050ef12 ckpt 2790: LD1c: app side written + geckoapp compiles: diag/LiveDiag.kt (master switch,
  5d077b35 ckpt 2789: LD1b: core livediag package written + tested (DiagScrub: links -> host, sens
  808aa4f6 ckpt 2787: alpha80 RELEASED AND RECORDED (kestrel-g-v0.3.0-alpha80, gecko code 181, run
  f12802a2 ckpt 2786: LD1 written into TASKS.md in the owner's words (live diagnostics engine with
  5c4edd25 ckpt 2785: REL80b: alpha80 DISPATCHED ONCE (android.yml app=gecko full_build=true ref c
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
