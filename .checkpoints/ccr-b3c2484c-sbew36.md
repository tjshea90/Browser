# CHECKPOINT 2571 — read me first, then TASKS.md

**Written:** 2026-11-16T23:02:23Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `1e7b8d2f` (this checkpoint is the commit after it)

## Just done
alpha61 build DISPATCHED ONCE: run 43975612672 (android.yml app=gecko full_build=true, head 1e7b8d2f, started 23:02Z); NEVER dispatch again for this tag

## Do this next
1) wait for run 43975612672: mcp__github__actions_list list_workflow_jobs resource_id 43975612672 (small: use it; do NOT list runs again, the head commit message holds the whole note and is huge); all jobs green (check, test-jvm, test-js, build-gecko, release); 2) git ls-remote --tags origin kestrel-g-v0.3.0-alpha61, mcp__github__get_release_by_tag tag kestrel-g-v0.3.0-alpha61 shows the APK; 3) ONLY THEN bash tools/record-release.sh kestrel-g-v0.3.0-alpha61 "$(cat docs/handoff/ALPHA61_RELEASE_NOTE.txt)" (identical note); 4) tick ANS60a-d in TASKS.md (evidence: lab 43958301833 gp+iq, final vp 43971358668 367 checks 0 failed, run 43975612672), HANDOFF alpha61 bullet (IN PROGRESS -> SHIPPED), CLAUDE.md untouched; 5) answer owner 60 in plain words with the Release link https://github.com/tjshea90/Browser/releases/tag/kestrel-g-v0.3.0-alpha61 on its own line and re-list the open items (ANS58a/b, ANS58e, ANS55h, VP12, VP10, ROT2e, ANS48, SOAK1d, LAB2f, ANS52j, new ANS60e, ANS60f); 6) delete heartbeat trig_01WwqLTpgB4vKso9Rn5q9WV1 with mcp__claude-code-remote__delete_trigger

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  1e7b8d2f ckpt 2570: gated kestrel-g-v0.3.0-alpha61 (gecko code 162) and pushed it: handoff self-
  d414de3b ckpt 2569: FINAL lab vp run 43971358668 (head ed7587da, includes all review fixes): 12 
  48102393 ckpt 2568: JVM suites (core, app, geckoapp) green on the head with the review fixes; fi
  2e34191c ckpt 2567: review of alpha61 applied (5 small fixes, unit tests 26/26 in PlayerGestures
  d98f2fd2 ckpt 2566: lab vp dispatched on head 8addd28f (cp warm-up page): run 43966056564 starte
  8addd28f ckpt 2565: lab cp scene made robust against a race found by reading the extension (firs
  abc3386d ckpt 2564: lab vp run 43960432251 (head 5beb6f7e) finished: new ANS60 checks all pass (
  c952601c ckpt 2563: docs for ANS60: CLAUDE.md decisions (gestures without a long press, shared P
  203d9ece ckpt 2562: lab vp (all video suites incl. gp + iq) dispatched: run 43960432251 on head 
  5beb6f7e ckpt 2561: lab run 43958301833 (gp + iq, head 3bf6994e): ALL 26 gesture checks OK (A: p
```
