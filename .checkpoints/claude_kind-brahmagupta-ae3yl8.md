# CHECKPOINT 3135 — read me first, then TASKS.md

**Written:** 2026-11-24T01:30:11Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/kind-brahmagupta-ae3yl8` · **builds on:** `d91445e4` (this checkpoint is the commit after it)

## Just done
alpha100 SHIPPED and recorded: run 45303459458 green on attempt 1 (check, test-js, test-jvm, build-gecko), Release has kestrel-g-0.3.0-alpha100.apk 96.7 MB, certificate verified, BUILDLOG.md updated. PERF17 a/b done, c/d measures shipped; PERF18 done

## Do this next
Waiting for the owner's tablet test of alpha100 and a new upload. On it: python3 -B -I tools/diag_digest.py --days 2 and read FOCUS REPORT: (1) PAGE ANIMATION lines (late frames, frames a second, by drawing mode) -> decide Calmer redrawing default / Software drawing cost; (2) bot checks line (time on the full page check) -> decide the fix (candidates: the 5 MAIN-world scripts on the interstitial, FPP, the one content process shared with a heavy tab); (3) tabs lazy-start / lazy-wake lines (did the start screen appear after 4 h? did Continue load the page?); (4) sec lines from alpha99 (cert-error, https-upgraded, popup-blocked, perm-asked). Then PERF19 cheaper blocker (benchmark in web-tests first), FLK1, PERF14e, PERF9, BackLab MISSED false positive. Never ship alpha100 again; next is alpha101 / versionCode 202

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  654a7ecd ckpt 3133: alpha100 (code 201) gated, pushed (ckpt 3132) and dispatched ONCE: run 45303
  4b69f1eb ckpt 3132: gated kestrel-g-v0.3.0-alpha100 (gecko code 201) and pushed it: handoff self
  39ac98cc ckpt 3130: PERF17/PERF18 coded and green: lazy restore after 4 h (core LazyStart, Home 
  2d2398f7 ckpt 3120: PERF17b lazy restore CODED and compiled (core LazyStart + LazyStartTest gree
  0cf14251 ckpt 3113: owner request PERF18 (research online what major browsers do for fast loadin
  9dcb9f9f ckpt 3111: owner request PERF17 written into TASKS.md in his words (a-e): faster speed,
  77f06ebc ckpt 3110: alpha99 SHIPPED and recorded: run 45292001575 green on attempt 1 (check, tes
  5ae527e4 ckpt 3108: alpha99 run is 45292001575 (workflow_dispatch, started 23:48Z, head da26da05
  fad5637b ckpt 3107: alpha99 (code 200) gated and pushed (ckpt 3106); workflow dispatched ONCE on
  da26da05 ckpt 3106: gated kestrel-g-v0.3.0-alpha99 (gecko code 200) and pushed it: handoff self-
```
