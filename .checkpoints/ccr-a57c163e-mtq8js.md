# CHECKPOINT 278 — read me first, then TASKS.md

**Written:** 2026-11-11T06:49:25Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `396b94d` (this checkpoint is the commit after it)

## Just done
alpha4 RELEASED + RECORDED: kestrel-g-v0.2.0-alpha4 (gecko code 103), run 42973747344 green, Release has the APK + sha256 in the body; BUILDLOG line 396b94d. N1-N5 ticked in TASKS.md with emulator evidence (not yet on the real tablet). Heartbeat trigger trig_01HjJLZki7r2XgUqZazC4oup (every 2 h) created; delete_trigger it when N6 ships.

## Do this next
N6 download manager (claim it: bash tools/claim.sh N6): 1) read research/DOWNLOADS.md; 2) core/ pure-Kotlin engine app.kestrel.core.download (segment planner, validators ETag/If-Range, state machine, ports Fetcher/Sink/Store/Clock/NetGate) + JVM tests with a fake server; 3) Android shell: SQLite store, MediaStore.Downloads sink, UrlConnectionFetcher, DownloadService (dataSync foreground) + notification, DownloadController; 4) Downloads screen (menu > Downloads, big buttons: pause/resume/cancel/open/share/delete, clear finished) replacing the DownloadManager/file-manager intent in geckoapp BrowserActivity.onMenuPick M.DOWNLOADS and net/Downloads.kt; 5) emulator test against scratchpad site (add a big file + Range support: python http.server lacks Range -> write a tiny Range-capable server), then alpha5 via ship.sh. Also queued: Settings restyle (tile rows), launcher icon check, private grid 'gray tint' fixed (root focus highlight), grid swipe-to-close, toolbar auto-hide on scroll (idea).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  c5df399 ckpt 275: gated kestrel-g-v0.2.0-alpha4 (gecko code 103) and pushed it: handoff self-tes
  276ef61 ckpt 266: N1 updater works end to end on the emulator (local feed): auto/manual check vi
  9470db6 ckpt 259: N2-N5 verified on emulator (Android 10 x86_64, 600x973): black Home + chamfere
  f6cb00e ckpt 252: N4/N5 code compiles: TabGrid (card grid with page previews, Tabs|Private segme
  bf260ea ckpt 246: N2/N3 part 1 compiles: CERE dark palette+angular icon set, ChamferDrawable, Bo
  9458fe0 ckpt 227: Owner request 3 recorded in TASKS.md (N1-N7); owner likes alpha3; UI-1 answere
  04080cd ckpt 224: Alpha3 (kestrel-g-v0.2.0-alpha3, run 42961586675) built, signed, released and 
  1ab978e ckpt 221: ship.sh gecko OK; dispatched android.yml app=gecko full_build=true: run 429615
  85eab84 ckpt 220: gated kestrel-g-v0.2.0-alpha3 (gecko code 102) and pushed it: handoff self-tes
  57a6d62 ckpt 219: alpha3 candidate: freeze fixes verified on emulator (worker bridge, cosmetics 
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
