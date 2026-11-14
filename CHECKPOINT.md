# CHECKPOINT 1994 — read me first, then TASKS.md

**Written:** 2026-11-14T03:42:01Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `77c252b` (this checkpoint is the commit after it)

## Just done
AU3 verified on the JVM side: core 876/877 (the one failure is a download timing test that passes alone: flaky under load), app 258 pass, lint + gecko compile green; lab page tools/sitelab/fieldlab.py written; debug APK built with PageFieldInput; emulator booting with wipe-data

## Do this next
emulator proof for AU1+AU2+AU3: when sys.boot_completed (scratchpad boot_wipe.log says BOOTED) run scratchpad au_flow.sh (to write): install geckoapp-debug.apk, start lab server (python3 tools/sitelab/fieldlab.py 8000 scratchpad/fieldlab.log is already running), open http://10.0.2.2:8000/, long-press the top-right corner (input swipe 580 40 580 40 700), tap a field, open the keyboard bar, type/glide, press Go, check fieldlab.log + screenshots; write results into research/MINI_NAV.md + research/GLIDE_TYPING.md; tick AU1b/AU2/AU3 in TASKS; then bump versionCode 136 / alpha35 and ship

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  85e6e7a ckpt 1990: AU3 code: TextEdit (core, 7 tests), PageField interface + MiniNav field mode 
  4ad5eb4 ckpt 1979: AU3 research written (research/MINI_NAV.md): GeckoView 160 input path read fr
  543c7ef ckpt 1978: AU2 learning keyboard: PersonalModel tests (11) + MiniKeyboard typedPairs tes
  a2b40ea ckpt 1965: alpha34 SHIPPED AND RECORDED (run 43549663530 green, Release kestrel-g-v0.3.0
  937563e ckpt 1958: Owner message 35 written into TASKS (AU1 invisible long-press corner instead 
  7898087 ckpt 1954: alpha34 (gecko code 135) DISPATCHED: android.yml run 43549663530 (run #85, st
  0a7c905 ckpt 1953: gated kestrel-g-v0.3.0-alpha34 (gecko code 135) and pushed it: handoff self-t
  eedfd73 ckpt 1949: AT1h emulator proof DONE with a real multi-point finger (sendevent-style bina
  58ae170 ckpt 1945: AT1e-g DONE and green: glide typing in the mini keyboard (KeysView glide vs t
  54eae48 ckpt 1931: AT1e core half done and green (45 tests in core input): MiniKeyboardModel.com
```

(3 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
