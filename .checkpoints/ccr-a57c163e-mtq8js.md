# CHECKPOINT 614 — read me first, then TASKS.md

**Written:** 2026-11-12T00:25:24Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `14eba76` (this checkpoint is the commit after it)

## Just done
gated kestrel-g-v0.3.0-alpha14 (gecko code 113) and pushed it: handoff self-test, unit suites and the versionCode check passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow_id android.yml, ref ccr-a57c163e-mtq8js, inputs {"app":"gecko","full_build":"true","note":"alpha14: the update screen no longer says "up to date" when a newer version exists. Cause: GitHub lists the releases by NAME (alpha9 comes before alpha10), and the app only looked at the first eight, so it never saw alpha10 or anything newer. The app now asks for the newest versions directly and cannot be fooled by the order. This release is numbered 0.3.0 on purpose: that sorts to the top of the list, so even the old broken check in alpha9-alpha13 can see it - open Menu > Updates and tap Check now."}.
When the run is green, confirm the Release shows the APK (mcp__github__get_release_by_tag kestrel-g-v0.3.0-alpha14), then:  bash tools/record-release.sh kestrel-g-v0.3.0-alpha14 "alpha14: the update screen no longer says "up to date" when a newer version exists. Cause: GitHub lists the releases by NAME (alpha9 comes before alpha10), and the app only looked at the first eight, so it never saw alpha10 or anything newer. The app now asks for the newest versions directly and cannot be fooled by the order. This release is numbered 0.3.0 on purpose: that sorts to the top of the list, so even the old broken check in alpha9-alpha13 can see it - open Menu > Updates and tap Check now."
NEVER ship kestrel-g-v0.3.0-alpha14 twice (check BUILDLOG.md and the Releases first).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  fec7b3d ckpt 609: alpha14 DISPATCHED: run 43126355049 (android.yml, head 3ed1b75, started 00:18:
  3ed1b75 ckpt 608: gated kestrel-g-v0.3.0-alpha14 (gecko code 113) and pushed it: handoff self-te
  b646b31 ckpt 606: X1 DONE in code and tests: updater root cause found (GitHub lists releases by 
  6400e12 ckpt 600: owner request 9 written into TASKS.md as X1-X10 in the owner's words (INBOX ha
  f1f1d48 ckpt 597: alpha13 SHIPPED and recorded (run 43123971432 green, BUILDLOG written): https:
  e860a70 ckpt 595: alpha13 DISPATCHED on GitHub Actions: run 43123971432 (android.yml, head 19e3a
  19e3a6d ckpt 594: gated kestrel-g-v0.2.0-alpha13 (gecko code 112) and pushed it: handoff self-te
  16223f8 ckpt 592: D1 code complete and verified in unit tests: real-lists differential (63,378 r
  4316802 ckpt 571: D1 steps 1+2 DONE and verified: core/.../filter/ExtImage.kt (KEX1 little-endia
  6e451c7 ckpt 555: alpha12 SHIPPED and recorded (run 43111148506 green, BUILDLOG written): https:
```

(4 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
