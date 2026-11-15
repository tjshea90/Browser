# CHECKPOINT 2413 — read me first, then TASKS.md

**Written:** 2026-11-15T18:51:45Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `1a55e440` (this checkpoint is the commit after it)

## Just done
owner message 46 written into TASKS.md (FSB1 bars cover the controls in full screen, ROT1 rotation lag, FSC1 no controls in landscape; screenshots measured: both system bars stay visible on the tablet and the page is laid out full-bleed under them = alpha48 FS1 root padding zero + LAYOUT_* flags); second review rows RW1-RW12 written; vfind.js already patched for RW1/RW6/RW8 (bare-layer rule, covered() ambiguity, all() cache, srcOf instead of menu)

## Do this next
NEXT: (1) FSB1 code in geckoapp BrowserActivity: applyImmersive without LAYOUT_* flags, insets listener without the fullscreenOn zero-padding branch, bars check +700 ms with one re-hide, onWindowFocusChanged re-hide, Diagnostics line, debug prop debug.kestrel.nohide; (2) ROT1: GeckoTabData.mediaSession + TurnSettler (pause via MediaSession, black cover, resume) with pure rules in core; (3) videobar.js RW2/RW3/RW5/RW7-RW11, content.js identity check + RW4, background.js ft src; tests in web-tests/videobar.test.js; (4) lab scene fsy; compat run 43747605584 is running (commit d288355b, ends about 20:10Z); snd run 43745675957 finished green: read its table (artifact) for SND1

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  b8c5d333 ckpt 2412: hour soak 43740398387 read (commit 281c48f4, hotspot-shaped, 70 min, 79 scen
  9edf1ab7 ckpt 2411: LAB2 harness written: lab-only page-health probe (vfind.js probe(), guard an
  4a9a28f4 ckpt 2410: SND1a lab scene written: lab page /video/audible?kind=video|audio (big butto
  44f9516c ckpt 2409: wrote owner message 45 into TASKS.md in the owner's words: AU1 (sound must n
  d5053cd6 ckpt 2408: owner answered in plain words for messages 43 and 44 (alpha48 Release link g
  055248fc ckpt 2407: alpha48 RELEASED: tag kestrel-g-v0.3.0-alpha48 (commit 281c48f4, APK 94.7 MB
  96a8ccce ckpt 2406: TASKS: VC1b/VC1c/LP1b/LP1c ticked with the lab evidence (run 43739450975); r
  fbbd7efc ckpt 2405: lab vc run 3 (43739450975, commit ecbdecc3) all green on the REAL engine inc
  2211e3fd ckpt 2404: gated kestrel-g-v0.3.0-alpha48 (gecko code 149) and pushed it: handoff self-
  b619f02e ckpt 2403: pre-ship: alpha48: videos with no controls now get Kestrel's own: tap a vide
```

(7 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
