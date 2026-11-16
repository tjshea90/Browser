# CHECKPOINT 2455 — read me first, then TASKS.md

**Written:** 2026-11-16T00:31:44Z · **tests:** 1 RED: test_handoff.sh (0 green)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `745728d1` (this checkpoint is the commit after it)

## Just done
lab fsy run 43778698760: kp scene 22/22 ok again, sound ok, Yahoo live player went full screen (page had the new size after 1.7-1.9 s, turn waits for it) with Kestrel's controls over it, BUT Yahoo's own bottom bar (play, volume, CC) was still visible under/next to Kestrel's row in picture yahoofs-full (headless Chromium test of the real player.js on the live Yahoo DOM hides .shaka-controls-container fine, so the Firefox page differs somehow): player.js now counts the elements it put away (aw in every report, relay + Health 'player away' + Diagnostics), takes display:none !important back onto an element the page re-shows, lab suite yh = only the live Yahoo scene with a check (controls started, away count); fsy's old 'tap shows Kestrel bar' check now accepts the native controls; red-strip test sees a dimmed strip (the controls' scrim made 'right' read 'stale': the slow-turn scene's 3 high issues were that, plus renice read back -10 so the scene does not slow only the engine); landscape picture kp-5b added; version bumped to 156 / alpha55 in geckoapp/build.gradle.kts; all Kotlin suites + web-tests (472) green before the last small edits (player/background tests 99 ok after)

## Do this next
dispatch android.yml app=lab lab_suite=yh lab_minutes=5 lab_net=full (ref ccr-b3c2484c-sbew36), read report.md section 'The owner's Yahoo Finance player': 'Kestrel's own controls started; page controls put away: [..]' and picture yahoofs-full; if away is empty or Yahoo's bar is still visible: find the real Firefox DOM (add a debug dump) or hide generic control elements; the independent code review agent a75f62ac18ae8368d is still running (apply its real findings); then full suites, bash ship.sh gecko with scratchpad note55.txt, dispatch app=gecko, record-release, final answer to owner message 49

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  8eab718d ckpt 2454: kp lab run 5 (43778006356, 512 MB heap boot): 22 of 22 checks GREEN on the r
  b74a874d ckpt 2453: kp lab run 3 (43776782679, good boot 512 MB heap): 18 of 21 checks pass on t
  4b0bd9aa ckpt 2452: kp lab run 2 (43775890786) read: it says NOTHING about the controls: the emu
  05a26786 ckpt 2451: VP1 lab scene kp fixed after the first real-engine run (soak.py kp_scene: pa
  a7ae2f8b ckpt 2450: VP1 lab scene kp written (soak.py kp_scene with exact multi-taps via the tou
  ca648b5d ckpt 2449: VP1 first cut compiles and is tested: player.js (state reports, tap shield, 
  df745dca ckpt 2448: gated kestrel-g-v0.3.0-alpha54 (gecko code 155) and pushed it: handoff self-
  17392233 ckpt 2447: pre-ship: alpha54: fixes from an independent check of alpha53 (the new video
  aa9dfa02 ckpt 2446: owner message 49 (video player audit + overhaul request, with observations o
  4e91027c ckpt 2445: ROT3 coded (not shipped): FullscreenTurner waits 650 ms of the tablet stayin
```

(7 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
