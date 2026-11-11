# CHECKPOINT 442 — read me first, then TASKS.md

**Written:** 2026-11-11T18:43:40Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `18ccff9` (this checkpoint is the commit after it)

## Just done
alpha8 build DISPATCHED: Actions run 43080438606 (android.yml, ref ccr-a57c163e-mtq8js, app=gecko, full_build=true) at 18:43Z; contains the U1 fix (updater knows Android's 10% free-space rule, real numbers, delete old installers, Diagnostics Storage line, release-notes hint)

## Do this next
WAIT for run 43080438606 (background until-loop on git ls-remote for tag kestrel-g-v0.2.0-alpha8); when actions_list shows conclusion=success and get_release_by_tag has the APK: bash tools/record-release.sh kestrel-g-v0.2.0-alpha8 '<note from ckpt 441>', ckpt, then reply to the owner (certificate is fine - checked all releases; Android keeps 10% free after any install; reproduced on the emulator; what to free: ~1.4 GB on a 16 GB tablet; install alpha8 fresh once, future updates tell exact MB; ask for Settings > Storage total/free). Emulator state: /data/local/tmp/big.bin + big2.bin fillers (rm them), debug feed prop set.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  18ccff9 ckpt 441: gated kestrel-g-v0.2.0-alpha8 (gecko code 107) and pushed it: handoff self-tes
  e3285db ckpt 438: U1 cause found + fixed in code: Android needs 10% of the partition free AFTER 
  b39879a ckpt 427: Owner message after alpha7 recorded as TASKS U1: in-place updates never instal
  378cb59 ckpt 423: alpha7 SHIPPED + RECORDED (kestrel-g-v0.2.0-alpha7, gecko code 106, run 430722
  d87281f ckpt 419: alpha7 build DISPATCHED: Actions run 43072284547 (android.yml, ref ccr-a57c163
  5f068ba ckpt 418: gated kestrel-g-v0.2.0-alpha7 (gecko code 106) and pushed it: handoff self-tes
  903dc4e ckpt 413: R2 first pass DONE on the emulator: Gecko source says display-port multipliers
  6cbe9d9 ckpt 406: R3 pull-down-to-refresh DONE + emulator-verified (KestrelGeckoView pull tracki
  a2797c7 ckpt 400: R1 neon DONE on the emulator (system animations off): pulse no longer gated on
  dec2a93 ckpt 392: S1+S2 code done: updater storage plan (core UpdateLogic.storagePlan + tests; U
```
