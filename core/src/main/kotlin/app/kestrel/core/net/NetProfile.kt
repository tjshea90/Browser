package app.kestrel.core.net

/**
 * Which network settings the browser engine (and the phone helper) should use RIGHT NOW, from the owner's own choices and what the network looks like. Pure, so every combination is tested.
 *
 * The one situation that changes things is a VPN being the active network: the owner runs LinkMeter's "Fast Wi-Fi" on the tablet (research/SPEED3.md section 2). LinkMeter's VPN carries every app's
 * traffic over the Wi-Fi to a relay on the phone, which opens the real connections with the phone's own network stack (the carrier caps the hotspot, not the phone). Its design decides what is
 * best inside it: QUIC (UDP 443) is refused at once, DNS-over-TLS is refused, IPv6 is blocked, and name lookups are answered by the PHONE's resolver (its cache, its Private DNS). So while it is up:
 * - DNS over HTTPS is paused: the system resolver goes to the phone's resolver in one hop (a DoH lookup would run through the VPN's userspace TCP engine on the tablet's weak CPU and ask for
 *   IPv6 addresses the VPN blocks);
 * - HTTP/3 is off: no QUIC attempt that is refused and then retried over TCP;
 * - the Courier (the owner's other phone helper) rests, unless the owner's Network lab switch [Input.keepHelperUnderVpn] says otherwise. The assumption behind resting it ("a second relay on top of the
 *   VPN only adds a hop, never speed") is UNTESTED: the Courier's proxy sits at the phone's private address, which the VPN does not carry, so for the browser it is one relay INSTEAD of the tunnel
 *   (the tablet kernel's TCP instead of a userspace TCP engine on the weak CPU), not on top of it. The switch exists to measure that (research/REVIEW-EXTERNAL-1.md section 5, EX-5; the three-way
 *   run of research/SPEED4.md section 5); rest stays the default until the owner's numbers say otherwise.
 * When the VPN is recognisably LinkMeter's own tunnel ([isFastTunnel]: its fixed addresses sit on the VPN interface), two more things hold, because the link behind it is the phone's own connection and
 * not the carrier-capped hotspot (research/SPEED4.md):
 * - the engine never asks for or tries IPv6 (the tunnel blocks it, so every AAAA answer would be a dead end);
 * - downloads leave a loading page [FAST_PAGE_YIELD_KBPS] instead of [PAGE_YIELD_KBPS]: 150 KB/s was set for a 5 Mbit/s hotspot and would crawl a big download for nothing on a link ten times faster.
 * Any other VPN keeps the careful values: nothing is known about its speed.
 * The owner's own switches are never changed: they apply again the moment the VPN goes away.
 */
object NetProfile {
    /** While a page loads, downloads are held to this many KB/s on the owner's normal hotspot (about a quarter of its 5 Mbit/s). */
    const val PAGE_YIELD_KBPS = 150
    /** The same, behind LinkMeter's tunnel: the phone's own connection leaves room for both, so a page still gets most of the Wi-Fi (about 3 to 5 MB/s on the tablet's single-stream 2.4 GHz radio). */
    const val FAST_PAGE_YIELD_KBPS = 1000

    /** LinkMeter's tunnel interface lives on this fixed private network (its FastProto.VPN_ADDR 10.111.222.2 and VPN_DNS 10.111.222.53). */
    const val TUNNEL_PREFIX = "10.111.222."

    /**
     * Is a network with these addresses (its own and its DNS servers, as text like "10.111.222.2") LinkMeter's Fast Wi-Fi tunnel? Only the tunnel interface carries these private addresses, so any
     * other VPN, Wi-Fi or mobile network never matches. If LinkMeter ever changes them this reads false and every value stays on the careful side.
     */
    fun isFastTunnel(addresses: Iterable<String?>): Boolean = addresses.any { it != null && it.startsWith(TUNNEL_PREFIX) && it.length > TUNNEL_PREFIX.length }

    class Input(
        /** The owner's "Encrypted DNS (DNS over HTTPS)" switch. */
        val dohOn: Boolean,
        /** The owner's "HTTP/3 (QUIC)" switch. */
        val http3On: Boolean,
        /** A VPN is the active network (LinkMeter's Fast Wi-Fi, or any VPN: the same reasoning holds). */
        val vpn: Boolean,
        /** The owner's Courier choice: 0 off, 1 when the phone is there, 2 always (it is OFF by default). */
        val helperMode: Int = 0,
        /** The owner's "Load pages through the phone" switch (OFF by default). */
        val pagesViaPhone: Boolean = false,
        /** The VPN is LinkMeter's own tunnel ([isFastTunnel]); only meaningful together with [vpn]. */
        val fastTunnel: Boolean = false,
        /** Network lab (EX-5): keep the owner's Courier choices (downloads and pages through the phone) even while a VPN is the active network. OFF by default. */
        val keepHelperUnderVpn: Boolean = false,
    )

    class Out(
        /** `network.trr.mode`: 2 = DNS over HTTPS first with the system resolver as the fall back, 0 = system resolver only. */
        val trrMode: Int,
        val http3: Boolean,
        /** True while the Fast Wi-Fi profile is in force. */
        val fastWifi: Boolean,
        /** The Courier mode that really applies (0 while a VPN is up). */
        val helperMode: Int,
        /** Pages go through the Courier's proxy (never together with a VPN). */
        val pagesViaProxy: Boolean,
        /** `network.dns.disableIPv6`: no AAAA lookups and no IPv6 connection attempts (true only behind LinkMeter's tunnel, which blocks IPv6). */
        val disableIPv6: Boolean = false,
        /** KB/s the downloads are held to while the page in front loads. */
        val pageYieldKBps: Int = PAGE_YIELD_KBPS,
        /** The VPN is LinkMeter's own tunnel (the full Fast Wi-Fi profile). */
        val tunnel: Boolean = false,
        /** The owner kept the phone helper going under the VPN ([Input.keepHelperUnderVpn]). */
        val helperKept: Boolean = false,
    ) {
        /** One plain sentence for Settings and Diagnostics. */
        val text: String get() = when {
            tunnel -> "Fast Wi-Fi profile: LinkMeter's tunnel is the active network, so lookups use the phone's own resolver, HTTP/3 and IPv6 are off, downloads give a loading page more room ($pageYieldKBps KB/s) and the phone helper " + (if (helperKept) "keeps working (your Network lab switch)." else "rests.")
            fastWifi -> "Fast Wi-Fi profile: a VPN is the active network, so lookups use the phone's own resolver, HTTP/3 is off and the phone helper " + (if (helperKept) "keeps working (your Network lab switch)." else "rests.")
            else -> "Standard profile: your own switches apply."
        }
    }

    fun effective(i: Input): Out {
        val helper = i.helperMode.coerceIn(0, 2)
        if (!i.vpn) return Out(trrMode = if (i.dohOn) 2 else 0, http3 = i.http3On, fastWifi = false, helperMode = helper, pagesViaProxy = helper != 0 && i.pagesViaPhone)
        return Out(
            trrMode = 0, http3 = false, fastWifi = true,
            helperMode = if (i.keepHelperUnderVpn) helper else 0, pagesViaProxy = i.keepHelperUnderVpn && helper != 0 && i.pagesViaPhone,
            disableIPv6 = i.fastTunnel, pageYieldKBps = if (i.fastTunnel) FAST_PAGE_YIELD_KBPS else PAGE_YIELD_KBPS, tunnel = i.fastTunnel,
            helperKept = i.keepHelperUnderVpn,
        )
    }
}
