# CHECKPOINT 448 — read me first, then TASKS.md

**Written:** 2026-11-11T19:08:35Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `3b0bd85` (this checkpoint is the commit after it)

## Just done
alpha9 DISPATCHED: Actions run 43084348983 (gecko code 108, 19:08Z). alpha8 (code 107, run 43080438606) was green and recorded; alpha9 = corrected storage formula measured on the emulator: threshold = 10% of partition + 2 x APK + unpacked libs + ~32 MiB (release APK 95.5 MiB: ok at 1010, fails at 1000; padded to 155.5 MiB: ok at 1130, fails at 1115)

## Do this next
WAIT for run 43084348983 (background until-loop on the tag kestrel-g-v0.2.0-alpha9); when success + APK on the Release: bash tools/record-release.sh kestrel-g-v0.2.0-alpha9 '<note from ckpt 447>', ckpt, then reply to the owner (plain words: certificate checked on all 7 releases = same; real cause = Android keeps 10% free after any install and holds the new app twice while installing; reproduced on the emulator; uninstalling first frees ~1 GB so the same APK passes; what to do: free ~1.5 GB (delete old Kestrel APKs in Downloads, Settings > Storage), or one more uninstall; ask for Settings > Storage total/free to confirm). Emulator: filler files removed; relthreshold scripts in scratchpad.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  3b0bd85 ckpt 447: gated kestrel-g-v0.2.0-alpha9 (gecko code 108) and pushed it: handoff self-tes
  30794d6 ckpt 446: pre-ship: alpha9: the update screen's storage numbers now match what Android r
  e822130 ckpt 442: alpha8 build DISPATCHED: Actions run 43080438606 (android.yml, ref ccr-a57c163
  18ccff9 ckpt 441: gated kestrel-g-v0.2.0-alpha8 (gecko code 107) and pushed it: handoff self-tes
  e3285db ckpt 438: U1 cause found + fixed in code: Android needs 10% of the partition free AFTER 
  b39879a ckpt 427: Owner message after alpha7 recorded as TASKS U1: in-place updates never instal
  378cb59 ckpt 423: alpha7 SHIPPED + RECORDED (kestrel-g-v0.2.0-alpha7, gecko code 106, run 430722
  d87281f ckpt 419: alpha7 build DISPATCHED: Actions run 43072284547 (android.yml, ref ccr-a57c163
  5f068ba ckpt 418: gated kestrel-g-v0.2.0-alpha7 (gecko code 106) and pushed it: handoff self-tes
  903dc4e ckpt 413: R2 first pass DONE on the emulator: Gecko source says display-port multipliers
```
