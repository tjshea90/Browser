# CHECKPOINT 1053 — read me first, then TASKS.md

**Written:** 2026-11-12T15:00:38Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `2a514af` (this checkpoint is the commit after it)

## Just done
gated kestrel-g-v0.3.0-alpha21 (gecko code 120) and pushed it: handoff self-test, unit suites and the versionCode check passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow_id android.yml, ref ccr-a57c163e-mtq8js, inputs {"app":"gecko","full_build":"true","note":"alpha21: Phone helper. Pages now load through your phone (Settings > Phone helper > Load pages through the phone; needs Kestrel Courier alpha3 on the phone): the phone opens the connections with its own network stack, which your carrier does not slow down like the hotspot, and pages load directly by themselves if the phone stops answering. New Phone helper tile in the main menu: one tap = hotspot only, the phone does nothing. Typing the pairing code switches the helper on. The data saver is now OFF by default (your hotspot is unmetered: no metered connection label, no big-download questions; the levels stay in Settings). Faster Back and fewer white areas: 4 pages kept for an hour (pages that send Cache-Control: no-store cannot be cached by any browser), 64 MB page cache, decoded images kept 3 minutes, images decoded two screens ahead, lazy images start loading earlier. Link test now also measures pages through the phone. With an older Courier the tablet loads pages by itself."}.
When the run is green, confirm the Release shows the APK (mcp__github__get_release_by_tag kestrel-g-v0.3.0-alpha21), then:  bash tools/record-release.sh kestrel-g-v0.3.0-alpha21 "alpha21: Phone helper. Pages now load through your phone (Settings > Phone helper > Load pages through the phone; needs Kestrel Courier alpha3 on the phone): the phone opens the connections with its own network stack, which your carrier does not slow down like the hotspot, and pages load directly by themselves if the phone stops answering. New Phone helper tile in the main menu: one tap = hotspot only, the phone does nothing. Typing the pairing code switches the helper on. The data saver is now OFF by default (your hotspot is unmetered: no metered connection label, no big-download questions; the levels stay in Settings). Faster Back and fewer white areas: 4 pages kept for an hour (pages that send Cache-Control: no-store cannot be cached by any browser), 64 MB page cache, decoded images kept 3 minutes, images decoded two screens ahead, lazy images start loading earlier. Link test now also measures pages through the phone. With an older Courier the tablet loads pages by itself."
NEVER ship kestrel-g-v0.3.0-alpha21 twice (check BUILDLOG.md and the Releases first).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  2a514af ckpt 1052: gated kestrel-courier-v0.1.0-alpha3 (courier code 3) and pushed it: handoff s
  d803d90 ckpt 1051: pre-ship: alpha3: pages through the phone. The phone now loads the tablet's w
  8c64199 ckpt 1048: Z8 proven on the emulator (Android 10, GeckoView 160 + Courier in one emulato
  060b94e ckpt 1042: Z8 tablet side + Z1 unmetered built and unit-tested: ProxyLink-driven PagePro
  6208816 ckpt 1027: Z8 core done + Courier wiring: Courier API port now 8780 (CourierPorts, off L
  d886b5f ckpt 1016: Z10 done (read LinkMeter): the owner's carrier caps the HOTSPOT at 5 Mbit/s, 
  83be731 ckpt 1013: Owner message 13 written into TASKS.md as Z0-Z10 in their words (INBOX has it
  3a332d4 ckpt 1010: Y2 stage 2 SHIPPED and recorded: Kestrel Courier alpha2 (kestrel-courier-v0.1
  4389f39 ckpt 1007: gated kestrel-g-v0.3.0-alpha20 (gecko code 119) and pushed it: handoff self-t
  d3a8580 ckpt 1005: gated kestrel-g-v0.3.0-alpha20 (gecko code 119) and pushed it: handoff self-t
```
