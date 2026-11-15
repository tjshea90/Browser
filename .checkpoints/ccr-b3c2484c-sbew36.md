# CHECKPOINT 2411 — read me first, then TASKS.md

**Written:** 2026-11-15T18:21:47Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `794c3114` (this checkpoint is the commit after it)

## Just done
LAB2 harness written: lab-only page-health probe (vfind.js probe(), guard answer lab flag 256 only in a DEBUG build, background relay, Health line probe page), scroll-burst Health lines, debug hooks measure / shields on|off|hosts / clearcache, driver: screen_stats (blank-page reading of the raw framebuffer), console_summary (the engine's own page script errors from logcat), per-page probe / console / scroll / memory on every measured page, ~100-site COMPAT_SITES, compat_round (every site twice: Shields ON then OFF from a cold cache), report sections (speed, pages not loading correctly, Shields ON vs OFF with a WORSE WITH SHIELDS verdict, scrolling, memory, errors); web test for the probe; Kotlin compile + guard tests green

## Do this next
1) hour soak 43740398387 ends ~18:36 (read it: artifacts, research/SOAK_RUN1.md); 2) the snd run 43745675957 (sound while the app is left) starts after it: read the 'Sound when the app is left' table; if audio plays while away fix it (SND1b); 3) dispatch android.yml app=lab lab_suite=compat lab_minutes=90 lab_net=full after the snd run starts (a second lab dispatch before that would replace the pending snd run); read research COMPAT_RUN1; 4) reviewer af040def2527a6855 (video bar scripts) result pending

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  4a9a28f4 ckpt 2410: SND1a lab scene written: lab page /video/audible?kind=video|audio (big butto
  44f9516c ckpt 2409: wrote owner message 45 into TASKS.md in the owner's words: AU1 (sound must n
  d5053cd6 ckpt 2408: owner answered in plain words for messages 43 and 44 (alpha48 Release link g
  055248fc ckpt 2407: alpha48 RELEASED: tag kestrel-g-v0.3.0-alpha48 (commit 281c48f4, APK 94.7 MB
  96a8ccce ckpt 2406: TASKS: VC1b/VC1c/LP1b/LP1c ticked with the lab evidence (run 43739450975); r
  fbbd7efc ckpt 2405: lab vc run 3 (43739450975, commit ecbdecc3) all green on the REAL engine inc
  2211e3fd ckpt 2404: gated kestrel-g-v0.3.0-alpha48 (gecko code 149) and pushed it: handoff self-
  b619f02e ckpt 2403: pre-ship: alpha48: videos with no controls now get Kestrel's own: tap a vide
  ecbdecc3 ckpt 2402: lab vc run 2 (43738479064, commit b5fd15c4) on the REAL Firefox engine: VC1 
  b5fd15c4 ckpt 2401: lab vc run 1 (43737466605) read: LP1 long press menu on all four lab pages a
```

(8 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
