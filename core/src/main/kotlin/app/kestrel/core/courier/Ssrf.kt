package app.kestrel.core.courier

import app.kestrel.core.download.FetchException
import java.net.InetAddress
import java.net.URL
import java.net.UnknownHostException

/**
 * The phone fetches whatever address the tablet sends it, so a page must not be able to aim the phone at devices on the networks the phone sits on
 * (the router, the hotspot's own interface, the phone itself, the carrier's private space). Used as the `guard` of the phone's [app.kestrel.core.download.UrlConnectionFetcher]
 * so every redirect hop is checked too (research/PHONE_OFFLOAD.md section 6.2).
 */
object Ssrf {
    /** True for every address a courier must never connect to. */
    fun blocked(addr: ByteArray): Boolean {
        val a = PeerGuard.normalize(addr)
        if (a.size == 4) {
            val b0 = a[0].toInt() and 0xFF; val b1 = a[1].toInt() and 0xFF
            return b0 == 0 || b0 == 127 || b0 == 10 || (b0 == 172 && b1 in 16..31) || (b0 == 192 && b1 == 168) || (b0 == 169 && b1 == 254) ||
                (b0 == 100 && b1 in 64..127) ||            // carrier-grade NAT
                (b0 == 198 && (b1 == 18 || b1 == 19)) ||   // benchmarking range, used inside some networks
                (b0 == 192 && b1 == 0 && (a[2].toInt() and 0xFF) == 0) ||
                b0 >= 224                                  // multicast, reserved, broadcast
        }
        if (a.size == 16) {
            var zeros = true
            for (i in 0 until 15) if (a[i].toInt() != 0) { zeros = false; break }
            if (zeros && (a[15].toInt() == 0 || a[15].toInt() == 1)) return true                      // :: and ::1
            val b0 = a[0].toInt() and 0xFF; val b1 = a[1].toInt() and 0xFF
            if ((b0 and 0xFE) == 0xFC) return true                                                      // fc00::/7 unique local
            if (b0 == 0xFE && (b1 and 0xC0) != 0) return true                                           // fe80::/10 link-local, fec0::/10 site-local
            if (b0 == 0xFF) return true                                                                 // multicast
            // 64:ff9b::/96 (NAT64) carries an IPv4 address in its last four bytes: judge that
            if (b0 == 0x00 && b1 == 0x64 && (a[2].toInt() and 0xFF) == 0xFF && (a[3].toInt() and 0xFF) == 0x9B) {
                var rest = true
                for (i in 4 until 12) if (a[i].toInt() != 0) { rest = false; break }
                if (rest) return blocked(a.copyOfRange(12, 16))
            }
            return false
        }
        return true                                                                                     // not an address we understand
    }

    /** Throws [FetchException] (LOCAL_NET) when [host] is, or resolves to, a blocked address. A host that does not resolve is left to fail in the connection itself. */
    fun check(host: String, resolve: (String) -> List<ByteArray> = ::systemResolve) {
        val addrs = try { resolve(host) } catch (e: UnknownHostException) { return } catch (e: SecurityException) { return }
        for (a in addrs) if (blocked(a)) throw FetchException(FetchException.Kind.LOCAL_NET, "address on a private network")
    }

    /** The guard for [app.kestrel.core.download.UrlConnectionFetcher]. */
    fun guard(resolve: (String) -> List<ByteArray> = ::systemResolve): (URL) -> Unit = { u -> check(u.host.removePrefix("[").removeSuffix("]"), resolve) }

    private fun systemResolve(host: String): List<ByteArray> = InetAddress.getAllByName(host).map { it.address }
}
