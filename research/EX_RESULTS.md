# EX_RESULTS - what happened to the rows of research/REVIEW-EXTERNAL-1.md (work log of 2026-11-12, alpha25)

The outside review (a separate Claude session) ordered 18 rows. This file keeps what was PROVEN, what only compiles, and what turned out differently from the review. The per-row status lives in TASKS.md (EX-1 .. EX-18).
Evidence tags as in the review: [V-EMU] seen on the Android 10 x86_64 emulator (no KVM, slow), [V-JVM] a unit/web test, [E] estimate, [N] needs the real tablet.

## Verified claims of the review
- GeckoView 160 internals exist exactly as described: `GeckoThread.speculativeConnect(String)` is public static, `XPCOMEventTarget.mainThread()` returns an `IXPCOMEventTarget` (an Executor) [V-AAR, javap]; `GeckoRuntimeSettings.setDoubleTapZoomingEnabled` / `setInputAutoZoomEnabled` and `GeckoSession.flushSessionState` exist.
- Every pref name the review cites exists as a whole line in libxul's strings (image.jxl.enabled, dom.prefetch_dns_for_anchor_https_document, network.buffer.cache.size, gfx.downloadable_fonts.fallback_delay, dom.speculation_rules.enabled, dom.speculation_rules.prefetch.dry_run, image.http.accept, apz.allow_double_tap_zooming, formhelper.autozoom, network.predictor.enable-hover-on-ssl).
- EX-1: `WIFI_MODE_FULL` really was the mode the download service took on API 29+ (the repo code is exactly as described); it is documented as non-functional.

## What the emulator showed
- [V-EMU] EX-4 / EX-3 / EX-8: Diagnostics read-back `jxl.enabled=false(b)`, `allow_double_tap_zooming=false(b)`, `autozoom=true(b)`, config version 6.
- [V-EMU] EX-11: the keyboard probe window delivers layout events on Android 10: 2 at start-up, 6 after one typing session (show + hide). The event path works; the poll is only a safety net once an event has arrived.
- [V-EMU] EX-2 (partly): after typing a host that is a bookmark, `GeckoEngine.speculativeConnect` is called once and Diagnostics counts "Warm-ups: 1". No connection reached a host-side TLS listener at 10.0.2.2:8443, but a NORMAL navigation to the same address does reach it (10 ClientHellos while HTTPS retried). 10.0.2.2 is an RFC 1918 address; Gecko refuses speculative connects to private addresses by design (DISABLE_RFC1918: link-prefetch hints must not probe the LAN), so the emulator's only reachable host address cannot prove the mechanism. See the loopback / `adb reverse` attempt below.

## Things the review did not say, found while doing the rows
- `SiteShieldStore.decode` rejected every line whose mask was above 127. A new per-site flag (EMBEDS = 128) would have been dropped as "damage" at the next start: per-site choices for it would never have persisted. Found by the new core test, fixed with `ALL_FLAGS`.
- `preconnect()` built `https://<host>/` and dropped the port: a suggestion with its own port was warmed on 443. It now keeps the port.
- The read-back names used only the last pref component, so `image.jxl.enabled`, `media.av1.enabled` and `speculation_rules.enabled` all printed as `enabled`. `shortPref()` prints `jxl.enabled` etc.

## Decisions
- EX-15 triggers 2 (the first three search results at idle) and 3 (finger down on a cross-site link) are NOT built: they contact sites the owner has not chosen (a scroll that starts on a link would warm it; three search results per search), for a head start of about 0.1-0.3 s [E]. Trigger 1 (the page in front, after 5 s away) is built. Revisit when the owner asks.
- EX-6 (look up links ahead), EX-7 (keep Wi-Fi awake), EX-12 (speculation rules), EX-16 (two A/B rows) are Network lab switches, all OFF: each has a cost (a lookup for links never tapped, battery, pre-release engine code) and no number from the tablet yet.
- EX-3 is ON by default (double-tap zoom OFF): the tap delay hits every tap on desktop-style pages and the engine documents it as caused by double-tap zoom. Pinch zoom is untouched. The Settings row says how to bring double-tap back.
- EX-10 is ON by default (embeds wait for a tap); per site in the Shields panel and globally in Settings > Shields.
