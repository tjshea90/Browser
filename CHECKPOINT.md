# CHECKPOINT 2494 — read me first, then TASKS.md

**Written:** 2026-11-16T07:01:11Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `efc4f774` (this checkpoint is the commit after it)

## Just done
alpha57 RELEASE DISPATCHED (ONE time): android.yml run 43820755532 (app=gecko, full_build=true, head efc4f774); the exact note text is saved in docs/handoff/ALPHA57_RELEASE_NOTE.txt (the CHECKPOINT text of ckpt 2493 carries an OLDER note: use the file). DO NOT DISPATCH AGAIN.

## Do this next
wait for run 43820755532: poll mcp__github__actions_list list_workflow_jobs resource_id 43820755532 (workflow_jobs_filter latest, perPage 10) until every job is completed+success (test-jvm, test-js, build-gecko; lint runs in the build); tag kestrel-g-v0.3.0-alpha57 appears (git ls-remote --tags origin | grep alpha57); mcp__github__get_release_by_tag tjshea90 Browser kestrel-g-v0.3.0-alpha57 must show the APK; THEN bash tools/record-release.sh kestrel-g-v0.3.0-alpha57 "$(cat docs/handoff/ALPHA57_RELEASE_NOTE.txt)" (identical note); then answer the owner (message 53 point by point + open 52 follow-ups) with the Release link https://github.com/tjshea90/Browser/releases/tag/kestrel-g-v0.3.0-alpha57 on its own line; if the run FAILS: read the failed job (get_job_logs only after the job ended), fix on the branch, bump nothing unless it published, and make ONE fresh dispatch (never re-run failed jobs: read-only token, HTTP 403); heartbeat trig_01Y8CFHKVb6hmQmEEM78p6gv

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    ?? docs/handoff/ALPHA57_RELEASE_NOTE.txt

## Last ten checkpoints
```
  efc4f774 ckpt 2493: gated kestrel-g-v0.3.0-alpha57 (gecko code 158) and pushed it: handoff self-
  411b667f ckpt 2492: lab run 3 dispatched: 43818471174 (suite vp, head c002903d = ckpt 2491 = the
  c002903d ckpt 2491: ALPHA57 CANDIDATE: version 158 / 0.3.0-alpha57 (extension 1.0.158); lab run 
  152027b8 ckpt 2490: lab run 2 dispatched: 43816308648 (android.yml app=lab lab_suite=vp, head 99
  99c99a77 ckpt 2489: pre-dispatch checks green for lab run 2: web suite 542/542, gradle :core :ap
  a1776ee8 ckpt 2488: lab run 43812820865 read (research/ANS53_LAB_VP1.md): kp 27/27 + cp + hls + 
  55fe4f13 ckpt 2487: lab extras for the NEXT dispatch (the in-flight run 43812820865 uses ckpt 24
  a7dfeb23 ckpt 2486: lab run 43812820865 dispatched (android.yml app=lab lab_suite=vp, ref ccr-b3
  51374ebb ckpt 2485: ANS53d.2 coded + verified: Quality button in the page's full-screen controls
  50a021f2 ckpt 2484: ANS53 wiring verified: player web tests fixed (27/27), full web suite 541/54
```
