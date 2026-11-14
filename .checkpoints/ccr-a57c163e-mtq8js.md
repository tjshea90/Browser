# CHECKPOINT 1920 — read me first, then TASKS.md

**Written:** 2026-11-14T01:57:46Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `0d9f75e` (this checkpoint is the commit after it)

## Just done
AT1b/c/d first cut works: MIT symspell word list compiled to geckoapp/src/main/assets/glide/en.klx (82,971 words + 242,342 pairs, 2.7 MB; tools/glide/extra-words.txt adds web names + apostrophe words; :tools:glideLexicon task), core/input/glide (Lexicon.kt format+reader, GlideDecoder.kt two-pass decoder, MiniKeyboardGeometry), test-side GestureSim + GlideEval simulator. Simulated accuracy (normal finger, common words): top1 90.5 %, top3 97.3 %; sloppy 79/91; with the word before 97/99; decode about 3.5 ms here.

## Do this next
1) write core tests (LexiconFileTest round trip + glide decoder unit tests + a seeded accuracy test with thresholds: normal common words top1>=85 %, top3>=95 %; keep the test fast, 800 words) and docs/DICTIONARY.md (sources, MIT licence text of symspell/symspellpy 6.10.2 wheel, how to regenerate) + research/GLIDE_TYPING.md numbers; 2) key-size experiment (width=240/292/380 with the same dp noise: scratchpad/geval.sh n=2000 pop=freq noise=aim8 width=...) 3) then AT1e: refactor ui/MiniNav.kt KeysView to use core MiniKeyboardGeometry.boxes, add glide gesture + trail + candidate strip + model commitGlide/swap/backspace; Settings switch; AT1f learning; AT1g Diagnostics. scratchpad/geval.sh runs the yardstick (needs :core:compileTestKotlin first). Heartbeat trig_01LLoVKsjuTUaDn61UGP1pBN refreshed (generic prompt).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  b547793 ckpt 1898: Owner message 34 written into TASKS (AT1 glide typing on the mini keyboard, b
  307f5cf ckpt 1883: alpha33 SHIPPED AND RECORDED: run 43536066565 green (all jobs success, pref c
  79e5865 ckpt 1877: alpha33 (gecko code 134) DISPATCHED: android.yml run 43536066565 (run #84, st
  f11808c ckpt 1876: gated kestrel-g-v0.3.0-alpha33 (gecko code 134) and pushed it: handoff self-t
  cc028b28 ckpt 1875: AO1 ticked in TASKS (emulator Diagnostics screen shows the CPU by process, S
  9689afe ckpt 1860: AQ1, AR1 and AR2 ticked in TASKS with evidence: Home restore proven on the em
  2874e84 ckpt 1844: AQ1: found on the emulator that restore's show() cleared the Home flag (the p
  ebe2fa5 ckpt 1829: Owner message 32 written into TASKS (AR1 blocked-page icon bottom middle 1 s,
  39cf669 ckpt 1819: Owner message 31 written into TASKS (AQ1 Home restored as last page, AQ2 cont
  fa60b0e ckpt 1812: AO1 code + research written: core/diag/CpuUse.kt (ProcStat/CpuSeries, CpuUseT
```

(21 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
