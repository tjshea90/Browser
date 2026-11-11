# CHECKPOINT 224 — read me first, then TASKS.md

**Written:** 2026-11-11T04:54:22Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `516b47a` (this checkpoint is the commit after it)

## Just done
Alpha3 (kestrel-g-v0.2.0-alpha3, run 42961586675) built, signed, released and recorded; research R1-R6 done and decisions in TASKS.md; M1-M4 done; A1-A5 largely done (see TASKS). Owner decisions + alpha3 test request listed at the end of TASKS.md.

## Do this next
WAIT for the owner's alpha3 report (typing freeze? Claude.ai?) and UI answers UI-1..3 (defaults if silent: split chrome, CERE, tab strip). Meanwhile: claim B1 (bash tools/claim.sh B1) and build the pure-Kotlin download engine in core/ per research/DOWNLOADS.md sections 2-4 (Model, HttpSemantics, FileNames, Planner, Meter, Backoff, Engine with Fetcher/Sink/Store ports, FakeFetcher tests); then the Android shell (SQLite store, MediaStore sink, UrlConnectionFetcher, DownloadService, notifications, Downloads screen); then D1 (engine.js from research/proto/kfs-engine-prototype.mjs into geckoapp assets + KFS2/real PSL + differential tests); then C (docs/DESIGN.md first). Next ship: bump geckoapp versionCode (extension manifest syncs via ship.sh).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  1ab978e ckpt 221: ship.sh gecko OK; dispatched android.yml app=gecko full_build=true: run 429615
  85eab84 ckpt 220: gated kestrel-g-v0.2.0-alpha3 (gecko code 102) and pushed it: handoff self-tes
  57a6d62 ckpt 219: alpha3 candidate: freeze fixes verified on emulator (worker bridge, cosmetics 
  04dab0c ckpt 214: A2/A4 part: gecko-prefs.txt -> YAML config (AV1 off, memory/cache budget, no a
  1739d76 ckpt 209: A3 evidence layer: core RingLog (+tests), app Health (watchdog, frame jank, re
  9c46642 ckpt 175: A3 P0 part 1 (Kestrel G): GeckoView attached ONCE (ntp/error as overlays, setS
  21706f6 ckpt 119: A1 part 1: native UI animations off - instant pressed-state overlay instead of
  3cd7e40 ckpt 117: M1-M2 done: tools/sync.sh (auto-merge other sessions), per-branch cards .check
  cb69194 ckpt 109: Owner request 2 recorded: INBOX captured by the hook, TASKS.md boxes M1-M4 R1-
  286f09a ckpt 104: GitHub Actions pipeline proven: run 42951742468 built, signed, verified and pu
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
