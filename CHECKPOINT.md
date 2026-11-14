# CHECKPOINT 2013 — read me first, then TASKS.md

**Written:** 2026-11-14T04:33:56Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `0996ada` (this checkpoint is the commit after it)

## Just done
owner message 36 written into TASKS (AV1 resize+detach the mini keyboard, AV2 research Gboard/Samsung glide + learning upgrades, AV3 polish/responsiveness); AT2 Spanish closed ('Do not include Spanish')

## Do this next
start AV1: read MiniNav panel layout (panel LinearLayout in a FrameLayout, topGap, PANEL_KEYS_W_DP, KEYS_H_DP), design geometry prefs (right/top offsets, keys width/height, detached flag) then implement corner drag + grip drag; in parallel launch a research agent for AV2a (Agent general-purpose, brief in scratchpad) and write research/GLIDE_RESEARCH.md from its report

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    M  TASKS.md

## Last ten checkpoints
```
  d4f48f8 ckpt 2010: alpha35 (kestrel-g-v0.3.0-alpha35, code 136) released and RECORDED: run 43556
  b59f884 ckpt 2005: alpha35 dispatched: android.yml queued for ref ccr-a57c163e-mtq8js (app=gecko
  072c85d ckpt 2004: gated kestrel-g-v0.3.0-alpha35 (gecko code 136) and pushed it: handoff self-t
  90d1fd1 ckpt 2002: version bump 136 / 0.3.0-alpha35 for the release of AU1-AU3
  21e5616 ckpt 1998: AU1+AU2+AU3 verified on the emulator (corner long press, page-field typing: l
  95be9ed ckpt 1994: AU3 verified on the JVM side: core 876/877 (the one failure is a download tim
  85e6e7a ckpt 1990: AU3 code: TextEdit (core, 7 tests), PageField interface + MiniNav field mode 
  4ad5eb4 ckpt 1979: AU3 research written (research/MINI_NAV.md): GeckoView 160 input path read fr
  543c7ef ckpt 1978: AU2 learning keyboard: PersonalModel tests (11) + MiniKeyboard typedPairs tes
  a2b40ea ckpt 1965: alpha34 SHIPPED AND RECORDED (run 43549663530 green, Release kestrel-g-v0.3.0
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
