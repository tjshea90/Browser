# CHECKPOINT 1990 — read me first, then TASKS.md

**Written:** 2026-11-14T03:37:06Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `602545c` (this checkpoint is the commit after it)

## Just done
AU3 code: TextEdit (core, 7 tests), PageField interface + MiniNav field mode (15 new MiniNavTest tests, 38 pass), geckoapp PageFieldInput wrapper hooked into createSession + activity listener (navPageField/navKeyboardChanged/navPrivateTab); both builds compile

## Do this next
run the full suite in background (./gradlew :core:test :app:testDebugUnitTest :geckoapp:compileDebugKotlin :app:lintDebug, log in scratchpad full.log); meanwhile write the lab page tools/sitelab/field.html (input in a form, search input, textarea, password, standalone input; logs typed values to the lab server) and prepare the emulator: free disk (delete avd userdata-qemu.img.qcow2), set hw.keyboard=no in the AVD config, boot -wipe-data, build debug geckoapp APK, install, and do one proof session for AU1+AU2+AU3

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  4ad5eb4 ckpt 1979: AU3 research written (research/MINI_NAV.md): GeckoView 160 input path read fr
  543c7ef ckpt 1978: AU2 learning keyboard: PersonalModel tests (11) + MiniKeyboard typedPairs tes
  a2b40ea ckpt 1965: alpha34 SHIPPED AND RECORDED (run 43549663530 green, Release kestrel-g-v0.3.0
  937563e ckpt 1958: Owner message 35 written into TASKS (AU1 invisible long-press corner instead 
  7898087 ckpt 1954: alpha34 (gecko code 135) DISPATCHED: android.yml run 43549663530 (run #85, st
  0a7c905 ckpt 1953: gated kestrel-g-v0.3.0-alpha34 (gecko code 135) and pushed it: handoff self-t
  eedfd73 ckpt 1949: AT1h emulator proof DONE with a real multi-point finger (sendevent-style bina
  58ae170 ckpt 1945: AT1e-g DONE and green: glide typing in the mini keyboard (KeysView glide vs t
  54eae48 ckpt 1931: AT1e core half done and green (45 tests in core input): MiniKeyboardModel.com
  7aca167 ckpt 1925: AT1c/d: tests written and green: LexiconFileTest 6, GlideDecoderTest 15 (tiny
```

(10 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
