# CHECKPOINT 2573 — read me first, then TASKS.md

**Written:** 2026-11-16T23:20:51Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `a7a3711a` (this checkpoint is the commit after it)

## Just done
alpha61 SHIPPED and RECORDED: Release kestrel-g-v0.3.0-alpha61 (code 162, run 43975612672 green first time: check, test-js, test-jvm, build-gecko incl. the APK certificate verification, Publish the Release; APK 95 MB + sha256; BUILDLOG.md line pushed by tools/record-release.sh); final lab vp 43971358668 = 12 scenes, 367 checks, 0 failed; independent review: 5 small fixes applied before shipping; ANS60 + ANS60a-d ticked, HANDOFF flipped to SHIPPED, heartbeat trig_01WwqLTpgB4vKso9Rn5q9WV1 deleted; owner's first report: 'The release is out. It works well'; owner answer for message 60 is given in the chat

## Do this next
NOTHING is running and no release is pending: wait for the owner's tablet report (TASKS.md row OWNER TEST alpha61: how the no-long-press drags feel, the Quality button inside a page, and the Diagnostics 'stats' lines after a film); when it comes: write it into TASKS.md in his words first, read the stats lines (research/ANS60_PLAYBACK_OPTIMISATION.md sections 3, 5, 8 say which tuning each number decides); open items: ANS58a/b (slow Cloudflare check + typing lag, unmeasured), ANS58e, ANS55h (back to inline after full screen), VP12 (YouTube-style films with no playlist address), VP10, ROT2e, ANS48, SOAK1d, LAB2f, ANS52j, ANS60e (first page after a Settings > video formats change can see the old policy: guardAnswer bounded wait, web test in web-tests/background.test.js), ANS60f (NativePlayerActivity OnBackInvokedCallback on API 33+)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  6d7a9cdb ckpt 2572: ANS60a-d ticked in TASKS.md with the lab evidence; HANDOFF alpha61 bullet re
  c5280d94 ckpt 2571: alpha61 build DISPATCHED ONCE: run 43975612672 (android.yml app=gecko full_b
  1e7b8d2f ckpt 2570: gated kestrel-g-v0.3.0-alpha61 (gecko code 162) and pushed it: handoff self-
  d414de3b ckpt 2569: FINAL lab vp run 43971358668 (head ed7587da, includes all review fixes): 12 
  48102393 ckpt 2568: JVM suites (core, app, geckoapp) green on the head with the review fixes; fi
  2e34191c ckpt 2567: review of alpha61 applied (5 small fixes, unit tests 26/26 in PlayerGestures
  d98f2fd2 ckpt 2566: lab vp dispatched on head 8addd28f (cp warm-up page): run 43966056564 starte
  8addd28f ckpt 2565: lab cp scene made robust against a race found by reading the extension (firs
  abc3386d ckpt 2564: lab vp run 43960432251 (head 5beb6f7e) finished: new ANS60 checks all pass (
  c952601c ckpt 2563: docs for ANS60: CLAUDE.md decisions (gestures without a long press, shared P
```

(4 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
