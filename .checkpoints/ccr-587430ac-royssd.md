# CHECKPOINT 2667 — read me first, then TASKS.md

**Written:** 2026-11-18T04:48:05Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-587430ac-royssd` · **builds on:** `3f51b70a` (this checkpoint is the commit after it)

## Just done
alpha69 DISPATCHED ONCE at 04:47Z: android.yml run 44239780214 (run_number 237, app=gecko full_build=true, head 3f51b70a). Do NOT dispatch again.

## Do this next
WAIT for run 44239780214 (get_workflow_run / list_workflow_jobs, no foreground sleep over 55 s): if ALL jobs green -> git ls-remote --tags origin kestrel-g-v0.3.0-alpha69, mcp__github__get_release_by_tag (APK asset present), then bash tools/record-release.sh kestrel-g-v0.3.0-alpha69 with the IDENTICAL note in docs/handoff/ALPHA69_RELEASE_NOTE.txt, then tick DM1c / VS1c / QL1c in TASKS.md and answer the owner in plain words (what changed, what is NOT proven on the tablet) with the Release link on its own line. If a job is red: read it (get_job_logs only after the job ended); test flakes = BrowserFlowTest.viewIntentOpensTheUrlInTheBrowser (Thread.sleep 300) and core PhoneBackendTest:94 -> make them deterministic, bump nothing, and ship a FRESH dispatch only if no tag exists; 403 on Publish the Release = check workflow permissions contents: write on build-gecko

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  3f51b70a ckpt 2666: gated kestrel-g-v0.3.0-alpha69 (gecko code 170) and pushed it: handoff self-
  de1a9d82 ckpt 2665: Lab run 44236411726 suite new ALL GREEN (13 scenes, 0 issues): dm 9 checks, 
  1c980267 ckpt 2664: Lab run 44233990309 (suite new): dm scenes all OK; FOUND+FIXED: background.j
  1dc61021 ckpt 2663: research/VS1_VP2_NM1_FD1.md written; ALPHA69_RELEASE_NOTE.txt rewritten (FD1
  c8a727cc ckpt 2662: FD1 floating download window done: 8 DownloadPopupFloatTest + OwnerRulesTest
  bb4a5af6 ckpt 2661: Owner message 5 of this session written into TASKS.md BEFORE any code: FD1 (
  78aff752 ckpt 2660: VS1d: vidfast 'stuck on loading' recorded in TASKS (owner words). Lab: vidla
  5375bf9c ckpt 2659: alpha69 CORRECTION: run 44224672021 did NOT build: test-jvm FAILED on GitHub
  de4ea102 ckpt 2658: QL1 + VS1 + DM2 coded and self-tested. QL1: page side (quality-main.js, qual
  96a653ee ckpt 2657: Owner message 3 of this session written into TASKS.md BEFORE any code: VS1 (
```
