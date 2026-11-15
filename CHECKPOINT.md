# CHECKPOINT 2416 — read me first, then TASKS.md

**Written:** 2026-11-15T19:14:15Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `b2bd0691` (this checkpoint is the commit after it)

## Just done
gated kestrel-g-v0.3.0-alpha49 (gecko code 150) and pushed it: handoff self-test, unit suites and the versionCode check passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow_id android.yml, ref ccr-b3c2484c-sbew36, inputs {"app":"gecko","full_build":"true","note":"alpha49: full-screen video fixes after your three screenshots. (1) In full screen the page is now laid out INSIDE the system bars when this tablet keeps them (both bars stayed visible and a player's controls sat under the navigation bar); where the system hides the bars the picture is still full-bleed; Diagnostics has a new line Full-screen system bars that says what your tablet did. (2) Turning the tablet under a full-screen or playing video: the video is paused and the picture covered with black until the screen has settled (about a second), then it plays again by itself; a video you had paused stays paused and a touch during the turn leaves it paused. (3) Kestrel's own video bar now also shows inside a full-screen player that is a web component (Yahoo's kind), and a film that a page takes full screen on its own gets the engine's controls. Sound: it already stopped when you leave the app or change tab (checked on the emulator: the clip's clock stood still while the app was away); as a safety net the app now also pauses a video that is left playing under Kestrel's Home screen or on another tab. From a second independent check of the video bar: a link, button or picture over a decorative video no longer opens the video menu or the bar, a press released a moment after the long press no longer brings the bar up behind the video menu, two ad frames of one size no longer confuse it, a web page can no longer switch the menus off or cancel your taps, and the page's own controls are found on pages with very many links. Tested in headless browsers and on an Android 10 emulator with the real Firefox engine, NOT on your tablet: please try the Yahoo Finance video in full screen, turn the tablet both ways, and send Diagnostics if a bar still covers anything."}.
When the run is green, confirm the Release shows the APK (mcp__github__get_release_by_tag kestrel-g-v0.3.0-alpha49), then:  bash tools/record-release.sh kestrel-g-v0.3.0-alpha49 "alpha49: full-screen video fixes after your three screenshots. (1) In full screen the page is now laid out INSIDE the system bars when this tablet keeps them (both bars stayed visible and a player's controls sat under the navigation bar); where the system hides the bars the picture is still full-bleed; Diagnostics has a new line Full-screen system bars that says what your tablet did. (2) Turning the tablet under a full-screen or playing video: the video is paused and the picture covered with black until the screen has settled (about a second), then it plays again by itself; a video you had paused stays paused and a touch during the turn leaves it paused. (3) Kestrel's own video bar now also shows inside a full-screen player that is a web component (Yahoo's kind), and a film that a page takes full screen on its own gets the engine's controls. Sound: it already stopped when you leave the app or change tab (checked on the emulator: the clip's clock stood still while the app was away); as a safety net the app now also pauses a video that is left playing under Kestrel's Home screen or on another tab. From a second independent check of the video bar: a link, button or picture over a decorative video no longer opens the video menu or the bar, a press released a moment after the long press no longer brings the bar up behind the video menu, two ad frames of one size no longer confuse it, a web page can no longer switch the menus off or cancel your taps, and the page's own controls are found on pages with very many links. Tested in headless browsers and on an Android 10 emulator with the real Firefox engine, NOT on your tablet: please try the Yahoo Finance video in full screen, turn the tablet both ways, and send Diagnostics if a bar still covers anything."
NEVER ship kestrel-g-v0.3.0-alpha49 twice (check BUILDLOG.md and the Releases first).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  afdbb7c6 ckpt 2415: pre-ship: alpha49: full-screen video fixes after your three screenshots. (1)
  7c987e9f ckpt 2414: code for owner message 46 + second review: FSB1 (applyImmersive without LAYO
  7f2c4c92 ckpt 2413: owner message 46 written into TASKS.md (FSB1 bars cover the controls in full
  b8c5d333 ckpt 2412: hour soak 43740398387 read (commit 281c48f4, hotspot-shaped, 70 min, 79 scen
  9edf1ab7 ckpt 2411: LAB2 harness written: lab-only page-health probe (vfind.js probe(), guard an
  4a9a28f4 ckpt 2410: SND1a lab scene written: lab page /video/audible?kind=video|audio (big butto
  44f9516c ckpt 2409: wrote owner message 45 into TASKS.md in the owner's words: AU1 (sound must n
  d5053cd6 ckpt 2408: owner answered in plain words for messages 43 and 44 (alpha48 Release link g
  055248fc ckpt 2407: alpha48 RELEASED: tag kestrel-g-v0.3.0-alpha48 (commit 281c48f4, APK 94.7 MB
  96a8ccce ckpt 2406: TASKS: VC1b/VC1c/LP1b/LP1c ticked with the lab evidence (run 43739450975); r
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
