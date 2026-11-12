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

## 3. Courier: closed
See PHONE_OFFLOAD.md section 17. Everything off by default, tile only when paired, rests under a VPN.

## 4. Review plan (AA3/AA4): three independent read-only reviews, then fixes
Prompt audit -> research/PROMPT_AUDIT.md; Kotlin performance review and extension-JS performance review -> findings below as they are confirmed.
