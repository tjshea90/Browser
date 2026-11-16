# CHECKPOINT 2526 — read me first, then TASKS.md

**Written:** 2026-11-16T16:36:22Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-b3c2484c-sbew36` · **builds on:** `430cb0fa` (this checkpoint is the commit after it)

## Just done
ANS55f step 2 coded and COMPILES (:geckoapp:compileDebugKotlin ok): PlayerFactory (activity uses it), PlayerControlsView inline mode (close x, full-screen button, scale), InlinePlayer view, InlineHost (CompositorScrollDelegate follow, touch pass-through, stop reasons), VideoFlow.start inline branch + repeat guards + pageOf(g,tp), BrowserActivity hooks (back, pause, config, attachContent, pageStarted, tab list, page fullscreen, destroy), Prefs web_inline + BackupManager spec + Settings toggle, WebHandStats inline counters

## Do this next
Run the unit suites (./gradlew --offline -q :core:test :app:testDebugUnitTest :geckoapp:testDebugUnitTest) and fix guard tests that read NativePlayerActivity/VideoFlow source text; add OwnerRulesTest guards for the inline rules (needs a real tap, private not logged, stop hooks, no page message moves the overlay); then lab: soaklab page with a tall scrollable page + HLS/file video in the middle, new suite ip in tools/emu/soak.py + android.yml lab_suite, checks: overlay box == element box at rest (Health 'inline placed'), after a known scroll (Health 'inline scroll' view box vs page-reported rect in lab evt log), screenshot pixels in the box = film colours, full-screen button hands same second to activity, Back closes inline first, scroll out of view stops, second film replaces first. Then ship alpha60 (versionCode 161).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  16795a42 ckpt 2525: ANS55f step 1 done: gate-main.js geomOf + content.js geo() + background.js g
  50071840 ckpt 2524: alpha59 SHIPPED and RECORDED: Release kestrel-g-v0.3.0-alpha59 (code 160, ru
  2e6fccf5 ckpt 2523: alpha59 RELEASE DISPATCHED (ONE time): android.yml run 43905841979 (run 174,
  d0daa8b8 ckpt 2522: gated kestrel-g-v0.3.0-alpha59 (gecko code 160) and pushed it: handoff self-
  c4c4f1df ckpt 2521: pre-ship: alpha59: your message 55 (on alpha58). (1) FIXED: the black pictur
  04ffa9c9 ckpt 2520: lab ho run dispatched: 43903287513 (head 09476513 = ckpt 2519, started 16:03
  09476513 ckpt 2519: owner message 56 written into TASKS.md (ANS56a-b): 'even when I selected a t
  f7ab7a01 ckpt 2518: ANS55e fix coded (core tests + gecko compile green): Yahoo/Brightcove root c
  5851f383 ckpt 2517: owner message 55 (alpha58 on the tablet: Yahoo video black, resumed the wron
  6f31c2fb ckpt 2516: session handoff saved by the handoff tool (.handoff/handoff.md + json + arch
```

(9 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
