# EX_RESULTS - what happened to the rows of research/REVIEW-EXTERNAL-1.md (work log of 2026-11-12, alpha25)

The outside review (a separate Claude session) ordered 18 rows. This file keeps what was PROVEN, what only compiles, and what turned out differently from the review. The per-row status lives in TASKS.md (EX-1 .. EX-18).
Evidence tags as in the review: [V-EMU] seen on the Android 10 x86_64 emulator (no KVM, slow), [V-JVM] a unit/web test, [E] estimate, [N] needs the real tablet.

## Verified claims of the review
- GeckoView 160 internals exist exactly as described: `GeckoThread.speculativeConnect(String)` is public static, `XPCOMEventTarget.mainThread()` returns an `IXPCOMEventTarget` (an Executor) [V-AAR, javap]; `GeckoRuntimeSettings.setDoubleTapZoomingEnabled` / `setInputAutoZoomEnabled` and `GeckoSession.flushSessionState` exist.
- Every pref name the review cites exists as a whole line in libxul's strings (image.jxl.enabled, dom.prefetch_dns_for_anchor_https_document, network.buffer.cache.size, gfx.downloadable_fonts.fallback_delay, dom.speculation_rules.enabled, dom.speculation_rules.prefetch.dry_run, image.http.accept, apz.allow_double_tap_zooming, formhelper.autozoom, network.predictor.enable-hover-on-ssl).
- EX-1: `WIFI_MODE_FULL` really was the mode the download service took on API 29+ (the repo code is exactly as described); it is documented as non-functional.

## What the emulator showed
- [V-EMU] EX-4 / EX-3 / EX-8: Diagnostics read-back `jxl.enabled=false(b)`, `allow_double_tap_zooming=false(b)`, `autozoom=true(b)`, config version 6.
- [V-EMU] EX-3 MEASURED with the Tap lab page (viewport width=1200, `adb shell input tap`, six taps one second apart): "Double-tap to zoom" OFF (the new default) -> click 5-45 ms after the finger lifts, median 18 ms; ON -> 234, 235, 130, 270, 253 ms (and one 1065 ms while the page was still settling), median 253 ms. The tap delay of the review exists in this engine and the setting removes it. (Emulator timing is noisy; the tablet's own run is one tap on Diagnostics > Tap lab.)
- [V-EMU] EX-1 PROVEN: with the throttled test server (100 kB/s per connection, 12 MB, parallel Range segments with If-Match) Android's WifiService logged `acquireWifiLock uid=10118 lockMode=3` (FULL_HIGH_PERF) at the start of the transfer and `releaseWifiLock` when the last byte arrived (00:18:36 -> 00:19:38); `dumpsys wifi` showed acquired 2 / released 1 in between. Earlier manual runs that showed 0 locks were only waiting on the "Download this file?" confirmation (nobody tapped Download). Side finding fixed: MediaProvider renames a clashing name itself (big.bin -> big (1).bin); when THAT name is held by a stale row (file removed behind its back) the insert fails with `UNIQUE constraint failed: files._data` and the download died with "could not create the file" - MediaStoreSinks.insertRow now retries with the next free-looking names and finally a time-stamped name (the retried run finished as `big (4).bin`).
- [V-EMU] EX-11: the keyboard probe window delivers layout events on Android 10: 2 at start-up, 6 after one typing session (show + hide). The event path works; the poll is only a safety net once an event has arrived.
- [V-EMU] EX-2 PROVEN, with Gecko's own log (debug-only `setprop debug.kestrel.moz_log "nsHttp:5,..."`, written to files/gecko-moz.log.moz_log) and a host-side TLS listener (scratchpad tlslog.py):
  - Old path (the public `GeckoWebExecutor.speculativeConnect` called from an app thread, `debug.kestrel.warmpath old`): typing a bookmarked host leaves NO trace in the engine log and the listener sees nothing. The review's claim holds.
  - New path (dispatched onto the Gecko thread): the log shows `MaybeSpeculativeConnectWithHTTPSRR for ci=...127.0.0.1:8443`, `nsHttpConnectionMgr::SpeculativeConnect`, `OnMsgSpeculativeConnect` on the socket thread, and a TLS ClientHello reaches the listener at 23:37:38, before Enter was pressed (target 127.0.0.1:8443 through `adb reverse`).
  - A target in RFC 1918 space (the emulator's only host alias 10.0.2.2) is refused by the engine on purpose: `nsHttpConnectionMgr::SpeculativeConnect skipping RFC1918 address [10.0.2.2]` (link-prefetch hints must not probe the LAN). So the warm-up never touches addresses on the owner's own network; only public sites are warmed.
  - Not tested: that the following navigation reuses the warmed connection (the second half of the review's proof).

## Things the review did not say, found while doing the rows
- `SiteShieldStore.decode` rejected every line whose mask was above 127. A new per-site flag (EMBEDS = 128) would have been dropped as "damage" at the next start: per-site choices for it would never have persisted. Found by the new core test, fixed with `ALL_FLAGS`.
- `preconnect()` built `https://<host>/` and dropped the port: a suggestion with its own port was warmed on 443. It now keeps the port.
- The read-back names used only the last pref component, so `image.jxl.enabled`, `media.av1.enabled` and `speculation_rules.enabled` all printed as `enabled`. `shortPref()` prints `jxl.enabled` etc.

## Decisions
- EX-15 triggers 2 (the first three search results at idle) and 3 (finger down on a cross-site link) are NOT built: they contact sites the owner has not chosen (a scroll that starts on a link would warm it; three search results per search), for a head start of about 0.1-0.3 s [E]. Trigger 1 (the page in front, after 5 s away) is built. Revisit when the owner asks.
- EX-6 (look up links ahead), EX-7 (keep Wi-Fi awake), EX-12 (speculation rules), EX-16 (two A/B rows) are Network lab switches, all OFF: each has a cost (a lookup for links never tapped, battery, pre-release engine code) and no number from the tablet yet.
- EX-3 is ON by default (double-tap zoom OFF): the tap delay hits every tap on desktop-style pages and the engine documents it as caused by double-tap zoom. Pinch zoom is untouched. The Settings row says how to bring double-tap back.
- EX-10 is ON by default (embeds wait for a tap); per site in the Shields panel and globally in Settings > Shields.
