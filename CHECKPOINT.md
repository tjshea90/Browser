# CHECKPOINT 1373 — read me first, then TASKS.md

**Written:** 2026-11-13T01:44:50Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `3415211` (this checkpoint is the commit after it)

## Just done
AD1b-1 download manager DONE in core + UI wiring: Start now / Move up / Move down (Engine.startNow/move, prio column, forced limit 3), Repr-Digest verification at completion (+wipe on mismatch), read coalescing; EngineTest +8 (52), ConnectProxyTest counter flake fixed; app+geckoapp compile green; slow-connection recycle and T0 raw-socket server deliberately not built (reasons in TASKS AA3-5). Full suites not yet rerun

## Do this next
Rerun full gates (core+app tests, web tests) in background; then AD1b-2 mp4 always: read TASKS X19 + core/media (Mp4Mux, TsToMp4, StreamEngine, Avc.kt), plan H.265 hvcC + AC-3/E-AC-3/MP3 + progressive finish + skipped ad runs, generate test vectors with ffmpeg (/usr/bin/ffmpeg exists) and validate outputs with ffprobe/ffmpeg decode

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  7b80ff7 ckpt 1365: AD1a-1 DONE: link warm-up triggers (finger down + top three search results) i
  ac0f553 ckpt 1353: Owner message 17 written into TASKS as AD1 (AD1a review rows, AD1b earlier AA
  16fc889 ckpt 1350: alpha25 SHIPPED and RECORDED: kestrel-g-v0.3.0-alpha25 (gecko code 124), run 
  62b643d ckpt 1348: alpha25 (code 124) gated + pushed (ckpt 1347) and DISPATCHED: android.yml run
  e95b292 ckpt 1347: gated kestrel-g-v0.3.0-alpha25 (gecko code 124) and pushed it: handoff self-t
  bede630 ckpt 1346: EX-10 PROVEN on the emulator (Tap to load box replaced the third-party frame;
  d7c8bb0 ckpt 1341: EX-1 PROVEN on the emulator (WifiService acquireWifiLock lockMode=3 at the st
  ae14f0d ckpt 1334: :style() restyling rules in core (697 of 770 real rules accepted; CosmeticSty
  217004a ckpt 1327: EX-2 PROVEN on the emulator (Gecko's own nsHttp log + host TLS listener: old 
  59c0013 ckpt 1322: Data saver choice re-applies the engine prefs (the look-up-links lab switch f
```

(7 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
