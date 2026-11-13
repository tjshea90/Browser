# CHECKPOINT 1473 — read me first, then TASKS.md

**Written:** 2026-11-13T04:49:59Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `6fab31c` (this checkpoint is the commit after it)

## Just done
alpha28 gates green: JVM (core + app + gecko compile), 123 web tests, pref names 44/44; EtpCategories arithmetic corrected (STRICT 766 has no STP)

## Do this next
bash ship.sh gecko "$(cat scratchpad/note28.txt)" (note file in the session scratchpad), then dispatch android.yml (app=gecko full_build=true note), wait tag kestrel-g-v0.3.0-alpha28 + green run, get_release_by_tag, record-release.sh, answer owner

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  a3a8e82 ckpt 1470: AF1 cause found + fixed + proven in real Gecko on the emulator: Kestrel asked
  a0403ec ckpt 1458: AF1 diagnostics built (not yet compiled/shipped): extension page log (blocked
  697f62b ckpt 1451: Owner message 20 (their Video lab / Tap lab / Decode lab numbers from the tab
  101b4b1 ckpt 1447: Owner message 19 (doraby.com: no photos, no search bar in Kestrel; Brave fine
  c0c149f ckpt 1443: alpha27 SHIPPED and RECORDED: kestrel-g-v0.3.0-alpha27 (gecko code 126), run 
  d107bc4 ckpt 1440: alpha27 (kestrel-g-v0.3.0-alpha27, gecko code 126) gated, pushed and DISPATCH
  f453a50 ckpt 1439: gated kestrel-g-v0.3.0-alpha27 (gecko code 126) and pushed it: handoff self-t
  6b35128 ckpt 1437: AE1 alpha27 built+tested: autoplay fix PROVEN on emulator, DRM prompt proven,
  fd0f98f ckpt 1426: AE1: CAUSE OF STUCK LOADING SYMBOL PROVEN on emulator with alpha26 (muted aut
  08a5ebd ckpt 1410: AE1 research phase done (evidence in summary); re-oriented after compaction; 
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
