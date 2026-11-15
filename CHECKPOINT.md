# CHECKPOINT 2381 — read me first, then TASKS.md

**Written:** 2026-11-15T05:51:34Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `1cbc464e` (this checkpoint is the commit after it)

## Just done
FS1 code: core/media/FullscreenTurn (pure rule + 20 tests), geckoapp/web/FullscreenTurner (requestedOrientation, sensor release, rotate button), OrientationDelegate wired in BrowserActivity, Settings choice 'Turn the screen for full-screen video' (default Automatic), Diagnostics line, picture size per tab; compile + core/app unit tests green

## Do this next
FS1: check dom.screenorientation.allow-lock exists in libxul (unzip AAR in scratchpad) and add to gecko-prefs.txt + readBackPrefs; add tools/sitelab fs page (video + lock buttons) and tools/emu driver for dumpsys requestedOrientation; write research/VIDEO_SMOOTHNESS.md section FS1; then SOAK1 (emu-lab.yml + tools/emu/soak.py)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  f81e0818 ckpt 2380: saved research/REVIEW-EXTERNAL-4.md (byte-identical copy of the upload) and 
  1f33dcab ckpt 2379: alpha47 RELEASED and recorded (run 43680608062 green, BUILDLOG line written,
  0665c59e ckpt 2378: alpha47 DISPATCHED: workflow android.yml run 43680608062 (run_number 99, ref
  99978930 ckpt 2377: gated kestrel-g-v0.3.0-alpha47 (gecko code 148) and pushed it: handoff self-
  943f5a5e ckpt 2376: YS1: independent review of seekbar.js applied (real YouTube bar markup from 
  021ec124 ckpt 2375: YS1: seekbar.js wired in (manifest, guard flag 16, sbk relay, Settings row '
  b225c11a ckpt 2374: YS1b: seekbar.js (tap/slide on a player's progress bar -> video jumps when t
  a7c3b677 ckpt 2373: owner message 42 written into TASKS.md as YS1 (+YS1a find why a tap on the Y
  43ba62e0 ckpt 2372: alpha46 (gecko code 147) RELEASED + RECORDED (run 43671662558 green, APK on 
  ba09cf6f ckpt 2371: alpha46 (gecko code 147) gated (JVM: core, tools, app incl. MiniNavTest 95, 
```

(9 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
