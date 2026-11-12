# CHECKPOINT 1327 — read me first, then TASKS.md

**Written:** 2026-11-12T23:39:20Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `167921b` (this checkpoint is the commit after it)

## Just done
EX-2 PROVEN on the emulator (Gecko's own nsHttp log + host TLS listener: old public call leaves no trace, Gecko-thread dispatch makes the engine run SpeculativeConnect and a ClientHello reaches the listener before Enter; RFC1918 targets are skipped by design). Version bumped to code 124 / 0.3.0-alpha25 (extension 1.0.124, needed so the emulator reinstalls the changed built-in extension), lab switches also written into the engine's start-up config, HANDOFF monthly check extended, full suites green (core+app 1m42)

## Do this next
Emulator proofs left: ex10.sh (embed: running, scratchpad ex10.log + ex10-a/b.png), ex3.sh (tap lab off vs on), ex1.sh (wifi lock while a download moves); then ship.sh gecko with the alpha25 note, dispatch, record, reply to the owner

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  59c0013 ckpt 1322: Data saver choice re-applies the engine prefs (the look-up-links lab switch f
  49dea49 ckpt 1320: Docs: docs/ARCHITECTURE.md alpha25 section, research/EX_RESULTS.md (what was 
  c560ca9 ckpt 1316: EX ladder: EX-10 embeds-wait-for-a-tap coded (Shields flag EMBEDS=128 + Prefs
  352efcb ckpt 1306: More of the EX ladder coded (compiles; unit tests green: NetProfile 12, Backu
  edaf04d ckpt 1288: EX-2/3/4/8/9/11 code done (compile green, unit tests for EX-9; emulator proof
  8a4f7fd ckpt 1283: EX-1 code done: DownloadService Wi-Fi lock = WIFI_MODE_FULL_HIGH_PERF on ever
  eb2eddd ckpt 1280: Owner message 16 handled per section 0: research/REVIEW-EXTERNAL-1.md saved (
  884b8ab ckpt 1263: alpha24 SHIPPED and recorded (tag kestrel-g-v0.3.0-alpha24, run 43321730068 g
  a6028f3 ckpt 1257: AA3-3b step 1 (after dispatching alpha24 run 43321730068): engine-image chunk
  efdc3a2 ckpt 1248: alpha24 gated and pushed (ckpt 1247), workflow DISPATCHED: run 43321730068 (#
```

(4 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
