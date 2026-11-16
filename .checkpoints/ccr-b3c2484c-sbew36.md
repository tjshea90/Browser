# CHECKPOINT 2572 — read me first, then TASKS.md

**Written:** 2026-11-16T23:03:32Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `9d7692ef` (this checkpoint is the commit after it)

## Just done
ANS60a-d ticked in TASKS.md with the lab evidence; HANDOFF alpha61 bullet rewritten (BUILD DISPATCHED, becomes SHIPPED only after the run is green and BUILDLOG has the line); release run 43975612672: check job ok, test-js + test-jvm running at 23:05Z

## Do this next
1) list_workflow_jobs resource_id 43975612672 until test-js, test-jvm, build-gecko and the release job are green (a send_later wake at 23:17Z is set); 2) git ls-remote --tags origin kestrel-g-v0.3.0-alpha61 + get_release_by_tag shows the APK; 3) ONLY THEN bash tools/record-release.sh kestrel-g-v0.3.0-alpha61 "$(cat docs/handoff/ALPHA61_RELEASE_NOTE.txt)" and flip the HANDOFF bullet to SHIPPED; 4) answer owner 60 with the Release link https://github.com/tjshea90/Browser/releases/tag/kestrel-g-v0.3.0-alpha61 on its own line + open items; 5) bash tools/claim.sh ANS60 --done with the evidence; delete heartbeat trig_01WwqLTpgB4vKso9Rn5q9WV1

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M HANDOFF.md

## Last ten checkpoints
```
  c5280d94 ckpt 2571: alpha61 build DISPATCHED ONCE: run 43975612672 (android.yml app=gecko full_b
  1e7b8d2f ckpt 2570: gated kestrel-g-v0.3.0-alpha61 (gecko code 162) and pushed it: handoff self-
  d414de3b ckpt 2569: FINAL lab vp run 43971358668 (head ed7587da, includes all review fixes): 12 
  48102393 ckpt 2568: JVM suites (core, app, geckoapp) green on the head with the review fixes; fi
  2e34191c ckpt 2567: review of alpha61 applied (5 small fixes, unit tests 26/26 in PlayerGestures
  d98f2fd2 ckpt 2566: lab vp dispatched on head 8addd28f (cp warm-up page): run 43966056564 starte
  8addd28f ckpt 2565: lab cp scene made robust against a race found by reading the extension (firs
  abc3386d ckpt 2564: lab vp run 43960432251 (head 5beb6f7e) finished: new ANS60 checks all pass (
  c952601c ckpt 2563: docs for ANS60: CLAUDE.md decisions (gestures without a long press, shared P
  203d9ece ckpt 2562: lab vp (all video suites incl. gp + iq) dispatched: run 43960432251 on head 
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
