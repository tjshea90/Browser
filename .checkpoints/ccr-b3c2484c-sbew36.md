# CHECKPOINT 2566 — read me first, then TASKS.md

**Written:** 2026-11-16T22:03:54Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `8addd28f` (this checkpoint is the commit after it)

## Just done
lab vp dispatched on head 8addd28f (cp warm-up page): run 43966056564 started 22:02Z (about 25 min); an independent code review agent (ship:ship-code-reviewer, read-only, scope git diff ce6ca1f7..HEAD of the product code) runs in parallel and reports in this session

## Do this next
1) wait for run 43966056564 (list_workflow_jobs for the lab job id; get_workflow_job; when completed list_workflow_run_artifacts + download_workflow_run_artifact + python3 -I tools/emu/fetch_artifact.py <url with & not u0026> <empty dir>, read report.md): cp 0 failed, kp passes (else look at PlayerGestures.up / VideoGestures for a real regression); 2) read the review agent's findings (notification), fix real ones, re-run unit tests (./gradlew --offline -q :core:test :app:testDebugUnitTest :geckoapp:testDebugUnitTest) and web tests (cd web-tests && npm test); a fix that touches product code needs a lab re-run of the touched scene before shipping; 3) fill TESTED_PLACEHOLDER in docs/handoff/ALPHA61_RELEASE_NOTE.txt, ship.sh gecko, ONE run_workflow app=gecko full_build=true, verify, record-release.sh with the identical note; 4) tick ANS60a-d, answer owner 60, delete heartbeat trig_01WwqLTpgB4vKso9Rn5q9WV1

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  8addd28f ckpt 2565: lab cp scene made robust against a race found by reading the extension (firs
  abc3386d ckpt 2564: lab vp run 43960432251 (head 5beb6f7e) finished: new ANS60 checks all pass (
  c952601c ckpt 2563: docs for ANS60: CLAUDE.md decisions (gestures without a long press, shared P
  203d9ece ckpt 2562: lab vp (all video suites incl. gp + iq) dispatched: run 43960432251 on head 
  5beb6f7e ckpt 2561: lab run 43958301833 (gp + iq, head 3bf6994e): ALL 26 gesture checks OK (A: p
  c905a3c3 ckpt 2560: JVM suites (core, app, geckoapp) green on the ANS60 head; lab gp + iq dispat
  f0dbcacd ckpt 2559: lab scenes for ANS60 written (not yet run): soak.py gp_scene = A: plain side
  d8aa91f1 ckpt 2558: ANS60d CODED (the emulator cannot show speed, only that nothing broke): Play
  c8fca4df ckpt 2557: ANS60c CODED (lab proof pending): quality list in the player inside the page
  756afc69 ckpt 2556: ANS60a+b CODED (lab proof pending): core PlayerGestures has NO long press an
```
