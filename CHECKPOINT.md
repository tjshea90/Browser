# CHECKPOINT 2458 — read me first, then TASKS.md

**Written:** 2026-11-16T01:13:32Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `bb6516b2` (this checkpoint is the commit after it)

## Just done
alpha55 RELEASED and recorded: Release kestrel-g-v0.3.0-alpha55 (run 43784082722 green; APK 94.8 MB signed; BUILDLOG line written); owner message 49 gets its FINAL answer in plain words (new full-screen controls: tap toggle, double tap seek, buttons, seek bar, LIVE/length rule, no stretch, page's own bar put away incl. Yahoo's player shell, Hide mode room, rotation-proof; found and fixed on the real engine: paused flag overwritten by the page address, stale-report race, transient nav bar; independent review of 12 findings fixed; web tests 479, Kotlin suites green, kp lab 22/22 twice, Yahoo live probed); compat emulator run dispatched (lab_suite=compat 120 min, ends ~03:30Z)

## Do this next
WAIT for the owner's tablet report on alpha55 (Diagnostics lines Full-screen video controls, Turning under a video, Full-screen page size, CPU by process) and answer it; read the compat run when it ends (artifacts emulator-lab-text / emulator-lab-shots, check health-lines.txt for 'java heap limit' first) into research/COMPAT_RUN1.md + TASKS rows LAB2/PD1; open rows: VP10 (Yahoo shell: re-confirm in Firefox via lab suite yh when the live player opens), VP12 (caption/quality buttons in hidden chrome), ROT2e (slow-turn scene does not discriminate), ANS48, SOAK1d; heartbeat trig_01Y8CFHKVb6hmQmEEM78p6gv stays (next fire 02:07Z) until the compat run is read

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  90d4d6e8 ckpt 2457: gated kestrel-g-v0.3.0-alpha55 (gecko code 156) and pushed it: handoff self-
  6d36261e ckpt 2456: independent review of the alpha55 code (agent, 12 findings) ALL addressed wi
  1bf5e48d ckpt 2455: lab fsy run 43778698760: kp scene 22/22 ok again, sound ok, Yahoo live playe
  8eab718d ckpt 2454: kp lab run 5 (43778006356, 512 MB heap boot): 22 of 22 checks GREEN on the r
  b74a874d ckpt 2453: kp lab run 3 (43776782679, good boot 512 MB heap): 18 of 21 checks pass on t
  4b0bd9aa ckpt 2452: kp lab run 2 (43775890786) read: it says NOTHING about the controls: the emu
  05a26786 ckpt 2451: VP1 lab scene kp fixed after the first real-engine run (soak.py kp_scene: pa
  a7ae2f8b ckpt 2450: VP1 lab scene kp written (soak.py kp_scene with exact multi-taps via the tou
  ca648b5d ckpt 2449: VP1 first cut compiles and is tested: player.js (state reports, tap shield, 
  df745dca ckpt 2448: gated kestrel-g-v0.3.0-alpha54 (gecko code 155) and pushed it: handoff self-
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
