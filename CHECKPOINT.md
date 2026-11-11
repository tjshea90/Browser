# CHECKPOINT 592 — read me first, then TASKS.md

**Written:** 2026-11-11T23:43:55Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `7538353` (this checkpoint is the commit after it)

## Just done
D1 code complete and verified in unit tests: real-lists differential (63,378 requests from default.kfs, 0 mismatches, 7.07 us/check on x86 V8, image load+verify+warm-up 23.5 ms), 22 JS tests (engine 9 + background 13), core+app suites green. Emulator A/B (noisy, emulator degraded: System UI ANR): fast mode 38-44 s vs compat 45-51 s for a 300-request page in the first runs; engine path = 4.9 ms/check on the emulator, legacy path held first requests 3 s each at cold start. Provisional cfg (gen 0) + saved copy, top-frame-only generic CSS, PSL ASCII fast path, chunkReply extracted+tested, 60 s cfg TTL refresh added.

## Do this next
Final functional pass on the emulator with the LATEST code: ./gradlew :geckoapp:assembleDebug -PgeckoAbi=x86_64, adb install -r, pm clear, launch, load ads.html (blocked 3/3?), force-stop + relaunch (saved copy: 0 transfers, first page judged in engine), Diagnostics Blocker line. Then bump geckoapp versionCode 112 / 0.2.0-alpha13, bash ship.sh gecko "note", dispatch android.yml, wait, record-release.sh, tell the owner (release link, what changed, honest caveats: emulator only, tablet numbers via Diagnostics > Blocker > Copy).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  4316802 ckpt 571: D1 steps 1+2 DONE and verified: core/.../filter/ExtImage.kt (KEX1 little-endia
  6e451c7 ckpt 555: alpha12 SHIPPED and recorded (run 43111148506 green, BUILDLOG written): https:
  66aa048 ckpt 551: alpha12 DISPATCHED on GitHub Actions: run 43111148506 (android.yml, head 77eac
  77eac10 ckpt 550: gated kestrel-g-v0.2.0-alpha12 (gecko code 111) and pushed it: handoff self-te
  e82c7f6 ckpt 543: DASH + merge DONE and emulator-verified: core/media/Dash.kt (MPD parser: Segme
  5220430 ckpt 528: TS->MP4 (Remuxer + ADTS->raw AAC frames with an exact AudioClock; verified on 
  bb6a984 ckpt 523: alpha11 SHIPPED and recorded: https://github.com/tjshea90/Browser/releases/tag
  5d55a7b ckpt 513: alpha11 DISPATCHED: run 43101503028 (android.yml, head 6d529ad, 21:15Z). Gate 
  6d529ad ckpt 512: gated kestrel-g-v0.2.0-alpha11 (gecko code 110) and pushed it: handoff self-te
  db137ac ckpt 509: V4/V5 P1 VERIFIED on the emulator: HLS streams via the Download video sheet. M
```

(20 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
