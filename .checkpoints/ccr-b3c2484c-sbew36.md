# CHECKPOINT 2527 — read me first, then TASKS.md

**Written:** 2026-11-16T16:46:16Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `e17daab2` (this checkpoint is the commit after it)

## Just done
ANS55f: inline player coded + unit suites ALL green (core, app, geckoapp; guards OwnerRulesTest incl. new ownerMessage55TheFilmPlaysInsideThePage; 3 old guards re-pointed at PlayerFactory/pressed); compile ok; lab suite ip written (soak.py ip_scene, soaklab /ho?v=inline, android.yml option ip, webinline debug cmd); ho_scene now forces webinline 0

## Do this next
1) check web-run1.txt in scratchpad (npm test, background) is green; 2) dispatch the emulator lab: mcp__github__actions_run_trigger run_workflow android.yml ref=ccr-b3c2484c-sbew36 inputs app=lab lab_suite=ip lab_minutes=1 (builds the debug APK itself, about 15 min); 3) fetch artifact emulator-lab-text via actions_get download_workflow_run_artifact then python3 -I tools/emu/fetch_artifact.py (see research notes); read the ip checks + numbers (overlay box vs page box, scroll lines, pixel diffs); fix findings; repeat; 4) also lab ho (the full-screen path must still pass, it now sets webinline 0); 5) bump geckoapp versionCode 161 / alpha60, ship, answer owner 55c/d.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  441aa19c ckpt 2526: ANS55f step 2 coded and COMPILES (:geckoapp:compileDebugKotlin ok): PlayerFa
  16795a42 ckpt 2525: ANS55f step 1 done: gate-main.js geomOf + content.js geo() + background.js g
  50071840 ckpt 2524: alpha59 SHIPPED and RECORDED: Release kestrel-g-v0.3.0-alpha59 (code 160, ru
  2e6fccf5 ckpt 2523: alpha59 RELEASE DISPATCHED (ONE time): android.yml run 43905841979 (run 174,
  d0daa8b8 ckpt 2522: gated kestrel-g-v0.3.0-alpha59 (gecko code 160) and pushed it: handoff self-
  c4c4f1df ckpt 2521: pre-ship: alpha59: your message 55 (on alpha58). (1) FIXED: the black pictur
  04ffa9c9 ckpt 2520: lab ho run dispatched: 43903287513 (head 09476513 = ckpt 2519, started 16:03
  09476513 ckpt 2519: owner message 56 written into TASKS.md (ANS56a-b): 'even when I selected a t
  f7ab7a01 ckpt 2518: ANS55e fix coded (core tests + gecko compile green): Yahoo/Brightcove root c
  5851f383 ckpt 2517: owner message 55 (alpha58 on the tablet: Yahoo video black, resumed the wron
```

(10 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
