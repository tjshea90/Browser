# CHECKPOINT 3110 — read me first, then TASKS.md

**Written:** 2026-11-24T00:07:08Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/kind-brahmagupta-ae3yl8` · **builds on:** `eed05f91` (this checkpoint is the commit after it)

## Just done
alpha99 SHIPPED and recorded: run 45292001575 green on attempt 1 (check, test-jvm, test-js, build-gecko), Release has kestrel-g-0.3.0-alpha99.apk (96.7 MB), certificate pin verified, BUILDLOG.md updated. PERF16 a-d done and in the build. Owner told: install it, press Send diagnostics, the focus report reads the first alpha99 upload

## Do this next
Waiting for the owner's tablet test of alpha99 and a new upload. On it: python3 -B -I tools/diag_digest.py --days 2 and read the FOCUS REPORT first, then check the sec lines really appeared (cert-error, https-upgraded, popup-blocked, perm-asked) and that bookkeeping thinning left room (INTEGRITY section: lost lines). Then in order: FLK1 harden PhoneBackendTest/courier ServerTest/StreamEngineTest timing tests; kept-page MISSED false positive in core BackLab (a kept page needs no place check); PERF14e Calmer redrawing A/B via digest SETTING A/B; PERF9 second film; claude.ai first-paint (Software drawing cost 2.0 -> 14.6 s) option list in research/PAGE_SPEED_2026-11-23.md. Never ship alpha99 again; next version is alpha100 / versionCode 201

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  5ae527e4 ckpt 3108: alpha99 run is 45292001575 (workflow_dispatch, started 23:48Z, head da26da05
  fad5637b ckpt 3107: alpha99 (code 200) gated and pushed (ckpt 3106); workflow dispatched ONCE on
  da26da05 ckpt 3106: gated kestrel-g-v0.3.0-alpha99 (gecko code 200) and pushed it: handoff self-
  feb56817 ckpt 3104: PERF16 a-d done: focus_report, sec lines, journal bookkeeping thinning, advi
  a6a2207d ckpt 3092: owner after alpha98: it seems faster; NEW PERF16: focus future diagnostics o
  c5e4207f ckpt 3091: alpha98 RELEASED AND RECORDED: kestrel-g-v0.3.0-alpha98 (gecko code 199, run
  946e2e68 ckpt 3089: alpha98 (gecko code 199) gated by ship.sh and ONE dispatch made on claude/ki
  665bc7a9 ckpt 3088: gated kestrel-g-v0.3.0-alpha98 (gecko code 199) and pushed it: handoff self-
  6b92a4b3 ckpt 3087: pre-ship: alpha98: switching between tabs no longer reloads them. Your diagn
  a5e04498 ckpt 3085: PERF15 coded (not gated): LiveBudget.afterTrim level 60+ follows free memory
```
