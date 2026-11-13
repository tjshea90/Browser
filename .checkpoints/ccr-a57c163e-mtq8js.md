# CHECKPOINT 1388 — read me first, then TASKS.md

**Written:** 2026-11-13T02:26:47Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `ade8cb0` (this checkpoint is the commit after it)

## Just done
AD1b-3 UI polish: Settings sections are chamfered cards with dividers (like the menu tiles and the Downloads rows), cold-start launch screen (shield on black from the first frame, plain black window restored in onCreate), Downloads queue buttons seen working on the emulator (Waiting in line / Start now / Move down; Start now ran a 3rd download over the limit of 2); emulator screenshots reviewed: Home, menu, tab grid, Settings, Downloads. Also P5 re-checked: a restored page tab no longer shows Home, it shows the launch shield then the dock with an 'Opening <host>' cover

## Do this next
Finish the C4 look at Shields/Diagnostics/Bookmarks/History/error/private screens (c4f.sh), tick AA3-6 + P5/P6 in TASKS, run full gates; then AD1b-4 ad blocking: regenerate default.kfs (scripts/make-default-lists.sh, uAssets checkout at /home/user/ublockorigin/uassets) and procedural cosmetics; AD1b-5 Gradle verification; AD1a-3 Decode lab recommendation

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  7cb96e0 ckpt 1384: AD1b-2 mp4 always DONE in core: Hevc.kt (hvc1/hvcC), AudioFrames.kt (AC-3/E-A
  7919d0f ckpt 1373: AD1b-1 download manager DONE in core + UI wiring: Start now / Move up / Move 
  7b80ff7 ckpt 1365: AD1a-1 DONE: link warm-up triggers (finger down + top three search results) i
  ac0f553 ckpt 1353: Owner message 17 written into TASKS as AD1 (AD1a review rows, AD1b earlier AA
  16fc889 ckpt 1350: alpha25 SHIPPED and RECORDED: kestrel-g-v0.3.0-alpha25 (gecko code 124), run 
  62b643d ckpt 1348: alpha25 (code 124) gated + pushed (ckpt 1347) and DISPATCHED: android.yml run
  e95b292 ckpt 1347: gated kestrel-g-v0.3.0-alpha25 (gecko code 124) and pushed it: handoff self-t
  bede630 ckpt 1346: EX-10 PROVEN on the emulator (Tap to load box replaced the third-party frame;
  d7c8bb0 ckpt 1341: EX-1 PROVEN on the emulator (WifiService acquireWifiLock lockMode=3 at the st
  ae14f0d ckpt 1334: :style() restyling rules in core (697 of 770 real rules accepted; CosmeticSty
```

(3 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
