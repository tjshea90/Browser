# CHECKPOINT 600 — read me first, then TASKS.md

**Written:** 2026-11-12T00:10:32Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `1a17e69` (this checkpoint is the commit after it)

## Just done
owner request 9 written into TASKS.md as X1-X10 in the owner's words (INBOX has it verbatim). alpha13 shipped and recorded just before.

## Do this next
Start with X1 (updater says up to date): read core/.../update/UpdateLogic.kt + app/.../update/UpdateManager.kt, find why alpha9 (and later) say up to date while newer releases exist (suspect lexical ordering alpha9 > alpha10, /releases/latest ignoring prereleases, ETag/304), reproduce in a unit test with the real tag/versionName/versionCode strings, fix, then X6/X7/X8/X9/X10 (popup+redirect guard, DOM scan so video can be found without pressing play, long-press and a floating Download chip, MP4 not .ts), then X2 (daily check dialog for app + engine + ad-block lists), X3 (download manager research + upgrades), X5 (ad-block assessment + CI snapshot auto-update), X4 (cold start: baseline profile etc.).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  f1f1d48 ckpt 597: alpha13 SHIPPED and recorded (run 43123971432 green, BUILDLOG written): https:
  e860a70 ckpt 595: alpha13 DISPATCHED on GitHub Actions: run 43123971432 (android.yml, head 19e3a
  19e3a6d ckpt 594: gated kestrel-g-v0.2.0-alpha13 (gecko code 112) and pushed it: handoff self-te
  16223f8 ckpt 592: D1 code complete and verified in unit tests: real-lists differential (63,378 r
  4316802 ckpt 571: D1 steps 1+2 DONE and verified: core/.../filter/ExtImage.kt (KEX1 little-endia
  6e451c7 ckpt 555: alpha12 SHIPPED and recorded (run 43111148506 green, BUILDLOG written): https:
  66aa048 ckpt 551: alpha12 DISPATCHED on GitHub Actions: run 43111148506 (android.yml, head 77eac
  77eac10 ckpt 550: gated kestrel-g-v0.2.0-alpha12 (gecko code 111) and pushed it: handoff self-te
  e82c7f6 ckpt 543: DASH + merge DONE and emulator-verified: core/media/Dash.kt (MPD parser: Segme
  5220430 ckpt 528: TS->MP4 (Remuxer + ADTS->raw AAC frames with an exact AudioClock; verified on 
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
