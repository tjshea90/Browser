# CHECKPOINT 2611 — read me first, then TASKS.md

**Written:** 2026-11-17T08:23:25Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a10e07b7-6zxjmi` · **builds on:** `1bec1b63` (this checkpoint is the commit after it)

## Just done
run 9 read: degraded run (emulator restarted, no network, Google A/B all 60 s timeouts both modes -> no Google rule); start-up profile = software GL render sync + 166 ms runtime create -> no start-up change (evidence rule). JVM + web tests green (handover flake passes alone). Bumped alpha63 code 164 + note.

## Do this next
bash ship.sh gecko "$(cat docs/handoff/ALPHA63_RELEASE_NOTE.txt)"; ONE run_workflow app=gecko full_build=true ref=ccr-a10e07b7-6zxjmi; verify tag+jobs+release; record-release.sh with identical note

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  ede3c889 ckpt 2610: OWNER RESCHEDULE (usage running out, 2026-11-17 04:58Z): ALL work moved to O
  8bf31a36 ckpt 2609: LAB4b: decisions recorded: ANS60e NOT done (would delay every navigation's g
  b68aca47 ckpt 2608: LAB4a/b: run 8 (44017705030, instrumented app, fixed lab order, fast link) i
  edacce50 ckpt 2607: LAB4a: run 7 (44010606968, instrumented app, hotspot, OLD lab order) CANCELL
  7f6901e6 ckpt 2606: LAB4a: run 6 (44010165696) was CANCELLED after 53 min in the lab step; its a
  c2e8ae15 ckpt 2605: LAB4b: ANS60f DONE in code (NOT testable on the Android 10 lab, compile-chec
  4ada6834 ckpt 2604: LAB4b: instrumentation step verified: JVM suites :core:test :app:testDebugUn
  d7f8b405 ckpt 2603: LAB3/LAB4a: run 5 (44008297246) READ: all lab fixes held (Features + Securit
  4dc88c0d ckpt 2602: LAB4b step 1 (measure before fixing, evidence rule): app instrumentation onl
  7a95ed47 ckpt 2601: LAB4a: daily.py got two steps: idle-cpu-60-seconds (top -H + dumpsys cpuinfo
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
