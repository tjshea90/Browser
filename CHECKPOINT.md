# CHECKPOINT 711 — read me first, then TASKS.md

**Written:** 2026-11-12T02:21:20Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `7d6778a` (this checkpoint is the commit after it)

## Just done
gated kestrel-g-v0.3.0-alpha16 (gecko code 115) and pushed it: handoff self-test, unit suites and the versionCode check passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow_id android.yml, ref ccr-a57c163e-mtq8js, inputs {"app":"gecko","full_build":"true","note":"alpha16: daily update dialog (app, engine, ad-block lists) with Update button, ad-block lists refresh every day, download manager survives kills/outages/refused connections and starts faster, Network lab switches, Page loads timing in Diagnostics"}.
When the run is green, confirm the Release shows the APK (mcp__github__get_release_by_tag kestrel-g-v0.3.0-alpha16), then:  bash tools/record-release.sh kestrel-g-v0.3.0-alpha16 "alpha16: daily update dialog (app, engine, ad-block lists) with Update button, ad-block lists refresh every day, download manager survives kills/outages/refused connections and starts faster, Network lab switches, Page loads timing in Diagnostics"
NEVER ship kestrel-g-v0.3.0-alpha16 twice (check BUILDLOG.md and the Releases first).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M geckoapp/src/main/assets/kestrel-shields/manifest.json

## Last ten checkpoints
```
  7d6778a ckpt 710: pre-ship: alpha16: daily update dialog (app, engine, ad-block lists) with Upda
  6ab16b5 ckpt 709: X3 first batch done+tested: auto-resume after kill (both engines, SQLite v3 cr
  2d7afd4 ckpt 700: X2 verified on emulator (dialog found/ready/install flow); X12 first pieces co
  2cfcc7a ckpt 691: alpha15 SHIPPED+RECORDED (run 43136503549 green, tag kestrel-g-v0.3.0-alpha15,
  2f132e9 ckpt 682: alpha15 shipped to GitHub: workflow dispatched (run queued on ccr-a57c163e-mtq
  4b80497 ckpt 681: gated kestrel-g-v0.3.0-alpha15 (gecko code 114) and pushed it: handoff self-te
  bede833 ckpt 680: pre-ship: alpha15: pop-up and forced-redirect guard, always-visible Save video
  8e96087 ckpt 678: emulator-verified: Save-video chip opens the Download video sheet; page scan l
  e1721e0 ckpt 647: X7 page scan coded: content.js finds video/source/og:video/links/inline-script
  2c7b49a ckpt 628: owner request 10 written into TASKS.md as X11 (everything still on the list) a
```
