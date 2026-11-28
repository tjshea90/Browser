# CHECKPOINT 2807 — read me first, then TASKS.md

**Written:** 2026-11-21T08:08:55Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/sweet-gates-gr3bj5` · **builds on:** `47024cb0` (this checkpoint is the commit after it)

## Just done
alpha83 SHIPPED and RECORDED: run 44882471063 (run_number 264, head 805cabb9) success; tag kestrel-g-v0.3.0-alpha83; Release has kestrel-g-0.3.0-alpha83.apk (96.6 MB) + .sha256 + .dm; certificate verified; BUILDLOG written. https://github.com/tjshea90/Browser/releases/tag/kestrel-g-v0.3.0-alpha83 . alpha80 and alpha81 also shipped+recorded. alpha82 never existed (folded in). LD1 and LD2 DONE.

## Do this next
Nothing is running. Waiting for the owner's tablet test of alpha83: (1) create the fine-grained token (only kestrel-private, Contents read+write), paste in Settings > Live diagnostics (for Claude) > GitHub token, tap Send now: the first real upload; (2) Claude sessions need ../kestrel-private cloned (add_repo kestrel-private push was DENIED this session: owner may have to allow it) then python3 -B -I tools/diag_digest.py; (3) still open: YTC1/YTC2 (owner's Settings bisect), ATT1 (File chooser Diagnostics line), FLK1 PhoneBackendTest flake, PB8, a lab scene for file input, tune Advisor thresholds with real data

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  6b5bf826 ckpt 2806: alpha83 DISPATCHED once at 07:55Z on 2026-11-21 (android.yml, app=gecko, ful
  805cabb9 ckpt 2805: gated kestrel-g-v0.3.0-alpha83 (gecko code 184) and pushed it: handoff self-
  6eac0f1a ckpt 2804: LD2e: full gate green (:core:test :app:testDebugUnitTest :geckoapp:compileDe
  d2831830 ckpt 2803: LD2a-d built and tested: app LiveDiag (tables fed after switch+whitelist, fa
  9ebf9afd ckpt 2802: LD2 core written + tested (33 tests in core livediag): DiagStats (event hist
  6719ce3e ckpt 2801: gated kestrel-g-v0.3.0-alpha82 (gecko code 183) and pushed it: handoff self-
  f95e13ba ckpt 2800: LD2 written into TASKS.md in the owner's words (LD2a-e): smarter + more comp
  946369a8 ckpt 2799: alpha82 prepared: versionCode 183 / 0.3.0-alpha82, extension version synced,
  2d4c1e81 ckpt 2798: alpha81 SHIPPED and RECORDED: run 44878029773 green (check, test-js, test-jv
  dba49a28 ckpt 2797: LD1 privacy fix 2 (for alpha82): DiagScrub removes dotted-quad network addre
```
