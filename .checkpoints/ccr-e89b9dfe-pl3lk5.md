# CHECKPOINT 2609 — read me first, then TASKS.md

**Written:** 2026-11-17T04:58:05Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-e89b9dfe-pl3lk5` · **builds on:** `00bb1886` (this checkpoint is the commit after it)

## Just done
LAB4b: decisions recorded: ANS60e NOT done (would delay every navigation's guard answer up to 250 ms to cure a once-after-a-settings-change effect), ANS60f DONE (API 33+ back callback in NativePlayerActivity; Android 10 lab cannot test it); readBack log quieted (count all, log only >= 15 ms). Run 9 (instrumented v2: start-up profiler + Google Shields A/B) running, wake-up trig_014UcriabRF9we3X6nfKcxDt 05:19Z

## Do this next
At the wake-up read run 9 ('ui prof' lines, shields_ab_google), then decide the last app changes with evidence; then full tests (JVM + web), version bump geckoapp/build.gradle.kts + tools/sync-ext-version.sh, release note docs/handoff/ALPHA63_RELEASE_NOTE.txt, ship.sh gecko, ONE dispatch full_build=true, verify, record-release.sh, after-run vs lab-baseline

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  b68aca47 ckpt 2608: LAB4a/b: run 8 (44017705030, instrumented app, fixed lab order, fast link) i
  edacce50 ckpt 2607: LAB4a: run 7 (44010606968, instrumented app, hotspot, OLD lab order) CANCELL
  7f6901e6 ckpt 2606: LAB4a: run 6 (44010165696) was CANCELLED after 53 min in the lab step; its a
  c2e8ae15 ckpt 2605: LAB4b: ANS60f DONE in code (NOT testable on the Android 10 lab, compile-chec
  4ada6834 ckpt 2604: LAB4b: instrumentation step verified: JVM suites :core:test :app:testDebugUn
  d7f8b405 ckpt 2603: LAB3/LAB4a: run 5 (44008297246) READ: all lab fixes held (Features + Securit
  4dc88c0d ckpt 2602: LAB4b step 1 (measure before fixing, evidence rule): app instrumentation onl
  7a95ed47 ckpt 2601: LAB4a: daily.py got two steps: idle-cpu-60-seconds (top -H + dumpsys cpuinfo
  0f3bfce1 ckpt 2600: LAB4 (owner message 63: do the other runs that should be done, then implemen
  dd595cbe ckpt 2599: LAB3e: lab run 4 (44005894797) READ: Security GREEN 16 pass / 0 fail, camera
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
