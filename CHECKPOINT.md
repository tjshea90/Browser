# CHECKPOINT 2820 — read me first, then TASKS.md

**Written:** 2026-11-21T17:26:18Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/peaceful-darwin-4sbmdz` · **builds on:** `1b156928` (this checkpoint is the commit after it)

## Just done
LD4b core done: DiagScrub caps (event 480, snapshot lines 1600: the CPU/Blocker lines were cut), DiagJournal never thins 'page timing', DiagStats parses page timing per site (ttfb, fcp, busy share lagPct, requests) persisted + merged, SiteStat/DiagBatch carry timed/ttfbMs/fcpMs/lagPct/reqs, Advisor: busy-<site>, shields-heavy-<site>, slow-phone no longer for busy pages; core livediag 63 tests green (LoadAnalysisTest 14)

## Do this next
Next: extension side. (1) geckoapp/src/main/assets/kestrel-shields/pagetime.js (ES5, top frame, document_start): lag sampler 100 ms until load+3 s or 45 s, PerformanceObserver resource aggregation (buffer is only 250!), nav timing, paint, LCP, visibility; ONE message k=pt; manifest content_scripts entry like names.js; background.js onMsg k=pt validate numbers + send({k:'pt',p:tabTop...}); web-tests/pagetime.test.js (look at names.test.js for the harness). (2) ShieldsBridge pageTiming + BrowserActivity.onPageTiming -> Health.log('page','timing', ...) with shields/net/avail/cpu/tabs. (3) liveDiagExtraFacts numbers (extReqCalls/extReqUs/judge avg/max, cpu, pss, cold start, jank). (4) LiveDiag snapshot lines use DiagScrub.MAX_SNAPSHOT_LINE. (5) digest LOAD ANALYSIS

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  0b7be92b ckpt 2819: LD4a done: research/SLOW_LOADS.md written from the real uploads: claude.ai 1
  a6b43548 ckpt 2818: LD4 written into TASKS.md in the owner's words (richer live diagnostics + cl
  db4c9602 ckpt 2817: alpha84 RELEASED AND RECORDED: https://github.com/tjshea90/Browser/releases/
  bcfe8a73 ckpt 2816: alpha84 build running: run id 44924979560 (run_number 265, head 2cf128f4), d
  4a42d19d ckpt 2815: alpha84 gated (core+app tests, gecko compile, lint 0 errors, self-test 80/80
  2cf128f4 ckpt 2814: gated kestrel-g-v0.3.0-alpha84 (gecko code 185) and pushed it: handoff self-
  0efd4ff7 ckpt 2813: ALPHA84 release note written (docs/handoff/ALPHA84_RELEASE_NOTE.txt); platfo
  c4318ee7 ckpt 2812: LD3 docs + TASKS ticks (a-d) written; digest prints sends automatic/manual +
  7369e028 ckpt 2811: LD3 code done and green: core DiagJournal (exact commit, honest lost/thinned
  bb443992 ckpt 2810: LD3d tools done: private.sh + diag_digest.py find the clone in any letter ca
```

(3 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
