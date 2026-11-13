# CHECKPOINT 1535 — read me first, then TASKS.md

**Written:** 2026-11-13T09:15:14Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `ac7766e` (this checkpoint is the commit after it)

## Just done
gated kestrel-g-v0.3.0-alpha30 (gecko code 129) and pushed it: handoff self-test, unit suites and the versionCode check passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow_id android.yml, ref ccr-a57c163e-mtq8js, inputs {"app":"gecko","full_build":"true","note":"alpha30 (includes alpha29 and alpha28): your three requests. 1) Settings now stays in the menu in private windows too (and opens in the private colours). 2) Long-press a link and choose Open in new tab: the page opens in a new tab behind the one you are on; you stay where you are and the tab counter goes up. 3) Doraby movies and the Cloudflare message: on the old version Firefox's own protection listed cloudflare.com (Cloudflare's visitor check and library host) and jsDelivr as content and refused them, so a movie player's frame and the Cloudflare check could not load; alpha28 stopped that, Shields off for a site (alpha29) also switches Firefox's own protection off for that site, and a new test makes sure Kestrel's own lists never block the Doraby players or Cloudflare's check. Everything from alpha29 and alpha28 is in this build: the Updates screen that says how much free room an install needs (this tablet needs about 3 GB free), Clear cache, Menu > Diagnostics > Page report, Reload without cache."}.
When the run is green, confirm the Release shows the APK (mcp__github__get_release_by_tag kestrel-g-v0.3.0-alpha30), then:  bash tools/record-release.sh kestrel-g-v0.3.0-alpha30 "alpha30 (includes alpha29 and alpha28): your three requests. 1) Settings now stays in the menu in private windows too (and opens in the private colours). 2) Long-press a link and choose Open in new tab: the page opens in a new tab behind the one you are on; you stay where you are and the tab counter goes up. 3) Doraby movies and the Cloudflare message: on the old version Firefox's own protection listed cloudflare.com (Cloudflare's visitor check and library host) and jsDelivr as content and refused them, so a movie player's frame and the Cloudflare check could not load; alpha28 stopped that, Shields off for a site (alpha29) also switches Firefox's own protection off for that site, and a new test makes sure Kestrel's own lists never block the Doraby players or Cloudflare's check. Everything from alpha29 and alpha28 is in this build: the Updates screen that says how much free room an install needs (this tablet needs about 3 GB free), Clear cache, Menu > Diagnostics > Page report, Reload without cache."
NEVER ship kestrel-g-v0.3.0-alpha30 twice (check BUILDLOG.md and the Releases first).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M geckoapp/src/main/assets/kestrel-shields/manifest.json

## Last ten checkpoints
```
  ac7766e ckpt 1534: pre-ship: alpha30 (includes alpha29 and alpha28): your three requests. 1) Set
  613fb9e ckpt 1531: AI1a + AI1b EMULATOR-PROVEN (Settings in the private-window menu; long-press 
  53ae8c1 ckpt 1525: new context window (session restart): the owner re-sent items 1-3 of message 
  5930d95 ckpt 1519: handoff saved (.handoff/handoff.md; CLAUDE.md/AGENTS.md Session Learnings upd
  5c37df1 ckpt 1518: alpha29 SHIPPED and RECORDED (run 43383744581 green, BUILDLOG line written, R
  01ea47c ckpt 1515: alpha29 DISPATCHED: android.yml run 43383744581 (run_number 79) started 07:09
  78c9506 ckpt 1514: gated kestrel-g-v0.3.0-alpha29 (gecko code 128) and pushed it: handoff self-t
  f2230eb ckpt 1511: AH1b DONE and emulator-proven with the real engine (heads-up before a refused
  11126ad ckpt 1495: AH1c done and emulator-proven end to end (Shields master switch off -> Firefo
  939a120 ckpt 1487: AH1d done and emulator-proven: the Video line stayed on 'decoders unknown' be
```
