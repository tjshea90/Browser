# CHECKPOINT 715 — read me first, then TASKS.md

**Written:** 2026-11-12T02:23:54Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `ad39d13` (this checkpoint is the commit after it)

## Just done
Last-Modified splice guard (EngineTest), research/AUDIT-2026-11.md written, docs/ARCHITECTURE.md sections for X2/X3. alpha16 build still running

## Do this next
when tag kestrel-g-v0.3.0-alpha16 exists: verify run success + release asset, record-release, tell owner. Then: 1) M1 validator-less resume (Sink.pread 4 KiB compare) or other X3 items; 2) preconnect on suggestion/tile touch; 3) typing/page-resize idea (needs owner numbers); 4) owner asks pending: Proton numbers, scheduled CI list build OK

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M docs/ARCHITECTURE.md

## Last ten checkpoints
```
  7be9a12 ckpt 712: alpha16 dispatched to GitHub Actions (tag kestrel-g-v0.3.0-alpha16 appears whe
  21a22c2 ckpt 711: gated kestrel-g-v0.3.0-alpha16 (gecko code 115) and pushed it: handoff self-te
  7d6778a ckpt 710: pre-ship: alpha16: daily update dialog (app, engine, ad-block lists) with Upda
  6ab16b5 ckpt 709: X3 first batch done+tested: auto-resume after kill (both engines, SQLite v3 cr
  2d7afd4 ckpt 700: X2 verified on emulator (dialog found/ready/install flow); X12 first pieces co
  2cfcc7a ckpt 691: alpha15 SHIPPED+RECORDED (run 43136503549 green, tag kestrel-g-v0.3.0-alpha15,
  2f132e9 ckpt 682: alpha15 shipped to GitHub: workflow dispatched (run queued on ccr-a57c163e-mtq
  4b80497 ckpt 681: gated kestrel-g-v0.3.0-alpha15 (gecko code 114) and pushed it: handoff self-te
  bede833 ckpt 680: pre-ship: alpha15: pop-up and forced-redirect guard, always-visible Save video
  8e96087 ckpt 678: emulator-verified: Save-video chip opens the Download video sheet; page scan l
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
