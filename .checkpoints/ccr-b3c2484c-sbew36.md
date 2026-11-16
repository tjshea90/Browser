# CHECKPOINT 2510 — read me first, then TASKS.md

**Written:** 2026-11-16T09:40:49Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `f7dec7b9` (this checkpoint is the commit after it)

## Just done
alpha58 release prepared (NOT shipped yet): geckoapp versionCode 159 / 0.3.0-alpha58, extension manifest 1.0.159, docs/handoff/ALPHA58_RELEASE_NOTE.txt final (573 web tests, about 270 emulator checks, 9 review problems, honest 'not on your tablet')

## Do this next
wait for the kp rerun (dispatched 09:5x, find it with list_workflow_runs status in_progress perPage 1 -> its lab job -> artifact emulator-lab-text): kp all green? then bash ship.sh gecko with the note (note=$(cat docs/handoff/ALPHA58_RELEASE_NOTE.txt)), ONE dispatch (android.yml app=gecko full_build=true note=same text, ref ccr-b3c2484c-sbew36), tag kestrel-g-v0.3.0-alpha58 (git ls-remote --tags origin) + all jobs success + get_release_by_tag shows the APK, bash tools/record-release.sh kestrel-g-v0.3.0-alpha58 with the IDENTICAL note, tick ANS54a-f in TASKS, answer owner message 54

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M docs/handoff/ALPHA58_RELEASE_NOTE.txt

## Last ten checkpoints
```
  a25fa09f ckpt 2509: final vp lab run 43838527885 (head 5567de58 = the independent-review fixes) 
  4430c87e ckpt 2508: final lab vp run dispatched: 43838527885 (head 5567de58 = ckpt 2507, started
  5567de58 ckpt 2507: independent review (9 findings, all real) applied: content.js relays only wh
  9ba75b5b ckpt 2506: tap window widened for a slow tablet (core WebHandOver.pressWindowMs 15 s, 3
  51380853 ckpt 2505: ANS54e.9 SECURITY: a page script can fake the gate's question, so every hand
  4ad3a730 ckpt 2504: lab vp run dispatched: 43832408756 (lab job 133180783158, head 6b32808a = ck
  6b32808a ckpt 2503: lab run 43831112200 (ho run 2) read: file now hands over on the real engine 
  57239dd2 ckpt 2502: lab run 43831112200 (suite ho, head bbfc9c3f = ckpt 2501) dispatched; gradle
  bbfc9c3f ckpt 2501: ANS54e: lab run 43826954106 (suite ho) read: overlay/hls/unknown/never ok, f
  00b6ff6b ckpt 2500: lab run 43826954106 dispatched: suite ho (head 97294fcb): web videos play in
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
