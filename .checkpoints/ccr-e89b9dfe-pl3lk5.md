# CHECKPOINT 2610 — read me first, then TASKS.md

**Written:** 2026-11-17T04:59:19Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-e89b9dfe-pl3lk5` · **builds on:** `8bf31a36` (this checkpoint is the commit after it)

## Just done
OWNER RESCHEDULE (usage running out, 2026-11-17 04:58Z): ALL work moved to ONE wake-up at 2026-11-17T08:59Z = send_later trig_01WsogxX6eh1EXEzCLNJPDn1 (the run-9 check trig_014UcriabRF9we3X6nfKcxDt and the 2-hourly heartbeat trig_01K9rvGwTaZNy2NbUQeBeWMg were DELETED so nothing runs sooner). State: everything committed and pushed (head = this ckpt). Lab run 9 (daily, fast link, instrumented app v2: lab-only start-up profiler 'ui prof' + Google Shields on/off A/B) was dispatched ~04:57Z, do NOT dispatch it again, its artifacts live 7 days. Baseline = run 8 (44017705030) in research/lab-baseline.json + docs/handoff/LABREPORT_0.3.0-alpha62.md. DONE in the app so far: instrumentation (watchdog slow-at stacks, engine burst timing, read-back counter), ANS60f (API 33+ back callback in NativePlayerActivity); REFUSED by data/cost: read-back move (1-10 ms), burst slicing (56 ms), ANS60e, candidate 1. NOT shipped: no APK release yet

## Do this next
At 08:59Z: (1) find run 9 (list_workflow_runs perPage 1 branch + event workflow_dispatch) and read its emulator-lab-text: grep 'ui prof' health-lines.txt, shields_ab_google in events.jsonl, buildreport --notes research/LAB3_IMPROVEMENTS.md; (2) change the app ONLY where the data shows a cause (start-up block: name the biggest chunk first; a Google rule only if Shields ON is >30 % slower than OFF); (3) ./gradlew -q :core:test :app:testDebugUnitTest :geckoapp:testDebugUnitTest and cd web-tests && npm test (579 expected); (4) version bump 0.3.0-alpha63 code 164, tools/sync-ext-version.sh, docs/handoff/ALPHA63_RELEASE_NOTE.txt, bash ship.sh gecko with that note, ONE run_workflow app=gecko full_build=true, verify tag + jobs + release, tools/record-release.sh <tag> with the IDENTICAL note; (5) an after-run (lab_suite=daily) vs research/lab-baseline.json; (6) answer the owner in plain words incl. the Neon idle-cost choice; tick LAB3a-g and LAB4a-d in TASKS.md with evidence

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  8bf31a36 ckpt 2609: LAB4b: decisions recorded: ANS60e NOT done (would delay every navigation's g
  b68aca47 ckpt 2608: LAB4a/b: run 8 (44017705030, instrumented app, fixed lab order, fast link) i
  edacce50 ckpt 2607: LAB4a: run 7 (44010606968, instrumented app, hotspot, OLD lab order) CANCELL
  7f6901e6 ckpt 2606: LAB4a: run 6 (44010165696) was CANCELLED after 53 min in the lab step; its a
  c2e8ae15 ckpt 2605: LAB4b: ANS60f DONE in code (NOT testable on the Android 10 lab, compile-chec
  4ada6834 ckpt 2604: LAB4b: instrumentation step verified: JVM suites :core:test :app:testDebugUn
  d7f8b405 ckpt 2603: LAB3/LAB4a: run 5 (44008297246) READ: all lab fixes held (Features + Securit
  4dc88c0d ckpt 2602: LAB4b step 1 (measure before fixing, evidence rule): app instrumentation onl
  7a95ed47 ckpt 2601: LAB4a: daily.py got two steps: idle-cpu-60-seconds (top -H + dumpsys cpuinfo
  0f3bfce1 ckpt 2600: LAB4 (owner message 63: do the other runs that should be done, then implemen
```
