# CHECKPOINT 301 — read me first, then TASKS.md

**Written:** 2026-11-11T07:15:54Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `a6b3158` (this checkpoint is the commit after it)

## Just done
N6 step 2 compiles: Android download shell (SqliteStore, MediaStoreSinks, UrlConnectionFetcher, DownloadController, DownloadService dataSync FGS + Notifier, ui/DownloadsScreen), GeckoFetcher via GeckoWebExecutor (bounded 4 MiB Range windows, falls back to URLConnection), Gecko activity wired: startDownload -> DownloadController.enqueue, Menu > Downloads -> DownloadsScreen, notification tap opens it, onExternalResponse closes the body; manifest perms+service; 266 unit tests green; NOT yet run on the emulator

## Do this next
Emulator test (emulator rebooting after worker restart; emulator-5554, helper source scratchpad/emu.sh; Range server: python3 scratchpad/rangeserver.py scratchpad/dl 8001 700000 already running, files big.bin 60 MB + medium.bin 9 MB, log scratchpad/server.log): install debug APK, open http://10.0.2.2:8001/big.bin, Download, check Menu > Downloads progress/speed/connections, pause/resume, home-button + return, force-stop + resume, result in /sdcard/Download with sha256 match; then fix, ship alpha5 (bump geckoapp code 104 / 0.2.0-alpha5)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  c7112dc ckpt 294: N6 step 1 done: pure-Kotlin download engine in core/ (app.kestrel.core.downloa
  ce4c6e7 ckpt 281: handoff saved via context-handoff plugin (.handoff/handoff.md + json + archive
  8a1eac9 ckpt 278: alpha4 RELEASED + RECORDED: kestrel-g-v0.2.0-alpha4 (gecko code 103), run 4297
  c5df399 ckpt 275: gated kestrel-g-v0.2.0-alpha4 (gecko code 103) and pushed it: handoff self-tes
  276ef61 ckpt 266: N1 updater works end to end on the emulator (local feed): auto/manual check vi
  9470db6 ckpt 259: N2-N5 verified on emulator (Android 10 x86_64, 600x973): black Home + chamfere
  f6cb00e ckpt 252: N4/N5 code compiles: TabGrid (card grid with page previews, Tabs|Private segme
  bf260ea ckpt 246: N2/N3 part 1 compiles: CERE dark palette+angular icon set, ChamferDrawable, Bo
  9458fe0 ckpt 227: Owner request 3 recorded in TASKS.md (N1-N7); owner likes alpha3; UI-1 answere
  04080cd ckpt 224: Alpha3 (kestrel-g-v0.2.0-alpha3, run 42961586675) built, signed, released and 
```

(6 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
