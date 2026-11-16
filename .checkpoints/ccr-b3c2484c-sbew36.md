# CHECKPOINT 2569 — read me first, then TASKS.md

**Written:** 2026-11-16T23:00:37Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `946ba921` (this checkpoint is the commit after it)

## Just done
FINAL lab vp run 43971358668 (head ed7587da, includes all review fixes): 12 of 12 scenes ok, 367 checks ok, 0 failed, 0 issues, no restart (gp 27/27, iq 10/10, np 43/43, kp 37/37 with the exact double tap, cp 101/101, hls 28/28, ap 40/40, ho 53/53, ip 21/21, pv 7/7); release note TESTED text filled in docs/handoff/ALPHA61_RELEASE_NOTE.txt (ASCII, no quotes/backslashes)

## Do this next
1) run bash /tmp/claude-0/-home-user-Browser/b87c69cb-83c4-59c8-95ee-0d82312438d2/scratchpad/ship61.sh (it runs bash ship.sh gecko with the note; log in the scratchpad ship61.log; wait for SHIP EXIT 0) then read ship61.log; 2) ONE mcp__github__actions_run_trigger run_workflow android.yml ref ccr-b3c2484c-sbew36 inputs app=gecko full_build=true note=<the identical note>; 3) verify tag kestrel-g-v0.3.0-alpha61 (git ls-remote --tags origin), all jobs green (list_workflow_jobs), get_release_by_tag shows the APK; 4) bash tools/record-release.sh kestrel-g-v0.3.0-alpha61 with the IDENTICAL note; 5) tick ANS60a-d in TASKS.md (evidence: lab runs 43958301833 gp+iq, 43971358668 vp), HANDOFF alpha61 bullet, answer owner 60 in plain words with the Release link on its own line, delete heartbeat trig_01WwqLTpgB4vKso9Rn5q9WV1

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M docs/handoff/ALPHA61_RELEASE_NOTE.txt

## Last ten checkpoints
```
  48102393 ckpt 2568: JVM suites (core, app, geckoapp) green on the head with the review fixes; fi
  2e34191c ckpt 2567: review of alpha61 applied (5 small fixes, unit tests 26/26 in PlayerGestures
  d98f2fd2 ckpt 2566: lab vp dispatched on head 8addd28f (cp warm-up page): run 43966056564 starte
  8addd28f ckpt 2565: lab cp scene made robust against a race found by reading the extension (firs
  abc3386d ckpt 2564: lab vp run 43960432251 (head 5beb6f7e) finished: new ANS60 checks all pass (
  c952601c ckpt 2563: docs for ANS60: CLAUDE.md decisions (gestures without a long press, shared P
  203d9ece ckpt 2562: lab vp (all video suites incl. gp + iq) dispatched: run 43960432251 on head 
  5beb6f7e ckpt 2561: lab run 43958301833 (gp + iq, head 3bf6994e): ALL 26 gesture checks OK (A: p
  c905a3c3 ckpt 2560: JVM suites (core, app, geckoapp) green on the ANS60 head; lab gp + iq dispat
  f0dbcacd ckpt 2559: lab scenes for ANS60 written (not yet run): soak.py gp_scene = A: plain side
```

(7 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
