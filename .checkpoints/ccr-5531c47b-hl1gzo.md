# CHECKPOINT 2634 — read me first, then TASKS.md

**Written:** 2026-11-17T16:22:18Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-5531c47b-hl1gzo` · **builds on:** `55e756fd` (this checkpoint is the commit after it)

## Just done
gated kestrel-g-v0.3.0-alpha66 (gecko code 167) and pushed it: handoff self-test, unit suites and the versionCode check passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow_id android.yml, ref ccr-5531c47b-hl1gzo, inputs {"app":"gecko","full_build":"true","note":"alpha66: your Swoop keyboard and Kestrel are linked. The small navigation panel now opens your Swoop keyboard floating over the page when you tap its bar, if Swoop is the keyboard you have chosen in Android: you type into the page's text box or the address bar the normal way, and Swoop learns what you type, so there is ONE memory for both. Settings has a switch, Use the Swoop keyboard from the panel (on): off, or Swoop not chosen, and the panel's own small keyboard opens as before. That small keyboard got the Swoop fixes: a quick swipe to the next key is now a glide and not one letter, one special character brings the letters back at once, and its word engine is the same code as Swoop's (the bigger word list, rare words such as tui no longer beat common ones). Everything else is alpha65 unchanged. NOT proven on the tablet: Kestrel opening Swoop and making it float (the phone and the tablet need Swoop 0.5.0-alpha2 or newer chosen as the keyboard); tell me exactly what you see if the keyboard does not appear or does not float."}.
When the run is green, confirm the Release shows the APK (mcp__github__get_release_by_tag kestrel-g-v0.3.0-alpha66), then:  bash tools/record-release.sh kestrel-g-v0.3.0-alpha66 "alpha66: your Swoop keyboard and Kestrel are linked. The small navigation panel now opens your Swoop keyboard floating over the page when you tap its bar, if Swoop is the keyboard you have chosen in Android: you type into the page's text box or the address bar the normal way, and Swoop learns what you type, so there is ONE memory for both. Settings has a switch, Use the Swoop keyboard from the panel (on): off, or Swoop not chosen, and the panel's own small keyboard opens as before. That small keyboard got the Swoop fixes: a quick swipe to the next key is now a glide and not one letter, one special character brings the letters back at once, and its word engine is the same code as Swoop's (the bigger word list, rare words such as tui no longer beat common ones). Everything else is alpha65 unchanged. NOT proven on the tablet: Kestrel opening Swoop and making it float (the phone and the tablet need Swoop 0.5.0-alpha2 or newer chosen as the keyboard); tell me exactly what you see if the keyboard does not appear or does not float."
NEVER ship kestrel-g-v0.3.0-alpha66 twice (check BUILDLOG.md and the Releases first).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  60f8deca ckpt 2633: alpha65 SHIPPED and RECORDED (run 44120758574 all 7 jobs green, Release kest
  f7d26151 ckpt 2632: alpha65 run is android.yml run 44120758574 (run_number 216, started 16:00:17
  0fa23623 ckpt 2631: alpha65 DISPATCHED ONCE (compile at install = LAB6: release script publishes
  d32b8cf2 ckpt 2630: gated kestrel-g-v0.3.0-alpha65 (gecko code 166) and pushed it: handoff self-
  a6159180 ckpt 2629: OWNER SAID GO on LAB6 (compile at install through the updater). Recorded in 
  20e28803 ckpt 2628: LAB5 PASS COMPLETE. alpha64 shipped 11:52Z (https://github.com/tjshea90/Brow
  cda703be ckpt 2627: alpha64 SHIPPED + RECORDED (Release kestrel-g-v0.3.0-alpha64, run 4407646379
  e5e2e612 ckpt 2626: alpha64 DISPATCHED ONCE at 11:42Z: android.yml run 44076463792 (app=gecko fu
  d9efb312 ckpt 2625: gated kestrel-g-v0.3.0-alpha64 (gecko code 165) and pushed it: handoff self-
  4692623b ckpt 2624: RELEASE PREP (not shipped): versionCode 165 / versionName 0.3.0-alpha64 in g
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
