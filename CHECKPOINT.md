# CHECKPOINT 352 — read me first, then TASKS.md

**Written:** 2026-11-11T14:56:03Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `c229c06` (this checkpoint is the commit after it)

## Just done
alpha5 SHIPPED + RECORDED (run 42989432755 green, kestrel-g-v0.2.0-alpha5, APK sha256 9366bef1...; BUILDLOG line pushed). The owner's updater parses the real alpha5 body (checked UpdateLogic regexes vs the live release text; feed = releases?per_page=8, prereleases offered). New owner request 5 (INBOX 14:50Z: Cromite pros/cons, scroll jank + white screens on the tablet, use GPU/decoding fully, consider Cromite instead of Gecko) written into TASKS.md as Q1-Q3 (+Q2a-e). Heartbeat trig_01HjJLZki7r2XgUqZazC4oup still exists (next fire 12:48Z passed; prompt is stale: it says alpha4/N6) - DELETE it. Findings so far: sessionGone() in BrowserActivity.kt drops SessionState and does plain loadUri (scroll position lost = 'has to reload'); gecko-prefs.txt sets image.mem.surfacecache.max_size_kb=98304 but Gecko caps the surface cache at RAM/size_factor(64) = ~30 MB on this tablet.

## Do this next
1) delete heartbeat trigger trig_01HjJLZki7r2XgUqZazC4oup (mcp__claude-code-remote__delete_trigger); 2) Q2a: BrowserActivity.sessionGone: keep SessionState and restoreState (scroll), preview overlay until onFirstContentfulPaint, Health log with MemoryInfo + trim level, crash-loop guard stays; 3) research/SCROLL.md (agent or self): Gecko 160 knobs for scroll/white tiles/memory on 2 GB Mali-G52, verify pref names in the real AAR; 4) Q2e Diagnostics scroll test; 5) answer the owner: Cromite pros/cons (research/ENGINES.md) + tell them alpha5 is out (Release link on its own line: https://github.com/tjshea90/Browser/releases/tag/kestrel-g-v0.2.0-alpha5); 6) ship alpha6 (versionCode 105) via ship.sh gecko + android.yml dispatch (mcp__github__actions_run_trigger, ToolSearch select first)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  e9f12cd ckpt 344: gated kestrel-g-v0.2.0-alpha5 (gecko code 104) and pushed it: handoff self-tes
  b84308a ckpt 342: P3+P4 DONE and verified on the emulator (blue REAPER theme: Home flames/scythe
  bf1ed62 ckpt 316: P3/P4 neon wired and compiling (both modules): Neon engine (RenderThread alpha
  6438c0a ckpt 306: Owner request 4 recorded (P1-P4: hardware speed research, engine research incl
  8d8143c ckpt 301: N6 step 2 compiles: Android download shell (SqliteStore, MediaStoreSinks, UrlC
  c7112dc ckpt 294: N6 step 1 done: pure-Kotlin download engine in core/ (app.kestrel.core.downloa
  ce4c6e7 ckpt 281: handoff saved via context-handoff plugin (.handoff/handoff.md + json + archive
  8a1eac9 ckpt 278: alpha4 RELEASED + RECORDED: kestrel-g-v0.2.0-alpha4 (gecko code 103), run 4297
  c5df399 ckpt 275: gated kestrel-g-v0.2.0-alpha4 (gecko code 103) and pushed it: handoff self-tes
  276ef61 ckpt 266: N1 updater works end to end on the emulator (local feed): auto/manual check vi
```

(6 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
