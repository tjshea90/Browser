# CHECKPOINT 2766 — read me first, then TASKS.md

**Written:** 2026-11-20T21:05:30Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/sweet-gates-gr3bj5` · **builds on:** `855b173d` (this checkpoint is the commit after it)

## Just done
gated kestrel-g-v0.3.0-alpha77 (gecko code 178) and pushed it: handoff self-test, unit suites and the versionCode check passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow_id android.yml, ref claude/sweet-gates-gr3bj5, inputs {"app":"gecko","full_build":"true","note":"alpha77: a way to find out why the video controls vanish (no fix yet, said plainly). You reported that when you tap a YouTube video the player controls pop up and disappear at once, and that playback is laggy with long buffering. The controls code was read line by line (the rules for when they hide, and how a tap on the glass and the same tap reported by the page are told apart) and found sound, and the YouTube page could be loaded here but not its film (YouTube shows a sign-in-to-prove-you-are-not-a-bot screen to this computer), so the problem could not be reproduced. NOT fixed and not guessed at. What is new: Diagnostics, in the line Full-screen video controls, now says how many times Kestrel own controls showed and how many of those hid again within one and a half seconds, and why the last one did (a tap, the clock, or a page button). After it happens, send that line plus the lines Tap on a video bar and Video bar, and say whether the film was full screen or still inside the page: that tells us which of Kestrel own controls or YouTube own controls is the one that vanishes. Known and unchanged: Kestrel does not play YouTube films itself (YouTube plays them in its own player), the hotspot is capped at 5 Mbit/s, and the phone-side codec rule already keeps YouTube on H.264 so the tablet decodes it in hardware. NOT run on the tablet."}.
When the run is green, confirm the Release shows the APK (mcp__github__get_release_by_tag kestrel-g-v0.3.0-alpha77), then:  bash tools/record-release.sh kestrel-g-v0.3.0-alpha77 "alpha77: a way to find out why the video controls vanish (no fix yet, said plainly). You reported that when you tap a YouTube video the player controls pop up and disappear at once, and that playback is laggy with long buffering. The controls code was read line by line (the rules for when they hide, and how a tap on the glass and the same tap reported by the page are told apart) and found sound, and the YouTube page could be loaded here but not its film (YouTube shows a sign-in-to-prove-you-are-not-a-bot screen to this computer), so the problem could not be reproduced. NOT fixed and not guessed at. What is new: Diagnostics, in the line Full-screen video controls, now says how many times Kestrel own controls showed and how many of those hid again within one and a half seconds, and why the last one did (a tap, the clock, or a page button). After it happens, send that line plus the lines Tap on a video bar and Video bar, and say whether the film was full screen or still inside the page: that tells us which of Kestrel own controls or YouTube own controls is the one that vanishes. Known and unchanged: Kestrel does not play YouTube films itself (YouTube plays them in its own player), the hotspot is capped at 5 Mbit/s, and the phone-side codec rule already keeps YouTube on H.264 so the tablet decodes it in hardware. NOT run on the tablet."
NEVER ship kestrel-g-v0.3.0-alpha77 twice (check BUILDLOG.md and the Releases first).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  855b173d ckpt 2765: YTC1: not reproduced (the YouTube film does not start behind its bot check i
  1cfd914e ckpt 2764: YTC1 written into TASKS.md in the owner's words (mid-turn owner message: You
  61ca6460 ckpt 2763: REL76b: alpha76 DISPATCHED ONCE (android.yml app=gecko full_build=true ref c
  5eb5e6aa ckpt 2762: gated kestrel-g-v0.3.0-alpha76 (gecko code 177) and pushed it: handoff self-
  7df4d1c8 ckpt 2761: RVW5 gates all green: :core:test 1916 (OwnerRulesTest guard kept: VideoLevel
  8665429e ckpt 2760: RVW5: coded + tested in core (media tests 524 of 524 green): NA1 title clean
  58b7c173 ckpt 2759: RVW5 written into TASKS.md in the owner's words (new levers: speed, playback
  80ae116d ckpt 2758: RVW4 DONE AND SHIPPED: alpha75 RELEASED AND RECORDED = kestrel-g-v0.3.0-alph
  a0ad95ec ckpt 2757: REL75b: alpha75 DISPATCHED ONCE (android.yml app=gecko full_build=true ref c
  eebefd5f ckpt 2756: gated kestrel-g-v0.3.0-alpha75 (gecko code 176) and pushed it: handoff self-
```
