# CHECKPOINT 1525 — read me first, then TASKS.md

**Written:** 2026-11-13T08:19:41Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `6b60451` (this checkpoint is the commit after it)

## Just done
new context window (session restart): the owner re-sent items 1-3 of message 22 alone at 08:18:03Z (INBOX), so the earlier stop-and-save order is satisfied and the work starts: TASKS AI1a-c claimed; AI1d (unanswered prompts) goes into the final reply and the final handoff

## Do this next
AI1a: find the Gecko menu builder that drops Settings for private tabs (geckoapp BrowserActivity / Panels) and keep it; AI1b: long-press link sheet 'Open in new tab' must open a BACKGROUND tab (no show()); AI1c: evidence first on doraby movie pages + the Cloudflare 'incompatible browser' site (top suspect: the Embeds-wait-for-a-tap guard replacing third-party frames incl. challenges.cloudflare.com and the movie player iframe; then FPP, cookies); build once, emulator-check 1+2, then ship alpha30 and answer ALL three plainly + re-list open items; final handoff must list the unanswered earlier prompts (TASKS AI1d)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  5930d95 ckpt 1519: handoff saved (.handoff/handoff.md; CLAUDE.md/AGENTS.md Session Learnings upd
  5c37df1 ckpt 1518: alpha29 SHIPPED and RECORDED (run 43383744581 green, BUILDLOG line written, R
  01ea47c ckpt 1515: alpha29 DISPATCHED: android.yml run 43383744581 (run_number 79) started 07:09
  78c9506 ckpt 1514: gated kestrel-g-v0.3.0-alpha29 (gecko code 128) and pushed it: handoff self-t
  f2230eb ckpt 1511: AH1b DONE and emulator-proven with the real engine (heads-up before a refused
  11126ad ckpt 1495: AH1c done and emulator-proven end to end (Shields master switch off -> Firefo
  939a120 ckpt 1487: AH1d done and emulator-proven: the Video line stayed on 'decoders unknown' be
  7d3044a ckpt 1482: Owner message 21 (their Diagnostics copy from the tablet, alpha27) written in
  29bfbb4 ckpt 1478: handoff saved (.handoff/handoff.md + archive 2026-11-13T05-01-56Z; CLAUDE.md/
  2e2935d ckpt 1476: alpha28 SHIPPED and RECORDED: kestrel-g-v0.3.0-alpha28 (gecko code 127), run 
```

(5 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
