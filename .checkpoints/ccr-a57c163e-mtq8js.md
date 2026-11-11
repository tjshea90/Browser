# CHECKPOINT 571 — read me first, then TASKS.md

**Written:** 2026-11-11T22:38:14Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `e4e68af` (this checkpoint is the commit after it)

## Just done
D1 steps 1+2 DONE and verified: core/.../filter/ExtImage.kt (KEX1 little-endian image writer, Psl.tables()), core ExtImageTest (writes + checks web-tests/fixtures/mini.* ; KESTREL_REGEN=1 to regenerate), geckoapp/.../kestrel-shields/engine.js (line-by-line JS port of FilterSet.check/Psl/hostOf, 32-bit-limb FNV+murmur hash), web-tests/engine.test.js: 9 node tests pass, 2210-request corpus + 317 hash vectors + PSL + hostOf equal Kotlin 100%; a mutation harness (10 injected bugs) is caught every time (28-552 mismatches each). Speed on x86 V8 printed by the informational test.

## Do this next
D1 step 3: rewrite geckoapp/src/main/assets/kestrel-shields/background.js per research/D1-DESIGN.md (cfg per main_frame deduped per host, image pull in 256 KB chunks with setTimeout gaps, sync onBeforeRequest with engine.check, legacy 'req' fallback while no engine/mode L, counters flushed as {k:'cnt',p,c:[a,t,m,n]} every 500 ms, st stats every 5 s, 20 errors -> engine off) + manifest.json background.scripts = [engine.js, background.js]; then step 4 Kotlin ShieldsBridge cfg/img/cnt(c[])/st + Engine.generation + Prefs.engineInExtension + Diagnostics row; the ExtImage chunk cache = SoftReference.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  6e451c7 ckpt 555: alpha12 SHIPPED and recorded (run 43111148506 green, BUILDLOG written): https:
  66aa048 ckpt 551: alpha12 DISPATCHED on GitHub Actions: run 43111148506 (android.yml, head 77eac
  77eac10 ckpt 550: gated kestrel-g-v0.2.0-alpha12 (gecko code 111) and pushed it: handoff self-te
  e82c7f6 ckpt 543: DASH + merge DONE and emulator-verified: core/media/Dash.kt (MPD parser: Segme
  5220430 ckpt 528: TS->MP4 (Remuxer + ADTS->raw AAC frames with an exact AudioClock; verified on 
  bb6a984 ckpt 523: alpha11 SHIPPED and recorded: https://github.com/tjshea90/Browser/releases/tag
  5d55a7b ckpt 513: alpha11 DISPATCHED: run 43101503028 (android.yml, head 6d529ad, 21:15Z). Gate 
  6d529ad ckpt 512: gated kestrel-g-v0.2.0-alpha11 (gecko code 110) and pushed it: handoff self-te
  db137ac ckpt 509: V4/V5 P1 VERIFIED on the emulator: HLS streams via the Download video sheet. M
  696b6f0 ckpt 501: V4 progress: P0 (direct files) WORKS on the emulator (sniffer in kestrel-shiel
```

(15 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
