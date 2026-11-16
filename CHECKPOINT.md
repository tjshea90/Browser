# CHECKPOINT 2513 — read me first, then TASKS.md

**Written:** 2026-11-16T09:57:26Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `7d1d4e04` (this checkpoint is the commit after it)

## Just done
release run 43843245891 FAILED before any build: GitHub's web-test job failed ONE old test (player.test.js VP1 'a = 0 four times': it read the page before the player had started, a timing race on a slow runner; the 22 new hand-over tests and the other 572 passed), so build-gecko was skipped and NO tag / release exists (git ls-remote shows none). Test made race-free (waits for the player to start, then to give up); player tests 27/27 locally

## Do this next
dispatch the alpha58 release AGAIN, once (the first run made no release): android.yml app=gecko full_build=true ref ccr-b3c2484c-sbew36 note = the exact text of docs/handoff/ALPHA58_RELEASE_NOTE.txt; record its run id in a ckpt at once; then wait for tag kestrel-g-v0.3.0-alpha58 + all jobs success (test-js can flake: if a DIFFERENT old test fails read it and decide) + get_release_by_tag shows the APK; ONLY THEN bash tools/record-release.sh kestrel-g-v0.3.0-alpha58 with the IDENTICAL note; tick ANS54a-f; answer owner message 54

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  d639fb36 ckpt 2512: alpha58 RELEASE DISPATCHED (ONE time): android.yml run 43843245891 (run 170,
  59ccf63d ckpt 2511: gated kestrel-g-v0.3.0-alpha58 (gecko code 159) and pushed it: handoff self-
  eb003faf ckpt 2510: alpha58 release prepared (NOT shipped yet): geckoapp versionCode 159 / 0.3.0
  a25fa09f ckpt 2509: final vp lab run 43838527885 (head 5567de58 = the independent-review fixes) 
  4430c87e ckpt 2508: final lab vp run dispatched: 43838527885 (head 5567de58 = ckpt 2507, started
  5567de58 ckpt 2507: independent review (9 findings, all real) applied: content.js relays only wh
  9ba75b5b ckpt 2506: tap window widened for a slow tablet (core WebHandOver.pressWindowMs 15 s, 3
  51380853 ckpt 2505: ANS54e.9 SECURITY: a page script can fake the gate's question, so every hand
  4ad3a730 ckpt 2504: lab vp run dispatched: 43832408756 (lab job 133180783158, head 6b32808a = ck
  6b32808a ckpt 2503: lab run 43831112200 (ho run 2) read: file now hands over on the real engine 
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
