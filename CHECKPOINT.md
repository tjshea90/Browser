# CHECKPOINT 2968 — read me first, then TASKS.md

**Written:** 2026-11-22T07:24:57Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-144aedec-7apwin` · **builds on:** `3736b46d` (this checkpoint is the commit after it)

## Just done
PERF8 code (not released): (a) VideoFlow.offerSiblings gives the player that is up the film's other lists after every playlist look (the late hand-over started before the sibling lists were read: no quality button), PlayerQuality.updateAlts, NativePlayerActivity.offerAlts + live ref, InlinePlayer.offerAlts, lookAsap on a declined press, a look pass reads up to 6 lists; (b) new page script calm.js (turns the stuck loading circle of a paused never-played video into a play mark, reports 'video calm <element> <size> <parent clip cutBottom>' via verr code 98) + web-tests/calm.test.js 5 green + background.test.js updated (130 green); (c) GeckoEngine.logDrawPrefs 'engine draw-prefs' line 20 s after start; OwnerRulesTest.perf8 pin; compile + OwnerRulesTest green

## Do this next
Next: wait for web-tests full run (w1.log in scratchpad, EXIT line), then versionCode 194 / alpha93: write docs/handoff/ALPHA93_RELEASE_NOTE.txt, bash ship.sh gecko with the note, ONE dispatch, tag + jobs + get_release_by_tag, record-release.sh with the identical note, tick PERF8a (alts late) as shipped-not-confirmed, PERF8b calm.js shipped-not-confirmed (the next upload line 'video calm' names the circle element and whether a parent clips it), PERF8c draw-prefs line; then answer the owner

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  06774fc1 ckpt 2959: PERF8 written to TASKS.md (owner after alpha92: fullscreen Kestrel player wo
  f41f8f8b ckpt 2958: alpha92 RELEASED AND RECORDED: kestrel-g-v0.3.0-alpha92 (gecko code 193, run
  292067e6 ckpt 2955: alpha92 (gecko code 193) gated by ship.sh (core+app suites, lint, handoff se
  df0b4a8a ckpt 2954: gated kestrel-g-v0.3.0-alpha92 (gecko code 193) and pushed it: handoff self-
  2db9f72a ckpt 2952: pre-ship: alpha92: quality choices for pornhub in Kestrel's player, and a Sa
  ee6c59f9 ckpt 2938: PERF7c step 1: Settings > Network lab 'Safe drawing' (lab_draw, default ON) 
  449480c7 ckpt 2933: PERF7 started on branch ccr-144aedec-7apwin: owner request + 2 screenshots w
  4da54c18 ckpt 2915: alpha91 RELEASED and recorded (run 44987313373 green, Release has the APK). 
  a42e5268 ckpt 2913: alpha91 (code 192) gated (ship.sh green on attempt 3) and ONE dispatch made 
  df3d34d1 ckpt 2912: gated kestrel-g-v0.3.0-alpha91 (gecko code 192) and pushed it: handoff self-
```

(8 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
