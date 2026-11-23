# CHECKPOINT 3106 — read me first, then TASKS.md

**Written:** 2026-11-23T23:48:35Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/kind-brahmagupta-ae3yl8` · **builds on:** `61e76716` (this checkpoint is the commit after it)

## Just done
gated kestrel-g-v0.3.0-alpha99 (gecko code 200) and pushed it: handoff self-test, unit suites and the versionCode check passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow_id android.yml, ref claude/kind-brahmagupta-ae3yl8, inputs {"app":"gecko","full_build":"true","note":"alpha99: diagnostics now focus on the five things you named: speed of the app, page loading speed, video playback, page compatibility, and privacy and security. The tablet now also writes a short line for each security decision on normal tabs (never for private tabs): an address upgraded to https, https not available, a certificate error, a pop-up or forced redirect blocked, and a page asking for a permission such as location. Routine bookkeeping lines are cut to a few examples so the upload has room for the five areas, and security lines are never thinned. The report I read from your upload (the digest) now starts with one verdict per area with the limit it is judged by. The advice list also speaks up when the browser refused three or more insecure connections in a week: check the tablet date and time and that the hotspot has no sign-in page. Alpha98 numbers from your last upload: switching to a tab that is still alive takes about 0.3 s (0.08 to 0.43 s), so the tab sleeping fix works. Not run on the tablet: the new security lines have only been tested in the unit tests and the report script, so the first upload from alpha99 shows whether a real certificate error or pop-up block writes its line."}.
When the run is green, confirm the Release shows the APK (mcp__github__get_release_by_tag kestrel-g-v0.3.0-alpha99), then:  bash tools/record-release.sh kestrel-g-v0.3.0-alpha99 "alpha99: diagnostics now focus on the five things you named: speed of the app, page loading speed, video playback, page compatibility, and privacy and security. The tablet now also writes a short line for each security decision on normal tabs (never for private tabs): an address upgraded to https, https not available, a certificate error, a pop-up or forced redirect blocked, and a page asking for a permission such as location. Routine bookkeeping lines are cut to a few examples so the upload has room for the five areas, and security lines are never thinned. The report I read from your upload (the digest) now starts with one verdict per area with the limit it is judged by. The advice list also speaks up when the browser refused three or more insecure connections in a week: check the tablet date and time and that the hotspot has no sign-in page. Alpha98 numbers from your last upload: switching to a tab that is still alive takes about 0.3 s (0.08 to 0.43 s), so the tab sleeping fix works. Not run on the tablet: the new security lines have only been tested in the unit tests and the report script, so the first upload from alpha99 shows whether a real certificate error or pop-up block writes its line."
NEVER ship kestrel-g-v0.3.0-alpha99 twice (check BUILDLOG.md and the Releases first).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  feb56817 ckpt 3104: PERF16 a-d done: focus_report, sec lines, journal bookkeeping thinning, advi
  a6a2207d ckpt 3092: owner after alpha98: it seems faster; NEW PERF16: focus future diagnostics o
  c5e4207f ckpt 3091: alpha98 RELEASED AND RECORDED: kestrel-g-v0.3.0-alpha98 (gecko code 199, run
  946e2e68 ckpt 3089: alpha98 (gecko code 199) gated by ship.sh and ONE dispatch made on claude/ki
  665bc7a9 ckpt 3088: gated kestrel-g-v0.3.0-alpha98 (gecko code 199) and pushed it: handoff self-
  6b92a4b3 ckpt 3087: pre-ship: alpha98: switching between tabs no longer reloads them. Your diagn
  a5e04498 ckpt 3085: PERF15 coded (not gated): LiveBudget.afterTrim level 60+ follows free memory
  adeb2192 ckpt 3082: owner after alpha97: Back is faster and images attach (CONFIRMED); NEW PERF1
  8a9c210e ckpt 3081: alpha97 RELEASED AND RECORDED: kestrel-g-v0.3.0-alpha97 (gecko code 198, run
  a7a096c8 ckpt 3079: alpha97 run 45259809870 FAILED at test-jvm on the GitHub runner: core Stream
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
