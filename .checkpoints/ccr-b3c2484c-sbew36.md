# CHECKPOINT 2565 — read me first, then TASKS.md

**Written:** 2026-11-16T22:02:08Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `531f917d` (this checkpoint is the commit after it)

## Just done
lab cp scene made robust against a race found by reading the extension (first page after a vfmt change can see the old policy: background.js asks the app at navigation start, not awaited, and answers the page's guard from cache): cp now loads a throw-away /page/text first (soak.py cp_scene), finding written into research/ANS60_PLAYBACK_OPTIMISATION.md section 7 and TASKS ANS60e; cancelled the cp-only run 43964880053 (it would have run the old scene); kp double-tap failure (3 checks, one root: the second of two parallel 'input tap' never arrived) matches the known lab flake but must be re-proven

## Do this next
1) dispatch lab vp (app=lab lab_suite=vp lab_minutes=1 lab_net=full full_build=false) on this head, read its report: cp must be 0 failed and kp must pass (kp double tap in the page's own full screen: if it fails again the same way look at BrowserActivity.pageFilmGestureTouch / VideoGestures / PlayerGestures.up for a real regression of the no-long-press change); 2) fill TESTED_PLACEHOLDER in docs/handoff/ALPHA61_RELEASE_NOTE.txt, ship.sh gecko, ONE run_workflow app=gecko full_build=true, verify tag + jobs + release, record-release.sh with the identical note; 3) tick ANS60a-d in TASKS.md, answer owner 60, delete heartbeat trig_01WwqLTpgB4vKso9Rn5q9WV1

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  abc3386d ckpt 2564: lab vp run 43960432251 (head 5beb6f7e) finished: new ANS60 checks all pass (
  c952601c ckpt 2563: docs for ANS60: CLAUDE.md decisions (gestures without a long press, shared P
  203d9ece ckpt 2562: lab vp (all video suites incl. gp + iq) dispatched: run 43960432251 on head 
  5beb6f7e ckpt 2561: lab run 43958301833 (gp + iq, head 3bf6994e): ALL 26 gesture checks OK (A: p
  c905a3c3 ckpt 2560: JVM suites (core, app, geckoapp) green on the ANS60 head; lab gp + iq dispat
  f0dbcacd ckpt 2559: lab scenes for ANS60 written (not yet run): soak.py gp_scene = A: plain side
  d8aa91f1 ckpt 2558: ANS60d CODED (the emulator cannot show speed, only that nothing broke): Play
  c8fca4df ckpt 2557: ANS60c CODED (lab proof pending): quality list in the player inside the page
  756afc69 ckpt 2556: ANS60a+b CODED (lab proof pending): core PlayerGestures has NO long press an
  6d684f43 ckpt 2555: heartbeat trig_01WwqLTpgB4vKso9Rn5q9WV1 created (cron every 2 h at :45, boun
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
