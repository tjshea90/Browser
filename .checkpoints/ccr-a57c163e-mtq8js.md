# CHECKPOINT 989 — read me first, then TASKS.md

**Written:** 2026-11-12T08:52:11Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `761a583` (this checkpoint is the commit after it)

## Just done
Y2h stage 2a ticked with emulator evidence (video through the phone: live row, pause/resume, Courier killed mid-video then resumed, fallback with note; final MP4 mdat identical to the tablet's own); Settings 'Good to know' no longer says videos are never done by the phone; research/PHONE_OFFLOAD.md 15.1 written; Y2h2 (Remuxer port for DASH/H.265/MP3 on the phone) opened

## Do this next
decide Y2h2 vs ship: look at app/.../download/Remuxer.kt (MediaExtractor/MediaMuxer, Android classes) and how to share it with :courier (srcDirs include or a tiny shared module); if cheap do the port (finisher in CourierRuntime, drop the companion exclusion in PhoneBackend.offloadStream, JVM test for the not-offered case flips), else ship Courier alpha2 + Kestrel G alpha20 now: bump courier 0.1.0-alpha2 vc2 + geckoapp 0.3.0-alpha20 vc119 (+ bash tools/sync-ext-version.sh), bash ship.sh courier then gecko, dispatch android.yml app=both full_build=true, wait with a background until loop, bash tools/record-release.sh only after green, tell the owner the Release links. Heartbeat trig_01LLoVKsjuTUaDn61UGP1pBN stays until the owner has been silent 24 h

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  325467d ckpt 986: Y2 stage 2 (video streams through the phone) code complete, JVM + build verifi
  f6958c7 ckpt 976: Y2 stage 1 SHIPPED and recorded: kestrel-courier-v0.1.0-alpha1 (run 4317795327
  6dbba44 ckpt 973: gated kestrel-g-v0.3.0-alpha19 (gecko code 118) and pushed it: handoff self-te
  cebfa82 ckpt 969: gated kestrel-courier-v0.1.0-alpha1 (courier code 1) and pushed it: handoff se
  919c2a3 ckpt 968: pre-ship: first Courier release: the small app for your PHONE. It downloads fi
  1df58a2 ckpt 967: pre-ship: first Courier release: the small app for your PHONE. It downloads fi
  354d25f ckpt 960: Y2g (CI plumbing for the third APK) written: appinfo.sh/ship.sh/record-release
  c7820e2 ckpt 957: Y2e BUILT (not yet shipped): module :courier (CourierRuntime/Service/Activity/
  b6b44e0 ckpt 945: Y2d DONE incl. Android glue: PhoneBridge (pairing code, gateway discovery, deb
  46aaf8d ckpt 938: Y2d steps 1+2 DONE (pure core): SocketHttp, LanFetcher, CourierClient, PhoneBa
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
