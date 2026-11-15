# CHECKPOINT 2424 — read me first, then TASKS.md

**Written:** 2026-11-15T20:42:52Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `0011bb85` (this checkpoint is the commit after it)

## Just done
lab run 43753283814 (fsy) read: it says NOTHING about the full-screen code: the first emulator boot gave apps a 16 MB java heap limit (growth limit 16777216 in 177 log lines; later boot and all earlier runs 512 MB), the app died of OutOfMemoryError in ShieldsBridge.imageChunk / main in a crash loop (13 process starts), no page action ever worked; hardened: ShieldsBridge lane catches OutOfMemoryError (extension asks again), Health 'start heap' line records the heap limit and memory class, workflow prints dalvik.vm.heap props after boot

## Do this next
NEXT: (1) alpha50 release run (dispatched 20:35Z): when the tag kestrel-g-v0.3.0-alpha50 exists and the run is green: bash tools/record-release.sh kestrel-g-v0.3.0-alpha50 "$(cat scratchpad/note50.one)" (identical note); (2) re-dispatch app=lab lab_suite=fsy lab_minutes=10 and read it (if the 16 MB heap repeats: set dalvik.vm.heapgrowthlimit in the workflow before the run); (3) then compat with lab_minutes=120; (4) answer owner

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M .github/workflows/emu-lab.yml

## Last ten checkpoints
```
  6689210f ckpt 2423: gated kestrel-g-v0.3.0-alpha50 (gecko code 151) and pushed it: handoff self-
  ca8bc3fb ckpt 2422: pre-ship: alpha50: fixes from an independent check of alpha49 (everything el
  342c62e0 ckpt 2421: compat run 1 (43747605584) read: only 4 sites measured, the emulated phone's
  b0aa0482 ckpt 2420: lab driver: Yahoo Full-button tap x 556 (inside the button for film widths 5
  a88bd7cb ckpt 2419: alpha49 review (a1b60178ab815123f) fixed: RA1 media state forgotten with the
  2de3f20b ckpt 2418: alpha49 shipped and recorded: Release kestrel-g-v0.3.0-alpha49 (run 43751318
  7e7d3319 ckpt 2417: lab scene states fixed (the app turns a wide video sideways by itself at ful
  0a81c2b8 ckpt 2416: gated kestrel-g-v0.3.0-alpha49 (gecko code 150) and pushed it: handoff self-
  afdbb7c6 ckpt 2415: pre-ship: alpha49: full-screen video fixes after your three screenshots. (1)
  7c987e9f ckpt 2414: code for owner message 46 + second review: FSB1 (applyImmersive without LAYO
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
