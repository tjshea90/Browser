# CHECKPOINT 1953 — read me first, then TASKS.md

**Written:** 2026-11-14T03:04:01Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `e1dd191` (this checkpoint is the commit after it)

## Just done
gated kestrel-g-v0.3.0-alpha34 (gecko code 135) and pushed it: handoff self-test, unit suites and the versionCode check passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow_id android.yml, ref ccr-a57c163e-mtq8js, inputs {"app":"gecko","full_build":"true","note":"alpha34 (includes alpha33): Glide typing on the mini keyboard (the small keyboard in the top right panel). Slide your finger over the letters of a word without lifting it, then lift: the word is typed with a space after it, a glowing trail follows your finger, and the two next-best guesses show above the keys (tap one to swap it in). One Backspace right after a glide removes the whole word. Tapping single letters works as before. The keyboard learns how your thumb lands beside the keys, the words you type that it did not know, and the names of the sites you visit (so a name like doraby can be glided). It uses a built-in list of 83,000 English words and how often words follow each other (free MIT-licensed data, about 1.4 MB packed). Settings > Appearance > Glide typing on the mini keyboard switches it off. Menu > Diagnostics has a new Glide typing line (how many glides, how often the first guess was kept, swapped or deleted, and speed): please paste it after you have used it. Tested with a simulated finger (a typical finger gets the right word first about 91 percent of the time and within the three guesses 97 percent) and with a real multi-point touch on the emulator; NOT yet tried on your tablet. English words only."}.
When the run is green, confirm the Release shows the APK (mcp__github__get_release_by_tag kestrel-g-v0.3.0-alpha34), then:  bash tools/record-release.sh kestrel-g-v0.3.0-alpha34 "alpha34 (includes alpha33): Glide typing on the mini keyboard (the small keyboard in the top right panel). Slide your finger over the letters of a word without lifting it, then lift: the word is typed with a space after it, a glowing trail follows your finger, and the two next-best guesses show above the keys (tap one to swap it in). One Backspace right after a glide removes the whole word. Tapping single letters works as before. The keyboard learns how your thumb lands beside the keys, the words you type that it did not know, and the names of the sites you visit (so a name like doraby can be glided). It uses a built-in list of 83,000 English words and how often words follow each other (free MIT-licensed data, about 1.4 MB packed). Settings > Appearance > Glide typing on the mini keyboard switches it off. Menu > Diagnostics has a new Glide typing line (how many glides, how often the first guess was kept, swapped or deleted, and speed): please paste it after you have used it. Tested with a simulated finger (a typical finger gets the right word first about 91 percent of the time and within the three guesses 97 percent) and with a real multi-point touch on the emulator; NOT yet tried on your tablet. English words only."
NEVER ship kestrel-g-v0.3.0-alpha34 twice (check BUILDLOG.md and the Releases first).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M geckoapp/src/main/assets/kestrel-shields/manifest.json

## Last ten checkpoints
```
  eedfd73 ckpt 1949: AT1h emulator proof DONE with a real multi-point finger (sendevent-style bina
  58ae170 ckpt 1945: AT1e-g DONE and green: glide typing in the mini keyboard (KeysView glide vs t
  54eae48 ckpt 1931: AT1e core half done and green (45 tests in core input): MiniKeyboardModel.com
  7aca167 ckpt 1925: AT1c/d: tests written and green: LexiconFileTest 6, GlideDecoderTest 15 (tiny
  5ae6905 ckpt 1920: AT1b/c/d first cut works: MIT symspell word list compiled to geckoapp/src/mai
  b547793 ckpt 1898: Owner message 34 written into TASKS (AT1 glide typing on the mini keyboard, b
  307f5cf ckpt 1883: alpha33 SHIPPED AND RECORDED: run 43536066565 green (all jobs success, pref c
  79e5865 ckpt 1877: alpha33 (gecko code 134) DISPATCHED: android.yml run 43536066565 (run #84, st
  f11808c ckpt 1876: gated kestrel-g-v0.3.0-alpha33 (gecko code 134) and pushed it: handoff self-t
  cc028b28 ckpt 1875: AO1 ticked in TASKS (emulator Diagnostics screen shows the CPU by process, S
```

(3 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
