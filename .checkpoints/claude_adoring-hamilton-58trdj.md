# CHECKPOINT 2753 — read me first, then TASKS.md

**Written:** 2026-11-20T18:09:30Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/adoring-hamilton-58trdj` · **builds on:** `9d8b0d06` (this checkpoint is the commit after it)

## Just done
RVW4: NL2 done: realfinder.js (real pages through the real background.js + content.js; sandbox CAN reach real video sites), HttpSemantics.probeTotal (HTML replies give no file size) with test, app + geckoapp compile EXIT 0; research NL1/NL2 written; TASKS RVW4-NL1/NL2 rows added

## Do this next
wait for lab run 44787922316 (mcp__github__actions_get get_workflow_run), read its artifact for sitefixes lines; more real-page probes in SMALL batches (<=3 urls, --wait 5000; a batch of 7 timed out at 290 s); then full suites (:core:test :app:testDebugUnitTest :geckoapp:testDebugUnitTest :app:lintDebug; cd web-tests && npm test), bump versionCode 176 / alpha75, sync-ext-version, write docs/handoff/ALPHA75_RELEASE_NOTE.txt, ship.sh gecko, ONE dispatch, record-release

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md
     M research/REVIEW_2026-11-22_NEWLEVERS.md

## Last ten checkpoints
```
  d637614b ckpt 2752: NL1: licence row (docs/FILTER-LISTS.md) + HANDOFF engine step written; emula
  85af50a4 ckpt 2751: RVW4-NL1 code done and compiling: Gradle prepareWebcompat (AAR from Mozilla 
  934b8dad ckpt 2750: RVW4a: researched. Engine prefs / downloads / finder are saturated (all chea
  8d40d448 ckpt 2749: RVW4 written into TASKS.md in the owner's words (new levers for speed, compa
  aeef6973 ckpt 2748: RVW3 DONE AND SHIPPED: alpha74 RELEASED AND RECORDED = kestrel-g-v0.3.0-alph
  a1da4068 ckpt 2747: REL74b: alpha74 DISPATCHED ONCE (android.yml app=gecko full_build=true ref c
  4c5ff5b3 ckpt 2746: gated kestrel-g-v0.3.0-alpha74 (gecko code 175) and pushed it: handoff self-
  6ab74f37 ckpt 2745: RVW3 fixes CODED + compile green (:geckoapp:compileDebugKotlin EXIT 0): secu
  7496a10a ckpt 2744: RVW3: security lane back and VERIFIED by me against the code (SC1 SC2 SC4 SC
  fe77b529 ckpt 2743: RVW3a launched: three read-only review lanes running as background agents (p
```

(6 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
