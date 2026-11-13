# CHECKPOINT 1474 — read me first, then TASKS.md

**Written:** 2026-11-13T04:50:23Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `a562cab` (this checkpoint is the commit after it)

## Just done
gated kestrel-g-v0.3.0-alpha28 (gecko code 127) and pushed it: handoff self-test, unit suites and the versionCode check passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow_id android.yml, ref ccr-a57c163e-mtq8js, inputs {"app":"gecko","full_build":"true","note":"alpha28: sites that showed no photos or icons, and a report of what failed. Why doraby.com (and probably the video that kept its loading symbol) broke: the browser asked Firefox's built-in tracking protection for its strictest set, which also blocks the libraries and video players that pages are built from (jsDelivr, unpkg, JW Player, Brightcove, Kaltura, Vimeo and more), so a page loaded but its pictures, menu and search icons, or its player never arrived. Those are no longer blocked; ads and trackers are still blocked by Kestrel's own lists. New: Menu > Diagnostics > Page report lists, for each page, what Shields blocked, which files FAILED to load and why, and script errors, plus what Firefox's own protection blocked (never for private tabs); Menu > Reload without cache loads a page and all its files fresh. The Video lab now measures the real frame rate (it showed 24.8 for clips that played perfectly)."}.
When the run is green, confirm the Release shows the APK (mcp__github__get_release_by_tag kestrel-g-v0.3.0-alpha28), then:  bash tools/record-release.sh kestrel-g-v0.3.0-alpha28 "alpha28: sites that showed no photos or icons, and a report of what failed. Why doraby.com (and probably the video that kept its loading symbol) broke: the browser asked Firefox's built-in tracking protection for its strictest set, which also blocks the libraries and video players that pages are built from (jsDelivr, unpkg, JW Player, Brightcove, Kaltura, Vimeo and more), so a page loaded but its pictures, menu and search icons, or its player never arrived. Those are no longer blocked; ads and trackers are still blocked by Kestrel's own lists. New: Menu > Diagnostics > Page report lists, for each page, what Shields blocked, which files FAILED to load and why, and script errors, plus what Firefox's own protection blocked (never for private tabs); Menu > Reload without cache loads a page and all its files fresh. The Video lab now measures the real frame rate (it showed 24.8 for clips that played perfectly)."
NEVER ship kestrel-g-v0.3.0-alpha28 twice (check BUILDLOG.md and the Releases first).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  a562cab ckpt 1473: alpha28 gates green: JVM (core + app + gecko compile), 123 web tests, pref na
  a3a8e82 ckpt 1470: AF1 cause found + fixed + proven in real Gecko on the emulator: Kestrel asked
  a0403ec ckpt 1458: AF1 diagnostics built (not yet compiled/shipped): extension page log (blocked
  697f62b ckpt 1451: Owner message 20 (their Video lab / Tap lab / Decode lab numbers from the tab
  101b4b1 ckpt 1447: Owner message 19 (doraby.com: no photos, no search bar in Kestrel; Brave fine
  c0c149f ckpt 1443: alpha27 SHIPPED and RECORDED: kestrel-g-v0.3.0-alpha27 (gecko code 126), run 
  d107bc4 ckpt 1440: alpha27 (kestrel-g-v0.3.0-alpha27, gecko code 126) gated, pushed and DISPATCH
  f453a50 ckpt 1439: gated kestrel-g-v0.3.0-alpha27 (gecko code 126) and pushed it: handoff self-t
  6b35128 ckpt 1437: AE1 alpha27 built+tested: autoplay fix PROVEN on emulator, DRM prompt proven,
  fd0f98f ckpt 1426: AE1: CAUSE OF STUCK LOADING SYMBOL PROVEN on emulator with alpha26 (muted aut
```
