# CHECKPOINT 2805 — read me first, then TASKS.md

**Written:** 2026-11-21T07:55:14Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/sweet-gates-gr3bj5` · **builds on:** `6eac0f1a` (this checkpoint is the commit after it)

## Just done
gated kestrel-g-v0.3.0-alpha83 (gecko code 184) and pushed it: handoff self-test, unit suites and the versionCode check passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow_id android.yml, ref claude/sweet-gates-gr3bj5, inputs {"app":"gecko","full_build":"true","note":"alpha83: the live diagnostics are smarter, more complete, and tell YOU which settings to change. (1) SUGGESTED CHANGES: Settings, Live diagnostics (for Claude), now lists suggestions made from what the tablet really measured, in the words Settings uses, with the numbers: for example Appearance, Tabs kept ready: change Automatic to Few (2), because the page engine was killed 3 times; or Load pages through the phone, because 2 sites took over 12 seconds; or HTTP/3 off when a quarter of page loads fail; a calmer Neon glow when screen updates are slow; At most 30 frames a second when the player drops frames; a smaller Picture size when video waits for the network; Instant Back; Tidy storage; older block lists; a failing site's Shields. A suggestion that is one setting has a tap that makes the change (only 8 settings can be changed this way); the rest are advice. Claude reads the same suggestions from the files and tells you too. (2) MORE COMPLETE DATA: per-site page load tables (loads, failures, median and worst time, never a sensitive site), how often each kind of event happened, memory, free storage, heat, link speed, the player counters, and which settings you changed (switches and small numbers only, never an address or your phone pairing code). (3) OFF MEANS ASLEEP: with the switch off nothing is recorded, counted, analysed or sent, no timer is left waiting (the 2 minute delayed send is cancelled), a send already in progress stops before sending anything, and what was kept is erased. (4) Two privacy fixes found before any data was sent: an address written without https in front now keeps only the site name, and network addresses (like the phone address on the hotspot) are removed. As before: only to your PRIVATE repository with your own token, at most 4 small files a day. NOT run on the tablet and not run against the real GitHub; the thresholds of the suggestions are first guesses that the real data will tune."}.
When the run is green, confirm the Release shows the APK (mcp__github__get_release_by_tag kestrel-g-v0.3.0-alpha83), then:  bash tools/record-release.sh kestrel-g-v0.3.0-alpha83 "alpha83: the live diagnostics are smarter, more complete, and tell YOU which settings to change. (1) SUGGESTED CHANGES: Settings, Live diagnostics (for Claude), now lists suggestions made from what the tablet really measured, in the words Settings uses, with the numbers: for example Appearance, Tabs kept ready: change Automatic to Few (2), because the page engine was killed 3 times; or Load pages through the phone, because 2 sites took over 12 seconds; or HTTP/3 off when a quarter of page loads fail; a calmer Neon glow when screen updates are slow; At most 30 frames a second when the player drops frames; a smaller Picture size when video waits for the network; Instant Back; Tidy storage; older block lists; a failing site's Shields. A suggestion that is one setting has a tap that makes the change (only 8 settings can be changed this way); the rest are advice. Claude reads the same suggestions from the files and tells you too. (2) MORE COMPLETE DATA: per-site page load tables (loads, failures, median and worst time, never a sensitive site), how often each kind of event happened, memory, free storage, heat, link speed, the player counters, and which settings you changed (switches and small numbers only, never an address or your phone pairing code). (3) OFF MEANS ASLEEP: with the switch off nothing is recorded, counted, analysed or sent, no timer is left waiting (the 2 minute delayed send is cancelled), a send already in progress stops before sending anything, and what was kept is erased. (4) Two privacy fixes found before any data was sent: an address written without https in front now keeps only the site name, and network addresses (like the phone address on the hotspot) are removed. As before: only to your PRIVATE repository with your own token, at most 4 small files a day. NOT run on the tablet and not run against the real GitHub; the thresholds of the suggestions are first guesses that the real data will tune."
NEVER ship kestrel-g-v0.3.0-alpha83 twice (check BUILDLOG.md and the Releases first).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  6eac0f1a ckpt 2804: LD2e: full gate green (:core:test :app:testDebugUnitTest :geckoapp:compileDe
  d2831830 ckpt 2803: LD2a-d built and tested: app LiveDiag (tables fed after switch+whitelist, fa
  9ebf9afd ckpt 2802: LD2 core written + tested (33 tests in core livediag): DiagStats (event hist
  6719ce3e ckpt 2801: gated kestrel-g-v0.3.0-alpha82 (gecko code 183) and pushed it: handoff self-
  f95e13ba ckpt 2800: LD2 written into TASKS.md in the owner's words (LD2a-e): smarter + more comp
  946369a8 ckpt 2799: alpha82 prepared: versionCode 183 / 0.3.0-alpha82, extension version synced,
  2d4c1e81 ckpt 2798: alpha81 SHIPPED and RECORDED: run 44878029773 green (check, test-js, test-jv
  dba49a28 ckpt 2797: LD1 privacy fix 2 (for alpha82): DiagScrub removes dotted-quad network addre
  db02a2b5 ckpt 2796: LD1 privacy fix (for alpha82): DiagScrub now drops the path after a BARE hos
  472a364c ckpt 2795: alpha81 DISPATCHED once: android.yml run 44878029773 (run_number 263, head 7
```
