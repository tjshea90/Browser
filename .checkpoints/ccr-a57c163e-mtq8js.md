# CHECKPOINT 1054 — read me first, then TASKS.md

**Written:** 2026-11-12T15:01:44Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `95c9050` (this checkpoint is the commit after it)

## Just done
Docs: CLAUDE.md + BRIEF.md record the owner's unmetered/5 Mbit/s facts and the LinkMeter port rule; releases Courier alpha3 + Kestrel G alpha21 dispatched (waiting for the runs)

## Do this next
WAIT for both Actions runs (courier then gecko), check mcp__github__get_release_by_tag kestrel-courier-v0.1.0-alpha3 and kestrel-g-v0.3.0-alpha21 (APK + sha256, certificate verified), confirm run conclusion success (mcp__github__actions_list list_workflow_runs perPage 1 status completed), then bash tools/record-release.sh <tag> '<note as passed to ship.sh>' for each, then send the owner the reply (draft in OUTBOX / this session): yes the phone can speed up page loads because the carrier caps the HOTSPOT not the phone; pages through the phone + Phone helper menu tile + unmetered + Back cache/scroll prefs; ad block stays in the engine; LinkMeter port clash fixed; tested on emulator only; 5 GHz advice; re-list open items. Heartbeat trig_01LLoVKsjuTUaDn61UGP1pBN stays

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M BRIEF.md
     M CLAUDE.md

## Last ten checkpoints
```
  95c9050 ckpt 1053: gated kestrel-g-v0.3.0-alpha21 (gecko code 120) and pushed it: handoff self-t
  2a514af ckpt 1052: gated kestrel-courier-v0.1.0-alpha3 (courier code 3) and pushed it: handoff s
  d803d90 ckpt 1051: pre-ship: alpha3: pages through the phone. The phone now loads the tablet's w
  8c64199 ckpt 1048: Z8 proven on the emulator (Android 10, GeckoView 160 + Courier in one emulato
  060b94e ckpt 1042: Z8 tablet side + Z1 unmetered built and unit-tested: ProxyLink-driven PagePro
  6208816 ckpt 1027: Z8 core done + Courier wiring: Courier API port now 8780 (CourierPorts, off L
  d886b5f ckpt 1016: Z10 done (read LinkMeter): the owner's carrier caps the HOTSPOT at 5 Mbit/s, 
  83be731 ckpt 1013: Owner message 13 written into TASKS.md as Z0-Z10 in their words (INBOX has it
  3a332d4 ckpt 1010: Y2 stage 2 SHIPPED and recorded: Kestrel Courier alpha2 (kestrel-courier-v0.1
  4389f39 ckpt 1007: gated kestrel-g-v0.3.0-alpha20 (gecko code 119) and pushed it: handoff self-t
```
