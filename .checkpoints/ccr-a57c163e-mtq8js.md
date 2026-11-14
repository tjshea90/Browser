# CHECKPOINT 1979 — read me first, then TASKS.md

**Written:** 2026-11-14T03:30:48Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `543c7ef` (this checkpoint is the commit after it)

## Just done
AU3 research written (research/MINI_NAV.md): GeckoView 160 input path read from the AAR (default delegate, onCreateInputConnection probe, ic.handler thread rule, private flag) and the design (PageFieldInput wrapper, PageField interface, field mode in MiniNav)

## Do this next
AU3 code: (1) core/input/TextEdit.kt diff helper + test; (2) app/ui/PageField.kt interface + MiniNav.Listener.navPageField/navPrivateTab/navKeyboardChanged + field mode in MiniNav (typed/openKeyboard/closeKeyboard/BarView tag+placeholder+dots) + MiniNavTest with a fake PageField; (3) geckoapp PageFieldInput.kt wrapper + createSession hook + activity listener; then lab page tools/sitelab/field.html and one emulator session (free disk first: delete avd userdata-qemu.img.qcow2 and boot -wipe-data)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    M  research/MINI_NAV.md

## Last ten checkpoints
```
  543c7ef ckpt 1978: AU2 learning keyboard: PersonalModel tests (11) + MiniKeyboard typedPairs tes
  a2b40ea ckpt 1965: alpha34 SHIPPED AND RECORDED (run 43549663530 green, Release kestrel-g-v0.3.0
  937563e ckpt 1958: Owner message 35 written into TASKS (AU1 invisible long-press corner instead 
  7898087 ckpt 1954: alpha34 (gecko code 135) DISPATCHED: android.yml run 43549663530 (run #85, st
  0a7c905 ckpt 1953: gated kestrel-g-v0.3.0-alpha34 (gecko code 135) and pushed it: handoff self-t
  eedfd73 ckpt 1949: AT1h emulator proof DONE with a real multi-point finger (sendevent-style bina
  58ae170 ckpt 1945: AT1e-g DONE and green: glide typing in the mini keyboard (KeysView glide vs t
  54eae48 ckpt 1931: AT1e core half done and green (45 tests in core input): MiniKeyboardModel.com
  7aca167 ckpt 1925: AT1c/d: tests written and green: LexiconFileTest 6, GlideDecoderTest 15 (tiny
  5ae6905 ckpt 1920: AT1b/c/d first cut works: MIT symspell word list compiled to geckoapp/src/mai
```
