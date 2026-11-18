# CHECKPOINT 2672 — read me first, then TASKS.md

**Written:** 2026-11-18T05:34:10Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-587430ac-royssd` · **builds on:** `0cecc2af` (this checkpoint is the commit after it)

## Just done
Lab run 44245067663 (suite new = lr freeze/kill + vs incl. keep + ql) DISPATCHED 05:33Z on head 5ce8de0c. alpha70 prepared in the tree: versionCode 171 / 0.3.0-alpha70, extension manifest 1.0.171, docs/handoff/ALPHA70_RELEASE_NOTE.txt written. NOT shipped.

## Do this next
WAIT for run 44245067663 (list_workflow_run_artifacts, ~15-25 min; ffmpeg step can take 12 min), read report.md: lr (front/background/freeze + memory line), vs (mse, msel, msel0 held, auto, keep, click, nest, worker), ql. Fix what fails (one lab at a time). When green: bash ship.sh gecko with the ALPHA70 note (cat file), ONE dispatch app=gecko full_build=true, verify tag kestrel-g-v0.3.0-alpha70 + jobs + release, bash tools/record-release.sh <tag> <same note>, answer owner (VS2 capture, crash evidence, VS1e), ask for Diagnostics after vidsrc/111movies

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  5ce8de0c ckpt 2671: CR1g memory evidence in the Last run note (VmRSS of every app process, MemFr
  97f69909 ckpt 2670: Owner answered CR1f-c: the app vanished to the home screen = process death, 
  1ac8a776 ckpt 2669: Owner message VS2 written into TASKS (doraby: vidsrc + 111movies find no dow
  676dd260 ckpt 2668: alpha69 RELEASED: tag kestrel-g-v0.3.0-alpha69, run 44239780214 all jobs gre
  cf070608 ckpt 2667: alpha69 DISPATCHED ONCE at 04:47Z: android.yml run 44239780214 (run_number 2
  3f51b70a ckpt 2666: gated kestrel-g-v0.3.0-alpha69 (gecko code 170) and pushed it: handoff self-
  de1a9d82 ckpt 2665: Lab run 44236411726 suite new ALL GREEN (13 scenes, 0 issues): dm 9 checks, 
  1c980267 ckpt 2664: Lab run 44233990309 (suite new): dm scenes all OK; FOUND+FIXED: background.j
  1dc61021 ckpt 2663: research/VS1_VP2_NM1_FD1.md written; ALPHA69_RELEASE_NOTE.txt rewritten (FD1
  c8a727cc ckpt 2662: FD1 floating download window done: 8 DownloadPopupFloatTest + OwnerRulesTest
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
