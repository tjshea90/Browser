# CHECKPOINT 868 — read me first, then TASKS.md

**Written:** 2026-11-12T05:58:15Z · **tests:** 1 RED: test_handoff.sh (0 green)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `879b10f` (this checkpoint is the commit after it)

## Just done
pre-ship: alpha18: the updater now always tries to install (it no longer stops on a free-storage estimate; Android is the only judge), and when Android does refuse it explains why and offers Try again, Storage settings or Install the other way (saves the update to Downloads and opens Android's uninstall prompt). New Make MP4 button on finished .ts downloads. Ad inserts nobody marked in a stream are spotted and left out. The download icon is found when the same page is open in two tabs.

## Do this next
ship.sh gates and releases this (gecko)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M geckoapp/build.gradle.kts

## Last ten checkpoints
```
  879b10f ckpt 867: Y1 DONE (updater always tries: no storage gate in download()/install(), floor2
  ceaa63c ckpt 860: owner message 12 written into TASKS (Y1 updater must always try, Y2 phone offl
  eadd5fc ckpt 856: post-alpha17 in the tree (NOT shipped): Make MP4 for finished .ts rows (Stream
  0948a0d ckpt 843: post-dispatch hardening (NOT in alpha17): TsToMp4.peekFormatChange (unmarked s
  376dc8d ckpt 837: alpha17 DISPATCHED: android.yml run 43157262742 (run_number 60) on ccr-a57c163
  2726199 ckpt 836: gated kestrel-g-v0.3.0-alpha17 (gecko code 116) and pushed it: handoff self-te
  7f4771d ckpt 835: pre-ship: alpha17: the download button is now an icon in the bottom bar (gray 
  791bba1 ckpt 833: X15 + X18 DONE and emulator-proven (alpha-debug x86_64, Android 10): dock popu
  4f4235e ckpt 826: X18b DONE: StreamEngine writes TS HLS straight into an MP4 (decideMux probes s
  70b4d0b ckpt 804: X18a DONE (core, JVM-tested, ffmpeg-verified): Avc.kt (SPS/NAL), TsDemux.kt (s
```
