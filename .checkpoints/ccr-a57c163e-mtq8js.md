# CHECKPOINT 833 — read me first, then TASKS.md

**Written:** 2026-11-12T04:42:40Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `3210d67` (this checkpoint is the commit after it)

## Just done
X15 + X18 DONE and emulator-proven (alpha-debug x86_64, Android 10): dock popup row says MP4; download through the dock icon wrote 'Kestrel HLS test.mp4' from the first byte, no .ts, ffmpeg clean, Diagnostics 'mp4 check ok video=1 audio=1'; app force-stopped mid-download resumed by itself, identical sample data. Docs: docs/ARCHITECTURE.md section, research/MP4_MUX.md, TASKS X15/X18 ticked, X19 follow-ups added. Owner remark logged (remux only, no re-encode = the design). Also this step: note under the Downloads row, Remuxer.verify, decline reasons, estimate = avg complete segment * n.

## Do this next
(1) X19b: size for a bare HLS media playlist in the popup (VideoFlow.loadStream: probe first segment length, DownloadController has a size probe used for progressive files) - small. (2) Final checks before alpha17: cd web-tests && npm test; ./gradlew :core:test :app:testDebugUnitTest :geckoapp:compileDebugKotlin :app:lintDebug; (3) bump versionCode 116 / 0.3.0-alpha17 (geckoapp/build.gradle.kts, check how ship.sh reads it), write BUILDLOG/RELEASE note text (X14 long-press menu, X16 spoof guard, X17 dock download icon + popup, X18 HLS straight to MP4 + resume, M8-lite Settings>Downloads), bash ship.sh gecko note, dispatch android.yml (mcp__github__actions_run_trigger, ref ccr-a57c163e-mtq8js, app gecko, full_build true), wait with an until loop, bash tools/record-release.sh <tag> only after green, give the owner the Release link on its own line + answer message 11 and the 'remux only' remark. (4) Owner still owes: Proton Page loads numbers, scheduled CI list-build workflow yes/no. Heartbeat trig_01LLoVKsjuTUaDn61UGP1pBN.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  4f4235e ckpt 826: X18b DONE: StreamEngine writes TS HLS straight into an MP4 (decideMux probes s
  70b4d0b ckpt 804: X18a DONE (core, JVM-tested, ffmpeg-verified): Avc.kt (SPS/NAL), TsDemux.kt (s
  1b3baa2 ckpt 756: X17 dock download icon VERIFIED on emulator after a real redraw (home+relaunch
  51942ec ckpt 744: owner message 11 written into TASKS as X14-X18 in their words (long-press down
  e8ca3f6 ckpt 741: M8-lite: Settings > Downloads (speed limit 2 MB/s..250 KB/s applied at once, d
  06945e0 ckpt 733: Owner told (alpha16 reply: Release link, repeated message 10 answered part by 
  b87f19d ckpt 732: alpha16 SHIPPED + RECORDED (run 43143728373 green; the two earlier runs failed
  dc57dde ckpt 729: stream engine: a dropped job (merged-away sound of a pair, cancelled, cleared)
  9226e37 ckpt 726: TASKS X4/X5/X12 statuses with evidence; HLS auto-resume test; lintRelease clea
  d41caca ckpt 724: M7 content sanity (web page where a file/video was expected -> BAD_CONTENT, HL
```

(6 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
