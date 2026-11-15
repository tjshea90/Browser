# CHECKPOINT 2404 — read me first, then TASKS.md

**Written:** 2026-11-15T17:15:16Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `822b2a16` (this checkpoint is the commit after it)

## Just done
gated kestrel-g-v0.3.0-alpha48 (gecko code 149) and pushed it: handoff self-test, unit suites and the versionCode check passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow_id android.yml, ref ccr-b3c2484c-sbew36, inputs {"app":"gecko","full_build":"true","note":"alpha48: videos with no controls now get Kestrel's own: tap a video whose page shows no buttons of its own (the player on finance.yahoo.com has none while its ad plays) and a small bar comes up on the picture with play / pause, a track you can tap or drag to jump (with the time), mute and full screen; it goes away by itself and does not appear where the page has its own controls (Settings row Video controls: Automatic, Always, Off). A long press on a video now opens the video options (save the video, copy its address) instead of the floating panel, also for players that keep the film inside a web component, under a see-through layer or under an ad frame (that is why it failed on Yahoo). Full-screen video turns like in a normal video player: a film that is wider than tall goes sideways by itself when you tap full screen with the tablet upright, turning the tablet follows (also when Android's own rotation lock is on), a round turn button at the left edge turns it the other way, Back leaves full screen first (Settings row Turn the screen for full-screen video). A download that nobody tapped now asks first, and an installer that claims to be Kestrel but did not come from the updater is refused, even when renamed to look like a video. Fixes from an independent check of this version: a site's own screen lock was misread, full screen could stay stuck after its tab was killed, closed or put to sleep, a web page could have posed as the updater, a tapped link that redirects was treated as started by itself, and several questions could pile up. The About text on Safe Browsing is corrected (the engine's own protection works here). New numbers in Diagnostics: video controls, dropped video frames, repeated page checks, the wait before pages with ad-hiding rules, and an off-by-default switch Measure scrolling on real pages. Tested in an Android 10 emulator on GitHub with the real Firefox engine and in test browsers, not on your tablet: please try the Yahoo Finance video, a long press on it, and one film in full screen."}.
When the run is green, confirm the Release shows the APK (mcp__github__get_release_by_tag kestrel-g-v0.3.0-alpha48), then:  bash tools/record-release.sh kestrel-g-v0.3.0-alpha48 "alpha48: videos with no controls now get Kestrel's own: tap a video whose page shows no buttons of its own (the player on finance.yahoo.com has none while its ad plays) and a small bar comes up on the picture with play / pause, a track you can tap or drag to jump (with the time), mute and full screen; it goes away by itself and does not appear where the page has its own controls (Settings row Video controls: Automatic, Always, Off). A long press on a video now opens the video options (save the video, copy its address) instead of the floating panel, also for players that keep the film inside a web component, under a see-through layer or under an ad frame (that is why it failed on Yahoo). Full-screen video turns like in a normal video player: a film that is wider than tall goes sideways by itself when you tap full screen with the tablet upright, turning the tablet follows (also when Android's own rotation lock is on), a round turn button at the left edge turns it the other way, Back leaves full screen first (Settings row Turn the screen for full-screen video). A download that nobody tapped now asks first, and an installer that claims to be Kestrel but did not come from the updater is refused, even when renamed to look like a video. Fixes from an independent check of this version: a site's own screen lock was misread, full screen could stay stuck after its tab was killed, closed or put to sleep, a web page could have posed as the updater, a tapped link that redirects was treated as started by itself, and several questions could pile up. The About text on Safe Browsing is corrected (the engine's own protection works here). New numbers in Diagnostics: video controls, dropped video frames, repeated page checks, the wait before pages with ad-hiding rules, and an off-by-default switch Measure scrolling on real pages. Tested in an Android 10 emulator on GitHub with the real Firefox engine and in test browsers, not on your tablet: please try the Yahoo Finance video, a long press on it, and one film in full screen."
NEVER ship kestrel-g-v0.3.0-alpha48 twice (check BUILDLOG.md and the Releases first).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  b619f02e ckpt 2403: pre-ship: alpha48: videos with no controls now get Kestrel's own: tap a vide
  ecbdecc3 ckpt 2402: lab vc run 2 (43738479064, commit b5fd15c4) on the REAL Firefox engine: VC1 
  b5fd15c4 ckpt 2401: lab vc run 1 (43737466605) read: LP1 long press menu on all four lab pages a
  0acf028c ckpt 2400: lab driver: hotspot shaping without the emulator delay option (it made hacke
  4e233ebe ckpt 2399: VC1/LP1/RV1-RV9 verified: Kotlin suite (core + app + geckoapp tests + geckoa
  31e0d580 ckpt 2398: RV3 (updater spoof: fromUpdater needs the app-private download kind), RV4 (r
  a61cff67 ckpt 2397: RV1 (site orientation lock = Android constants, verified in the GeckoView 16
  fa81920c ckpt 2396: VC1 + LP1 first working version: vfind.js (finds videos in shadow roots / un
  6b3d7aba ckpt 2395: independent review of the alpha48 diff read (research/REVIEW_ALPHA48.md, 9 f
  a74e3649 ckpt 2394: wrote owner message 44 into TASKS.md in the owner's words: VC1 (video has no
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
