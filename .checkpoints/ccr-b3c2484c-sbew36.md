# CHECKPOINT 2512 — read me first, then TASKS.md

**Written:** 2026-11-16T09:50:11Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `58edf4a6` (this checkpoint is the commit after it)

## Just done
alpha58 RELEASE DISPATCHED (ONE time): android.yml run 43843245891 (run 170, app=gecko full_build=true, head 58edf4a6, queued 09:49Z); kp rerun 43841795073 read: the earlier flaky button checks passed, one other timing check of the old controls scene missed (the lab's second tap of a double tap never reached the app), none of the changed code involved; hand-over scene green on the final code in vp run 43838527885

## Do this next
do NOT dispatch the release again. Wait for run 43843245891: tag (git ls-remote --tags origin | grep alpha58) + all jobs success (actions_list list_workflow_jobs resource_id 43843245891 workflow_jobs_filter latest perPage 4, small) + mcp__github__get_release_by_tag kestrel-g-v0.3.0-alpha58 shows the APK; ONLY THEN bash tools/record-release.sh kestrel-g-v0.3.0-alpha58 with the IDENTICAL note (docs/handoff/ALPHA58_RELEASE_NOTE.txt, passed as $(cat ...)); tick ANS54a-f in TASKS.md with the evidence (lab runs 43826954106, 43831112200, 43838527885, 43841795073, run 43843245891); update HANDOFF.md alpha58 bullet to SHIPPED; answer owner message 54 point by point (a lag -> Kestrel's player; b landscape -> Follow default; c x box -> site box vs Kestrel's list with an x; d thanks; e the request; f this answer) with the Release link on its own line, say plainly nothing ran on the tablet, re-list open items (VP10, VP12 films with no playlist address e.g. YouTube, ROT2e, ANS48, SOAK1d, LAB2f, ANS52j, owner's Diagnostics lines)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  59ccf63d ckpt 2511: gated kestrel-g-v0.3.0-alpha58 (gecko code 159) and pushed it: handoff self-
  eb003faf ckpt 2510: alpha58 release prepared (NOT shipped yet): geckoapp versionCode 159 / 0.3.0
  a25fa09f ckpt 2509: final vp lab run 43838527885 (head 5567de58 = the independent-review fixes) 
  4430c87e ckpt 2508: final lab vp run dispatched: 43838527885 (head 5567de58 = ckpt 2507, started
  5567de58 ckpt 2507: independent review (9 findings, all real) applied: content.js relays only wh
  9ba75b5b ckpt 2506: tap window widened for a slow tablet (core WebHandOver.pressWindowMs 15 s, 3
  51380853 ckpt 2505: ANS54e.9 SECURITY: a page script can fake the gate's question, so every hand
  4ad3a730 ckpt 2504: lab vp run dispatched: 43832408756 (lab job 133180783158, head 6b32808a = ck
  6b32808a ckpt 2503: lab run 43831112200 (ho run 2) read: file now hands over on the real engine 
  57239dd2 ckpt 2502: lab run 43831112200 (suite ho, head bbfc9c3f = ckpt 2501) dispatched; gradle
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
