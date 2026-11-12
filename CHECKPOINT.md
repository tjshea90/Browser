# CHECKPOINT 996 — read me first, then TASKS.md

**Written:** 2026-11-12T08:59:54Z · **tests:** all 1 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-a57c163e-mtq8js` · **builds on:** `a0f0b58` (this checkpoint is the commit after it)

## Just done
Y2h2 code: Remuxer moved to shared-android/kotlin (package app.kestrel.platform, device/tag/roomDir/note injectable) and compiled into :app, :geckoapp and :courier; Courier runtime uses it as the stream finisher; picture+sound (DASH) request carried end to end (CourierClient StreamSpec.companion, PhoneBackend.offloadStream + persistence, CourierHost builds the hidden sound job with the same SSRF check); harness got finisherOf + per-host dns; courier suite 122 tests green (new: picture+sound merged on the phone with a fake finisher, companion address checks, other kinds not offered). All three modules compile

## Do this next
Emulator proof of Y2h2: ./gradlew :courier:assembleDebug :geckoapp:assembleDebug -PgeckoAbi=x86_64 --offline, adb install -r -g both (packages app.kestrel.courier.debug / app.kestrel.browser.gecko.debug), site servers: python3 -I mediaserver.py site 8000 in the scratchpad (check ports with a python socket, ss is missing), props debug.kestrel.phone 127.0.0.1:8765 + debug.kestrel.courier.noguard 1; open http://10.0.2.2:8000/dash.html (picture+sound) and hls.html?src=hls/hevc/index.m3u8 via am start VIEW (adb input text drops chars), dock download icon -> pick -> DOWNLOAD -> Menu > Downloads; expect row 'On your phone' then 'Copied from your phone', and check the MP4 with ffprobe/ffmpeg -f null. Then app tests + lint (app, courier), full :core:test, docs (PHONE_OFFLOAD 15.2, TASKS Y2h2 evidence), versions (courier 0.1.0-alpha2 vc2, geckoapp 0.3.0-alpha20 vc119 + bash tools/sync-ext-version.sh), ship.sh, dispatch android.yml app=both, record-release.sh after green. Heartbeat trig_01LLoVKsjuTUaDn61UGP1pBN stays

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" says how; the briefing is tools/resume.sh. Do that first, or autosave may stay off all session.)*

## Uncommitted right now
    (nothing — the tree is clean as of this checkpoint)

## Last ten checkpoints
```
  eab846d ckpt 989: Y2h stage 2a ticked with emulator evidence (video through the phone: live row,
  325467d ckpt 986: Y2 stage 2 (video streams through the phone) code complete, JVM + build verifi
  f6958c7 ckpt 976: Y2 stage 1 SHIPPED and recorded: kestrel-courier-v0.1.0-alpha1 (run 4317795327
  6dbba44 ckpt 973: gated kestrel-g-v0.3.0-alpha19 (gecko code 118) and pushed it: handoff self-te
  cebfa82 ckpt 969: gated kestrel-courier-v0.1.0-alpha1 (courier code 1) and pushed it: handoff se
  919c2a3 ckpt 968: pre-ship: first Courier release: the small app for your PHONE. It downloads fi
  1df58a2 ckpt 967: pre-ship: first Courier release: the small app for your PHONE. It downloads fi
  354d25f ckpt 960: Y2g (CI plumbing for the third APK) written: appinfo.sh/ship.sh/record-release
  c7820e2 ckpt 957: Y2e BUILT (not yet shipped): module :courier (CourierRuntime/Service/Activity/
  b6b44e0 ckpt 945: Y2d DONE incl. Android glue: PhoneBridge (pairing code, gateway discovery, deb
```

(6 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
