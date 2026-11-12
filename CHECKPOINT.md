# CHECKPOINT 1052 — read me first, then TASKS.md

**Written:** 2026-11-12T14:59:57Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `d803d90` (this checkpoint is the commit after it)

## Just done
gated kestrel-courier-v0.1.0-alpha3 (courier code 3) and pushed it: handoff self-test, unit suites and the versionCode check passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow_id android.yml, ref ccr-a57c163e-mtq8js, inputs {"app":"courier","full_build":"true","note":"alpha3: pages through the phone. The phone now loads the tablet's web pages with its OWN connection (your carrier caps the hotspot, not the phone's own traffic): the tablet's browser tunnels its https pages through a small proxy in this app, but only after the tablet proves the pairing code, and it loads pages directly by itself if the phone stops answering. Also moved off port 8765 (LinkMeter's control port) to 8780 so both apps run on the same phone, and the phone's CPU is kept awake only while something is loading. Install it over alpha2 (same signing key); the tablet needs Kestrel G alpha21 or newer."}.
When the run is green, confirm the Release shows the APK (mcp__github__get_release_by_tag kestrel-courier-v0.1.0-alpha3), then:  bash tools/record-release.sh kestrel-courier-v0.1.0-alpha3 "alpha3: pages through the phone. The phone now loads the tablet's web pages with its OWN connection (your carrier caps the hotspot, not the phone's own traffic): the tablet's browser tunnels its https pages through a small proxy in this app, but only after the tablet proves the pairing code, and it loads pages directly by itself if the phone stops answering. Also moved off port 8765 (LinkMeter's control port) to 8780 so both apps run on the same phone, and the phone's CPU is kept awake only while something is loading. Install it over alpha2 (same signing key); the tablet needs Kestrel G alpha21 or newer."
NEVER ship kestrel-courier-v0.1.0-alpha3 twice (check BUILDLOG.md and the Releases first).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  d803d90 ckpt 1051: pre-ship: alpha3: pages through the phone. The phone now loads the tablet's w
  8c64199 ckpt 1048: Z8 proven on the emulator (Android 10, GeckoView 160 + Courier in one emulato
  060b94e ckpt 1042: Z8 tablet side + Z1 unmetered built and unit-tested: ProxyLink-driven PagePro
  6208816 ckpt 1027: Z8 core done + Courier wiring: Courier API port now 8780 (CourierPorts, off L
  d886b5f ckpt 1016: Z10 done (read LinkMeter): the owner's carrier caps the HOTSPOT at 5 Mbit/s, 
  83be731 ckpt 1013: Owner message 13 written into TASKS.md as Z0-Z10 in their words (INBOX has it
  3a332d4 ckpt 1010: Y2 stage 2 SHIPPED and recorded: Kestrel Courier alpha2 (kestrel-courier-v0.1
  4389f39 ckpt 1007: gated kestrel-g-v0.3.0-alpha20 (gecko code 119) and pushed it: handoff self-t
  d3a8580 ckpt 1005: gated kestrel-g-v0.3.0-alpha20 (gecko code 119) and pushed it: handoff self-t
  5047bd2 ckpt 1004: gated kestrel-courier-v0.1.0-alpha2 (courier code 2) and pushed it: handoff s
```
