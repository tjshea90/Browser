# CHECKPOINT 712 — read me first, then TASKS.md

**Written:** 2026-11-12T02:21:40Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `21a22c2` (this checkpoint is the commit after it)

## Just done
alpha16 dispatched to GitHub Actions (tag kestrel-g-v0.3.0-alpha16 appears when the release is made); background waiter /tmp/wait16.log

## Do this next
1) when /tmp/wait16.log shows the tag: mcp__github__actions_list (parse saved JSON for conclusion of newest run on branch), get_release_by_tag, bash tools/record-release.sh kestrel-g-v0.3.0-alpha16 '<note from ship16.log>' ONLY if success, tell owner with Release link on its own line + list open items. 2) meanwhile: write research/AUDIT-2026-11.md (X12), X5 assessment section, M1 resume validation (research/DOWNLOAD_MANAGERS.md M1a: reopen length check + Last-Modified compare), preconnect idea

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  21a22c2 ckpt 711: gated kestrel-g-v0.3.0-alpha16 (gecko code 115) and pushed it: handoff self-te
  7d6778a ckpt 710: pre-ship: alpha16: daily update dialog (app, engine, ad-block lists) with Upda
  6ab16b5 ckpt 709: X3 first batch done+tested: auto-resume after kill (both engines, SQLite v3 cr
  2d7afd4 ckpt 700: X2 verified on emulator (dialog found/ready/install flow); X12 first pieces co
  2cfcc7a ckpt 691: alpha15 SHIPPED+RECORDED (run 43136503549 green, tag kestrel-g-v0.3.0-alpha15,
  2f132e9 ckpt 682: alpha15 shipped to GitHub: workflow dispatched (run queued on ccr-a57c163e-mtq
  4b80497 ckpt 681: gated kestrel-g-v0.3.0-alpha15 (gecko code 114) and pushed it: handoff self-te
  bede833 ckpt 680: pre-ship: alpha15: pop-up and forced-redirect guard, always-visible Save video
  8e96087 ckpt 678: emulator-verified: Save-video chip opens the Download video sheet; page scan l
  e1721e0 ckpt 647: X7 page scan coded: content.js finds video/source/og:video/links/inline-script
```
