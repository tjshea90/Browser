# CHECKPOINT 2768 — read me first, then TASKS.md

**Written:** 2026-11-20T21:21:22Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/sweet-gates-gr3bj5` · **builds on:** `dd5b735b` (this checkpoint is the commit after it)

## Just done
alpha77 RELEASED AND RECORDED (kestrel-g-v0.3.0-alpha77, gecko code 178, run 44817234719 all jobs green, APK 96.6 MB + sha256 + .dm in the Release, BUILDLOG.md line pushed). alpha76 likewise (run 44811598154). RVW5 ticked. YTC1 stays OPEN: not reproduced (YouTube's bot check stops the film in the sandbox), the Diagnostics line 'controls shown N times, M hid again within 1.5 s (last: why after ms)' ships in alpha77 so the owner's next paste decides.

## Do this next
NOTHING is running. Waiting for the owner: (1) is the YouTube film full screen or in the page when the controls vanish? (2) Diagnostics lines 'Full-screen video controls', 'Tap on a video bar', 'Video bar' after it happens (read with research/YTC1_YOUTUBE_CONTROLS.md: M>0 with 'tap after 300-400 ms' = a duplicate tap (check 'page copies dropped'), 'clock' = timer bug, M=0 = YouTube's own chrome in the page). Also open: INBOX/OUTBOX capture is OFF because the private clone ../kestrel-private is missing and add_repo kestrel-private was denied twice by the permission classifier (owner must allow it, or clone by hand). Open ideas: PB8 (time ExoPlayer.release), real-tablet numbers for PB9/PB10, a lab scene that taps a playing YouTube film and samples the controls (tools/emu/soak.py yt suite), og:title not used on purpose (research/REVIEW_2026-11-23_RVW5.md)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  3f0787f7 ckpt 2767: REL77b: alpha77 DISPATCHED ONCE (android.yml app=gecko full_build=true ref c
  2a4e281c ckpt 2766: gated kestrel-g-v0.3.0-alpha77 (gecko code 178) and pushed it: handoff self-
  855b173d ckpt 2765: YTC1: not reproduced (the YouTube film does not start behind its bot check i
  1cfd914e ckpt 2764: YTC1 written into TASKS.md in the owner's words (mid-turn owner message: You
  61ca6460 ckpt 2763: REL76b: alpha76 DISPATCHED ONCE (android.yml app=gecko full_build=true ref c
  5eb5e6aa ckpt 2762: gated kestrel-g-v0.3.0-alpha76 (gecko code 177) and pushed it: handoff self-
  7df4d1c8 ckpt 2761: RVW5 gates all green: :core:test 1916 (OwnerRulesTest guard kept: VideoLevel
  8665429e ckpt 2760: RVW5: coded + tested in core (media tests 524 of 524 green): NA1 title clean
  58b7c173 ckpt 2759: RVW5 written into TASKS.md in the owner's words (new levers: speed, playback
  80ae116d ckpt 2758: RVW4 DONE AND SHIPPED: alpha75 RELEASED AND RECORDED = kestrel-g-v0.3.0-alph
```
