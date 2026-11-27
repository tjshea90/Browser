# CHECKPOINT 2644 — read me first, then TASKS.md

**Written:** 2026-11-17T22:16:06Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-5531c47b-hl1gzo` · **builds on:** `3b88d8d1` (this checkpoint is the commit after it)

## Just done
gated kestrel-g-v0.3.0-alpha68 (gecko code 169) and pushed it: handoff self-test, unit suites and the versionCode check passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow_id android.yml, ref ccr-5531c47b-hl1gzo, inputs {"app":"gecko","full_build":"true","note":"alpha68: Kestrel now always types through your Swoop keyboard when it is installed, so there is one keyboard and one memory for both apps. Words you type or glide in Kestrel (the address bar, the home search box, the boxes of a web page) teach Swoop, and everything Swoop learned elsewhere is used in Kestrel. The address bar and the home box tell Swoop they are the browser address box, so Swoop shows its browser keys (slash, full stop, Go) and does not add capitals or spaces. In a private tab Swoop learns nothing from the page boxes either. If Swoop is installed but not chosen yet, the small panel's keyboard button opens Android's keyboard picker. Settings has Always use the Swoop keyboard (recommended) and a button that sends what Kestrel's own small keyboard learned to Swoop (Swoop asks before it adds anything). Kestrel's own small keyboard stays as the fallback when Swoop is not installed, and it got the new word list. Install Swoop 0.5.0-alpha4 or newer first. Checked on a simulated Android 10 tablet with the real Firefox engine: typing and gliding into page boxes and the address bar, private tabs, pages loading. NOT proven on the tablet or the phone: opening Swoop floating from the panel, the keyboard picker, the hand-over button."}.
When the run is green, confirm the Release shows the APK (mcp__github__get_release_by_tag kestrel-g-v0.3.0-alpha68), then:  bash tools/record-release.sh kestrel-g-v0.3.0-alpha68 "alpha68: Kestrel now always types through your Swoop keyboard when it is installed, so there is one keyboard and one memory for both apps. Words you type or glide in Kestrel (the address bar, the home search box, the boxes of a web page) teach Swoop, and everything Swoop learned elsewhere is used in Kestrel. The address bar and the home box tell Swoop they are the browser address box, so Swoop shows its browser keys (slash, full stop, Go) and does not add capitals or spaces. In a private tab Swoop learns nothing from the page boxes either. If Swoop is installed but not chosen yet, the small panel's keyboard button opens Android's keyboard picker. Settings has Always use the Swoop keyboard (recommended) and a button that sends what Kestrel's own small keyboard learned to Swoop (Swoop asks before it adds anything). Kestrel's own small keyboard stays as the fallback when Swoop is not installed, and it got the new word list. Install Swoop 0.5.0-alpha4 or newer first. Checked on a simulated Android 10 tablet with the real Firefox engine: typing and gliding into page boxes and the address bar, private tabs, pages loading. NOT proven on the tablet or the phone: opening Swoop floating from the panel, the keyboard picker, the hand-over button."
NEVER ship kestrel-g-v0.3.0-alpha68 twice (check BUILDLOG.md and the Releases first).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  e38fb0d8 ckpt 2643: alpha67 SHIPPED and RECORDED (Release kestrel-g-v0.3.0-alpha67 code 168, run
  765d1c8a ckpt 2642: alpha67 DISPATCHED ONCE (android.yml app=gecko full_build=true, ref ccr-e89b
  d8340975 ckpt 2641: gated kestrel-g-v0.3.0-alpha67 (gecko code 168) and pushed it: handoff self-
  fd0ea0b5 ckpt 2640: Diagnostics line 'start oat' added (BrowserActivity.oatSizes: sizes of oat/<
  63fdb931 ckpt 2639: LAB7 CONFIRMATION lab run DISPATCHED ONCE: android.yml run 44137227924 (run_
  ebd3854a ckpt 2638: LAB7 first lab run 44126148207 READ (research/LAB7_RESULTS.md next): (1) the
  5b6a24c8 ckpt 2637: LAB7 CORRECTED lab run DISPATCHED ONCE: android.yml run 44126148207 (run_num
  e40a5b70 ckpt 2636: LAB7 lab run 44125632537 CANCELLED ON PURPOSE and replaced by a corrected on
  3206d5dd ckpt 2635: LAB7 lab run DISPATCHED ONCE (android.yml app=lab lab_suite=aot lab_profile=
  eaa4e46d ckpt 2634: LAB7a+b lab code written and dry-run against a fake adb (aotlab.py breadth_c
```

(15 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
