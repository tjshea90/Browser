# CHECKPOINT 2773 — read me first, then TASKS.md

**Written:** 2026-11-20T22:43:04Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/sweet-gates-gr3bj5` · **builds on:** `6732af16` (this checkpoint is the commit after it)

## Just done
alpha78 RELEASED AND RECORDED (kestrel-g-v0.3.0-alpha78, gecko code 179, run 44829167290 all jobs green, APK 96.6 MB + sha256 + .dm in the Release, BUILDLOG.md line pushed). YTC2: Mozilla's three YouTube interventions are switched off for YouTube only (run.js match patterns -> youtube.invalid at build time, build check swapped >= 3, core SiteFixesRulesTest guard). NOT proven to be the cause; not run on the tablet.

## Do this next
NOTHING is running. YTC1/YTC2 OPEN: waiting for the owner: does the YouTube control vanish still on alpha78? If yes, which Settings switch makes the controls stay (Site fixes from Mozilla / Videos start only when I press play / Skip ads inside video players / Video controls / Tap the video bar to jump / Block pop-ups), full screen or in the page, and the Diagnostics line 'Full-screen video controls' (alpha77: 'controls shown N times, M hid again within 1.5 s (last: why after ms)'). Read research/YTC1_YOUTUBE_CONTROLS.md first. If it is none of those: build a page-side tap timeline recorder (content.js: events + the player's class changes for 2 s after a trusted tap, sent as k=tl, shown in Diagnostics). Also open: INBOX/OUTBOX capture OFF (private clone ../kestrel-private missing, add_repo denied twice by the permission classifier), PB8, a lab scene for YouTube taps (the emulator lab never loaded YouTube)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  c8ad5f40 ckpt 2772: REL78b: alpha78 DISPATCHED ONCE (android.yml app=gecko full_build=true ref c
  74fab19a ckpt 2771: gated kestrel-g-v0.3.0-alpha78 (gecko code 179) and pushed it: handoff self-
  afb6d504 ckpt 2770: YTC2: cause NOT proven. Read every tap path again (ControlsMachine, tap matc
  2ffbb683 ckpt 2769: YTC2 written into TASKS.md in the owner's words: alpha77 did not change the 
  36547fc5 ckpt 2768: alpha77 RELEASED AND RECORDED (kestrel-g-v0.3.0-alpha77, gecko code 178, run
  3f0787f7 ckpt 2767: REL77b: alpha77 DISPATCHED ONCE (android.yml app=gecko full_build=true ref c
  2a4e281c ckpt 2766: gated kestrel-g-v0.3.0-alpha77 (gecko code 178) and pushed it: handoff self-
  855b173d ckpt 2765: YTC1: not reproduced (the YouTube film does not start behind its bot check i
  1cfd914e ckpt 2764: YTC1 written into TASKS.md in the owner's words (mid-turn owner message: You
  61ca6460 ckpt 2763: REL76b: alpha76 DISPATCHED ONCE (android.yml app=gecko full_build=true ref c
```
