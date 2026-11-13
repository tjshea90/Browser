# CHECKPOINT 1531 — read me first, then TASKS.md

**Written:** 2026-11-13T08:59:08Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `9fddec1` (this checkpoint is the commit after it)

## Just done
AI1a + AI1b EMULATOR-PROVEN (Settings in the private-window menu; long-press Open in new tab opens a background private tab and the foreground stays), AI1c analysed (Firefox Content list names cloudflare.com + jsdelivr; our lists do not block the Doraby players or Turnstile: regression test added)

## Do this next
rebuild the debug APK with showSettings skin rebuild, check Settings opens in a private window and again in a regular one (emulator, slow: manual steps, tapxy 557 892 for the menu), then bash ship.sh gecko with the alpha30 note (bump geckoapp versionCode 129 / 0.3.0-alpha30), dispatch, record, then answer the owner (all three items + AI1d question + open items) and save the handoff with the unanswered-prompt list

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  53ae8c1 ckpt 1525: new context window (session restart): the owner re-sent items 1-3 of message 
  5930d95 ckpt 1519: handoff saved (.handoff/handoff.md; CLAUDE.md/AGENTS.md Session Learnings upd
  5c37df1 ckpt 1518: alpha29 SHIPPED and RECORDED (run 43383744581 green, BUILDLOG line written, R
  01ea47c ckpt 1515: alpha29 DISPATCHED: android.yml run 43383744581 (run_number 79) started 07:09
  78c9506 ckpt 1514: gated kestrel-g-v0.3.0-alpha29 (gecko code 128) and pushed it: handoff self-t
  f2230eb ckpt 1511: AH1b DONE and emulator-proven with the real engine (heads-up before a refused
  11126ad ckpt 1495: AH1c done and emulator-proven end to end (Shields master switch off -> Firefo
  939a120 ckpt 1487: AH1d done and emulator-proven: the Video line stayed on 'decoders unknown' be
  7d3044a ckpt 1482: Owner message 21 (their Diagnostics copy from the tablet, alpha27) written in
  29bfbb4 ckpt 1478: handoff saved (.handoff/handoff.md + archive 2026-11-13T05-01-56Z; CLAUDE.md/
```

(5 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
