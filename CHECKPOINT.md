# CHECKPOINT 2544 — read me first, then TASKS.md

**Written:** 2026-11-16T18:06:02Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `6537ebb9` (this checkpoint is the commit after it)

## Just done
dispatched lab gv run 43925029376 (head 3c1b38d8: input motionevent finger path, touch-probe.txt, pv tab ids, life destroy log); release note ALPHA60 has the private/regular tab, challenge-frame paragraphs (TESTED_PLACEHOLDER still to fill); JVM unit suites running in the background

## Do this next
1) when run 43925029376 ends (about 18:13Z): download emulator-lab-text (artifacts via mcp__github__actions_list list_workflow_run_artifacts, then download_workflow_run_artifact + python3 -I tools/emu/fetch_artifact.py), read touch-probe.txt + report.md (gp A/B hold+drag seek, volume/brightness, pv private+regular tab ids); fix what it shows; 2) lab vp (all video suites) once gv is green; 3) web tests (cd web-tests && npm test) + JVM suites on the final head; 4) ship alpha60: versionCode 161 / 0.3.0-alpha60, tools/sync-ext-version.sh, fill TESTED_PLACEHOLDER in docs/handoff/ALPHA60_RELEASE_NOTE.txt, bash ship.sh gecko, ONE dispatch, record-release.sh; 5) answer owner 55, 57, 58, 59.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  3c1b38d8 ckpt 2543: lab touch fix coded (not yet run): raw dd finger paths never reached the app
  dcf083e4 ckpt 2542: ANS59a coded: regular tabs are not put to sleep by a trip to another app eit
  421a0dd9 ckpt 2541: owner message 59 written into TASKS.md (ANS59a-b): 'It should work for regul
  66aa33a7 ckpt 2540: ANS58a/b first cheap measure: kestrel-shields manifest exclude_matches for t
  a23e57dc ckpt 2539: lab suite gv = gp + pv in ONE emulator boot (the queue of GitHub was slow: t
  c70f2aac ckpt 2538: ANS58c FIX coded + tested: private tabs are NEVER put to sleep by the memory
  c0017fa7 ckpt 2537: owner message 58 written into TASKS.md (ANS58a-d): a PRIVATE tab was reloade
  3fb0e402 ckpt 2536: ANS57 lab scene: the gesture info and the level bars are now photographed wh
  9a20b91e ckpt 2535: ANS57 lab gp run 1 (43918159286): volume / brightness drags, dead zone, plai
  e3cd26c4 ckpt 2534: ANS57: lab suite gp DISPATCHED: run 43918159286 (head 55e61f02, started 17:2
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
