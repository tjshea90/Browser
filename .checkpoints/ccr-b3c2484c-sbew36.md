# CHECKPOINT 2412 — read me first, then TASKS.md

**Written:** 2026-11-15T18:34:49Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `b3e44517` (this checkpoint is the commit after it)

## Just done
hour soak 43740398387 read (commit 281c48f4, hotspot-shaped, 70 min, 79 scenes): the emulator's 5 Mbit/s shaping makes ordinary pages (wikipedia, bbc, mdn, hacker-news ...) time out or fail, so its page and public-video numbers are NOT trustworthy (the owner says fast wifi is the normal case: main runs use the fast link); VALID results: FS1 all 9 steps pass incl. Back leaves full screen, X2 guard table all 4 rows right, X1 apk guard right, VC1/LP1 lab pages right; 'crashes' were only :media / :sandboxed_tab helper processes exiting (am_proc_died counted any process: now only the main process); looped lab clips look 'stalling' only because the played-seconds read wraps at the loop point

## Do this next
1) read the snd run 43745675957 (sound while the app is left) when it finishes (~18:50): fix SND1b if audio plays while away; 2) dispatch the compat run (android.yml app=lab lab_suite=compat lab_minutes=90 lab_net=full) as soon as the snd run is running; 3) reviewer af040def2527a6855 result; 4) write research/SOAK_RUN1.md + COMPAT_RUN1.md; 5) delete heartbeat trig_01Y8CFHKVb6hmQmEEM78p6gv at the end

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M tools/emu/soak.py

## Last ten checkpoints
```
  9edf1ab7 ckpt 2411: LAB2 harness written: lab-only page-health probe (vfind.js probe(), guard an
  4a9a28f4 ckpt 2410: SND1a lab scene written: lab page /video/audible?kind=video|audio (big butto
  44f9516c ckpt 2409: wrote owner message 45 into TASKS.md in the owner's words: AU1 (sound must n
  d5053cd6 ckpt 2408: owner answered in plain words for messages 43 and 44 (alpha48 Release link g
  055248fc ckpt 2407: alpha48 RELEASED: tag kestrel-g-v0.3.0-alpha48 (commit 281c48f4, APK 94.7 MB
  96a8ccce ckpt 2406: TASKS: VC1b/VC1c/LP1b/LP1c ticked with the lab evidence (run 43739450975); r
  fbbd7efc ckpt 2405: lab vc run 3 (43739450975, commit ecbdecc3) all green on the REAL engine inc
  2211e3fd ckpt 2404: gated kestrel-g-v0.3.0-alpha48 (gecko code 149) and pushed it: handoff self-
  b619f02e ckpt 2403: pre-ship: alpha48: videos with no controls now get Kestrel's own: tap a vide
  ecbdecc3 ckpt 2402: lab vc run 2 (43738479064, commit b5fd15c4) on the REAL Firefox engine: VC1 
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
