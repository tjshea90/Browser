# CHECKPOINT 1322 — read me first, then TASKS.md

**Written:** 2026-11-12T23:36:53Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `3122909` (this checkpoint is the commit after it)

## Just done
Data saver choice re-applies the engine prefs (the look-up-links lab switch follows it); evidence from the engine's own log (debug.kestrel.moz_log): the app's speculative connect now REACHES Gecko's connection manager on the Gecko main thread ('MaybeSpeculativeConnectWithHTTPSRR for ci=10.0.2.2:8443' then 'SpeculativeConnect skipping RFC1918 address [10.0.2.2]'), i.e. EX-2 works and the emulator host address is refused by design; baseline (old public call) + public-name comparison runs in progress

## Do this next
Wait for ex2f/ex2g results (scratchpad), then ex10.sh (embed), ex3.sh (tap lab), ex1.sh (wifi lock while a download moves; media server on 8001 rate 100000), full suites (full3.log running), bump 124/alpha25, ship

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  49dea49 ckpt 1320: Docs: docs/ARCHITECTURE.md alpha25 section, research/EX_RESULTS.md (what was 
  c560ca9 ckpt 1316: EX ladder: EX-10 embeds-wait-for-a-tap coded (Shields flag EMBEDS=128 + Prefs
  352efcb ckpt 1306: More of the EX ladder coded (compiles; unit tests green: NetProfile 12, Backu
  edaf04d ckpt 1288: EX-2/3/4/8/9/11 code done (compile green, unit tests for EX-9; emulator proof
  8a4f7fd ckpt 1283: EX-1 code done: DownloadService Wi-Fi lock = WIFI_MODE_FULL_HIGH_PERF on ever
  eb2eddd ckpt 1280: Owner message 16 handled per section 0: research/REVIEW-EXTERNAL-1.md saved (
  884b8ab ckpt 1263: alpha24 SHIPPED and recorded (tag kestrel-g-v0.3.0-alpha24, run 43321730068 g
  a6028f3 ckpt 1257: AA3-3b step 1 (after dispatching alpha24 run 43321730068): engine-image chunk
  efdc3a2 ckpt 1248: alpha24 gated and pushed (ckpt 1247), workflow DISPATCHED: run 43321730068 (#
  3f19341 ckpt 1247: gated kestrel-g-v0.3.0-alpha24 (gecko code 123) and pushed it: handoff self-t
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
