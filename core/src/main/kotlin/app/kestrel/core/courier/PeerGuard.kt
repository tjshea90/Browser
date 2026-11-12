package app.kestrel.core.courier

/** One address of one of this phone's network interfaces, with the length of its subnet prefix. */
class LocalAddr(val address: ByteArray, val prefix: Int, val ifName: String)

/**
 * Who may even talk to the courier (research/PHONE_OFFLOAD.md section 6.2): this phone itself, or a device on a private subnet that one of the phone's own
 * NON-cellular interfaces is on (the hotspot). A peer on the carrier side, a public IPv6 address, or a carrier-grade-NAT address is dropped before a single byte is parsed.
 * Pure functions over byte arrays so it is testable on the JVM; the Android layer lists the interfaces.
 */
object PeerGuard {
    private val CELLULAR_PREFIXES = listOf("rmnet", "ccmni", "wwan", "v4-rmnet", "clat", "pdp", "ppp", "tun", "ipsec", "dummy", "rev_rmnet", "r_rmnet")

    /** An IPv4-mapped IPv6 address (::ffff:a.b.c.d) is judged as the IPv4 address it carries. */
    fun normalize(a: ByteArray): ByteArray {
        if (a.size == 16) {
            for (i in 0 until 10) if (a[i].toInt() != 0) return a
            if ((a[10].toInt() and 0xFF) == 0xFF && (a[11].toInt() and 0xFF) == 0xFF) return a.copyOfRange(12, 16)
        }
        return a
    }

    fun isLoopback(addr: ByteArray): Boolean {
        val a = normalize(addr)
        if (a.size == 4) return (a[0].toInt() and 0xFF) == 127
        if (a.size == 16) { for (i in 0 until 15) if (a[i].toInt() != 0) return false; return a[15].toInt() == 1 }
        return false
    }

    /** RFC 1918, IPv4 link-local, IPv6 unique-local (fc00::/7) and link-local (fe80::/10). NOT 100.64.0.0/10: that is carrier-grade NAT space, the carrier's side. */
    fun isPrivate(addr: ByteArray): Boolean {
        val a = normalize(addr)
        if (a.size == 4) {
            val b0 = a[0].toInt() and 0xFF; val b1 = a[1].toInt() and 0xFF
            return b0 == 10 || (b0 == 172 && b1 in 16..31) || (b0 == 192 && b1 == 168) || (b0 == 169 && b1 == 254)
        }
        if (a.size == 16) {
            val b0 = a[0].toInt() and 0xFF; val b1 = a[1].toInt() and 0xFF
            return (b0 and 0xFE) == 0xFC || (b0 == 0xFE && (b1 and 0xC0) == 0x80)
        }
        return false
    }

    fun sameSubnet(a: ByteArray, b: ByteArray, prefix: Int): Boolean {
        if (a.size != b.size || prefix < 0 || prefix > a.size * 8) return false
        var bits = prefix
        var i = 0
        while (bits > 0) {
            val m = if (bits >= 8) 0xFF else (0xFF shl (8 - bits)) and 0xFF
            if ((a[i].toInt() and m) != (b[i].toInt() and m)) return false
            bits -= 8; i++
        }
        return true
    }

    fun cellular(ifName: String): Boolean = CELLULAR_PREFIXES.any { ifName.startsWith(it) }

    /**
     * The rule the server applies to a connection: [peer] is who connected, [local] is the address of THIS phone they connected to (the hotspot address, when it is the tablet).
     * The list of interfaces is used when Android lets the app read it; when it does not (Android 11 and newer limit it) the address the tablet connected to is trusted if it is private,
     * and the tablet must share its /24 (IPv6 /64).
     */
    fun allowed(peer: ByteArray, local: ByteArray, locals: List<LocalAddr>): Boolean {
        val p = normalize(peer); val l = normalize(local)
        if (isLoopback(p)) return true
        if (!isPrivate(p) || !isPrivate(l) || p.size != l.size) return false
        val own = locals.firstOrNull { normalize(it.address).contentEquals(l) }
        if (own != null) return !cellular(own.ifName) && sameSubnet(p, l, if (own.address.size != l.size) own.prefix - 96 else own.prefix)
        return sameSubnet(p, l, if (p.size == 4) 24 else 64)
    }

    fun allowed(peer: ByteArray, locals: List<LocalAddr>): Boolean {
        val p = normalize(peer)
        if (isLoopback(p)) return true
        if (!isPrivate(p)) return false
        for (l in locals) {
            val la = normalize(l.address)
            if (cellular(l.ifName) || !isPrivate(la) || la.size != p.size) continue
            val prefix = if (l.address.size != la.size) l.prefix - 96 else l.prefix        // a mapped address brings its IPv6 prefix along
            if (sameSubnet(p, la, prefix)) return true
        }
        return false
    }
}
