# CHECKPOINT 2033 — read me first, then TASKS.md

**Written:** 2026-11-14T04:59:04Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `1df9a5d` (this checkpoint is the commit after it)

## Just done
AV2/AV3 core: PersonalModel now remembers corrections (guessed -> meant), the last TWO words, and a short recency memory (all saved, old files still load); decoder uses them (confGain, prev2, recentMass); smart spacing after a glide (doraby + .com = doraby.com, punctuation closes the word); MiniNav sends two-word context, learns corrections from picks (1.0) and deletions (0.5), takes words from visited page titles; panel padding bug (inset background reset it) fixed; tests: core glide 44, MiniKeyboardTest 25, MiniNavTest 56 all pass. Simulation (normal finger, 70 % own phrases): learning 90.3/94.0/94.7/93.7/95.0/96.3 baseline; corrections +0.3, two-word and recency ~0 on this synthetic person (it repeats a small phrase set), numbers in scratchpad abl.txt

## Do this next
(1) emulator proof of AV1 with the fixed padding build: scratchpad emu helpers, install x86_64 apk (adb install) then drag grip / corners with input swipe, screenshots; (2) when the research agent ab679ad8ef33678f3 reports: write research/GLIDE_RESEARCH.md; (3) then AV3a live word while gliding (GlideTyping.decodePreview + strip), key preview bubble, caps lock, long-press alternates; (4) per-key thumb centres only if research supports; ship alpha36

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  411b5fb ckpt 2020: AV1 code: MiniNav panel geometry (grip drag detaches/moves with dock snap + d
  2287b4e ckpt 2013: owner message 36 written into TASKS (AV1 resize+detach the mini keyboard, AV2
  d4f48f8 ckpt 2010: alpha35 (kestrel-g-v0.3.0-alpha35, code 136) released and RECORDED: run 43556
  b59f884 ckpt 2005: alpha35 dispatched: android.yml queued for ref ccr-a57c163e-mtq8js (app=gecko
  072c85d ckpt 2004: gated kestrel-g-v0.3.0-alpha35 (gecko code 136) and pushed it: handoff self-t
  90d1fd1 ckpt 2002: version bump 136 / 0.3.0-alpha35 for the release of AU1-AU3
  21e5616 ckpt 1998: AU1+AU2+AU3 verified on the emulator (corner long press, page-field typing: l
  95be9ed ckpt 1994: AU3 verified on the JVM side: core 876/877 (the one failure is a download tim
  85e6e7a ckpt 1990: AU3 code: TextEdit (core, 7 tests), PageField interface + MiniNav field mode 
  4ad5eb4 ckpt 1979: AU3 research written (research/MINI_NAV.md): GeckoView 160 input path read fr
```

(12 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
