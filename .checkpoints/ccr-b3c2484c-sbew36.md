# CHECKPOINT 2545 — read me first, then TASKS.md

**Written:** 2026-11-16T18:16:27Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `fa50ac2a` (this checkpoint is the commit after it)

## Just done
lab run 43925029376 (gv, head 3c1b38d8): pv scene 7/7 OK on the real engine (private tab id 2 and regular tab id 4 kept awake and not reloaded after Home + trim, a second private tab sees the login) = ANS58c + ANS59b proven; gp A vertical drags OK (volume 27->91, brightness 40->92); hold+drag still lost: the lab cannot inject a long press (dd /dev/input Permission denied = adb shell not root; Android 10 has no input motionevent). New: debug-only web/DebugTouch plays a finger path inside the app (am start --es kestrel_debug touch), Phone.hold_drag builds the script; compile + OwnerRules guards + selftest green; JVM suites + web tests (576) green on 3c1b38d8

## Do this next
1) dispatch lab gp (android.yml app=lab lab_suite=gp lab_minutes=1 lab_net=full) on this head: hold+drag seek in Kestrel's player (A) and the three inline hold-first drags (B) must now show gesture-armed / gesture-seek / gesture-level lines; read report.md + console.txt (artifacts: list_workflow_run_artifacts, download_workflow_run_artifact, python3 -I tools/emu/fetch_artifact.py); fix what it shows; 2) lab vp (all video suites) once gp is green; 3) ship alpha60 (versionCode 161, 0.3.0-alpha60, tools/sync-ext-version.sh, fill TESTED_PLACEHOLDER in docs/handoff/ALPHA60_RELEASE_NOTE.txt, bash ship.sh gecko, ONE dispatch, record-release.sh); 4) answer owner 55, 57, 58, 59.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  306ef253 ckpt 2544: dispatched lab gv run 43925029376 (head 3c1b38d8: input motionevent finger p
  3c1b38d8 ckpt 2543: lab touch fix coded (not yet run): raw dd finger paths never reached the app
  dcf083e4 ckpt 2542: ANS59a coded: regular tabs are not put to sleep by a trip to another app eit
  421a0dd9 ckpt 2541: owner message 59 written into TASKS.md (ANS59a-b): 'It should work for regul
  66aa33a7 ckpt 2540: ANS58a/b first cheap measure: kestrel-shields manifest exclude_matches for t
  a23e57dc ckpt 2539: lab suite gv = gp + pv in ONE emulator boot (the queue of GitHub was slow: t
  c70f2aac ckpt 2538: ANS58c FIX coded + tested: private tabs are NEVER put to sleep by the memory
  c0017fa7 ckpt 2537: owner message 58 written into TASKS.md (ANS58a-d): a PRIVATE tab was reloade
  3fb0e402 ckpt 2536: ANS57 lab scene: the gesture info and the level bars are now photographed wh
  9a20b91e ckpt 2535: ANS57 lab gp run 1 (43918159286): volume / brightness drags, dead zone, plai
```

(16 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
