# CHECKPOINT 2528 — read me first, then TASKS.md

**Written:** 2026-11-16T16:49:59Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `728009cc` (this checkpoint is the commit after it)

## Just done
ANS55f: lab run 1 of suite ip DISPATCHED: run 43911183498 (head 72df6d09, started 16:46Z, expect it done ~17:06Z; artifact emulator-lab-text appears only after the job ends). Since then (NOT in that run): long press on the inline picture opens the download options (InlinePlayer.onLongPress -> BrowserActivity.inlineLongPress -> VideoFlow.longPress), Back order (inline closes after the sheets/tab list, before page history), a screen opened over the page (Settings, Downloads, Diagnostics, lists) stops the film, ip_scene gained the long-press check; real Yahoo page probed: its player is in the TOP document, 532x299 CSS px at x 16, visual viewport scale 1, no fixed/sticky/scroller ancestor (geomOf fx 0) so inline will place there

## Do this next
Poll the lab run 43911183498 (actions_list list_workflow_jobs resource_id 43911183498 latest perPage 8: only the 'lab / lab' job matters; ~6 KB; poll no sooner than 17:05Z). When it ends: actions_list list_workflow_run_artifacts 43911183498 -> emulator-lab-text id -> actions_get download_workflow_run_artifact -> python3 -I tools/emu/fetch_artifact.py "<url>" <empty dir> -> read report.md (the ip section + numbers). Fix what it shows (overlay box vs page box, scroll lines/units, pixel diffs, controls positions), push, dispatch lab ip again (the second run includes the long press check) and lab ho (webinline 0 path must stay green). Then bump geckoapp versionCode 161 / alpha60 and ship.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  72df6d09 ckpt 2527: ANS55f: inline player coded + unit suites ALL green (core, app, geckoapp; gu
  441aa19c ckpt 2526: ANS55f step 2 coded and COMPILES (:geckoapp:compileDebugKotlin ok): PlayerFa
  16795a42 ckpt 2525: ANS55f step 1 done: gate-main.js geomOf + content.js geo() + background.js g
  50071840 ckpt 2524: alpha59 SHIPPED and RECORDED: Release kestrel-g-v0.3.0-alpha59 (code 160, ru
  2e6fccf5 ckpt 2523: alpha59 RELEASE DISPATCHED (ONE time): android.yml run 43905841979 (run 174,
  d0daa8b8 ckpt 2522: gated kestrel-g-v0.3.0-alpha59 (gecko code 160) and pushed it: handoff self-
  c4c4f1df ckpt 2521: pre-ship: alpha59: your message 55 (on alpha58). (1) FIXED: the black pictur
  04ffa9c9 ckpt 2520: lab ho run dispatched: 43903287513 (head 09476513 = ckpt 2519, started 16:03
  09476513 ckpt 2519: owner message 56 written into TASKS.md (ANS56a-b): 'even when I selected a t
  f7ab7a01 ckpt 2518: ANS55e fix coded (core tests + gecko compile green): Yahoo/Brightcove root c
```

(4 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
