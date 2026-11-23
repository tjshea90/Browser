# CHECKPOINT 3052 — read me first, then TASKS.md

**Written:** 2026-11-23T19:03:53Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/kind-brahmagupta-ae3yl8` · **builds on:** `df59c76b` (this checkpoint is the commit after it)

## Just done
gated kestrel-g-v0.3.0-alpha96 (gecko code 197) and pushed it: handoff self-test, unit suites and the versionCode check passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow_id android.yml, ref claude/kind-brahmagupta-ae3yl8, inputs {"app":"gecko","full_build":"true","note":"alpha96: the diagnostics tell the truth, and Back. (1) Your diagnostics counted every page that was cut short by a redirect or by you tapping a result as a failed load (19 of 44 shopping loads and 15 of 37 search loads looked failed), which also gave wrong advice such as turn Shields off for a tracking redirect; a cut-short load now counts as a load, not a failure. (2) The upload journal lost 602 lines overnight because routine memory trims and dropped tab pictures were never thinned; they are thinned now and real memory trouble is still always kept. (3) New lines: when the same site is loaded again and again in one tab (one site was loaded 72 times in one run), which extension version the engine really runs, and when a page received a file you attached. (4) Back: in 3 days your tablet made 7 Back presses to real pages and the engine kept none of them; 4 of 6 came back at the wrong scroll place and each took about 3 s. Other browsers keep the whole page frozen in memory (the back/forward cache) and restore it at once; this engine can do it and does it in the lab, but on your tablet the pages are left while still loading or with requests open. This release records the engine own verdict each time a page is left, so the next upload says exactly why; it keeps trying to scroll to your place for 9 s instead of 3 s on slow pages; and the Still loading box on the Back picture is gone (only the thin loading line stays, after 0.6 s). What other browsers do and what is left to try is in research/INSTANT_BACK.md. Not run on the tablet."}.
When the run is green, confirm the Release shows the APK (mcp__github__get_release_by_tag kestrel-g-v0.3.0-alpha96), then:  bash tools/record-release.sh kestrel-g-v0.3.0-alpha96 "alpha96: the diagnostics tell the truth, and Back. (1) Your diagnostics counted every page that was cut short by a redirect or by you tapping a result as a failed load (19 of 44 shopping loads and 15 of 37 search loads looked failed), which also gave wrong advice such as turn Shields off for a tracking redirect; a cut-short load now counts as a load, not a failure. (2) The upload journal lost 602 lines overnight because routine memory trims and dropped tab pictures were never thinned; they are thinned now and real memory trouble is still always kept. (3) New lines: when the same site is loaded again and again in one tab (one site was loaded 72 times in one run), which extension version the engine really runs, and when a page received a file you attached. (4) Back: in 3 days your tablet made 7 Back presses to real pages and the engine kept none of them; 4 of 6 came back at the wrong scroll place and each took about 3 s. Other browsers keep the whole page frozen in memory (the back/forward cache) and restore it at once; this engine can do it and does it in the lab, but on your tablet the pages are left while still loading or with requests open. This release records the engine own verdict each time a page is left, so the next upload says exactly why; it keeps trying to scroll to your place for 9 s instead of 3 s on slow pages; and the Still loading box on the Back picture is gone (only the thin loading line stays, after 0.6 s). What other browsers do and what is left to try is in research/INSTANT_BACK.md. Not run on the tablet."
NEVER ship kestrel-g-v0.3.0-alpha96 twice (check BUILDLOG.md and the Releases first).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  bd5370ef ckpt 3049: PERF12 + PERF13 coded and tested (targeted): cut-short loads, page loop, qui
  fab351e4 ckpt 3039: PERF13 written to TASKS.md (owner: instant first load and instant Back, find
  d41227d4 ckpt 3034: PERF12 written to TASKS.md (owner 2026-11-23: look at the latest diagnostics
  b375e29e ckpt 3033: PERF11a done + research written (alpha95 uploads: Safe drawing + PBO off app
  489d966d ckpt 3032: PERF11 written to TASKS.md (owner after alpha95: invisible blocks covering w
  eefcc152 ckpt 3031: alpha95 RELEASED AND RECORDED: kestrel-g-v0.3.0-alpha95 (gecko code 196, run
  f107edb6 ckpt 3029: alpha95 (gecko code 196) gated by ship.sh (core+app+geckoapp suites, handoff
  12cc7bbd ckpt 3028: gated kestrel-g-v0.3.0-alpha95 (gecko code 196) and pushed it: handoff self-
  36dea037 ckpt 3024: pre-ship: alpha95: half-drawn text, attaching photos, and the files permissi
  a732be58 ckpt 3020: PERF10 coded (not yet gated): pbo-uploads off in Safe drawing + Software dra
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
