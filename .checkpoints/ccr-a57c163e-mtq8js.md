# CHECKPOINT 1401 — read me first, then TASKS.md

**Written:** 2026-11-13T02:36:54Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `4b222b5` (this checkpoint is the commit after it)

## Just done
AD1b-3/4 + AD1a-3 DONE: Settings cards + launch screen + Downloads queue UI verified on the emulator, procedural cosmetic rules (core ProceduralRules + proc bridge + content.js evaluator, 8 JVM + 8 web tests, 522 of 1,172 real lines), default.kfs regenerated with :style()+procedural rules (4,587 KB), No-AVIF-on-Fast-Wi-Fi switch; full gates green: 857 JVM tests, 114 web tests; TASKS/research updated

## Do this next
Version bump to 125 / 0.3.0-alpha26 / extension 1.0.125 (geckoapp/build.gradle.kts + kestrel-shields/manifest.json), write the alpha26 note (scratchpad note26.txt), bash ship.sh gecko with it, dispatch android.yml app=gecko, wait for tag kestrel-g-v0.3.0-alpha26 + green run, record-release.sh, reply to the owner (message 17) with the Release link on its own line; AFTER shipping: AD1b-5 Gradle verification metadata (dry-run with the cheap full_build=false gate first), optional Wi-Fi sleep test (EX-7), fMP4 defragmenter

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M TASKS.md

## Last ten checkpoints
```
  af1938b ckpt 1388: AD1b-3 UI polish: Settings sections are chamfered cards with dividers (like t
  7cb96e0 ckpt 1384: AD1b-2 mp4 always DONE in core: Hevc.kt (hvc1/hvcC), AudioFrames.kt (AC-3/E-A
  7919d0f ckpt 1373: AD1b-1 download manager DONE in core + UI wiring: Start now / Move up / Move 
  7b80ff7 ckpt 1365: AD1a-1 DONE: link warm-up triggers (finger down + top three search results) i
  ac0f553 ckpt 1353: Owner message 17 written into TASKS as AD1 (AD1a review rows, AD1b earlier AA
  16fc889 ckpt 1350: alpha25 SHIPPED and RECORDED: kestrel-g-v0.3.0-alpha25 (gecko code 124), run 
  62b643d ckpt 1348: alpha25 (code 124) gated + pushed (ckpt 1347) and DISPATCHED: android.yml run
  e95b292 ckpt 1347: gated kestrel-g-v0.3.0-alpha25 (gecko code 124) and pushed it: handoff self-t
  bede630 ckpt 1346: EX-10 PROVEN on the emulator (Tap to load box replaced the third-party frame;
  d7c8bb0 ckpt 1341: EX-1 PROVEN on the emulator (WifiService acquireWifiLock lockMode=3 at the st
```

(12 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
