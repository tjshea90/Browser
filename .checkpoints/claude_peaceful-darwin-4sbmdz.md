# CHECKPOINT 2822 — read me first, then TASKS.md

**Written:** 2026-11-21T17:35:38Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/peaceful-darwin-4sbmdz` · **builds on:** `fe709ae7` (this checkpoint is the commit after it)

## Just done
LD4b app side done and compiling: GeckoEngine.networkProfileCode, ShieldsBridge pageTiming + 'pt' dispatch, BrowserActivity.onPageTiming (host from the TAB, never private, context shields/net/avail/cpu/tabs) -> Health 'page timing' line (core PageTimingLine), liveDiagExtraFacts adds extension cost (ExtCost), CPU (CpuMeter.numbers), memory (LastRun.memoryNumbers), start marks (StartTimeline.numbers), jankFrames; LiveDiag snapshot lines keep 1600 chars; geckoapp compile green, app diag tests green; core livediag 65

## Do this next
Next: (1) app test for the new facts (LiveDiagTest: extra facts in the batch are numbers only) - optional; (2) tools/diag_digest.py LOAD ANALYSIS: parse 'page timing' event lines + sites timing fields + Blocker numbers: per site loads, median load, fcp, ttfb, lag share, requests, classification NETWORK / CPU / SHIELDS with numbers; show device ext numbers; test_handoff checks; (3) full gate (:core:test :app:testDebugUnitTest :geckoapp:testDebugUnitTest :app:lintDebug, web-tests npm test), (4) bump 0.3.0-alpha85 code 186, sync-ext-version, note file docs/handoff/ALPHA85_RELEASE_NOTE.txt, ship.sh gecko, ONE dispatch, record-release; (5) answer owner

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  ca1f0fe2 ckpt 2821: LD4b extension side done: pagetime.js (ES5, top frame, document_start, laten
  029be5ca ckpt 2820: LD4b core done: DiagScrub caps (event 480, snapshot lines 1600: the CPU/Bloc
  0b7be92b ckpt 2819: LD4a done: research/SLOW_LOADS.md written from the real uploads: claude.ai 1
  a6b43548 ckpt 2818: LD4 written into TASKS.md in the owner's words (richer live diagnostics + cl
  db4c9602 ckpt 2817: alpha84 RELEASED AND RECORDED: https://github.com/tjshea90/Browser/releases/
  bcfe8a73 ckpt 2816: alpha84 build running: run id 44924979560 (run_number 265, head 2cf128f4), d
  4a42d19d ckpt 2815: alpha84 gated (core+app tests, gecko compile, lint 0 errors, self-test 80/80
  2cf128f4 ckpt 2814: gated kestrel-g-v0.3.0-alpha84 (gecko code 185) and pushed it: handoff self-
  0efd4ff7 ckpt 2813: ALPHA84 release note written (docs/handoff/ALPHA84_RELEASE_NOTE.txt); platfo
  c4318ee7 ckpt 2812: LD3 docs + TASKS ticks (a-d) written; digest prints sends automatic/manual +
```

(4 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
