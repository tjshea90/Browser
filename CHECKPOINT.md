# CHECKPOINT 2801 — read me first, then TASKS.md

**Written:** 2026-11-21T07:35:49Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/sweet-gates-gr3bj5` · **builds on:** `f95e13ba` (this checkpoint is the commit after it)

## Just done
gated kestrel-g-v0.3.0-alpha82 (gecko code 183) and pushed it: handoff self-test, unit suites and the versionCode check passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow_id android.yml, ref claude/sweet-gates-gr3bj5, inputs {"app":"gecko","full_build":"true","note":"alpha82: two privacy fixes for the live diagnostics that I found by reading what the app writes, before any data was ever sent. (1) A site address written without https in front (the video-frame notes write them that way) used to keep up to 40 characters of the path; now only the site name is kept. (2) Network addresses (for example the phone's address on the hotspot, shown on the Phone link line) are now removed from everything that is kept or sent. Nothing else changed: alpha81 stays as described (Settings, Live diagnostics (for Claude): master off switch, your own token for the PRIVATE repository, at most 4 small files a day). NOT run on the tablet and not run against the real GitHub."}.
When the run is green, confirm the Release shows the APK (mcp__github__get_release_by_tag kestrel-g-v0.3.0-alpha82), then:  bash tools/record-release.sh kestrel-g-v0.3.0-alpha82 "alpha82: two privacy fixes for the live diagnostics that I found by reading what the app writes, before any data was ever sent. (1) A site address written without https in front (the video-frame notes write them that way) used to keep up to 40 characters of the path; now only the site name is kept. (2) Network addresses (for example the phone's address on the hotspot, shown on the Phone link line) are now removed from everything that is kept or sent. Nothing else changed: alpha81 stays as described (Settings, Live diagnostics (for Claude): master off switch, your own token for the PRIVATE repository, at most 4 small files a day). NOT run on the tablet and not run against the real GitHub."
NEVER ship kestrel-g-v0.3.0-alpha82 twice (check BUILDLOG.md and the Releases first).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  f95e13ba ckpt 2800: LD2 written into TASKS.md in the owner's words (LD2a-e): smarter + more comp
  946369a8 ckpt 2799: alpha82 prepared: versionCode 183 / 0.3.0-alpha82, extension version synced,
  2d4c1e81 ckpt 2798: alpha81 SHIPPED and RECORDED: run 44878029773 green (check, test-js, test-jv
  dba49a28 ckpt 2797: LD1 privacy fix 2 (for alpha82): DiagScrub removes dotted-quad network addre
  db02a2b5 ckpt 2796: LD1 privacy fix (for alpha82): DiagScrub now drops the path after a BARE hos
  472a364c ckpt 2795: alpha81 DISPATCHED once: android.yml run 44878029773 (run_number 263, head 7
  7f084cff ckpt 2794: gated kestrel-g-v0.3.0-alpha81 (gecko code 182) and pushed it: handoff self-
  68f9d24e ckpt 2793: LD1e: full gate green (:core:test, :app:testDebugUnitTest, :geckoapp:compile
  a6c87bc8 ckpt 2792: LD1d done: tools/diag_digest.py (crashes/kills, slow sites by host, playback
  2a79bf47 ckpt 2791: LD1c tested: LiveDiagTest 13/13 (switch off = nothing kept, switch survives 
```
