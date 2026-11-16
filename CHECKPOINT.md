# CHECKPOINT 2514 — read me first, then TASKS.md

**Written:** 2026-11-16T09:57:55Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `596e4e4a` (this checkpoint is the commit after it)

## Just done
alpha58 RELEASE DISPATCHED AGAIN (the first run 43843245891 made no release: a racy old web test failed): android.yml run 43844463765 (run 171, app=gecko full_build=true, head 596e4e4a, queued 09:57Z)

## Do this next
do NOT dispatch again unless this run ends WITHOUT a tag: wait for run 43844463765 (actions_list list_workflow_jobs resource_id 43844463765 workflow_jobs_filter latest perPage 8 is ~6 KB; or git ls-remote --tags origin | grep alpha58 then get_release_by_tag kestrel-g-v0.3.0-alpha58): tag + test-jvm + test-js + build-gecko success + release with the APK; ONLY THEN bash tools/record-release.sh kestrel-g-v0.3.0-alpha58 "$(cat docs/handoff/ALPHA58_RELEASE_NOTE.txt)"; tick ANS54a-f in TASKS.md; HANDOFF alpha58 bullet -> SHIPPED; answer owner message 54 (Release link on its own line, nothing ran on the tablet, open items re-listed)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  596e4e4a ckpt 2513: release run 43843245891 FAILED before any build: GitHub's web-test job faile
  d639fb36 ckpt 2512: alpha58 RELEASE DISPATCHED (ONE time): android.yml run 43843245891 (run 170,
  59ccf63d ckpt 2511: gated kestrel-g-v0.3.0-alpha58 (gecko code 159) and pushed it: handoff self-
  eb003faf ckpt 2510: alpha58 release prepared (NOT shipped yet): geckoapp versionCode 159 / 0.3.0
  a25fa09f ckpt 2509: final vp lab run 43838527885 (head 5567de58 = the independent-review fixes) 
  4430c87e ckpt 2508: final lab vp run dispatched: 43838527885 (head 5567de58 = ckpt 2507, started
  5567de58 ckpt 2507: independent review (9 findings, all real) applied: content.js relays only wh
  9ba75b5b ckpt 2506: tap window widened for a slow tablet (core WebHandOver.pressWindowMs 15 s, 3
  51380853 ckpt 2505: ANS54e.9 SECURITY: a page script can fake the gate's question, so every hand
  4ad3a730 ckpt 2504: lab vp run dispatched: 43832408756 (lab job 133180783158, head 6b32808a = ck
```
