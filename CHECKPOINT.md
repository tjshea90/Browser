# CHECKPOINT 1965 — read me first, then TASKS.md

**Written:** 2026-11-14T03:12:44Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `b737cdb` (this checkpoint is the commit after it)

## Just done
alpha34 SHIPPED AND RECORDED (run 43549663530 green, Release kestrel-g-v0.3.0-alpha34, APK 93,857,868 B). AT1 ticked. AU1 in progress in the tree: ToggleView removed from MiniNav (toggle() instead), new ui/HotCorner.kt (invisible 72 dp square observed in BrowserActivity.dispatchTouchEvent, long press 380 ms, cancels the page's touch, toggles the panel; the panel hangs below the square), Settings row relabelled; compiles.

## Do this next
AU1: update MiniNavTest (the toggle tests use nav.pressToggleForTest() = toggle(), check they still pass) + new HotCornerTest (Robolectric: long press in the square fires once, a tap or a drag does not, a second finger cancels, an event outside the square never arms, the rest of the touch is swallowed after firing, zone()==null disables) ; then emulator proof with a stationary swipe (adb shell input swipe x y x y 700) at the top right corner, screenshots; then AU2 (PersonalModel), AU3 (page text field). Version for this batch = alpha35 (versionCode 136) - bump only when shipping. The owner has NOT yet been told that alpha34 is out (interim answer is next).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  937563e ckpt 1958: Owner message 35 written into TASKS (AU1 invisible long-press corner instead 
  7898087 ckpt 1954: alpha34 (gecko code 135) DISPATCHED: android.yml run 43549663530 (run #85, st
  0a7c905 ckpt 1953: gated kestrel-g-v0.3.0-alpha34 (gecko code 135) and pushed it: handoff self-t
  eedfd73 ckpt 1949: AT1h emulator proof DONE with a real multi-point finger (sendevent-style bina
  58ae170 ckpt 1945: AT1e-g DONE and green: glide typing in the mini keyboard (KeysView glide vs t
  54eae48 ckpt 1931: AT1e core half done and green (45 tests in core input): MiniKeyboardModel.com
  7aca167 ckpt 1925: AT1c/d: tests written and green: LexiconFileTest 6, GlideDecoderTest 15 (tiny
  5ae6905 ckpt 1920: AT1b/c/d first cut works: MIT symspell word list compiled to geckoapp/src/mai
  b547793 ckpt 1898: Owner message 34 written into TASKS (AT1 glide typing on the mini keyboard, b
  307f5cf ckpt 1883: alpha33 SHIPPED AND RECORDED: run 43536066565 green (all jobs success, pref c
```

(5 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
