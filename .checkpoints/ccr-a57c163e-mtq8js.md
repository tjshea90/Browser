# CHECKPOINT 1511 — read me first, then TASKS.md

**Written:** 2026-11-13T07:06:38Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `9252bef` (this checkpoint is the commit after it)

## Just done
AH1b DONE and emulator-proven with the real engine (heads-up before a refused download, Clear cache 24 MB -> 0.1 MB and +23.5 MB free, doomed cache files unlinked at once and at each engine start), AH1e answered (pelisjuanita.com public part is a login page); next: bump versionCode 128 / 0.3.0-alpha29, run the gates with ship.sh, dispatch, record, answer the owner

## Do this next
bump geckoapp/build.gradle.kts to versionCode 128 + versionName 0.3.0-alpha29, then bash ship.sh gecko with the note from scratchpad note29.txt (alpha29 text drafted in the TASKS AH1f line), dispatch android.yml app=gecko full_build=true on ccr-a57c163e-mtq8js, wait for tag kestrel-g-v0.3.0-alpha29 + green run, get_release_by_tag, bash tools/record-release.sh, then answer the owner (storage first: 1.62 GB free vs about 3.1 GB needed, how to free about 1.5 GB, install the newest Release, what alpha29 fixes; re-list open items)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  11126ad ckpt 1495: AH1c done and emulator-proven end to end (Shields master switch off -> Firefo
  939a120 ckpt 1487: AH1d done and emulator-proven: the Video line stayed on 'decoders unknown' be
  7d3044a ckpt 1482: Owner message 21 (their Diagnostics copy from the tablet, alpha27) written in
  29bfbb4 ckpt 1478: handoff saved (.handoff/handoff.md + archive 2026-11-13T05-01-56Z; CLAUDE.md/
  2e2935d ckpt 1476: alpha28 SHIPPED and RECORDED: kestrel-g-v0.3.0-alpha28 (gecko code 127), run 
  ec67869 ckpt 1474: gated kestrel-g-v0.3.0-alpha28 (gecko code 127) and pushed it: handoff self-t
  a562cab ckpt 1473: alpha28 gates green: JVM (core + app + gecko compile), 123 web tests, pref na
  a3a8e82 ckpt 1470: AF1 cause found + fixed + proven in real Gecko on the emulator: Kestrel asked
  a0403ec ckpt 1458: AF1 diagnostics built (not yet compiled/shipped): extension page log (blocked
  697f62b ckpt 1451: Owner message 20 (their Video lab / Tap lab / Decode lab numbers from the tab
```

(15 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
