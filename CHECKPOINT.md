# CHECKPOINT 2547 — read me first, then TASKS.md

**Written:** 2026-11-16T18:26:20Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `ac17a357` (this checkpoint is the commit after it)

## Just done
lab run 43927135769 (gp, head 42e933e5): Kestrel's OWN player passes every gesture check on the real engine (rest = gesture-armed, hold + 4 cm = +40 s, 6 cm = +193 s, 4 cm left = -40 s, an inch up = +13 s, volume 27->91, brightness 41->92, dead zone, double tap + controls away) = ANS57a/b(A)/c(A) proven; the inline film (B) failed only because the debug am start paused the screen ('the app was left'); DebugTouch is now a debug-only BROADCAST receiver (am broadcast -a app.kestrel.browser.DEBUG_TOUCH -p pkg --es script ...), attached in both screens, only the window with focus plays it; compile green; heartbeat trig_01LyMjhKLgSaFigb7F61XAfe

## Do this next
1) dispatch lab gp again on this head (android.yml app=lab lab_suite=gp lab_minutes=1 lab_net=full): B (inline hold-first volume / brightness / seek, plain drag scrolls the page, double tap hides controls) must pass; read report.md; fix what fails; 2) tick ANS57b/c (B) in TASKS.md with the run id; lab vp (all video suites) once gp is green; 3) ship alpha60 (versionCode 161, 0.3.0-alpha60, tools/sync-ext-version.sh, fill TESTED_PLACEHOLDER in docs/handoff/ALPHA60_RELEASE_NOTE.txt, bash ship.sh gecko, ONE dispatch, record-release.sh); 4) answer owner 55, 57, 58, 59 and delete the heartbeat.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  d6fe07e1 ckpt 2546: heartbeat created: trig_01LyMjhKLgSaFigb7F61XAfe (every 2 h, bound to this s
  42e933e5 ckpt 2545: lab run 43925029376 (gv, head 3c1b38d8): pv scene 7/7 OK on the real engine 
  306ef253 ckpt 2544: dispatched lab gv run 43925029376 (head 3c1b38d8: input motionevent finger p
  3c1b38d8 ckpt 2543: lab touch fix coded (not yet run): raw dd finger paths never reached the app
  dcf083e4 ckpt 2542: ANS59a coded: regular tabs are not put to sleep by a trip to another app eit
  421a0dd9 ckpt 2541: owner message 59 written into TASKS.md (ANS59a-b): 'It should work for regul
  66aa33a7 ckpt 2540: ANS58a/b first cheap measure: kestrel-shields manifest exclude_matches for t
  a23e57dc ckpt 2539: lab suite gv = gp + pv in ONE emulator boot (the queue of GitHub was slow: t
  c70f2aac ckpt 2538: ANS58c FIX coded + tested: private tabs are NEVER put to sleep by the memory
  c0017fa7 ckpt 2537: owner message 58 written into TASKS.md (ANS58a-d): a PRIVATE tab was reloade
```

(6 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
