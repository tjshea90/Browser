# CHECKPOINT 2508 — read me first, then TASKS.md

**Written:** 2026-11-16T09:18:54Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `5567de58` (this checkpoint is the commit after it)

## Just done
final lab vp run dispatched: 43838527885 (head 5567de58 = ckpt 2507, started 09:18Z, ends ~09:40Z); everything else is ready for the release (note drafted with @WEBTESTS@ @LABCHECKS@ placeholders, TASKS rows ANS54e.7-10 written)

## Do this next
read lab run 43838527885: find its 'lab / lab' job id (actions_list list_workflow_jobs resource_id 43838527885 workflow_jobs_filter latest perPage 2), wait for status completed (get_workflow_job), then artifacts -> emulator-lab-text -> python3 -I tools/emu/fetch_artifact.py <url> <empty dir>; all sections green? if the console starts with 'gave the app a 16 MB java heap limit' or scene app-start no-engine it is a lab flake: dispatch vp again (ONE more). Then fill @WEBTESTS@ (573) and @LABCHECKS@ in docs/handoff/ALPHA58_RELEASE_NOTE.txt, bump geckoapp/build.gradle.kts to 159 / 0.3.0-alpha58 + bash tools/sync-ext-version.sh, bash ship.sh gecko with the note, ONE dispatch (app=gecko full_build=true note), tag + jobs + release, bash tools/record-release.sh <tag> identical note, tick ANS54a-f, answer owner message 54 (Release link on its own line)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  5567de58 ckpt 2507: independent review (9 findings, all real) applied: content.js relays only wh
  9ba75b5b ckpt 2506: tap window widened for a slow tablet (core WebHandOver.pressWindowMs 15 s, 3
  51380853 ckpt 2505: ANS54e.9 SECURITY: a page script can fake the gate's question, so every hand
  4ad3a730 ckpt 2504: lab vp run dispatched: 43832408756 (lab job 133180783158, head 6b32808a = ck
  6b32808a ckpt 2503: lab run 43831112200 (ho run 2) read: file now hands over on the real engine 
  57239dd2 ckpt 2502: lab run 43831112200 (suite ho, head bbfc9c3f = ckpt 2501) dispatched; gradle
  bbfc9c3f ckpt 2501: ANS54e: lab run 43826954106 (suite ho) read: overlay/hls/unknown/never ok, f
  00b6ff6b ckpt 2500: lab run 43826954106 dispatched: suite ho (head 97294fcb): web videos play in
  97294fcb ckpt 2499: ANS54e: full web suite 562/562, gradle all green (before the late-hand-over 
  368a9961 ckpt 2498: ANS54e coded, compiles: core WebHandOver + test (10 ok), Prefs web_in_kestre
```
