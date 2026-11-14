# CHECKPOINT 1925 — read me first, then TASKS.md

**Written:** 2026-11-14T02:10:10Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `78ff83c` (this checkpoint is the commit after it)

## Just done
AT1c/d: tests written and green: LexiconFileTest 6, GlideDecoderTest 15 (tiny list + exact paths, tap is not a glide, ends must match, the word before decides, thumb habit learned, adaptive language weight, shipped list size, seeded accuracy on the real list: calm >=90/97, normal >=83/94, sloppy >=70/86, context helps, habit corrected, decode fast). Decoder now has a second look that moves the whole path by the thumb's offset + learnFrom (cross-glide habit): simulated normal finger common words top1 91.1 / top3 97.2; with a 10 dp habit 85.9 -> 91.2 after learning.

## Do this next
1) docs/DICTIONARY.md (sources: symspellpy 6.10.2 wheel = MIT, Wolf Garbe SymSpell frequency_dictionary_en_82_765.txt + frequency_bigramdictionary_en_243_342.txt from Google Books Ngram intersected with SCOWL; hand list tools/glide/extra-words.txt; regenerate: pip download symspellpy==6.10.2 --no-deps, unzip only the .txt files into an empty dir, ./gradlew :tools:glideLexicon --args=...), research/GLIDE_TYPING.md (numbers: freq/rank/ctx, key-size table 240/292/340/380 dp, robustness list, decode ms, limits: simulator not a real thumb). 2) AT1e: core MiniKeyboardModel.commitGlide/swapGlide/backspace-removes-word + tests; 3) ui/MiniNav.kt KeysView: use MiniKeyboardGeometry.boxes (strip height on top), glide vs tap, trail, candidate strip, decode on a single-thread executor, lexicon load off the UI thread; Settings switch glide_typing; learned habit prefs glide_bias_x/y.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  5ae6905 ckpt 1920: AT1b/c/d first cut works: MIT symspell word list compiled to geckoapp/src/mai
  b547793 ckpt 1898: Owner message 34 written into TASKS (AT1 glide typing on the mini keyboard, b
  307f5cf ckpt 1883: alpha33 SHIPPED AND RECORDED: run 43536066565 green (all jobs success, pref c
  79e5865 ckpt 1877: alpha33 (gecko code 134) DISPATCHED: android.yml run 43536066565 (run #84, st
  f11808c ckpt 1876: gated kestrel-g-v0.3.0-alpha33 (gecko code 134) and pushed it: handoff self-t
  cc028b28 ckpt 1875: AO1 ticked in TASKS (emulator Diagnostics screen shows the CPU by process, S
  9689afe ckpt 1860: AQ1, AR1 and AR2 ticked in TASKS with evidence: Home restore proven on the em
  2874e84 ckpt 1844: AQ1: found on the emulator that restore's show() cleared the Home flag (the p
  ebe2fa5 ckpt 1829: Owner message 32 written into TASKS (AR1 blocked-page icon bottom middle 1 s,
  39cf669 ckpt 1819: Owner message 31 written into TASKS (AQ1 Home restored as last page, AQ2 cont
```

(4 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
