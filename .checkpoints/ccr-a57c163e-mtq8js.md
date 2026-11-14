# CHECKPOINT 1876 — read me first, then TASKS.md

**Written:** 2026-11-14T00:43:03Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `cc028b28` (this checkpoint is the commit after it)

## Just done
gated kestrel-g-v0.3.0-alpha33 (gecko code 134) and pushed it: handoff self-test, unit suites and the versionCode check passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow_id android.yml, ref ccr-a57c163e-mtq8js, inputs {"app":"gecko","full_build":"true","note":"alpha33 (includes alpha32): 1) Kestrel opens on the Home screen when Home was the last thing in front (before, it loaded the last web page), and it draws the picture of the last page at once while the engine starts. 2) The new icon you picked, also in the Recents list, and the app is named Kestrel (the light build is Kestrel Light). 3) One press plays a video; fake play buttons that send you to spam are still blocked. 4) A blocked pop-up now shows a very small icon at the bottom middle for one second instead of the bar: press it and the blocked page opens in a new tab, no questions. 5) A one-hand mini panel at the top right: the small tab opens a black panel with Back, Home, Forward and an address bar; the bar selects the whole address and opens a small keyboard under it so the right thumb can type; the panel slides down and up and each button lights neon blue when pressed (Settings > Appearance turns the tab off). 6) Storage tidy: old installer files, update leftovers, old engine cache, old crash reports, pictures of closed tabs and stopped downloads are removed by themselves about once a day and after every update (Settings > Tidy storage now does it at once). 7) Video size estimates are measured from the stream itself, and the size shown while a video downloads follows the real bytes. 8) Diagnostics shows three new lines: Storage tidy, Video size estimates and CPU by process, so a laggy page or video can be explained from one paste."}.
When the run is green, confirm the Release shows the APK (mcp__github__get_release_by_tag kestrel-g-v0.3.0-alpha33), then:  bash tools/record-release.sh kestrel-g-v0.3.0-alpha33 "alpha33 (includes alpha32): 1) Kestrel opens on the Home screen when Home was the last thing in front (before, it loaded the last web page), and it draws the picture of the last page at once while the engine starts. 2) The new icon you picked, also in the Recents list, and the app is named Kestrel (the light build is Kestrel Light). 3) One press plays a video; fake play buttons that send you to spam are still blocked. 4) A blocked pop-up now shows a very small icon at the bottom middle for one second instead of the bar: press it and the blocked page opens in a new tab, no questions. 5) A one-hand mini panel at the top right: the small tab opens a black panel with Back, Home, Forward and an address bar; the bar selects the whole address and opens a small keyboard under it so the right thumb can type; the panel slides down and up and each button lights neon blue when pressed (Settings > Appearance turns the tab off). 6) Storage tidy: old installer files, update leftovers, old engine cache, old crash reports, pictures of closed tabs and stopped downloads are removed by themselves about once a day and after every update (Settings > Tidy storage now does it at once). 7) Video size estimates are measured from the stream itself, and the size shown while a video downloads follows the real bytes. 8) Diagnostics shows three new lines: Storage tidy, Video size estimates and CPU by process, so a laggy page or video can be explained from one paste."
NEVER ship kestrel-g-v0.3.0-alpha33 twice (check BUILDLOG.md and the Releases first).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  cc028b28 ckpt 1875: AO1 ticked in TASKS (emulator Diagnostics screen shows the CPU by process, S
  9689afe ckpt 1860: AQ1, AR1 and AR2 ticked in TASKS with evidence: Home restore proven on the em
  2874e84 ckpt 1844: AQ1: found on the emulator that restore's show() cleared the Home flag (the p
  ebe2fa5 ckpt 1829: Owner message 32 written into TASKS (AR1 blocked-page icon bottom middle 1 s,
  39cf669 ckpt 1819: Owner message 31 written into TASKS (AQ1 Home restored as last page, AQ2 cont
  fa60b0e ckpt 1812: AO1 code + research written: core/diag/CpuUse.kt (ProcStat/CpuSeries, CpuUseT
  33ceee7 ckpt 1804: AO2 DONE and ticked: SizeEstimate + VideoFlow calibration + engine running es
  da2a562 ckpt 1790: AO4 DONE and ticked: emulator proof with planted files (engine start pruned t
  6b3800f ckpt 1788: AO4 code written and green: core ApkFiles (+5 tests), app diag/StorageTidy (+
  c2b65d1 ckpt 1761: EY-1 last look ticked (emulator-proven: first-frame picture, scroll place res
```
