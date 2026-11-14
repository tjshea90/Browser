# CHECKPOINT 1945 — read me first, then TASKS.md

**Written:** 2026-11-14T02:26:53Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `b0aa50b` (this checkpoint is the commit after it)

## Just done
AT1e-g DONE and green: glide typing in the mini keyboard (KeysView glide vs tap, neon trail, strip of 3 guesses, one-Backspace-removes-the-word, swap, learned thumb habit + own words + visited site names, Settings switch, Diagnostics line, 'no word fits' limit). Full suite + lint green (core, app incl. MiniNavTest 23, gecko compile). research/GLIDE_TYPING.md written (measured accuracy tables, key-size table, limits). TASKS AT1a-g ticked, AT2 (Spanish list question) added.

## Do this next
AT1h: (1) git status clean? run bash tools/ckpt.sh again after any edit; (2) emulator proof: boot AVD kestrel29x64 (scratchpad/emu.sh, boot 3-5 min, no KVM), build debug gecko APK (scratchpad/build_look.sh pattern: ./gradlew :geckoapp:installDebug), serve nothing needed; open menu > mini panel toggle > bar > keyboard, then a REAL multi-point glide via on-device sendevent loop (find touch device with adb shell getevent -p, scale to its ABS range) for 'hello' / 'world'; screenshot the trail + strip (not kept in the repo); verify bar text, strip, Backspace removes the word, Diagnostics line shows the glide. (3) bump versionCode 135 / 0.3.0-alpha34 only when shipping, ship.sh gecko, dispatch android.yml, record-release, answer the owner in plain words (what glide does, accuracy numbers as SIMULATED, limits, ask the Spanish-list question AT2, ask how the glide feels + paste the Diagnostics Glide typing line).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  54eae48 ckpt 1931: AT1e core half done and green (45 tests in core input): MiniKeyboardModel.com
  7aca167 ckpt 1925: AT1c/d: tests written and green: LexiconFileTest 6, GlideDecoderTest 15 (tiny
  5ae6905 ckpt 1920: AT1b/c/d first cut works: MIT symspell word list compiled to geckoapp/src/mai
  b547793 ckpt 1898: Owner message 34 written into TASKS (AT1 glide typing on the mini keyboard, b
  307f5cf ckpt 1883: alpha33 SHIPPED AND RECORDED: run 43536066565 green (all jobs success, pref c
  79e5865 ckpt 1877: alpha33 (gecko code 134) DISPATCHED: android.yml run 43536066565 (run #84, st
  f11808c ckpt 1876: gated kestrel-g-v0.3.0-alpha33 (gecko code 134) and pushed it: handoff self-t
  cc028b28 ckpt 1875: AO1 ticked in TASKS (emulator Diagnostics screen shows the CPU by process, S
  9689afe ckpt 1860: AQ1, AR1 and AR2 ticked in TASKS with evidence: Home restore proven on the em
  2874e84 ckpt 1844: AQ1: found on the emulator that restore's show() cleared the Home flag (the p
```

(13 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
