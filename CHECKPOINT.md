# CHECKPOINT 867 — read me first, then TASKS.md

**Written:** 2026-11-12T05:56:54Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `6b70e4c` (this checkpoint is the commit after it)

## Just done
Y1 DONE (updater always tries: no storage gate in download()/install(), floor2 learning removed, Android's refusal remembered until free space grows 100 MB, new sheet 'Android would not install it over the old version' with Try again / Install the other way (save the update to Downloads + Android uninstall prompt) / Storage settings; REQUEST_DELETE_PACKAGES permission). Emulator-proven. Not shipped yet (alpha18 will bundle: Y1, Make MP4, splice detection, X20).

## Do this next
Y2 phone offload: write research/PHONE_OFFLOAD.md (design: Kestrel Courier companion app on the owner's Android phone, hotspot-gateway discovery, token pairing, authenticated HTTP API, same StreamEngine/Engine/TsToMp4 on the phone, tablet shows phone status in the same rows, pulls the finished file with the existing resumable engine, automatic fallback to tablet), then answer the owner (Y1 done + Y2 yes it is possible, what I will build, what I need: confirm Android phone, which phone OS version, OK to ship a third APK 'Kestrel Courier' via GitHub Releases). Optional Y1c: backup/restore (U1c) so 'Install the other way' loses nothing. Heartbeat trig_01LLoVKsjuTUaDn61UGP1pBN.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  ceaa63c ckpt 860: owner message 12 written into TASKS (Y1 updater must always try, Y2 phone offl
  eadd5fc ckpt 856: post-alpha17 in the tree (NOT shipped): Make MP4 for finished .ts rows (Stream
  0948a0d ckpt 843: post-dispatch hardening (NOT in alpha17): TsToMp4.peekFormatChange (unmarked s
  376dc8d ckpt 837: alpha17 DISPATCHED: android.yml run 43157262742 (run_number 60) on ccr-a57c163
  2726199 ckpt 836: gated kestrel-g-v0.3.0-alpha17 (gecko code 116) and pushed it: handoff self-te
  7f4771d ckpt 835: pre-ship: alpha17: the download button is now an icon in the bottom bar (gray 
  791bba1 ckpt 833: X15 + X18 DONE and emulator-proven (alpha-debug x86_64, Android 10): dock popu
  4f4235e ckpt 826: X18b DONE: StreamEngine writes TS HLS straight into an MP4 (decideMux probes s
  70b4d0b ckpt 804: X18a DONE (core, JVM-tested, ffmpeg-verified): Avc.kt (SPS/NAL), TsDemux.kt (s
  1b3baa2 ckpt 756: X17 dock download icon VERIFIED on emulator after a real redraw (home+relaunch
```

(6 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
