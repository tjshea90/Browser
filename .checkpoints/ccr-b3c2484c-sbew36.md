# CHECKPOINT 2568 — read me first, then TASKS.md

**Written:** 2026-11-16T22:35:05Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `ed7587da` (this checkpoint is the commit after it)

## Just done
JVM suites (core, app, geckoapp) green on the head with the review fixes; final lab vp dispatched: run 43971358668 on head ed7587da (started 22:34Z, about 23 min): must show gp 27/27, iq 10/10, np stats line, kp double tap (now Phone.double_tap = exact 90 ms gap played by the app), cp green

## Do this next
1) read run 43971358668 (get_workflow_run -> artifacts emulator-lab-text -> download_workflow_run_artifact -> python3 -I tools/emu/fetch_artifact.py <url with & not u0026> <empty dir>, report.md: Scene results, Issues, grep FAILED, count '| ok |'); if only lab flakes remain, fix the lab not the app; if a real app failure, fix + unit test + re-run that scene; 2) fill TESTED_PLACEHOLDER in docs/handoff/ALPHA61_RELEASE_NOTE.txt (ASCII, no double quotes, no backslashes) with the real numbers, bash ship.sh gecko "$(cat docs/handoff/ALPHA61_RELEASE_NOTE.txt)", ONE run_workflow app=gecko full_build=true note=identical, verify tag kestrel-g-v0.3.0-alpha61 (git ls-remote --tags origin) + all jobs + get_release_by_tag, then bash tools/record-release.sh kestrel-g-v0.3.0-alpha61 with the IDENTICAL note; 3) tick ANS60a-d in TASKS.md with evidence, HANDOFF.md alpha61 bullet, answer owner 60 in plain words with the Release link on its own line and the open items, delete heartbeat trig_01WwqLTpgB4vKso9Rn5q9WV1 (and the one-shot trig_01VLofQGKNqUE6ZECT28hKYX is spent)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  2e34191c ckpt 2567: review of alpha61 applied (5 small fixes, unit tests 26/26 in PlayerGestures
  d98f2fd2 ckpt 2566: lab vp dispatched on head 8addd28f (cp warm-up page): run 43966056564 starte
  8addd28f ckpt 2565: lab cp scene made robust against a race found by reading the extension (firs
  abc3386d ckpt 2564: lab vp run 43960432251 (head 5beb6f7e) finished: new ANS60 checks all pass (
  c952601c ckpt 2563: docs for ANS60: CLAUDE.md decisions (gestures without a long press, shared P
  203d9ece ckpt 2562: lab vp (all video suites incl. gp + iq) dispatched: run 43960432251 on head 
  5beb6f7e ckpt 2561: lab run 43958301833 (gp + iq, head 3bf6994e): ALL 26 gesture checks OK (A: p
  c905a3c3 ckpt 2560: JVM suites (core, app, geckoapp) green on the ANS60 head; lab gp + iq dispat
  f0dbcacd ckpt 2559: lab scenes for ANS60 written (not yet run): soak.py gp_scene = A: plain side
  d8aa91f1 ckpt 2558: ANS60d CODED (the emulator cannot show speed, only that nothing broke): Play
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
