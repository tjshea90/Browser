# CHECKPOINT 2509 — read me first, then TASKS.md

**Written:** 2026-11-16T09:39:46Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `ed4efbc9` (this checkpoint is the commit after it)

## Just done
final vp lab run 43838527885 (head 5567de58 = the independent-review fixes) read: 8 scenes, 7 ok; the whole hand-over scene green (29 checks: file incl. pressed again + 'from N s', overlay, hls, unknown, bad, never, Diagnostics), press-to-play, hls cap, video formats, own player ok; 271 check rows ok, 2 failed = the kp 'big button pauses / plays again' checks, a lab timing race (the command line came 1.2 s after the tap, 0.1 s after the lab read the log; the same button passes in the HLS pass of the same run). Lab waits for the four button checks widened 1.2 -> 2.6 s

## Do this next
dispatch lab suite kp ALONE (android.yml app=lab lab_suite=kp lab_minutes=10 lab_net=full full_build=false ref ccr-b3c2484c-sbew36) on this head to get the clean kp result; meanwhile fill @WEBTESTS@ (573) and @LABCHECKS@ in docs/handoff/ALPHA58_RELEASE_NOTE.txt, bump geckoapp/build.gradle.kts 158 -> 159 / 0.3.0-alpha57 -> 0.3.0-alpha58 + bash tools/sync-ext-version.sh, bash ship.sh gecko with the note (the app code is the vp5 head 5567de58 plus the version), ONE dispatch (app=gecko full_build=true), tag + jobs + release, bash tools/record-release.sh <tag> identical note, tick ANS54a-f, answer owner message 54

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  4430c87e ckpt 2508: final lab vp run dispatched: 43838527885 (head 5567de58 = ckpt 2507, started
  5567de58 ckpt 2507: independent review (9 findings, all real) applied: content.js relays only wh
  9ba75b5b ckpt 2506: tap window widened for a slow tablet (core WebHandOver.pressWindowMs 15 s, 3
  51380853 ckpt 2505: ANS54e.9 SECURITY: a page script can fake the gate's question, so every hand
  4ad3a730 ckpt 2504: lab vp run dispatched: 43832408756 (lab job 133180783158, head 6b32808a = ck
  6b32808a ckpt 2503: lab run 43831112200 (ho run 2) read: file now hands over on the real engine 
  57239dd2 ckpt 2502: lab run 43831112200 (suite ho, head bbfc9c3f = ckpt 2501) dispatched; gradle
  bbfc9c3f ckpt 2501: ANS54e: lab run 43826954106 (suite ho) read: overlay/hls/unknown/never ok, f
  00b6ff6b ckpt 2500: lab run 43826954106 dispatched: suite ho (head 97294fcb): web videos play in
  97294fcb ckpt 2499: ANS54e: full web suite 562/562, gradle all green (before the late-hand-over 
```

(5 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
