# SPEED3 - owner message 14 (2026-11-12): optimise for LinkMeter's Fast Wi-Fi, close the Courier, audit every prompt, review and optimise the whole browser

Builds on SPEED2.md, GECKO_SPEED.md, GECKO_TUNING_2.md, SCROLL.md, HW_SPEED.md. Tags: **[O]** the owner said it, **[L]** read in tjshea90/LinkMeter (docs/FAST_WIFI.md), **[V]** verified here (emulator / unit test / engine strings), **[N]** not verified (no real devices in this sandbox).

## 1. What the owner asked
1. "I will be using linkmeter fast wifi setting to get better hotspot speed. Optimize the browser to take advantage of this fast wifi feature and make sure websites and services load correctly and as fast as possible." (AA1)
2. "Finish and Close work on the kestrel courier method ... keep it turned off in the browser by default because I will use the fast wifi setting in linkmeter more than the courier." (AA2)
3. "review my past prompts for any unfinished work and finish it. Do not ignore my prompts ..." (AA3, research/PROMPT_AUDIT.md)
4. "review and optimize as much as possible this browser app for speed and smooth scrolling and fast webpage loading and caching where logical so the back button doesn't have to reload the last site ... Take advantage of the ram and hardware ... fast, well coded, and optimized for my hardware" (AA4)

## 2. LinkMeter's Fast Wi-Fi and what it means for the browser
**How it works [L]:** LinkMeter's `FastVpn` on the tablet (an Android VpnService) captures EVERY app's traffic; a small userspace TCP/IP engine (`TunStack`, plain Java NIO) turns each app connection into one relay connection to `FastRelay` on the phone (TCP and UDP port 8767, authenticated with a key derived from the pairing secret), and the phone opens the real connection with its own network stack (the carrier caps the hotspot at 5 Mbit/s, not the phone's own traffic). Stage 1 runs over the normal hotspot, stage 2 over a Wi-Fi Direct group (hotspot off).
**Design facts that decide the browser's settings [L]:**
- The VPN interface has a 10,000-byte MTU (fewer packets for the tablet's CPU to touch); the engine is deliberately small.
- **QUIC (HTTP/3 over UDP 443) is refused at once** ("port unreachable"), so apps fall back to TCP. DNS-over-TLS is refused too.
- **IPv4 only inside the VPN; IPv6 is blocked** (Android blocks a family the VPN does not configure); the system resolver then only hands out IPv4 addresses.
- **Name lookups are answered by the PHONE's resolver** (its cache, its Private DNS): a lookup is one hop over the Wi-Fi.
- Each new TCP connection costs one relay round trip (the phone answers "connected" only after ITS connect to the website succeeded) plus work in the tablet's TunStack: few fat connections beat many small ones.
- Private addresses (the phone, the hotspot network) are left out of the VPN's routes.
**What the browser does about it (AA1), automatically while a VPN is the active network** (`NetProfile` in :core, 5 JVM tests; `NetworkMonitor.vpn` from `TRANSPORT_VPN`, 2 Robolectric tests; `GeckoEngine.applyNetwork` re-applies it on every change):
| Setting | Standard | VPN (Fast Wi-Fi) profile | Why |
|---|---|---|---|
| `network.trr.mode` (DNS over HTTPS) | the owner's switch (default on = 2) | 0 = system resolver = the phone's | a DoH lookup would run through the VPN's userspace TCP on the tablet CPU and ask for AAAA records the VPN blocks; the phone resolves in one hop |
| `network.http.http3.enable` | the owner's switch | off | QUIC is refused: no attempt, no fallback delay |
| Courier (downloads, pages) | the owner's choice, OFF by default | rests completely | a relay on top of a relay only adds a hop |
The owner's own switches are never rewritten: the effective values are applied on the default branch and the same inputs without the VPN give the standard values back. Diagnostics has a "Network:" line, Settings > Network lab says what is in force. **Emulator proof**: see section 5.
**Not verified [N]:** anything on the real tablet with the real Fast Wi-Fi (speed, battery, whether DoH-off is faster than DoH through the relay on the owner's carrier). The Network lab switches stay available for the owner's A/B (Diagnostics > Page loads).

## 2.1 Emulator proof of the Fast Wi-Fi profile ([V], Android 10 x86_64 + GeckoView 160; the VPN is SIMULATED with `setprop debug.kestrel.vpn 1`, a debug-build-only switch, because the emulator has no VPN; the real detection is `TRANSPORT_VPN` on the default network and is covered by a Robolectric test)
- Diagnostics: `Network: Fast Wi-Fi profile: a VPN is the active network, so lookups use the phone's own resolver, HTTP/3 is off and the phone helper rests. (VPN active: true)`.
- Engine read-back: `network.trr.mode=0` (the owner's DoH switch was ON = 2 before), `network.http.http3.enable=false`, `network.proxy.type=5` (no proxy: the Courier's PAC is not applied).
- Main menu: the Phone helper tile reads "Phone helper: resting (Fast Wi-Fi)"; the Home screen no longer says "metered connection" (unmetered default, alpha21).
- Engine defaults read back on the emulator (160): `docshell.shistory.bfcache.allow_unload_listeners` = TRUE already (pages with an `unload` handler ARE eligible on Android), `require_no_opener` true, `ship_allow_beforeunload_listeners` true, `browser.sessionhistory.max_entries` 50, `network.dnsCacheExpiration` 60 s, `network.dnsCacheEntries` 3200, `javascript.options.mem.gc_parallel_marking` false, `dom.script_loader.bytecode_cache.strategy` 1 (ours).
- GeckoView 160 has NO preference that lets a `Cache-Control: no-store` page into the back/forward cache (searched libxul: the blocker list is UNLOAD_LISTENER, BEFOREUNLOAD_LISTENER, REQUEST, ACTIVE_GET_USER_MEDIA, ACTIVE_PEER_CONNECTION, ACTIVE_LOCK, ACTIVE_WEBTRANSPORT, CONTAINS_EME/MSE_CONTENT, PAGE_LOADING, HAS_USED_VR, ...; no setting relaxes no-store). So a page that sends it is reloaded on Back by design; only an app-level snapshot can mask that (section 4).

## 3. Courier: closed
See PHONE_OFFLOAD.md section 17. Everything off by default, tile only when paired, rests under a VPN.

## 4. Review plan (AA3/AA4): three independent read-only reviews, then fixes
Prompt audit -> research/PROMPT_AUDIT.md; Kotlin performance review and extension-JS performance review -> findings below as they are confirmed.

## 5. The three reviews are in (read-only agents; every cost is an estimate, nothing was measured)
- `research/PROMPT_AUDIT.md` (all 25 owner messages), `research/PERF_KOTLIN.md` (K1-K13) and `research/PERF_JS.md` (J1-J11, B1-B5) are the full reports with file:line evidence. Verdict of the Kotlin review: the app's own UI code is lean; what the owner feels as reloads, blank pages and a slow Back is POLICY in `BrowserActivity` (2 live tabs, every trim >= 10 closes the rest, only the URL survives a swipe-away).
- Verdict of the JS review: nothing high; the cost is multiplication (per frame, per XHR, per media URL). One real bug (B1: `cssCache` never invalidated, caches the empty answer).

## 6. AA4 speed batch (alpha22): order of work, with the box each item ticks
Done items get their evidence written next to them (test name or emulator observation) in 6.1 below.
1. K1 (tab memory policy): live-session budget from MemAvailable (floor 2), trim handling that does not purge on a quick app switch, private tab keeps its URL when hibernated, bfcache viewers 6 (gecko-prefs v5).
2. K2 (back stack and scroll survive a swipe-away / process kill): SessionState JSON on disk per regular tab (formdata stripped), restored at start.
3. K3 (uiMode|locale|layoutDirection in configChanges, ShieldsBridge threads quit), K4 (do not start the engine again while hidden), K6 (link launch does not load the restored tab too).
4. K5 (cover the page while a tab revives / cold start) and the Back picture for pages the engine must reload (no-store).
5. Cheap ones: K10 (MediaList usable count), K11 (Health.log without String.format), K12 (browser.db WAL), K9 (thumbnail lanes), K8 (init after first frame), K7 (adaptive download cap while a page loads).
6. Extension JS: B1 (css cache), J5.1 (in-flight css), J4.1-4.3 (scan), J7 (passive), B2 (tabTop LRU), J3.1 (small media), J9 (pageHashes memo), counters, then J1.1 / J2a.
7. Not now: J11 (needs a 10-minute check on the tablet: the scrolltest page), J8, J6, B3/B4, K13.
