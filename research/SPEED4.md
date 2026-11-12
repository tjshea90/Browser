# SPEED4 - owner message 15, item 3 (AB3): LinkMeter's Fast Wi-Fi, from both sides

Builds on SPEED3.md section 2 (the AA1 profile). Tags: **[O]** the owner said it, **[L]** read in the LinkMeter repo (read-only clone, nothing there was changed), **[V]** verified here (unit test / Robolectric / emulator), **[N]** not verified (no real tablet, no real LinkMeter relay in this sandbox).

## 1. What the owner asked
"do any optimizations available for better speed and handling of the fast wifi feature on the linkmeter app from the linkmeter. I will be using the fast wifi feature often, but the browser must also work without it. If I have the fast wifi feature in linkmeter turned on, make sure the browser is well optimized to connect with it and that websites and downloads behave properly and load as quickly as possible" **[O]**

Two halves: (a) is there anything in LinkMeter itself that makes Fast Wi-Fi slower than it has to be; (b) does the browser use the tunnel as well as it can, and still work exactly as before without it.

## 2. LinkMeter side: what I read, and the verdict
Files (`app/src/main/java/com/tj/linkmeter/`): `FastVpn.java` (570 lines), `TunStack.java` (1276), `FastRelayCore.java` (783), `FastRelay.java`, `FastProto.java`, `FastDirect.java`, `FastRules.java`.
- **The tunnel interface [L]** (`FastVpn.up`): MTU 10,000, address 10.111.222.2/24, DNS server 10.111.222.53, `setUnderlyingNetworks(the Wi-Fi)`, `setMetered(false)` ("as metered as the Wi-Fi under it"), public routes only (private addresses stay out of the VPN).
- **Tablet engine `TunStack` [L]**: `TCP_NODELAY` on (466); per-connection buffers 256 KB up and 256 KB down; window scale 5 (windows up to 2 MB can be said); 24 MB total budget; up to 1,024 connections; connect timeout 25 s; retransmit timeout 300 ms to 4 s; a zero-window probe; flow control end to end (an app that reads slowly closes its own window).
- **Phone relay `FastRelayCore` [L]**: `TCP_NODELAY` on both the accepted and the opened sockets (292, 384); 64 KB direct buffers; up to 1,024 pairs; 512 UDP flows; connect timeout 15 s; the phone answers "connected" only after ITS connect to the site succeeded.
- **Arithmetic [V by calculation only]**: a 256 KB window at a 50 ms round trip (Wi-Fi hop + phone to site) allows about 5 MB/s per connection, more than the tablet's single-stream 2.4 GHz radio can carry (about 3 to 5 MB/s). So the windows are not the limit. `TCP_NODELAY` is already on, so a TLS handshake is not held back by Nagle. A browser opens several connections anyway.

**Verdict: no defect found, and nothing I can change in LinkMeter safely without the real devices.** The tablet's only internet is this tunnel: a regression there would cut the owner off, and every candidate below needs a before/after number from the real phone and tablet first (LinkMeter's own notes say it has been tested in the sandbox on a Linux interface, not yet on the real devices). Candidates, ranked by expected gain, none done:
1. **Early "connected" for the app, real connect behind it.** Today the app's TCP handshake waits for the phone's own connect to the site (one extra phone-to-site round trip, about 20 to 60 ms on LTE, for EVERY new connection, and a page opens 10 to 20). Answering at once and buffering the app's first bytes until the real connect succeeds (reset the app if it fails) is what most proxies do. Risk: a refused connection is reported after the app has already sent data (the app sees a reset instead of "refused"). Needs an A/B on the real link.
2. **Warm connection pool on the phone** for the busiest hosts (the owner's own sites): saves the same round trip. Larger change, same risk.
3. **A byte-count readout in LinkMeter's Fast Wi-Fi card** (tablet-side bytes up/down, connections open): makes the next speed investigation take minutes instead of guesses. No behaviour change.
I will not make any of these without a go-ahead and a way to compare (section 5).

## 3. Browser side: what changed (alpha23) [V: unit tests + Robolectric; the real tunnel is [N]]
The AA1 profile (SPEED3 section 2) applies to ANY VPN as the default network: DNS over HTTPS paused (the system resolver is the phone's), HTTP/3 off (QUIC is refused), the Courier rests. New, only when the VPN is recognised as **LinkMeter's own tunnel** (`NetProfile.isFastTunnel`: the VPN network's link carries an address in 10.111.222.x, its own 10.111.222.2 or its DNS 10.111.222.53, read from `LinkProperties` in `NetworkMonitor`):

| Setting | Standard hotspot | Any other VPN | LinkMeter's tunnel | Why |
|---|---|---|---|---|
| `network.dns.disableIPv6` | false | false | **true** | the tunnel blocks IPv6, so an AAAA answer is a dead end; Android's own resolver may already hide it, so this is belt and braces (gain unmeasured) |
| downloads yield to a loading page at | 150 KB/s | 150 KB/s | **1000 KB/s** | 150 KB/s was set for a 5 Mbit/s hotspot (about a quarter of it); behind the tunnel the link is the phone's own connection, and a big download would crawl for nothing while the owner browses |
| `network.trr.mode`, HTTP/3, Courier | as AA1 | as AA1 | as AA1 | unchanged |

- Without a VPN everything is exactly as before (`NetProfileTest.withoutAVpnTheTunnelFlagChangesNothingAndTheStandardValuesStay`). Any other VPN keeps the careful values: nothing is known about its speed.
- If LinkMeter ever changes its addresses, detection reads false and every value falls back to the careful side (it can only lose the two new gains, never break browsing).
- `NetworkMonitor.fastTunnel` is set from the second callback (`onLinkPropertiesChanged`), so it may turn on a moment after `vpn`; `BrowserActivity.onNetworkChanged` watches both. Debug builds: `setprop debug.kestrel.vpn 1` pretends a plain VPN, `2` pretends LinkMeter's tunnel (the emulator has none).
- Home shows "Fast Wi-Fi" on its small line whenever a VPN is the active network; Diagnostics > "Network:" shows the profile, whether the tunnel was recognised and the page rate; Settings > Network lab shows the profile sentence; the engine read-back shows `disableIPv6`.
- Tests: `NetProfileTest` (+5: tunnel vs other VPN, same values as the generic profile, no change without a VPN, address matching incl. look-alikes, sane constants), `PageYieldTest` (+2), `VpnProfileTest` (+2 Robolectric: recognition from link addresses, forgotten with the VPN, another VPN not matched, flag reaches the profile).

## 4. Considered and NOT done (so nobody redoes the thinking)
- **Reload a still-loading page when the VPN flips.** Idea from the AA1 notes. Rejected: a reload does not drop pooled connections, so it would not fix the real problem (section 5, point 2), a page that really failed is already retried by `onNetworkChanged` (network error + ONLINE), and a reload of a page that came from a form POST could ask to resend or resend it.
- **Longer in-browser DNS cache** (`network.dnsCacheExpiration` 60 s): Gecko already serves an expired entry for another grace period while it re-asks, so a revisit within two minutes costs no wait; raising it risks stale IPs behind CDNs for almost no gain.
- **Fewer connections per host / `network.http.speculative-parallel-limit` 0**: each connection costs a relay round trip, but speculative connects are used far more often than not and fewer connections raise head-of-line blocking. A trade-off that needs the real A/B, not a guess.
- **Fewer or more parallel download segments behind the tunnel**: the download engine already runs 3 connections per file (low-RAM device); at 5 MB/s per connection the radio, not the window, is the limit.

## 5. What to measure on the real devices (10 minutes; nothing here needs a computer)
Use Diagnostics > Page loads (it times real loads) and a large file you know:
1. **Browser without Fast Wi-Fi** (LinkMeter off): load 3 sites, note the times; download a 50 MB file, note the speed.
2. **Fast Wi-Fi on**, THEN open a fresh tab and the same 3 sites, and the same download. Expectation: pages 1.5x to 3x quicker (latency-bound, not bandwidth-bound), the download several times faster than the 5 Mbit/s cap (about 3 to 5 MB/s on the tablet's radio).
3. Diagnostics > "Network:" must read "Fast Wi-Fi profile: LinkMeter's tunnel is the active network ..." with "LinkMeter tunnel recognised: true" and "1000 KB/s". If it says "a VPN is the active network" with "recognised: false", the address match failed: send me that line and I will fix the detection.
4. **Known unknown:** a connection opened BEFORE Fast Wi-Fi came up should stay on its old path (Android picks a socket's network when it connects), so a site you were just on may reuse a slow, warm connection for up to a few minutes. Test: after switching on, load the site you had open vs a site you had not visited. If the old one is slower, the fix is to drop the browser's idle connections when the VPN flips, and I will look for a way (Gecko has no public call for it; a WebExtension or a pref trick would be needed).
5. Switch Fast Wi-Fi off again while a page loads: the page error screen retries by itself once the hotspot is back (`NetworkMonitor` ONLINE + `ErrorScreen.Network`), unchanged.

## 6. Not verified, plainly
Everything on the real tablet with the real LinkMeter relay; whether `LinkProperties` of LinkMeter's VPN really lists 10.111.222.2 / 10.111.222.53 on Android 10 (read from the builder calls in `FastVpn.up`, not observed); the actual gain of IPv6-off; the 1000 KB/s page rate (chosen from the radio's likely 3 to 5 MB/s, not measured). The emulator has no VPN: it only shows the profile switching through `debug.kestrel.vpn`.
