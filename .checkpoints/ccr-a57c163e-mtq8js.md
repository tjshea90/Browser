# CHECKPOINT 1010 — read me first, then TASKS.md

**Written:** 2026-11-12T09:37:00Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `dcaf2fc` (this checkpoint is the commit after it)

## Just done
Y2 stage 2 SHIPPED and recorded: Kestrel Courier alpha2 (kestrel-courier-v0.1.0-alpha2, APK 249 KB) + Kestrel G alpha20 (kestrel-g-v0.3.0-alpha20, 92.8 MB), both Actions runs green, certificate verified on the artifacts, both lines in BUILDLOG.md. Videos (HLS, and DASH picture+sound) now go through the phone, the phone converts with the same Remuxer class; emulator-proven incl. pause/resume, Courier killed mid-video, fallback with note, and a merge byte-identical (mdat) to the tablet's own. The Settings sentence was corrected and the first alpha20 run cancelled before publishing so the shipped text is true

## Do this next
STANDING BY for the owner: (1) the real phone + tablet pair test (TASKS Y2k) and their answers to Y2a (Android 16 phone? speeds? everything or only big files?), (2) Proton Mail 'Page loads' numbers, (3) keyboard app / accessibility / autofill / 5 GHz facts, (4) a yes/no on a scheduled CI list-build workflow. Nothing owner-free is urgent. If the owner reports a problem on the phone path read research/PHONE_OFFLOAD.md sections 14, 15.1, 15.2 first; Courier log = adb logcat -s KestrelCourier, tablet = Diagnostics > Phone link. Optional owner-free work, ONE step per wake: Y2i stage 3 (carry the phone's 'kept as .ts' reason onto the tablet row, overlap copy, notifications), U1c backup/restore, M6/M10/T0. Heartbeat trig_01LLoVKsjuTUaDn61UGP1pBN: delete it when the owner has been silent 24 h and nothing safe is left

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  4389f39 ckpt 1007: gated kestrel-g-v0.3.0-alpha20 (gecko code 119) and pushed it: handoff self-t
  d3a8580 ckpt 1005: gated kestrel-g-v0.3.0-alpha20 (gecko code 119) and pushed it: handoff self-t
  5047bd2 ckpt 1004: gated kestrel-courier-v0.1.0-alpha2 (courier code 2) and pushed it: handoff s
  1b67245 ckpt 1003: Y2h2 verified end to end and documented; full gates green (core 506 tests, ap
  366f38a ckpt 996: Y2h2 code: Remuxer moved to shared-android/kotlin (package app.kestrel.platfor
  eab846d ckpt 989: Y2h stage 2a ticked with emulator evidence (video through the phone: live row,
  325467d ckpt 986: Y2 stage 2 (video streams through the phone) code complete, JVM + build verifi
  f6958c7 ckpt 976: Y2 stage 1 SHIPPED and recorded: kestrel-courier-v0.1.0-alpha1 (run 4317795327
  6dbba44 ckpt 973: gated kestrel-g-v0.3.0-alpha19 (gecko code 118) and pushed it: handoff self-te
  cebfa82 ckpt 969: gated kestrel-courier-v0.1.0-alpha1 (courier code 1) and pushed it: handoff se
```
