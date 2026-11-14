# CHECKPOINT 1931 — read me first, then TASKS.md

**Written:** 2026-11-14T02:13:58Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `70eda5a` (this checkpoint is the commit after it)

## Just done
AT1e core half done and green (45 tests in core input): MiniKeyboardModel.commitGlide/swapGlide/previousWord/plainWords + one-Backspace-removes-the-glided-word, GlideSupport.kt (GlideStats Diagnostics line, UserWords + wordOfHost, GlideTyping engine on one executor: load list, decode, accepted() learns the thumb habit), word list recompiled with the total count in the header, docs/DICTIONARY.md written (MIT notice + how to regenerate).

## Do this next
AT1e UI half: edit app/src/main/kotlin/app/kestrel/browser/ui/MiniNav.kt: (a) KeysView uses MiniKeyboardGeometry.boxes(rows, w, h, gap, top = strip) and keeps its tap/backspace-repeat behaviour; (b) glide: DOWN on a letter key starts a path, MOVE out of that key (+slop 0.25 pitch) = glide (cancel the key flash), trail drawn as a neon polyline fading out over ~220 ms on its own clock, UP -> GlideTyping.decode on a single-thread executor -> post to UI: model.commitGlide(best), strip shows 3 candidates (tap = model.swapGlide + stats.onPicked), Backspace after glide = stats.onUndone, anything else = stats.onKept, accepted() -> prefs glide_bias; (c) strip height STRIP_H_DP=30 only when glide is on, keys height grows by it; (d) MiniNav.Listener gets default methods navGlideEnabled(), navGlideHabit()/navGlideHabitChanged(x,y), navVocabulary(); (e) Settings switch glide_typing (default on) in the Appearance section next to the mini nav switch, Prefs.glideTyping + glideBiasX/Y; (f) BrowserActivity wires them + Diagnostics adds glide stats line; (g) Robolectric test with a hand-made polyline through key centres; then lint, full suite, emulator proof via sendevent, research/GLIDE_TYPING.md, ship alpha34.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  7aca167 ckpt 1925: AT1c/d: tests written and green: LexiconFileTest 6, GlideDecoderTest 15 (tiny
  5ae6905 ckpt 1920: AT1b/c/d first cut works: MIT symspell word list compiled to geckoapp/src/mai
  b547793 ckpt 1898: Owner message 34 written into TASKS (AT1 glide typing on the mini keyboard, b
  307f5cf ckpt 1883: alpha33 SHIPPED AND RECORDED: run 43536066565 green (all jobs success, pref c
  79e5865 ckpt 1877: alpha33 (gecko code 134) DISPATCHED: android.yml run 43536066565 (run #84, st
  f11808c ckpt 1876: gated kestrel-g-v0.3.0-alpha33 (gecko code 134) and pushed it: handoff self-t
  cc028b28 ckpt 1875: AO1 ticked in TASKS (emulator Diagnostics screen shows the CPU by process, S
  9689afe ckpt 1860: AQ1, AR1 and AR2 ticked in TASKS with evidence: Home restore proven on the em
  2874e84 ckpt 1844: AQ1: found on the emulator that restore's show() cleared the Home flag (the p
  ebe2fa5 ckpt 1829: Owner message 32 written into TASKS (AR1 blocked-page icon bottom middle 1 s,
```

(5 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
