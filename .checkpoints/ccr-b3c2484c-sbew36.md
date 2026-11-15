# CHECKPOINT 2435 — read me first, then TASKS.md

**Written:** 2026-11-15T21:32:11Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `cb405399` (this checkpoint is the commit after it)

## Just done
alpha52 shipped to GitHub (gates green, pushed): release run 43763812046 (android.yml app=gecko), lab fsy run 43763822027 (FB4 proof, old driver) and a second fsy lab run queued behind it (new driver: picture cap 120, Yahoo own full-screen button tries); HANDOFF.md alpha50-52 map + research/FULLSCREEN_BARS.md addendum C written

## Do this next
1) when the tag kestrel-g-v0.3.0-alpha52 exists (background watcher) check actions_list list_workflow_jobs 43763812046 all green, mcp__github__get_release_by_tag shows the APK, then bash tools/record-release.sh kestrel-g-v0.3.0-alpha52 "$(cat scratchpad note52.one text)" (identical note: the one in the dispatch inputs), 2) read the lab fsy artifacts (mcp__github__actions_list list_workflow_run_artifacts 43763822027 -> actions_get download_workflow_run_artifact -> python3 -I tools/emu/fetch_artifact.py URL emptydir): look at the first state of the first variant (red strip?), the fullscreen viewport Health lines, diagnostics line Full-screen page size; 3) once the second fsy run has STARTED dispatch app=lab lab_suite=compat lab_minutes=120 lab_net=full (a pending run in the same group is replaced by a newer pending one, so wait), fill research/COMPAT_RUN1.md; 4) answer owner message 47 in plain words with the alpha52 Release link on its own line, what was NOT run on the tablet, open items; heartbeat trig_01Y8CFHKVb6hmQmEEM78p6gv stays until all of this is done

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  49db2736 ckpt 2434: gated kestrel-g-v0.3.0-alpha52 (gecko code 153) and pushed it: handoff self-
  ab5859df ckpt 2433: pre-ship: alpha52: full-screen video, third step. Found with the emulator la
  80198e75 ckpt 2432: FB4 coded + all green: turn.js vpt report, background relay, ShieldsBridge v
  9780732e ckpt 2431: lab run 43760546128 (fsy, first clean run, alpha51 code) read: Keep mode put
  e9464a7e ckpt 2430: alpha51 gated and queued (FB1 bars kept by default + Settings row, FB2/FB3 t
  38a628d2 ckpt 2429: gated kestrel-g-v0.3.0-alpha51 (gecko code 152) and pushed it: handoff self-
  7c27ad91 ckpt 2428: pre-ship: alpha51: full-screen video, second attempt after alpha50 changed n
  88fb79ef ckpt 2427: FB1 + FB2 + FB3 coded: system bars kept by default (Prefs.fullscreenBars, Se
  691a247e ckpt 2426: owner message 47 written into TASKS.md (alpha50 changed nothing on the table
  d941af5c ckpt 2425: alpha50 shipped and recorded (Release kestrel-g-v0.3.0-alpha50, run 43758859
```

(3 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
