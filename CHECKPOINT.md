# CHECKPOINT 826 — read me first, then TASKS.md

**Written:** 2026-11-12T04:16:43Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `83163cd` (this checkpoint is the commit after it)

## Just done
X18b DONE: StreamEngine writes TS HLS straight into an MP4 (decideMux probes segment 0, name .mp4 from the first byte, TsToMp4 per job, journal mux.jnl in the staging dir checkpointed under the drain lock + at settle, finish() = moov only, restartRaw fallback on MuxBroken/no picture, note on the row, StreamRec.mux/note persisted, Sink.truncate, Config.directMp4, estimate = avg complete segment * n). 8 new engine tests (StreamEngineMp4Test: straight-to-mp4, AES-128, ad run, non-H264 stays .ts with reason, switch off, writer failure restarts raw, pause+kill+resume identical, journal behind record) + full :core:test :app:testDebugUnitTest :geckoapp:compileDebugKotlin green.

## Do this next
X18c Android + X18d emulator proof: (1) show DownloadView.note under the row in the Downloads screen (app/.../ui, grep DownloadsView/row builder) and in the notification if cheap; (2) Diagnostics 'mp4 check' = after finish MediaExtractor read-back (tracks, duration) logged via Health.log, in Remuxer-adjacent class or DownloadController; (3) 'Make MP4' action for existing .ts rows (Remuxer.convert on a joined .ts: needs StreamFinisher.Joined from a file; room check relaxed to 0.6*size+32MB) ; (4) build debug x86_64 (gradle :geckoapp:assembleDebug -PgeckoAbi=x86_64, see scratchpad build-g*.log), install, serve site/hls/720 via 10.0.2.2:8000 and download through the dock icon: expect file .mp4 from first byte, pull with adb, ffprobe/ffmpeg -f null clean; kill -9 the app mid-download and resume. Then TASKS ticks X15/X18, ARCHITECTURE.md section, alpha17 (versionCode 116) batching X14-X18 + M8-lite. Heartbeat trig_01LLoVKsjuTUaDn61UGP1pBN.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  70b4d0b ckpt 804: X18a DONE (core, JVM-tested, ffmpeg-verified): Avc.kt (SPS/NAL), TsDemux.kt (s
  1b3baa2 ckpt 756: X17 dock download icon VERIFIED on emulator after a real redraw (home+relaunch
  51942ec ckpt 744: owner message 11 written into TASKS as X14-X18 in their words (long-press down
  e8ca3f6 ckpt 741: M8-lite: Settings > Downloads (speed limit 2 MB/s..250 KB/s applied at once, d
  06945e0 ckpt 733: Owner told (alpha16 reply: Release link, repeated message 10 answered part by 
  b87f19d ckpt 732: alpha16 SHIPPED + RECORDED (run 43143728373 green; the two earlier runs failed
  dc57dde ckpt 729: stream engine: a dropped job (merged-away sound of a pair, cancelled, cleared)
  9226e37 ckpt 726: TASKS X4/X5/X12 statuses with evidence; HLS auto-resume test; lintRelease clea
  d41caca ckpt 724: M7 content sanity (web page where a file/video was expected -> BAD_CONTENT, HL
  61e1894 ckpt 721: M1b validator-less resume: Sink.readable/pread (Os.pread on both Android sinks
```

(21 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
