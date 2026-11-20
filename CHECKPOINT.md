# CHECKPOINT 2771 — read me first, then TASKS.md

**Written:** 2026-11-20T22:27:39Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/sweet-gates-gr3bj5` · **builds on:** `afb6d504` (this checkpoint is the commit after it)

## Just done
gated kestrel-g-v0.3.0-alpha78 (gecko code 179) and pushed it: handoff self-test, unit suites and the versionCode check passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow_id android.yml, ref claude/sweet-gates-gr3bj5, inputs {"app":"gecko","full_build":"true","note":"alpha78: YouTube player controls, a first fix attempt (NOT proven: it could not be tried on a real YouTube film from here). You reported twice that the controls on YouTube pop up and disappear at once. Nothing in Kestrel own controls, tap handling, ad skipper, pop-up guard or press-to-play code showed a second tap or a hide, so this release removes the one newer thing that rewrites YouTube own page behaviour: since alpha75 the bundled Mozilla site fixes carried three fixes for m.youtube.com that were written for Firefox picture-in-picture and background audio (they force the page to think it is always visible, swallow its visibility events and change the window size it reads). Kestrel has neither picture-in-picture nor background play, and YouTube had never run with them before alpha75, so they are switched off for YouTube only; every other site keeps its fixes. If the controls STILL vanish on alpha78, that was not the cause. To find the cause without waiting for a build, switch these off one at a time in Settings and tap the YouTube film after each: Site fixes from Mozilla (Network lab), Videos start only when I press play, Skip ads inside video players, Video controls, Tap the video bar to jump, Block pop-ups and forced redirects. Tell me which one makes the controls stay, and say whether the film was full screen or inside the page; the Diagnostics line Full-screen video controls from alpha77 also now counts controls that hid within a second and a half. NOT run on the tablet."}.
When the run is green, confirm the Release shows the APK (mcp__github__get_release_by_tag kestrel-g-v0.3.0-alpha78), then:  bash tools/record-release.sh kestrel-g-v0.3.0-alpha78 "alpha78: YouTube player controls, a first fix attempt (NOT proven: it could not be tried on a real YouTube film from here). You reported twice that the controls on YouTube pop up and disappear at once. Nothing in Kestrel own controls, tap handling, ad skipper, pop-up guard or press-to-play code showed a second tap or a hide, so this release removes the one newer thing that rewrites YouTube own page behaviour: since alpha75 the bundled Mozilla site fixes carried three fixes for m.youtube.com that were written for Firefox picture-in-picture and background audio (they force the page to think it is always visible, swallow its visibility events and change the window size it reads). Kestrel has neither picture-in-picture nor background play, and YouTube had never run with them before alpha75, so they are switched off for YouTube only; every other site keeps its fixes. If the controls STILL vanish on alpha78, that was not the cause. To find the cause without waiting for a build, switch these off one at a time in Settings and tap the YouTube film after each: Site fixes from Mozilla (Network lab), Videos start only when I press play, Skip ads inside video players, Video controls, Tap the video bar to jump, Block pop-ups and forced redirects. Tell me which one makes the controls stay, and say whether the film was full screen or inside the page; the Diagnostics line Full-screen video controls from alpha77 also now counts controls that hid within a second and a half. NOT run on the tablet."
NEVER ship kestrel-g-v0.3.0-alpha78 twice (check BUILDLOG.md and the Releases first).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  afb6d504 ckpt 2770: YTC2: cause NOT proven. Read every tap path again (ControlsMachine, tap matc
  2ffbb683 ckpt 2769: YTC2 written into TASKS.md in the owner's words: alpha77 did not change the 
  36547fc5 ckpt 2768: alpha77 RELEASED AND RECORDED (kestrel-g-v0.3.0-alpha77, gecko code 178, run
  3f0787f7 ckpt 2767: REL77b: alpha77 DISPATCHED ONCE (android.yml app=gecko full_build=true ref c
  2a4e281c ckpt 2766: gated kestrel-g-v0.3.0-alpha77 (gecko code 178) and pushed it: handoff self-
  855b173d ckpt 2765: YTC1: not reproduced (the YouTube film does not start behind its bot check i
  1cfd914e ckpt 2764: YTC1 written into TASKS.md in the owner's words (mid-turn owner message: You
  61ca6460 ckpt 2763: REL76b: alpha76 DISPATCHED ONCE (android.yml app=gecko full_build=true ref c
  5eb5e6aa ckpt 2762: gated kestrel-g-v0.3.0-alpha76 (gecko code 177) and pushed it: handoff self-
  7df4d1c8 ckpt 2761: RVW5 gates all green: :core:test 1916 (OwnerRulesTest guard kept: VideoLevel
```
