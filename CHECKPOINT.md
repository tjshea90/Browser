# CHECKPOINT 597 — read me first, then TASKS.md

**Written:** 2026-11-12T00:07:02Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `7264efb` (this checkpoint is the commit after it)

## Just done
alpha13 SHIPPED and recorded (run 43123971432 green, BUILDLOG written): https://github.com/tjshea90/Browser/releases/tag/kestrel-g-v0.2.0-alpha13 . D1 (blocking decisions inside the page engine) is in the owner's hands; tablet numbers come via Diagnostics > Update > Copy (Blocker line).

## Do this next
Make sure the owner HEARS about alpha13 (Release link on its own line, plain-words changes, honest caveats, open items). Then continue the V7/N7 backlog, one item at a time with ckpt each: (1) emulator is degraded (System UI ANR, 7 stale tabs): soft-restarted once; for UI work prefer force-stop + start and one tab; (2) tab-grid swipe-to-close; (3) Settings restyle (tile rows like the menu); (4) P5 rest (cold-start ordering: dock/Home agreement for restored page tabs); (5) S3 suggestion row reuse, S5 keyed tab-grid update; (6) V3b download-only process (design in TASKS.md); (7) U1c Back up / Restore only if updates still fail.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  e860a70 ckpt 595: alpha13 DISPATCHED on GitHub Actions: run 43123971432 (android.yml, head 19e3a
  19e3a6d ckpt 594: gated kestrel-g-v0.2.0-alpha13 (gecko code 112) and pushed it: handoff self-te
  16223f8 ckpt 592: D1 code complete and verified in unit tests: real-lists differential (63,378 r
  4316802 ckpt 571: D1 steps 1+2 DONE and verified: core/.../filter/ExtImage.kt (KEX1 little-endia
  6e451c7 ckpt 555: alpha12 SHIPPED and recorded (run 43111148506 green, BUILDLOG written): https:
  66aa048 ckpt 551: alpha12 DISPATCHED on GitHub Actions: run 43111148506 (android.yml, head 77eac
  77eac10 ckpt 550: gated kestrel-g-v0.2.0-alpha12 (gecko code 111) and pushed it: handoff self-te
  e82c7f6 ckpt 543: DASH + merge DONE and emulator-verified: core/media/Dash.kt (MPD parser: Segme
  5220430 ckpt 528: TS->MP4 (Remuxer + ADTS->raw AAC frames with an exact AudioClock; verified on 
  bb6a984 ckpt 523: alpha11 SHIPPED and recorded: https://github.com/tjshea90/Browser/releases/tag
```
