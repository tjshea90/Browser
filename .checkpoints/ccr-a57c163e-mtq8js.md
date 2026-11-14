# CHECKPOINT 1978 — read me first, then TASKS.md

**Written:** 2026-11-14T03:23:33Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `f8a2fb0` (this checkpoint is the commit after it)

## Just done
AU2 learning keyboard: PersonalModel tests (11) + MiniKeyboard typedPairs tests + research/GLIDE_TYPING.md section 8 (simulated gain: normal 86->95, sloppy 72->89 % first guess) + TASKS AU2a-d ticked; AU1 hot corner code + HotCornerTest done

## Do this next
AU3 first: write research/MINI_NAV.md page-field section after reading GeckoView 160 SessionTextInput (restartInput/showSoftInput/hideSoftInput) from the aar under scratchpad/aar; then BrowserActivity: wrap session.textInput.delegate, track field focus, route MiniNav typed/glided text via geckoView.onCreateInputConnection(EditorInfo()) commitText/deleteSurroundingText/Enter; then MiniNavTest + one emulator session for AU1+AU2+AU3 on a lab page under tools/sitelab; then bump versionCode 136 / alpha35 and ship

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    M  TASKS.md
    M  research/GLIDE_TYPING.md

## Last ten checkpoints
```
  a2b40ea ckpt 1965: alpha34 SHIPPED AND RECORDED (run 43549663530 green, Release kestrel-g-v0.3.0
  937563e ckpt 1958: Owner message 35 written into TASKS (AU1 invisible long-press corner instead 
  7898087 ckpt 1954: alpha34 (gecko code 135) DISPATCHED: android.yml run 43549663530 (run #85, st
  0a7c905 ckpt 1953: gated kestrel-g-v0.3.0-alpha34 (gecko code 135) and pushed it: handoff self-t
  eedfd73 ckpt 1949: AT1h emulator proof DONE with a real multi-point finger (sendevent-style bina
  58ae170 ckpt 1945: AT1e-g DONE and green: glide typing in the mini keyboard (KeysView glide vs t
  54eae48 ckpt 1931: AT1e core half done and green (45 tests in core input): MiniKeyboardModel.com
  7aca167 ckpt 1925: AT1c/d: tests written and green: LexiconFileTest 6, GlideDecoderTest 15 (tiny
  5ae6905 ckpt 1920: AT1b/c/d first cut works: MIT symspell word list compiled to geckoapp/src/mai
  b547793 ckpt 1898: Owner message 34 written into TASKS (AT1 glide typing on the mini keyboard, b
```

(12 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
