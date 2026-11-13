# CHECKPOINT 1514 — read me first, then TASKS.md

**Written:** 2026-11-13T07:09:31Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `f185029` (this checkpoint is the commit after it)

## Just done
gated kestrel-g-v0.3.0-alpha29 (gecko code 128) and pushed it: handoff self-test, unit suites and the versionCode check passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow_id android.yml, ref ccr-a57c163e-mtq8js, inputs {"app":"gecko","full_build":"true","note":"alpha29 (includes alpha28): fixes for pages that show no photos or buttons, and a clearer update screen. Shields off for a site now also switches Firefox's own tracker blocking off for that site: if a page is missing its pictures, icons or player, tap the shield icon and switch Shields off for the site (before this, Firefox's own protection kept blocking the libraries and players pages are built from, like on doraby.com; alpha28 already stopped blocking the common ones). The Updates screen now says exactly how much free room Android needs to install an update (this tablet needs about 3 GB free: a tenth of its storage stays free, plus room for the update), warns BEFORE a download that the installer would refuse, has a Clear cache button, lists your biggest downloads, and no longer offers install the other way when uninstalling would not free enough. Diagnostics: the Video line now shows the real decoders (it stayed on unknown), and the Storage line says how far short an update would be. Also from alpha28: Page report in Diagnostics, Reload without cache in the menu."}.
When the run is green, confirm the Release shows the APK (mcp__github__get_release_by_tag kestrel-g-v0.3.0-alpha29), then:  bash tools/record-release.sh kestrel-g-v0.3.0-alpha29 "alpha29 (includes alpha28): fixes for pages that show no photos or buttons, and a clearer update screen. Shields off for a site now also switches Firefox's own tracker blocking off for that site: if a page is missing its pictures, icons or player, tap the shield icon and switch Shields off for the site (before this, Firefox's own protection kept blocking the libraries and players pages are built from, like on doraby.com; alpha28 already stopped blocking the common ones). The Updates screen now says exactly how much free room Android needs to install an update (this tablet needs about 3 GB free: a tenth of its storage stays free, plus room for the update), warns BEFORE a download that the installer would refuse, has a Clear cache button, lists your biggest downloads, and no longer offers install the other way when uninstalling would not free enough. Diagnostics: the Video line now shows the real decoders (it stayed on unknown), and the Storage line says how far short an update would be. Also from alpha28: Page report in Diagnostics, Reload without cache in the menu."
NEVER ship kestrel-g-v0.3.0-alpha29 twice (check BUILDLOG.md and the Releases first).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  f2230eb ckpt 1511: AH1b DONE and emulator-proven with the real engine (heads-up before a refused
  11126ad ckpt 1495: AH1c done and emulator-proven end to end (Shields master switch off -> Firefo
  939a120 ckpt 1487: AH1d done and emulator-proven: the Video line stayed on 'decoders unknown' be
  7d3044a ckpt 1482: Owner message 21 (their Diagnostics copy from the tablet, alpha27) written in
  29bfbb4 ckpt 1478: handoff saved (.handoff/handoff.md + archive 2026-11-13T05-01-56Z; CLAUDE.md/
  2e2935d ckpt 1476: alpha28 SHIPPED and RECORDED: kestrel-g-v0.3.0-alpha28 (gecko code 127), run 
  ec67869 ckpt 1474: gated kestrel-g-v0.3.0-alpha28 (gecko code 127) and pushed it: handoff self-t
  a562cab ckpt 1473: alpha28 gates green: JVM (core + app + gecko compile), 123 web tests, pref na
  a3a8e82 ckpt 1470: AF1 cause found + fixed + proven in real Gecko on the emulator: Kestrel asked
  a0403ec ckpt 1458: AF1 diagnostics built (not yet compiled/shipped): extension page log (blocked
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
