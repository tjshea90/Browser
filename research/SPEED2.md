# SPEED2 - owner message 13 (2026-11-12): speed and snappiness first; what the phone can genuinely speed up

Written 2026-11-12. Builds on GECKO_SPEED.md, GECKO_TUNING_2.md, SCROLL.md, HW_SPEED.md, PHONE_OFFLOAD.md (read their decisions, do not redo them). Tags: **[O]** the owner said it, **[L]** read in the owner's LinkMeter repo (tjshea90/LinkMeter, docs/FAST_WIFI.md and code), **[V]** verified here, **[E]** my estimate, **[N]** not verified (no real devices in this sandbox).

## 1. Facts that change the picture
- [O] The hotspot is unmetered; "no data saving is necessary" (the option may stay in Settings). Phone: Android 16 (a Samsung, One UI [L]), Shizuku running. Phone speeds "around 8 Mbit/s in the day to around 150 Mbit/s at night"; the tablet "much less". The tablet's wifi supports 5 GHz but is set to 2.4 GHz for signal. 2 GB RAM, usually 800 MB to 1 GB free, Android 10 Go.
- [L] **The carrier (Total Wireless) caps the HOTSPOT at 5 Mbit/s** ("Currently my mobile carrier has a 5mbit/s hotspot speed cap ... I am approved to develop a hotspot link app which allows faster hotspot speeds", docs/FAST_WIFI.md). Tethered traffic is recognised by TTL, a tethering APN or inspection; traffic an APP ON THE PHONE sends is the phone's own. This is why "the phone downloads faster than the tablet": the cap, not the phone's CPU. At 5 Mbit/s a 3 MB page needs about 5 s of pure transfer.
- [L] LinkMeter already has **Fast Wi-Fi** (stage 1 over the hotspot, stage 2 Wi-Fi Direct with the hotspot off): a VPN on the tablet plus a relay on the phone (phone port 8767) so ALL tablet apps use the phone's own connection. It is "NOT TRIED ON THE DEVICES" yet. Its open question: does the carrier also slow the phone's own traffic while the hotspot is on? (then only stage 2 helps).
- [L] **Port clash: LinkMeter's phone control port is 8765, the Courier's default was 8765** -> the Courier would fail to start on the owner's phone ("port already used"). Changed (section 5).
- [V] `PhoneBridge.gateway()` used `activeNetwork`, which is the VPN when Fast Wi-Fi is on: no gateway -> the Courier would be unreachable exactly when the owner uses LinkMeter's VPN. Changed (section 5).

## 2. Where the time goes on this tablet (ranked, [E] unless noted)
1. **The 5 Mbit/s hotspot cap** on every byte of a page (bandwidth-bound loads: images, JS bundles, video).
2. **Round trips** (cellular 40-100 ms + wifi 3-30 ms, DNS over DoH, TCP + TLS per origin): 10-30 origins x 2-3 RTT.
3. **CPU**: 4 x Cortex-A53 class cores, 32-bit userland: JS parse/compile, layout, display-list building, image decode, TLS. Dominates once the network is fast (Proton Mail's bundle).
4. **Memory pressure**: 800-1000 MB free; Gecko flushes decoded images, WebRender caches and the back/forward cache on every Android low-memory signal (SCROLL.md Q2): white areas and slow Back.
5. The 2.4 GHz wifi hop (single stream, roughly 30-50 Mbit/s real [L]) caps the gain once the carrier cap is bypassed; 5 GHz would lift it when the tablet is close to the phone.

## 3. Can the phone genuinely speed browsing up? (the answer to Z3)
| Idea | Verdict | Why |
|---|---|---|
| The phone fetches for the tablet with ITS OWN connection (bypass the hotspot cap) | **YES, the big one** | removes cause 1: up to about 6x on the transfer part (5 Mbit/s -> wifi-limited 30+ Mbit/s). Done for files + videos (Courier); this round adds PAGE LOADS via an HTTP CONNECT proxy on the phone. |
| DNS and TCP connect done on the phone | yes, small, comes free with the proxy | the tablet stops doing DoH lookups for proxied hosts; one wifi hop instead of cellular for the CONNECT answer |
| Heavy download/convert work (merge, remux) | yes (done) | the phone's CPU/muxer |
| Better wifi hop | yes, advice not code | try 5 GHz / put the phone close; LinkMeter's Signal tab measures it |
| Render pages, run JS, decode images on the phone ("remote browser") | NO | scrolling and typing would wait for the wifi; every IME, selection, form and video path would have to be rebuilt; the tablet's own GPU compositor is what makes scrolling smooth |
| Transcode images / minify on the phone, prefetch the whole page, share a cache | NO | HTTPS: needs a man-in-the-middle certificate on the tablet (weakens security, breaks pinned sites); the gain is small once the cap is gone |
| Ad/tracker blocking on the phone | NO (keep in the engine) | at the proxy only the host name is visible for HTTPS, so no path / third-party / cosmetic rules; the engine already cancels blocked requests before they connect; the proxy would only duplicate part of the work (section 4.4) |
| Shizuku on the phone | not needed by the browser | LinkMeter already uses it (hotspot, Wi-Fi Direct, screen); a hotspot-band switch belongs there |

LinkMeter's Fast Wi-Fi and this proxy are complementary: the VPN covers every tablet app but costs tablet CPU (a userspace TCP engine); the proxy covers only Kestrel G but the data path on the tablet is the kernel's own TCP (no packet code of ours). Both can run together (the VPN leaves private addresses alone, so the tablet reaches the phone's proxy directly).

## 4. Decisions
1. **Pages through the phone = an HTTP CONNECT proxy in the Courier** (port 8781, bound to the same private non-cellular peers as the API, only for a tablet that proved the pairing key in the last 10 minutes: `POST /v1/proxy/arm`, signed like every call; no password prompt, no 407 round trip, nothing for Gecko to know). CONNECT only: plain http:// stays DIRECT (rare, small). QUIC/HTTP3 cannot ride a CONNECT proxy; TCP+TLS is used for proxied hosts.
2. **Gecko is pointed at it with a PAC data: URL** set live via `GeckoPreferenceController` (`network.proxy.type=2`, `network.proxy.autoconfig_url`, `network.proxy.failover_timeout=10`): `PROXY phone:8781; DIRECT` for https/wss, DIRECT for http, private and plain host names. If the phone stops answering Gecko falls back to DIRECT by itself; the app also removes the PAC the moment the phone is known unreachable and puts it back when it answers. A switch in Settings AND in the main menu turns the phone helper off completely.
3. **Unmetered**: the app stops acting metered by default (media cache values, `network.prefetch-next`, predictor prefetch, DNS prefetch from https pages, lazy-load margin); a Settings switch "Save data" restores the old behaviour.
4. **Ad block stays in the engine** (reasoning in section 3). Nothing moves to the phone.
5. **Back/forward and scrolling**: keep the bfcache and decoded images alive longer (RAM is allowed), start decoding and loading earlier, measure on the emulator what can be measured (bfcache hit on Back, prefs read back); real-tablet numbers stay the owner's to report (Diagnostics > Page loads, Compare 3 feels).

## 5. Done in this round (updated as it happens)
- Courier API port 8765 -> 8780 (tablet default too); proxy port 8781. Tablet gateway search over all Wi-Fi networks, ignoring a VPN.
