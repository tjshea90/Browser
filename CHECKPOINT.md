# CHECKPOINT 2549 — read me first, then TASKS.md

**Written:** 2026-11-16T18:34:15Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `cd7443ee` (this checkpoint is the commit after it)

## Just done
lab vp (all video suites kp np cp ap ho ip gp pv, one boot) dispatched: run 43930125170 on head cd7443ee (= the alpha60 code: versionCode 161, no code change after it, only notes) started 18:33:47Z, expect 18:50-18:55Z

## Do this next
1) when run 43930125170 ends: download emulator-lab-text (mcp__github__actions_list list_workflow_run_artifacts, actions_get download_workflow_run_artifact, python3 -I tools/emu/fetch_artifact.py with & not \u0026), read report.md issues + the kp / gp / pv sections; fix anything real (a code change means a fresh vp on the new head); 2) ship alpha60: fill TESTED_PLACEHOLDER in docs/handoff/ALPHA60_RELEASE_NOTE.txt, bash ship.sh gecko "$(cat docs/handoff/ALPHA60_RELEASE_NOTE.txt)", ONE run_workflow (app=gecko full_build=true note), verify tag kestrel-g-v0.3.0-alpha60 + all jobs green + get_release_by_tag, bash tools/record-release.sh kestrel-g-v0.3.0-alpha60 with the IDENTICAL note; 3) tick ANS55/ANS58/ANS59 rows, update HANDOFF; 4) answer owner 55, 57, 58, 59 (plain words, Release link on its own line, what was NOT run on the tablet, open items) and delete heartbeat trig_01LyMjhKLgSaFigb7F61XAfe.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  cd7443ee ckpt 2548: lab run 43928839921 (gp, head af4a8675): 21/21 checks, 0 issues = ANS57 prov
  11d927ab ckpt 2547: lab run 43927135769 (gp, head 42e933e5): Kestrel's OWN player passes every g
  d6fe07e1 ckpt 2546: heartbeat created: trig_01LyMjhKLgSaFigb7F61XAfe (every 2 h, bound to this s
  42e933e5 ckpt 2545: lab run 43925029376 (gv, head 3c1b38d8): pv scene 7/7 OK on the real engine 
  306ef253 ckpt 2544: dispatched lab gv run 43925029376 (head 3c1b38d8: input motionevent finger p
  3c1b38d8 ckpt 2543: lab touch fix coded (not yet run): raw dd finger paths never reached the app
  dcf083e4 ckpt 2542: ANS59a coded: regular tabs are not put to sleep by a trip to another app eit
  421a0dd9 ckpt 2541: owner message 59 written into TASKS.md (ANS59a-b): 'It should work for regul
  66aa33a7 ckpt 2540: ANS58a/b first cheap measure: kestrel-shields manifest exclude_matches for t
  a23e57dc ckpt 2539: lab suite gv = gp + pv in ONE emulator boot (the queue of GitHub was slow: t
```
