# CHECKPOINT 1790 — read me first, then TASKS.md

**Written:** 2026-11-13T23:11:12Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `3ceabeb` (this checkpoint is the commit after it)

## Just done
AO4 DONE and ticked: emulator proof with planted files (engine start pruned the 2020 cache entry + 2020 crash dump = 2.3 MB and kept the young entry; the tidy removed the old update apk + .part = 5.0 MB and a stale preview; Settings 'Tidy storage now' removed another 3 MB apk). Not run: tablet, MediaStore saved-installer sweep on a device

## Do this next
AO2 (video download size estimates): read core/media StreamEngine.kt view() lines ~1255-1295 (total/estimated: sum of fetched/have*n, rec.estimate, media.exactBytes), Hls.kt Variant.bytesPerSecond/avgBandwidth, Dash.kt Rep.bandwidth, MediaList size(), geckoapp VideoFlow.kt:431 (p.estimate) and the quality list builder; measure the error against the lab streams (tools/sitelab/vidlab_srv.py) and write research/VIDEO_SIZES.md; improve: HLS exact sum via EXT-X-BYTERANGE/HEAD of first segments or sample N segments with HEAD/Range 0-0, average-bandwidth x duration, audio+video sum, progressive Content-Length/Range total, switch to real total once known; tests in core. Then AO1 (research/VIDEO_SMOOTHNESS.md), full suite, ship alpha33. Heartbeat trig_01LLoVKsjuTUaDn61UGP1pBN: stand down on wakes.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  6b3800f ckpt 1788: AO4 code written and green: core ApkFiles (+5 tests), app diag/StorageTidy (+
  c2b65d1 ckpt 1761: EY-1 last look ticked (emulator-proven: first-frame picture, scroll place res
  edbe131 ckpt 1750: owner message 29 recorded (AO1 smoothness audit, AO2 download size accuracy, 
  4220445 ckpt 1729: alpha32 SHIPPED and recorded (run 43514151508 green, APK 92,539,679 B vs alph
  8999097 ckpt 1719: gated kestrel-g-v0.3.0-alpha32 (gecko code 131) and pushed it: handoff self-t
  e9da0f9 ckpt 1718: EY-2 (start timeline, emulator marks + Diagnostics line, research/COLD_START.
  caaf9c8 ckpt 1700: AL1-AL6 + AM1 ticked with evidence (AL5 final emulator chain: S1->S2 dropped=
  42dbcff ckpt 1683: AL5 root cause found with a debug logcat mirror of the Health events (new: He
  37e9055 ckpt 1678: AL5: found why a script-named address in a player frame outlived a server swi
  a3bd814 ckpt 1673: report saved as research/REVIEW-EXTERNAL-2.md and its TASKS block (EY-1..EY-3
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
