# CHECKPOINT 843 — read me first, then TASKS.md

**Written:** 2026-11-12T04:54:09Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `0e8014b` (this checkpoint is the commit after it)

## Just done
post-dispatch hardening (NOT in alpha17): TsToMp4.peekFormatChange (unmarked splice by new SPS size/profile -> implicit run, dropped unless main format again) + test; stress corpus tools/mux-corpus.sh + CorpusTest (14 H.264/AAC variants decode clean with equal frame counts, HEVC/AC-3/MP3/MPEG-2 refused with reasons); research/MP4_MUX.md sections 8-9; TASKS X19 follow-ups.

## Do this next
alpha17 run 43157262742 is building: when 'git ls-remote --tags origin kestrel-g-v0.3.0-alpha17' shows the tag (background watcher btkoqj0um prints it) -> mcp__github__get_release_by_tag, then bash tools/record-release.sh kestrel-g-v0.3.0-alpha17 '<note from ship.sh>' and answer the owner with the Release link. Next work after that: X19a Make MP4 for old .ts rows, X19f fMP4 progressive finish, X3 rest (M6 digest paste, M8 priorities, M9, M10, T0), polish P5, U1c backup/restore. Heartbeat trig_01LLoVKsjuTUaDn61UGP1pBN.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md
     M research/MP4_MUX.md

## Last ten checkpoints
```
  376dc8d ckpt 837: alpha17 DISPATCHED: android.yml run 43157262742 (run_number 60) on ccr-a57c163
  2726199 ckpt 836: gated kestrel-g-v0.3.0-alpha17 (gecko code 116) and pushed it: handoff self-te
  7f4771d ckpt 835: pre-ship: alpha17: the download button is now an icon in the bottom bar (gray 
  791bba1 ckpt 833: X15 + X18 DONE and emulator-proven (alpha-debug x86_64, Android 10): dock popu
  4f4235e ckpt 826: X18b DONE: StreamEngine writes TS HLS straight into an MP4 (decideMux probes s
  70b4d0b ckpt 804: X18a DONE (core, JVM-tested, ffmpeg-verified): Avc.kt (SPS/NAL), TsDemux.kt (s
  1b3baa2 ckpt 756: X17 dock download icon VERIFIED on emulator after a real redraw (home+relaunch
  51942ec ckpt 744: owner message 11 written into TASKS as X14-X18 in their words (long-press down
  e8ca3f6 ckpt 741: M8-lite: Settings > Downloads (speed limit 2 MB/s..250 KB/s applied at once, d
  06945e0 ckpt 733: Owner told (alpha16 reply: Release link, repeated message 10 answered part by 
```

(5 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
