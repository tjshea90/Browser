# CHECKPOINT 837 — read me first, then TASKS.md

**Written:** 2026-11-12T04:49:41Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `2726199` (this checkpoint is the commit after it)

## Just done
alpha17 DISPATCHED: android.yml run 43157262742 (run_number 60) on ccr-a57c163e-mtq8js at ckpt 836 (versionCode 116, tag kestrel-g-v0.3.0-alpha17). Emulator size estimate for bare HLS playlist verified ('about 6.0 MB' vs real 6.06 MB). Lint, 64 web tests, core/app suites green.

## Do this next
WAITING for run 43157262742: a background loop watches 'git ls-remote --tags origin kestrel-g-v0.3.0-alpha17' (up to 60 min); check mcp__github__actions_get get_workflow_run 43157262742 now and then (completed/success) and read the Releases page via mcp__github__get_release_by_tag kestrel-g-v0.3.0-alpha17 (APK asset + SHA-256 in the body). ONLY when green: bash tools/record-release.sh kestrel-g-v0.3.0-alpha17 '<the same note>' (note text is in ckpt 836 message / ship.sh output), then answer the owner (message 11 answers + 'remux only' remark) with the Release link on its own line. If the run is red: read mcp__github__get_job_logs (failed_only) via the signed-log web fetch trick, fix, bump nothing (the tag is not created on failure), re-dispatch. Heartbeat trig_01LLoVKsjuTUaDn61UGP1pBN.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  2726199 ckpt 836: gated kestrel-g-v0.3.0-alpha17 (gecko code 116) and pushed it: handoff self-te
  7f4771d ckpt 835: pre-ship: alpha17: the download button is now an icon in the bottom bar (gray 
  791bba1 ckpt 833: X15 + X18 DONE and emulator-proven (alpha-debug x86_64, Android 10): dock popu
  4f4235e ckpt 826: X18b DONE: StreamEngine writes TS HLS straight into an MP4 (decideMux probes s
  70b4d0b ckpt 804: X18a DONE (core, JVM-tested, ffmpeg-verified): Avc.kt (SPS/NAL), TsDemux.kt (s
  1b3baa2 ckpt 756: X17 dock download icon VERIFIED on emulator after a real redraw (home+relaunch
  51942ec ckpt 744: owner message 11 written into TASKS as X14-X18 in their words (long-press down
  e8ca3f6 ckpt 741: M8-lite: Settings > Downloads (speed limit 2 MB/s..250 KB/s applied at once, d
  06945e0 ckpt 733: Owner told (alpha16 reply: Release link, repeated message 10 answered part by 
  b87f19d ckpt 732: alpha16 SHIPPED + RECORDED (run 43143728373 green; the two earlier runs failed
```
