# CHECKPOINT 860 — read me first, then TASKS.md

**Written:** 2026-11-12T05:32:58Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `c8bae09` (this checkpoint is the commit after it)

## Just done
owner message 12 written into TASKS (Y1 updater must always try, Y2 phone offload); starting Y1 (UpdateManager.install: no storage gate)

## Do this next
Y1: UpdateManager.install() must start the PackageInstaller session whatever space() says (keep the numbers as a hint only, never block; handle INSUFFICIENT_STORAGE after the fact with the Android message + retry + 'uninstall first' hint), then Y2 design doc research/PHONE_OFFLOAD.md + answer the owner (ask: Android phone?). Heartbeat trig_01LLoVKsjuTUaDn61UGP1pBN.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  eadd5fc ckpt 856: post-alpha17 in the tree (NOT shipped): Make MP4 for finished .ts rows (Stream
  0948a0d ckpt 843: post-dispatch hardening (NOT in alpha17): TsToMp4.peekFormatChange (unmarked s
  376dc8d ckpt 837: alpha17 DISPATCHED: android.yml run 43157262742 (run_number 60) on ccr-a57c163
  2726199 ckpt 836: gated kestrel-g-v0.3.0-alpha17 (gecko code 116) and pushed it: handoff self-te
  7f4771d ckpt 835: pre-ship: alpha17: the download button is now an icon in the bottom bar (gray 
  791bba1 ckpt 833: X15 + X18 DONE and emulator-proven (alpha-debug x86_64, Android 10): dock popu
  4f4235e ckpt 826: X18b DONE: StreamEngine writes TS HLS straight into an MP4 (decideMux probes s
  70b4d0b ckpt 804: X18a DONE (core, JVM-tested, ffmpeg-verified): Avc.kt (SPS/NAL), TsDemux.kt (s
  1b3baa2 ckpt 756: X17 dock download icon VERIFIED on emulator after a real redraw (home+relaunch
  51942ec ckpt 744: owner message 11 written into TASKS as X14-X18 in their words (long-press down
```

(3 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
