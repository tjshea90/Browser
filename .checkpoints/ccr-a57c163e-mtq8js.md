# CHECKPOINT 550 — read me first, then TASKS.md

**Written:** 2026-11-11T22:21:51Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `68a65b4` (this checkpoint is the commit after it)

## Just done
gated kestrel-g-v0.2.0-alpha12 (gecko code 111) and pushed it: handoff self-test, unit suites and the versionCode check passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow_id android.yml, ref ccr-a57c163e-mtq8js, inputs {"app":"gecko","full_build":"true","note":"alpha12: more videos can be saved, and the browser should feel lighter. Download video now also handles DASH streams (Reddit-style, picture and sound kept apart: Kestrel downloads both and joins them into one MP4), turns .ts streams into MP4, joins separate picture and sound of HLS streams, and names the resolution of plain video files by reading their header (small peek, only when the list is open). Speed and smoothness: the ad-blocking helper is no longer restarted at every launch (the first page after starting is now blocked and scanned for videos straight away), its answers no longer take an extra detour through the interface thread, blocked-request counting is batched, download notifications refresh about once a second instead of up to five times, touch handling does less work, the tab grid, suggestions and keyboard tracking do less work. Verified on the emulator with generated test streams, not on your tablet."}.
When the run is green, confirm the Release shows the APK (mcp__github__get_release_by_tag kestrel-g-v0.2.0-alpha12), then:  bash tools/record-release.sh kestrel-g-v0.2.0-alpha12 "alpha12: more videos can be saved, and the browser should feel lighter. Download video now also handles DASH streams (Reddit-style, picture and sound kept apart: Kestrel downloads both and joins them into one MP4), turns .ts streams into MP4, joins separate picture and sound of HLS streams, and names the resolution of plain video files by reading their header (small peek, only when the list is open). Speed and smoothness: the ad-blocking helper is no longer restarted at every launch (the first page after starting is now blocked and scanned for videos straight away), its answers no longer take an extra detour through the interface thread, blocked-request counting is batched, download notifications refresh about once a second instead of up to five times, touch handling does less work, the tab grid, suggestions and keyboard tracking do less work. Verified on the emulator with generated test streams, not on your tablet."
NEVER ship kestrel-g-v0.2.0-alpha12 twice (check BUILDLOG.md and the Releases first).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  e82c7f6 ckpt 543: DASH + merge DONE and emulator-verified: core/media/Dash.kt (MPD parser: Segme
  5220430 ckpt 528: TS->MP4 (Remuxer + ADTS->raw AAC frames with an exact AudioClock; verified on 
  bb6a984 ckpt 523: alpha11 SHIPPED and recorded: https://github.com/tjshea90/Browser/releases/tag
  5d55a7b ckpt 513: alpha11 DISPATCHED: run 43101503028 (android.yml, head 6d529ad, 21:15Z). Gate 
  6d529ad ckpt 512: gated kestrel-g-v0.2.0-alpha11 (gecko code 110) and pushed it: handoff self-te
  db137ac ckpt 509: V4/V5 P1 VERIFIED on the emulator: HLS streams via the Download video sheet. M
  696b6f0 ckpt 501: V4 progress: P0 (direct files) WORKS on the emulator (sniffer in kestrel-shiel
  307128f ckpt 487: alpha10 SHIPPED and recorded: Release https://github.com/tjshea90/Browser/rele
  50d5718 ckpt 478: alpha10 CI run 43093416759 FAILED in test-jvm: EngineTest.plainDownloadWithout
  b9e656d ckpt 476: alpha10 DISPATCHED on GitHub Actions: run 43093416759 (android.yml, ref ccr-a5
```

(6 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
