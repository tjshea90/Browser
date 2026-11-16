# CHECKPOINT 2564 — read me first, then TASKS.md

**Written:** 2026-11-16T21:56:59Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `c83b7978` (this checkpoint is the commit after it)

## Just done
lab vp run 43960432251 (head 5beb6f7e) finished: new ANS60 checks all pass (gp 27/27, iq 10/10, np 43/43, ip/ho/ap/hs/pv green) but 17 checks failed in two scenes outside my change: cp (14: page codec policy seems applied one vfmt mode late) and kp (3: double tap in the page's own full screen); to tell flake from regression dispatched lab cp ALONE: run 43964880053 (head c83b7978, same code as 5beb6f7e)

## Do this next
1) read run 43964880053 (actions_list list_workflow_jobs for the job id, get_workflow_job; when done list_workflow_run_artifacts + download_workflow_run_artifact, python3 -I tools/emu/fetch_artifact.py <url with & not u0026> <empty dir>, read report.md): cp all green = flake (then also dispatch kp alone, run 3 must be green or explainable) ; cp red the same way = real regression: look at the vfmt debug command in BrowserActivity.kt (line ~890, applyVideo) and soak.py cp_scene (lines ~2089-2158: debug vfmt, sleep 2.5, open_url) 2) fill TESTED_PLACEHOLDER in docs/handoff/ALPHA61_RELEASE_NOTE.txt, ship.sh gecko, ONE run_workflow app=gecko full_build=true, verify, record-release.sh with identical note 3) tick ANS60a-d in TASKS.md, answer owner 60, delete heartbeat trig_01WwqLTpgB4vKso9Rn5q9WV1

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  c952601c ckpt 2563: docs for ANS60: CLAUDE.md decisions (gestures without a long press, shared P
  203d9ece ckpt 2562: lab vp (all video suites incl. gp + iq) dispatched: run 43960432251 on head 
  5beb6f7e ckpt 2561: lab run 43958301833 (gp + iq, head 3bf6994e): ALL 26 gesture checks OK (A: p
  c905a3c3 ckpt 2560: JVM suites (core, app, geckoapp) green on the ANS60 head; lab gp + iq dispat
  f0dbcacd ckpt 2559: lab scenes for ANS60 written (not yet run): soak.py gp_scene = A: plain side
  d8aa91f1 ckpt 2558: ANS60d CODED (the emulator cannot show speed, only that nothing broke): Play
  c8fca4df ckpt 2557: ANS60c CODED (lab proof pending): quality list in the player inside the page
  756afc69 ckpt 2556: ANS60a+b CODED (lab proof pending): core PlayerGestures has NO long press an
  6d684f43 ckpt 2555: heartbeat trig_01WwqLTpgB4vKso9Rn5q9WV1 created (cron every 2 h at :45, boun
  00db0efd ckpt 2554: owner message 60 written into TASKS.md (ANS60a-d) in his words: gestures wit
```

(5 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
