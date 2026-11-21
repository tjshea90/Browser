# CHECKPOINT 2798 — read me first, then TASKS.md

**Written:** 2026-11-21T07:13:58Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/sweet-gates-gr3bj5` · **builds on:** `4e6d770e` (this checkpoint is the commit after it)

## Just done
alpha81 SHIPPED and RECORDED: run 44878029773 green (check, test-js, test-jvm, build-gecko; lab/web/courier skipped by design), tag kestrel-g-v0.3.0-alpha81, Release has kestrel-g-0.3.0-alpha81.apk (96.6 MB) + .sha256 + .dm, certificate verified on the APK; BUILDLOG line written by record-release. https://github.com/tjshea90/Browser/releases/tag/kestrel-g-v0.3.0-alpha81

## Do this next
1) alpha82 (privacy fixes already committed in core: bare host/path keeps host only; dotted-quad addresses removed): run :core:test :app:testDebugUnitTest :geckoapp:compileDebugKotlin, bump versionCode 183 / 0.3.0-alpha82 + sync-ext-version, write docs/handoff/ALPHA82_RELEASE_NOTE.txt, ship.sh gecko, ONE dispatch, record-release. 2) answer the owner (alpha80 + alpha81 explanation, token steps, NOT run on tablet, open items)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  dba49a28 ckpt 2797: LD1 privacy fix 2 (for alpha82): DiagScrub removes dotted-quad network addre
  db02a2b5 ckpt 2796: LD1 privacy fix (for alpha82): DiagScrub now drops the path after a BARE hos
  472a364c ckpt 2795: alpha81 DISPATCHED once: android.yml run 44878029773 (run_number 263, head 7
  7f084cff ckpt 2794: gated kestrel-g-v0.3.0-alpha81 (gecko code 182) and pushed it: handoff self-
  68f9d24e ckpt 2793: LD1e: full gate green (:core:test, :app:testDebugUnitTest, :geckoapp:compile
  a6c87bc8 ckpt 2792: LD1d done: tools/diag_digest.py (crashes/kills, slow sites by host, playback
  2a79bf47 ckpt 2791: LD1c tested: LiveDiagTest 13/13 (switch off = nothing kept, switch survives 
  2050ef12 ckpt 2790: LD1c: app side written + geckoapp compiles: diag/LiveDiag.kt (master switch,
  5d077b35 ckpt 2789: LD1b: core livediag package written + tested (DiagScrub: links -> host, sens
  808aa4f6 ckpt 2787: alpha80 RELEASED AND RECORDED (kestrel-g-v0.3.0-alpha80, gecko code 181, run
```
