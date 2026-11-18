# CHECKPOINT 2671 — read me first, then TASKS.md

**Written:** 2026-11-18T05:33:15Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-587430ac-royssd` · **builds on:** `9ba86d07` (this checkpoint is the commit after it)

## Just done
CR1g memory evidence in the Last run note (VmRSS of every app process, MemFree, swap, PSI, 15 s tick from the watchdog thread) + VS2 capture (MediaList captured/orphan, same-page reload keeps, PageIdentity) + VS1e (404 retries, POST skipped) + CR1f (own flush thread, STALL + stack, 3 abnormal runs): full JVM suite green, web 616/616, LastRunTest 12. Lab suite new now = lastrun_round + vs_scene + ql_scene.

## Do this next
dispatch ONE lab: android.yml app=lab lab_suite=new lab_profile=standard lab_net=full lab_minutes=5 on ccr-587430ac-royssd; expect: lr front/background/freeze all ok incl. 'memory at the last note' line, vs keep variant status ok (frame-gone dropped=0, stream still listed), mse/msel play + msel0 held, ql ok; then bump versionCode 171 / 0.3.0-alpha70, rewrite docs/handoff/ALPHA70_RELEASE_NOTE.txt, bash ship.sh gecko, ONE dispatch, verify, record-release

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  97f69909 ckpt 2670: Owner answered CR1f-c: the app vanished to the home screen = process death, 
  1ac8a776 ckpt 2669: Owner message VS2 written into TASKS (doraby: vidsrc + 111movies find no dow
  676dd260 ckpt 2668: alpha69 RELEASED: tag kestrel-g-v0.3.0-alpha69, run 44239780214 all jobs gre
  cf070608 ckpt 2667: alpha69 DISPATCHED ONCE at 04:47Z: android.yml run 44239780214 (run_number 2
  3f51b70a ckpt 2666: gated kestrel-g-v0.3.0-alpha69 (gecko code 170) and pushed it: handoff self-
  de1a9d82 ckpt 2665: Lab run 44236411726 suite new ALL GREEN (13 scenes, 0 issues): dm 9 checks, 
  1c980267 ckpt 2664: Lab run 44233990309 (suite new): dm scenes all OK; FOUND+FIXED: background.j
  1dc61021 ckpt 2663: research/VS1_VP2_NM1_FD1.md written; ALPHA69_RELEASE_NOTE.txt rewritten (FD1
  c8a727cc ckpt 2662: FD1 floating download window done: 8 DownloadPopupFloatTest + OwnerRulesTest
  bb4a5af6 ckpt 2661: Owner message 5 of this session written into TASKS.md BEFORE any code: FD1 (
```

(3 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
