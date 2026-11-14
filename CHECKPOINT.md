# CHECKPOINT 1998 — read me first, then TASKS.md

**Written:** 2026-11-14T04:18:37Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `7f51110` (this checkpoint is the commit after it)

## Just done
AU1+AU2+AU3 verified on the emulator (corner long press, page-field typing: letters/glide/swap/backspace-word/Go/textarea/password, learning file + Diagnostics lines); HotCorner now also trusts the touch's own timestamps (late events); picked guess learned only once kept; field Diagnostics line; docs + TASKS ticked

## Do this next
ship alpha35: bump geckoapp/build.gradle.kts versionCode 136 / 0.3.0-alpha35, run the full JVM suite once more (core + app + lint), bash ship.sh gecko with the note in scratchpad/alpha35_note.txt, dispatch android.yml (app=gecko full_build=true ref ccr-a57c163e-mtq8js), wait for tag + green run, get_release_by_tag, bash tools/record-release.sh, then answer the owner in plain words (messages 34-35: AU1-AU3 results, what was only emulator-tested, re-list open items)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    M  TASKS.md
    M  research/GLIDE_TYPING.md
    M  research/MINI_NAV.md

## Last ten checkpoints
```
  95be9ed ckpt 1994: AU3 verified on the JVM side: core 876/877 (the one failure is a download tim
  85e6e7a ckpt 1990: AU3 code: TextEdit (core, 7 tests), PageField interface + MiniNav field mode 
  4ad5eb4 ckpt 1979: AU3 research written (research/MINI_NAV.md): GeckoView 160 input path read fr
  543c7ef ckpt 1978: AU2 learning keyboard: PersonalModel tests (11) + MiniKeyboard typedPairs tes
  a2b40ea ckpt 1965: alpha34 SHIPPED AND RECORDED (run 43549663530 green, Release kestrel-g-v0.3.0
  937563e ckpt 1958: Owner message 35 written into TASKS (AU1 invisible long-press corner instead 
  7898087 ckpt 1954: alpha34 (gecko code 135) DISPATCHED: android.yml run 43549663530 (run #85, st
  0a7c905 ckpt 1953: gated kestrel-g-v0.3.0-alpha34 (gecko code 135) and pushed it: handoff self-t
  eedfd73 ckpt 1949: AT1h emulator proof DONE with a real multi-point finger (sendevent-style bina
  58ae170 ckpt 1945: AT1e-g DONE and green: glide typing in the mini keyboard (KeysView glide vs t
```

(3 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
