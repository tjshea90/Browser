# CHECKPOINT 594 — read me first, then TASKS.md

**Written:** 2026-11-11T23:59:32Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `0487f2b` (this checkpoint is the commit after it)

## Just done
gated kestrel-g-v0.2.0-alpha13 (gecko code 112) and pushed it: handoff self-test, unit suites and the versionCode check passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow_id android.yml, ref ccr-a57c163e-mtq8js, inputs {"app":"gecko","full_build":"true","note":"alpha13: pages should load faster and smoother. Ad and tracker blocking now runs inside the browser engine itself instead of asking the app about every single request, which used to stall the screen on busy pages; the first page after you open the browser is protected sooner too. Diagnostics has a new Blocker line with timing numbers and a button to switch back to the old way if a site ever misbehaves. Also: smaller element-hiding styles inside embedded frames, and a quicker start of the filter-list loader. Checked against the old blocker on 60,000+ test requests built from the real filter lists (identical answers) and on the emulator; not measured on your tablet yet - after browsing a heavy site, open Diagnostics, tap Update, then Copy, and send me the Blocker line."}.
When the run is green, confirm the Release shows the APK (mcp__github__get_release_by_tag kestrel-g-v0.2.0-alpha13), then:  bash tools/record-release.sh kestrel-g-v0.2.0-alpha13 "alpha13: pages should load faster and smoother. Ad and tracker blocking now runs inside the browser engine itself instead of asking the app about every single request, which used to stall the screen on busy pages; the first page after you open the browser is protected sooner too. Diagnostics has a new Blocker line with timing numbers and a button to switch back to the old way if a site ever misbehaves. Also: smaller element-hiding styles inside embedded frames, and a quicker start of the filter-list loader. Checked against the old blocker on 60,000+ test requests built from the real filter lists (identical answers) and on the emulator; not measured on your tablet yet - after browsing a heavy site, open Diagnostics, tap Update, then Copy, and send me the Blocker line."
NEVER ship kestrel-g-v0.2.0-alpha13 twice (check BUILDLOG.md and the Releases first).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M geckoapp/src/main/assets/kestrel-shields/manifest.json

## Last ten checkpoints
```
  16223f8 ckpt 592: D1 code complete and verified in unit tests: real-lists differential (63,378 r
  4316802 ckpt 571: D1 steps 1+2 DONE and verified: core/.../filter/ExtImage.kt (KEX1 little-endia
  6e451c7 ckpt 555: alpha12 SHIPPED and recorded (run 43111148506 green, BUILDLOG written): https:
  66aa048 ckpt 551: alpha12 DISPATCHED on GitHub Actions: run 43111148506 (android.yml, head 77eac
  77eac10 ckpt 550: gated kestrel-g-v0.2.0-alpha12 (gecko code 111) and pushed it: handoff self-te
  e82c7f6 ckpt 543: DASH + merge DONE and emulator-verified: core/media/Dash.kt (MPD parser: Segme
  5220430 ckpt 528: TS->MP4 (Remuxer + ADTS->raw AAC frames with an exact AudioClock; verified on 
  bb6a984 ckpt 523: alpha11 SHIPPED and recorded: https://github.com/tjshea90/Browser/releases/tag
  5d55a7b ckpt 513: alpha11 DISPATCHED: run 43101503028 (android.yml, head 6d529ad, 21:15Z). Gate 
  6d529ad ckpt 512: gated kestrel-g-v0.2.0-alpha11 (gecko code 110) and pushed it: handoff self-te
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
