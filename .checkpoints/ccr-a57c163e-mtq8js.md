# CHECKPOINT 555 — read me first, then TASKS.md

**Written:** 2026-11-11T22:29:18Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `0495ffc` (this checkpoint is the commit after it)

## Just done
alpha12 SHIPPED and recorded (run 43111148506 green, BUILDLOG written): https://github.com/tjshea90/Browser/releases/tag/kestrel-g-v0.2.0-alpha12 . D1 DESIGN written to research/D1-DESIGN.md (cfg per main_frame, pull-protocol image transfer, KEX1 little-endian image written by :core from the loaded FilterSet, JS engine = port of FilterSet.check, legacy req path as start-up fallback + kill switch, Diagnostics numbers). D1 claimed.

## Do this next
Owner still has to HEAR about alpha12 (reply with the Release link on its own line + honest caveats + open items) if not already sent. Then implement D1 in this order, ckpt after each: (1) core/.../filter/ExtImage.kt (KEX1 encoder + Psl.tables() export) + ExtImageTest that also writes web-tests/fixtures/mini.kex + mini.corpus.tsv; (2) geckoapp/src/main/assets/kestrel-shields/engine.js + web-tests/engine.test.js (hash vectors, glob, PSL, 100% corpus equality); (3) new background.js (sync listener, cfg, image pull, counters, st) + manifest scripts order; (4) ShieldsBridge cfg/img/cnt(c[])/st handlers + Engine.generation + Prefs.engineInExtension + Diagnostics row; (5) emulator check with ads.html (3/3 blocked, req count ~0) and legacy-mode A/B; (6) alpha13 via ship.sh gecko.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  66aa048 ckpt 551: alpha12 DISPATCHED on GitHub Actions: run 43111148506 (android.yml, head 77eac
  77eac10 ckpt 550: gated kestrel-g-v0.2.0-alpha12 (gecko code 111) and pushed it: handoff self-te
  e82c7f6 ckpt 543: DASH + merge DONE and emulator-verified: core/media/Dash.kt (MPD parser: Segme
  5220430 ckpt 528: TS->MP4 (Remuxer + ADTS->raw AAC frames with an exact AudioClock; verified on 
  bb6a984 ckpt 523: alpha11 SHIPPED and recorded: https://github.com/tjshea90/Browser/releases/tag
  5d55a7b ckpt 513: alpha11 DISPATCHED: run 43101503028 (android.yml, head 6d529ad, 21:15Z). Gate 
  6d529ad ckpt 512: gated kestrel-g-v0.2.0-alpha11 (gecko code 110) and pushed it: handoff self-te
  db137ac ckpt 509: V4/V5 P1 VERIFIED on the emulator: HLS streams via the Download video sheet. M
  696b6f0 ckpt 501: V4 progress: P0 (direct files) WORKS on the emulator (sniffer in kestrel-shiel
  307128f ckpt 487: alpha10 SHIPPED and recorded: Release https://github.com/tjshea90/Browser/rele
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
