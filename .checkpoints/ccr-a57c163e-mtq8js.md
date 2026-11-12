# CHECKPOINT 1003 — read me first, then TASKS.md

**Written:** 2026-11-12T09:25:02Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `055992e` (this checkpoint is the commit after it)

## Just done
Y2h2 verified end to end and documented; full gates green (core 506 tests, app 97, lint app + courier, gecko compile); versions bumped: courier 0.1.0-alpha2 (code 2), Kestrel G 0.3.0-alpha20 (code 119)

## Do this next
RELEASE IN FLIGHT: bash ship.sh courier "<note>" then bash ship.sh gecko "<note>"; dispatch android.yml twice (mcp__github__actions_run_trigger run_workflow, ref ccr-a57c163e-mtq8js, inputs app=courier full_build=true note, then app=gecko ...); wait with a background until loop; verify each Release has its APK + sha256; bash tools/record-release.sh <tag> "note" ONLY after each run is green (tags kestrel-courier-v0.1.0-alpha2 and kestrel-g-v0.3.0-alpha20; check BUILDLOG.md + Releases first: never ship a tag twice); then tell the owner both Release links as plain text on their own lines. After that: owner answers to Y2a, heartbeat trig_01LLoVKsjuTUaDn61UGP1pBN stays

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
     M courier/build.gradle.kts
     M geckoapp/build.gradle.kts
     M geckoapp/src/main/assets/kestrel-shields/manifest.json

## Last ten checkpoints
```
  366f38a ckpt 996: Y2h2 code: Remuxer moved to shared-android/kotlin (package app.kestrel.platfor
  eab846d ckpt 989: Y2h stage 2a ticked with emulator evidence (video through the phone: live row,
  325467d ckpt 986: Y2 stage 2 (video streams through the phone) code complete, JVM + build verifi
  f6958c7 ckpt 976: Y2 stage 1 SHIPPED and recorded: kestrel-courier-v0.1.0-alpha1 (run 4317795327
  6dbba44 ckpt 973: gated kestrel-g-v0.3.0-alpha19 (gecko code 118) and pushed it: handoff self-te
  cebfa82 ckpt 969: gated kestrel-courier-v0.1.0-alpha1 (courier code 1) and pushed it: handoff se
  919c2a3 ckpt 968: pre-ship: first Courier release: the small app for your PHONE. It downloads fi
  1df58a2 ckpt 967: pre-ship: first Courier release: the small app for your PHONE. It downloads fi
  354d25f ckpt 960: Y2g (CI plumbing for the third APK) written: appinfo.sh/ship.sh/record-release
  c7820e2 ckpt 957: Y2e BUILT (not yet shipped): module :courier (CourierRuntime/Service/Activity/
```

(6 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
