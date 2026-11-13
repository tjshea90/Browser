# CHECKPOINT 1384 — read me first, then TASKS.md

**Written:** 2026-11-13T01:54:17Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `edab97a` (this checkpoint is the commit after it)

## Just done
AD1b-2 mp4 always DONE in core: Hevc.kt (hvc1/hvcC), AudioFrames.kt (AC-3/E-AC-3/MP3), Mp4Mux AudioCfg + setVideoHevc + journal events 7/8, TsToMp4 H.265 path + sound kinds + probe, 8 fixtures from ffmpeg, 6 TsToMp4 tests + engine test for skipping the fetch of a left-out ad run (StreamEngine.skipRestOfRun); ffmpeg decodes our output clean; two tests that faked H.265 now fake MPEG-2; research/MP4_MUX.md + TASKS AA3-4 updated; fMP4 defragmenter NOT done

## Do this next
Run the full gates in the background (core + app + geckoapp compile + web tests); then AD1b-3 UI polish: Settings as tile rows, P5 restored page vs Home while the engine starts, C4 emulator screenshots of every screen incl. the new Downloads buttons; then AD1b-4 ad blocking rest (default.kfs regeneration, procedural cosmetics) and AD1b-5 Gradle verification metadata; optional AD1a-3 Decode lab recommendation + Wi-Fi sleep test

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md
     M research/MP4_MUX.md

## Last ten checkpoints
```
  7919d0f ckpt 1373: AD1b-1 download manager DONE in core + UI wiring: Start now / Move up / Move 
  7b80ff7 ckpt 1365: AD1a-1 DONE: link warm-up triggers (finger down + top three search results) i
  ac0f553 ckpt 1353: Owner message 17 written into TASKS as AD1 (AD1a review rows, AD1b earlier AA
  16fc889 ckpt 1350: alpha25 SHIPPED and RECORDED: kestrel-g-v0.3.0-alpha25 (gecko code 124), run 
  62b643d ckpt 1348: alpha25 (code 124) gated + pushed (ckpt 1347) and DISPATCHED: android.yml run
  e95b292 ckpt 1347: gated kestrel-g-v0.3.0-alpha25 (gecko code 124) and pushed it: handoff self-t
  bede630 ckpt 1346: EX-10 PROVEN on the emulator (Tap to load box replaced the third-party frame;
  d7c8bb0 ckpt 1341: EX-1 PROVEN on the emulator (WifiService acquireWifiLock lockMode=3 at the st
  ae14f0d ckpt 1334: :style() restyling rules in core (697 of 770 real rules accepted; CosmeticSty
  217004a ckpt 1327: EX-2 PROVEN on the emulator (Gecko's own nsHttp log + host TLS listener: old 
```

(10 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
