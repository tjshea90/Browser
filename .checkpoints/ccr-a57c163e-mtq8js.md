# CHECKPOINT 681 — read me first, then TASKS.md

**Written:** 2026-11-12T01:43:46Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `bede833` (this checkpoint is the commit after it)

## Just done
gated kestrel-g-v0.3.0-alpha15 (gecko code 114) and pushed it: handoff self-test, unit suites and the versionCode check passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow_id android.yml, ref ccr-a57c163e-mtq8js, inputs {"app":"gecko","full_build":"true","note":"alpha15: pop-up and forced-redirect guard, always-visible Save video chip, videos found without pressing play, HLS to MP4 also with discontinuities (ads left out)"}.
When the run is green, confirm the Release shows the APK (mcp__github__get_release_by_tag kestrel-g-v0.3.0-alpha15), then:  bash tools/record-release.sh kestrel-g-v0.3.0-alpha15 "alpha15: pop-up and forced-redirect guard, always-visible Save video chip, videos found without pressing play, HLS to MP4 also with discontinuities (ads left out)"
NEVER ship kestrel-g-v0.3.0-alpha15 twice (check BUILDLOG.md and the Releases first).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M geckoapp/src/main/assets/kestrel-shields/manifest.json

## Last ten checkpoints
```
  bede833 ckpt 680: pre-ship: alpha15: pop-up and forced-redirect guard, always-visible Save video
  8e96087 ckpt 678: emulator-verified: Save-video chip opens the Download video sheet; page scan l
  e1721e0 ckpt 647: X7 page scan coded: content.js finds video/source/og:video/links/inline-script
  2c7b49a ckpt 628: owner request 10 written into TASKS.md as X11 (everything still on the list) a
  6b28616 ckpt 625: alpha14 SHIPPED and recorded (run 43127174913 green): https://github.com/tjshe
  d1af18c ckpt 615: alpha14 RE-DISPATCHED: run 43127174913 (android.yml, head 0cebcca, 00:25:31Z, 
  0cebcca ckpt 614: gated kestrel-g-v0.3.0-alpha14 (gecko code 113) and pushed it: handoff self-te
  fec7b3d ckpt 609: alpha14 DISPATCHED: run 43126355049 (android.yml, head 3ed1b75, started 00:18:
  3ed1b75 ckpt 608: gated kestrel-g-v0.3.0-alpha14 (gecko code 113) and pushed it: handoff self-te
  b646b31 ckpt 606: X1 DONE in code and tests: updater root cause found (GitHub lists releases by 
```
