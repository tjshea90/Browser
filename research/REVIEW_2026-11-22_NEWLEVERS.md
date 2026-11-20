# New levers, round 4 (RVW4, 2026-11-20 session, branch claude/adoring-hamilton-58trdj)

Owner request (verbatim): "Look for more ways to improve the browser speed, page compatibility, loading time, video playback smoothness, video download finding and server fetching".
Rule for this file: only levers that are NOT already closed in REVIEW-EXTERNAL-1..4, REVIEW_ALPHA48, REVIEW_2026-11-20_*, REVIEW_2026-11-21_*, GECKO_SPEED, GECKO_TUNING_2, LAB5_*.
Tags: [read] verified in code, [aar] verified in the real GeckoView 160 / feature-webcompat 160.0.1 AAR, [est] estimate. Nothing here ran on the tablet.

## Checked first and found ALREADY DONE (so nobody re-derives it)
- Engine prefs: gecko-prefs.txt v10 + the Network lab already cover memory caches, decoded-image budget, bfcache, bytecode cache, DoH A/B, HTTP/3 A/B, DNS prefetch (ON), web-font fallback 500 ms (ON since alpha26, Prefs.labFontDelay), network.buffer.cache.size (switch, OFF), speculation rules (switch), AVIF off on Fast Wi-Fi (switch), connection warm-ups (WarmPolicy). Every cheap pref in the external reviews is either set or deliberately a switch. No new pref lever was found; do not spend more time there without tablet numbers.
- Downloads already use Firefox's own network stack (GeckoFetcher: HTTP/2 + HTTP/3, shared cookies) with a fallback; the stream engine already has retry, Retry-After, key caching, read-ahead bound (RVW1 V1-V13).
- Finder already reads og:video / twitter:player:stream / preload as=video / data-* / JSON answers / shadow roots (RVW1 F1-F24).

## NL1 (compat, HIGH): Kestrel G does not carry Mozilla's site fixes
[aar] GeckoView 160's omni.ja holds no `webcompat` extension (only FingerprintingWebCompatService.sys.mjs). Stock Firefox for Android gets the built-in "Web Compatibility Interventions" extension (webcompat@mozilla.org) from Mozilla's `feature-webcompat` library, not from the engine. [read] nothing in this repo mentions it (grep webcompat: 0 hits), so Kestrel G is LESS compatible than stock Firefox on every site Mozilla has a fix for.
[aar] `org.mozilla.components:feature-webcompat:160.0.1` (357 KB, MPL-2.0) carries extension 160.4.0: 406 intervention groups, 321 applicable to Android (194 User-Agent header overrides, 129 content-script injections: unsupported-browser banners, desktop layout on phones, FastClick breakage, Office 365 / Outlook, TikTok comments, YouTube back/forward, Nintendo, LinkedIn, CNN, banks), 398 match patterns, 76 tracker shims. All data is baked into run.js at build time (no network needed).
Cost / risk: runs in the engine's parent process (extensionsProcessEnabled is false here), about 340 KB of JS in 13 background scripts, host-filtered webRequest listeners. Its Remote Settings listener would add a new background collection sync: stubbed out (no new background traffic, owner rule); updates then arrive with the monthly engine bump.
Plan (TASKS RVW4-NL1): fetch the AAR at build time from Mozilla's Maven (no third-party code committed), unpack assets/webcompat into the APK, stub the Remote Settings listener, install it with ensureBuiltIn 12 s after the first page (never on the cold-start path), Settings > Site fixes switch (default ON), Diagnostics line.
Not provable here: that every experiment API starts under extensionsProcessEnabled(false) on a real device; the first emulator start must show the extension active.
