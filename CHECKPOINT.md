# CHECKPOINT 2819 — read me first, then TASKS.md

**Written:** 2026-11-21T17:23:41Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/peaceful-darwin-4sbmdz` · **builds on:** `c7b369c7` (this checkpoint is the commit after it)

## Just done
LD4a done: research/SLOW_LOADS.md written from the real uploads: claude.ai 14.7 s to first paint / 28.5 s done on a revived tab, 91 percent of requests from cache (not network-bound), Shields req listener 1.6 ms per request = about 1.6 s per 1000-request page (5-7 percent, not the cause), Neon already paused during loads, Classifier Update thread 90-95 percent twice, Cache2 I/O 31 percent once; ranked hypotheses and what is NOT known

## Do this next
LD4b build: (1) core DiagScrub snapshot cap (lines are cut at 400 with '...': the CPU and Blocker lines lose their tails) + DiagJournal important 'page timing'; (2) extension pagetime.js (ES5, top frame, document_start, lag sampler + nav/paint/resource summary, ONE message per load) + manifest + background.js handler k=pt + web-tests/pagetime.test.js; (3) ShieldsBridge pageTiming callback + BrowserActivity.onPageTiming -> Health.log('page','timing',...) with shields/net/avail/cpu/tabs context, never private; (4) liveDiagExtraFacts: extension listener us, judge avg/max, cpu avg/peak (parent+tab), PSS, cold start phases, jank; (5) DiagStats timing per site + SiteStat fields + Advisor rules (cpu-bound vs network-bound vs Shields); (6) diag_digest.py LOAD ANALYSIS; tests each; gate; alpha85

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  a6b43548 ckpt 2818: LD4 written into TASKS.md in the owner's words (richer live diagnostics + cl
  db4c9602 ckpt 2817: alpha84 RELEASED AND RECORDED: https://github.com/tjshea90/Browser/releases/
  bcfe8a73 ckpt 2816: alpha84 build running: run id 44924979560 (run_number 265, head 2cf128f4), d
  4a42d19d ckpt 2815: alpha84 gated (core+app tests, gecko compile, lint 0 errors, self-test 80/80
  2cf128f4 ckpt 2814: gated kestrel-g-v0.3.0-alpha84 (gecko code 185) and pushed it: handoff self-
  0efd4ff7 ckpt 2813: ALPHA84 release note written (docs/handoff/ALPHA84_RELEASE_NOTE.txt); platfo
  c4318ee7 ckpt 2812: LD3 docs + TASKS ticks (a-d) written; digest prints sends automatic/manual +
  7369e028 ckpt 2811: LD3 code done and green: core DiagJournal (exact commit, honest lost/thinned
  bb443992 ckpt 2810: LD3d tools done: private.sh + diag_digest.py find the clone in any letter ca
  2015daad ckpt 2809: LD3 audit written into research/LIVE_DIAG.md from the 4 real uploads: manual
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
