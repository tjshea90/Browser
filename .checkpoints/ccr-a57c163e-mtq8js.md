# CHECKPOINT 294 — read me first, then TASKS.md

**Written:** 2026-11-11T07:09:01Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `8d24a22` (this checkpoint is the commit after it)

## Just done
N6 step 1 done: pure-Kotlin download engine in core/ (app.kestrel.core.download: Model+ports, HttpSemantics, FileNames, Planner/HillClimb/Meter/Format, Engine) + 43 JVM tests green x3 (HttpSemantics 7, FileNames 5, Planner 9, EngineTest 22 with in-memory fake server: pause/resume, parallel ranges, If-Match, changed file, no-range, 429/503, truncation, offline, no space, cancel, crash recovery, link refresh, queue, private)

## Do this next
N6 step 2 Android shell in app/src/main/kotlin/app/kestrel/browser/download/: SqliteStore (WAL), MediaStoreSink(s) (pending row, Os.pwrite/fsync/fallocate), UrlConnectionFetcher (manual redirects, no https->http), DownloadController (engine singleton, control HandlerThread, NetGate from NetworkMonitor), DownloadService (dataSync FGS + notifications), then ui/DownloadsScreen + Gecko hook (BrowserActivity.startDownload/M.DOWNLOADS, onExternalResponse closes body), GeckoFetcher via GeckoWebExecutor; manifest perms FOREGROUND_SERVICE(_DATA_SYNC), POST_NOTIFICATIONS, WAKE_LOCK; test on emulator with a Range-capable python server. heartbeat trig_01HjJLZki7r2XgUqZazC4oup

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  ce4c6e7 ckpt 281: handoff saved via context-handoff plugin (.handoff/handoff.md + json + archive
  8a1eac9 ckpt 278: alpha4 RELEASED + RECORDED: kestrel-g-v0.2.0-alpha4 (gecko code 103), run 4297
  c5df399 ckpt 275: gated kestrel-g-v0.2.0-alpha4 (gecko code 103) and pushed it: handoff self-tes
  276ef61 ckpt 266: N1 updater works end to end on the emulator (local feed): auto/manual check vi
  9470db6 ckpt 259: N2-N5 verified on emulator (Android 10 x86_64, 600x973): black Home + chamfere
  f6cb00e ckpt 252: N4/N5 code compiles: TabGrid (card grid with page previews, Tabs|Private segme
  bf260ea ckpt 246: N2/N3 part 1 compiles: CERE dark palette+angular icon set, ChamferDrawable, Bo
  9458fe0 ckpt 227: Owner request 3 recorded in TASKS.md (N1-N7); owner likes alpha3; UI-1 answere
  04080cd ckpt 224: Alpha3 (kestrel-g-v0.2.0-alpha3, run 42961586675) built, signed, released and 
  1ab978e ckpt 221: ship.sh gecko OK; dispatched android.yml app=gecko full_build=true: run 429615
```

(12 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
