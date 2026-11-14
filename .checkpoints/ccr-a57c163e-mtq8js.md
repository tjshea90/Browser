# CHECKPOINT 2002 — read me first, then TASKS.md

**Written:** 2026-11-14T04:19:33Z · **tests:** 1 RED: test_handoff.sh (0 green)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `5d36cf8` (this checkpoint is the commit after it)

## Just done
version bump 136 / 0.3.0-alpha35 for the release of AU1-AU3

## Do this next
bash ship.sh gecko with scratchpad/alpha35_note.txt (runs the gates + unit suites + pushes), then dispatch android.yml app=gecko full_build=true ref ccr-a57c163e-mtq8js, wait, get_release_by_tag, record-release, answer the owner

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    M  geckoapp/build.gradle.kts

## Last ten checkpoints
```
  21e5616 ckpt 1998: AU1+AU2+AU3 verified on the emulator (corner long press, page-field typing: l
  95be9ed ckpt 1994: AU3 verified on the JVM side: core 876/877 (the one failure is a download tim
  85e6e7a ckpt 1990: AU3 code: TextEdit (core, 7 tests), PageField interface + MiniNav field mode 
  4ad5eb4 ckpt 1979: AU3 research written (research/MINI_NAV.md): GeckoView 160 input path read fr
  543c7ef ckpt 1978: AU2 learning keyboard: PersonalModel tests (11) + MiniKeyboard typedPairs tes
  a2b40ea ckpt 1965: alpha34 SHIPPED AND RECORDED (run 43549663530 green, Release kestrel-g-v0.3.0
  937563e ckpt 1958: Owner message 35 written into TASKS (AU1 invisible long-press corner instead 
  7898087 ckpt 1954: alpha34 (gecko code 135) DISPATCHED: android.yml run 43549663530 (run #85, st
  0a7c905 ckpt 1953: gated kestrel-g-v0.3.0-alpha34 (gecko code 135) and pushed it: handoff self-t
  eedfd73 ckpt 1949: AT1h emulator proof DONE with a real multi-point finger (sendevent-style bina
```

(3 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
