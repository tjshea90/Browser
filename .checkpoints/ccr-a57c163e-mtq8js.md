# CHECKPOINT 2020 — read me first, then TASKS.md

**Written:** 2026-11-14T04:41:57Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `9de48ee` (this checkpoint is the commit after it)

## Just done
AV1 code: MiniNav panel geometry (grip drag detaches/moves with dock snap + double-tap dock, four corner marks resize the open keyboard with limits, transparent band passes touches to the page), HotCorner.covers guard, prefs miniGeom, Settings reset button, activity wiring (MiniNav now fills the page area); MiniNavTest 51 + HotCornerTest 14 pass

## Do this next
emulator proof of AV1: build ./gradlew :geckoapp:assembleDebug -PgeckoAbi=x86_64, boot scratchpad boot_wipe.sh (avd kestrel29x64), install, long press corner, drag grip, drag corners, screenshots; then AV2b/AV3 code in core (per-key thumb centres from taps, confusion learning, recency, trigram) while the research agent report (agent ab679ad8ef33678f3) arrives -> write research/GLIDE_RESEARCH.md

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  2287b4e ckpt 2013: owner message 36 written into TASKS (AV1 resize+detach the mini keyboard, AV2
  d4f48f8 ckpt 2010: alpha35 (kestrel-g-v0.3.0-alpha35, code 136) released and RECORDED: run 43556
  b59f884 ckpt 2005: alpha35 dispatched: android.yml queued for ref ccr-a57c163e-mtq8js (app=gecko
  072c85d ckpt 2004: gated kestrel-g-v0.3.0-alpha35 (gecko code 136) and pushed it: handoff self-t
  90d1fd1 ckpt 2002: version bump 136 / 0.3.0-alpha35 for the release of AU1-AU3
  21e5616 ckpt 1998: AU1+AU2+AU3 verified on the emulator (corner long press, page-field typing: l
  95be9ed ckpt 1994: AU3 verified on the JVM side: core 876/877 (the one failure is a download tim
  85e6e7a ckpt 1990: AU3 code: TextEdit (core, 7 tests), PageField interface + MiniNav field mode 
  4ad5eb4 ckpt 1979: AU3 research written (research/MINI_NAV.md): GeckoView 160 input path read fr
  543c7ef ckpt 1978: AU2 learning keyboard: PersonalModel tests (11) + MiniKeyboard typedPairs tes
```

(6 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
