# CHECKPOINT 2973 — read me first, then TASKS.md

**Written:** 2026-11-22T07:43:16Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-144aedec-7apwin` · **builds on:** `bcad82c7` (this checkpoint is the commit after it)

## Just done
gated kestrel-g-v0.3.0-alpha93 (gecko code 194) and pushed it: handoff self-test, unit suites and the versionCode check passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow_id android.yml, ref ccr-144aedec-7apwin, inputs {"app":"gecko","full_build":"true","note":"alpha93: quality button in the full-screen player, the stuck loading circle, and a proof line for the clipped circle. (1) Your upload shows why the full-screen player had no quality controls: after a fresh page load the first press finds no film yet, so Kestrel's player starts a moment later (the late hand-over) and at that moment it had not read the film's other qualities. It now reads all the film's lists at once when a press is declined, and gives the player that is already up the other qualities as soon as they are read, so the quality button appears in full screen and in the page. (2) The loading circle that stays on the site's player (pornhub's player waits for a film Kestrel is holding or has taken over): a big video that is paused and never played, with a circle that spins for ever on it, now shows a play mark instead of the circle (tap it as before); the mark goes away when the video plays. A box cutting the circle off is a page layout fact, so the app also logs which element the circle is and whether a parent box clips it and by how many pixels (line video calm in Diagnostics): that settles whether the clipped circle is the page or the engine's drawing. (3) A line engine draw-prefs now tells what the engine really has for the Safe drawing settings. Not run on the tablet: the circle change and the quality button (the emulator has no pornhub)."}.
When the run is green, confirm the Release shows the APK (mcp__github__get_release_by_tag kestrel-g-v0.3.0-alpha93), then:  bash tools/record-release.sh kestrel-g-v0.3.0-alpha93 "alpha93: quality button in the full-screen player, the stuck loading circle, and a proof line for the clipped circle. (1) Your upload shows why the full-screen player had no quality controls: after a fresh page load the first press finds no film yet, so Kestrel's player starts a moment later (the late hand-over) and at that moment it had not read the film's other qualities. It now reads all the film's lists at once when a press is declined, and gives the player that is already up the other qualities as soon as they are read, so the quality button appears in full screen and in the page. (2) The loading circle that stays on the site's player (pornhub's player waits for a film Kestrel is holding or has taken over): a big video that is paused and never played, with a circle that spins for ever on it, now shows a play mark instead of the circle (tap it as before); the mark goes away when the video plays. A box cutting the circle off is a page layout fact, so the app also logs which element the circle is and whether a parent box clips it and by how many pixels (line video calm in Diagnostics): that settles whether the clipped circle is the page or the engine's drawing. (3) A line engine draw-prefs now tells what the engine really has for the Safe drawing settings. Not run on the tablet: the circle change and the quality button (the emulator has no pornhub)."
NEVER ship kestrel-g-v0.3.0-alpha93 twice (check BUILDLOG.md and the Releases first).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  bf393943 ckpt 2971: pre-ship: alpha93: quality button in the full-screen player, the stuck loadi
  335cc029 ckpt 2968: PERF8 code (not released): (a) VideoFlow.offerSiblings gives the player that
  06774fc1 ckpt 2959: PERF8 written to TASKS.md (owner after alpha92: fullscreen Kestrel player wo
  f41f8f8b ckpt 2958: alpha92 RELEASED AND RECORDED: kestrel-g-v0.3.0-alpha92 (gecko code 193, run
  292067e6 ckpt 2955: alpha92 (gecko code 193) gated by ship.sh (core+app suites, lint, handoff se
  df0b4a8a ckpt 2954: gated kestrel-g-v0.3.0-alpha92 (gecko code 193) and pushed it: handoff self-
  2db9f72a ckpt 2952: pre-ship: alpha92: quality choices for pornhub in Kestrel's player, and a Sa
  ee6c59f9 ckpt 2938: PERF7c step 1: Settings > Network lab 'Safe drawing' (lab_draw, default ON) 
  449480c7 ckpt 2933: PERF7 started on branch ccr-144aedec-7apwin: owner request + 2 screenshots w
  4da54c18 ckpt 2915: alpha91 RELEASED and recorded (run 44987313373 green, Release has the APK). 
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
