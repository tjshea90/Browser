# CHECKPOINT 606 — read me first, then TASKS.md

**Written:** 2026-11-12T00:16:50Z · **tests:** 1 RED: test_handoff.sh (0 green)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `8f0f121` (this checkpoint is the commit after it)

## Just done
X1 DONE in code and tests: updater root cause found (GitHub lists releases by TAG NAME; old check read the first 8 = alpha9..alpha3, so alpha10+ invisible). New fetch: tag list + newest 4 tags in natural order + ETag-cached per-release GETs; offer recomputed against installed code; tests UpdateLogicTest (+5) and UpdateCheckTest (3, local GitHub-like server). Rescue for installed alpha9-13: versionName 0.3.0-alpha14 so tag kestrel-g-v0.3.0-alpha14 sorts inside their window. Version bumped to code 113 / 0.3.0-alpha14.

## Do this next
Gate + ship alpha14 now (small, urgent for the owner): bash ship.sh gecko "note" (note: updater fix in plain words + how it rescues old versions), dispatch android.yml, wait, then VERIFY the rescue empirically: mcp__github__list_releases page 1 must show kestrel-g-v0.3.0-alpha14 within the first 8 entries; record-release.sh after green; tell the owner. Then continue X6/X7 (popup+redirect guard, DOM scan), X8/X10 (floating Download chip, long-press), X9 (MP4).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md
     M docs/ARCHITECTURE.md
     M geckoapp/build.gradle.kts

## Last ten checkpoints
```
  6400e12 ckpt 600: owner request 9 written into TASKS.md as X1-X10 in the owner's words (INBOX ha
  f1f1d48 ckpt 597: alpha13 SHIPPED and recorded (run 43123971432 green, BUILDLOG written): https:
  e860a70 ckpt 595: alpha13 DISPATCHED on GitHub Actions: run 43123971432 (android.yml, head 19e3a
  19e3a6d ckpt 594: gated kestrel-g-v0.2.0-alpha13 (gecko code 112) and pushed it: handoff self-te
  16223f8 ckpt 592: D1 code complete and verified in unit tests: real-lists differential (63,378 r
  4316802 ckpt 571: D1 steps 1+2 DONE and verified: core/.../filter/ExtImage.kt (KEX1 little-endia
  6e451c7 ckpt 555: alpha12 SHIPPED and recorded (run 43111148506 green, BUILDLOG written): https:
  66aa048 ckpt 551: alpha12 DISPATCHED on GitHub Actions: run 43111148506 (android.yml, head 77eac
  77eac10 ckpt 550: gated kestrel-g-v0.2.0-alpha12 (gecko code 111) and pushed it: handoff self-te
  e82c7f6 ckpt 543: DASH + merge DONE and emulator-verified: core/media/Dash.kt (MPD parser: Segme
```

(5 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
