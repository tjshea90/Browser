# CHECKPOINT 1949 — read me first, then TASKS.md

**Written:** 2026-11-14T02:55:34Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `9b68567` (this checkpoint is the commit after it)

## Just done
AT1h emulator proof DONE with a real multi-point finger (sendevent-style binary input events): hello -> 'hello ' + strip hello/help/jello, world after it, tap on 2nd guess swaps, ONE Backspace removes the whole word, neon trail visible mid-glide, Diagnostics 'Glide typing' line seen. Then: cheaper defaults (48 steps, 200 candidates, 40 second looks: -45 % time, -0.5 points) + a warm-up decode when the keyboard opens (the emulator needed 1.6 s per glide with no acceleration: software emulation, NOT a tablet number); research/GLIDE_TYPING.md tables re-measured + section 7 (what was and was not run). 68 glide tests green.

## Do this next
1) wait for scratchpad/build_glide2.log + install_glide2.log (background: rebuild + install of the warm-up build), then one glide + Diagnostics to see 'first decode (warm-up) N ms' (python3 -I scratchpad/glide_emu.py hello after opening the panel keyboard via tapxy 500 133). 2) run the full suite once more: ./gradlew :core:test :app:testDebugUnitTest :geckoapp:compileDebugKotlin :app:lintDebug --continue + cd web-tests && npm test (unchanged but the gate). 3) bump versionCode 135 / versionName 0.3.0-alpha34 in geckoapp/build.gradle.kts, ckpt, bash ship.sh gecko with a plain-words note, dispatch android.yml (app=gecko full_build=true ref=ccr-a57c163e-mtq8js), wait tag + green, get_release_by_tag, bash tools/record-release.sh, tick AT1h + AT1 with claim.sh AT1 --done, answer the owner (what glide does, SIMULATED accuracy table, key-size finding, what was not run on the tablet, ask: Spanish list AT2, how the glide feels, paste Diagnostics Glide typing line).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  58ae170 ckpt 1945: AT1e-g DONE and green: glide typing in the mini keyboard (KeysView glide vs t
  54eae48 ckpt 1931: AT1e core half done and green (45 tests in core input): MiniKeyboardModel.com
  7aca167 ckpt 1925: AT1c/d: tests written and green: LexiconFileTest 6, GlideDecoderTest 15 (tiny
  5ae6905 ckpt 1920: AT1b/c/d first cut works: MIT symspell word list compiled to geckoapp/src/mai
  b547793 ckpt 1898: Owner message 34 written into TASKS (AT1 glide typing on the mini keyboard, b
  307f5cf ckpt 1883: alpha33 SHIPPED AND RECORDED: run 43536066565 green (all jobs success, pref c
  79e5865 ckpt 1877: alpha33 (gecko code 134) DISPATCHED: android.yml run 43536066565 (run #84, st
  f11808c ckpt 1876: gated kestrel-g-v0.3.0-alpha33 (gecko code 134) and pushed it: handoff self-t
  cc028b28 ckpt 1875: AO1 ticked in TASKS (emulator Diagnostics screen shows the CPU by process, S
  9689afe ckpt 1860: AQ1, AR1 and AR2 ticked in TASKS with evidence: Home restore proven on the em
```

(3 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
