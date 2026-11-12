# CHECKPOINT 1334 — read me first, then TASKS.md

**Written:** 2026-11-12T23:43:56Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `2fa3bf2` (this checkpoint is the commit after it)

## Just done
:style() restyling rules in core (697 of 770 real rules accepted; CosmeticStyleTest 7 + RealStyleRulesTest), EX-2 (B) warm the launch link's and the front page's origin while the engine starts (GeckoEngine.warmOrigin/warmAtStart), lab switches also in the engine config, embed page web-tests (5, headless Chromium; 99 web tests). EX-10 first emulator run showed the frame loading its real address (the built-in extension was not up yet 25 s after a cold start: the page loaded before its listener existed); a second run that gives the extension time (ex10b.sh) is queued behind the Tap lab / Wi-Fi lock chain

## Do this next
Wait for the chain (ex3.log, ex1.log), then run ex10b.sh (embed with a warm extension), full suites, ship.sh gecko "$(cat scratchpad/note25.txt)" (note + include ':style()' sentence), dispatch, record, reply

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  217004a ckpt 1327: EX-2 PROVEN on the emulator (Gecko's own nsHttp log + host TLS listener: old 
  59c0013 ckpt 1322: Data saver choice re-applies the engine prefs (the look-up-links lab switch f
  49dea49 ckpt 1320: Docs: docs/ARCHITECTURE.md alpha25 section, research/EX_RESULTS.md (what was 
  c560ca9 ckpt 1316: EX ladder: EX-10 embeds-wait-for-a-tap coded (Shields flag EMBEDS=128 + Prefs
  352efcb ckpt 1306: More of the EX ladder coded (compiles; unit tests green: NetProfile 12, Backu
  edaf04d ckpt 1288: EX-2/3/4/8/9/11 code done (compile green, unit tests for EX-9; emulator proof
  8a4f7fd ckpt 1283: EX-1 code done: DownloadService Wi-Fi lock = WIFI_MODE_FULL_HIGH_PERF on ever
  eb2eddd ckpt 1280: Owner message 16 handled per section 0: research/REVIEW-EXTERNAL-1.md saved (
  884b8ab ckpt 1263: alpha24 SHIPPED and recorded (tag kestrel-g-v0.3.0-alpha24, run 43321730068 g
  a6028f3 ckpt 1257: AA3-3b step 1 (after dispatching alpha24 run 43321730068): engine-image chunk
```

(6 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
