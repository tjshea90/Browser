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
 * - the Courier (the owner's other phone helper) rests: a second relay on top of the VPN only adds a hop, never speed.
 * The owner's own switches are never changed: they apply again the moment the VPN goes away.
 */
object NetProfile {
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
    ) {
        /** One plain sentence for Settings and Diagnostics. */
        val text: String get() = if (fastWifi) "Fast Wi-Fi profile: a VPN is the active network, so lookups use the phone's own resolver, HTTP/3 is off and the phone helper rests."
        else "Standard profile: your own switches apply."
    }

    fun effective(i: Input): Out {
        val helper = i.helperMode.coerceIn(0, 2)
        return if (i.vpn) Out(trrMode = 0, http3 = false, fastWifi = true, helperMode = 0, pagesViaProxy = false)
        else Out(trrMode = if (i.dohOn) 2 else 0, http3 = i.http3On, fastWifi = false, helperMode = helper, pagesViaProxy = helper != 0 && i.pagesViaPhone)
    }
}
