# CHECKPOINT 3173 — read me first, then TASKS.md

**Written:** 2026-11-24T05:05:23Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/kind-brahmagupta-ae3yl8` · **builds on:** `84df6b2e` (this checkpoint is the commit after it)

## Just done
alpha102 SHIPPED and recorded: run 45326840944 green on attempt 2 (attempt 1: the known flaky StreamEngineTest), Release has kestrel-g-0.3.0-alpha102.apk 96.7 MB, certificate verified, BUILDLOG.md updated; PERF21 closed

## Do this next
NEXT in this order: (1) FLK1: harden core/src/test/kotlin/app/kestrel/core/media/StreamEngineTest.kt refusalsThatStartHalfWayAreAskedAgainBeforeTheLadderIsTried (the perPart>=4 assertion depends on worker interleaving; make it deterministic or assert the real property), then PhoneBackendTest and courier ServerTest; run the test 30x locally to prove (gradle --tests with --rerun-tasks in a loop, one gradle at a time); (2) PERF22 deep low-end research: write research/LOW_END_PLAYBOOK.md from the web findings already collected plus new targeted searches (Android 10 Go specifics: lmkd/zram, cached process limit, PiP disabled on low-RAM, dex2oat/baseline profile, WebView-less quirks; GeckoView/Mozilla low-end: processPrelaunch, GC, JIT thresholds 100/1500, WebRender software fallback bugs on Mali; Chromium low-end mode: RGB565/4444 textures, discardable memory, idle-timeouts; web.dev JS cost on weak CPUs), table with have/do/skip verdicts, then implement the safe ones (PERF22c), ship alpha103 / versionCode 204; (3) after alpha103: wait for the owner's uploads (python3 -B -I tools/diag_digest.py --days 2): PAGE ANIMATION, bot checks, lazy start, TrimGate held/sent, Decorative page clips. Never ship alpha102 again

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  b1b76738 ckpt 3171: alpha102 run 45326840944 attempt 1: test-jvm FAILED on the known flaky Strea
  997968a3 ckpt 3169: alpha102 (code 203) gated (ckpt 3168, head 0a423ffb) and dispatched ONCE: ru
  0a423ffb ckpt 3168: gated kestrel-g-v0.3.0-alpha102 (gecko code 203) and pushed it: handoff self
  c251ca3b ckpt 3167: pre-ship: alpha102: the animations on that page. (1) NO PLAY BAR OVER ANIMAT
  15d8bb97 ckpt 3165: owner request PERF22 (deep research: popular browsers' speed techniques, cod
  d452ebef ckpt 3163: PERF21 coded: no bar on decorative clips (vfind decorative + videobar judge)
  09db2cad ckpt 3153: owner request PERF21 (screenshot: a page of looping picture clips is very la
  2057b01b ckpt 3152: alpha101 SHIPPED and recorded: run 45312620908 green on attempt 1 (check, te
  1e465904 ckpt 3150: alpha101 (code 202) gated (ckpt 3148, head 94c20d92 + the corrected note fil
  94c20d92 ckpt 3148: gated kestrel-g-v0.3.0-alpha101 (gecko code 202) and pushed it: handoff self
```
