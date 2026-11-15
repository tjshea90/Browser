# CHECKPOINT 2432 — read me first, then TASKS.md

**Written:** 2026-11-15T21:26:05Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `9141496d` (this checkpoint is the commit after it)

## Just done
FB4 coded + all green: turn.js vpt report, background relay, ShieldsBridge viewport param, BrowserActivity.onPageViewport/resyncEngineSize/viewportLine, OwnerRulesTest guard, soak.py viewport lines, research/FULLSCREEN_BARS.md addendum C; Kotlin suites EXIT 0, npm test 453 pass

## Do this next
bump geckoapp/build.gradle.kts to versionCode 153 / 0.3.0-alpha52, write note file in scratchpad, bash ship.sh gecko "$(cat note)", dispatch android.yml app=gecko full_build=true note=SAME, in parallel dispatch lab fsy (app=lab lab_suite=fsy lab_minutes=10 lab_net=full) and read the fullscreen viewport lines + first-state red strip; record-release only when the run is green

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  9780732e ckpt 2431: lab run 43760546128 (fsy, first clean run, alpha51 code) read: Keep mode put
  e9464a7e ckpt 2430: alpha51 gated and queued (FB1 bars kept by default + Settings row, FB2/FB3 t
  38a628d2 ckpt 2429: gated kestrel-g-v0.3.0-alpha51 (gecko code 152) and pushed it: handoff self-
  7c27ad91 ckpt 2428: pre-ship: alpha51: full-screen video, second attempt after alpha50 changed n
  88fb79ef ckpt 2427: FB1 + FB2 + FB3 coded: system bars kept by default (Prefs.fullscreenBars, Se
  691a247e ckpt 2426: owner message 47 written into TASKS.md (alpha50 changed nothing on the table
  d941af5c ckpt 2425: alpha50 shipped and recorded (Release kestrel-g-v0.3.0-alpha50, run 43758859
  4415fbaf ckpt 2424: lab run 43753283814 (fsy) read: it says NOTHING about the full-screen code: 
  6689210f ckpt 2423: gated kestrel-g-v0.3.0-alpha50 (gecko code 151) and pushed it: handoff self-
  ca8bc3fb ckpt 2422: pre-ship: alpha50: fixes from an independent check of alpha49 (everything el
```

(5 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
