package app.kestrel.courier

import android.os.SystemClock
import app.kestrel.core.courier.LocalAddr
import app.kestrel.core.courier.PeerGuard
import java.net.InetAddress
import java.net.NetworkInterface

/** The phone's own network addresses, for the server's "who may connect" rule and for telling the owner where the tablet will find the phone. */
object CourierNet {
    private var cache: List<LocalAddr> = emptyList()
    private var cacheAt = 0L

    /** Every address of every interface that is up (read at most every 3 seconds; Android 11 and newer may hide some, the rule copes with that). */
    @Synchronized fun locals(): List<LocalAddr> {
        val now = SystemClock.elapsedRealtime()
        if (cacheAt != 0L && now - cacheAt < 3000) return cache
        val out = ArrayList<LocalAddr>()
        try {
            val all = NetworkInterface.getNetworkInterfaces()
            while (all != null && all.hasMoreElements()) {
                val ni = all.nextElement()
                if (!ni.isUp) continue
                for (ia in ni.interfaceAddresses) out.add(LocalAddr(ia.address.address, ia.networkPrefixLength.toInt(), ni.name))
            }
        } catch (e: Exception) { /* an empty list: the rule then trusts the address the tablet connected to */ }
        cache = out; cacheAt = now
        return out
    }

    /** The private IPv4 addresses of the phone's non-cellular interfaces: where the tablet reaches it (the hotspot's own address). */
    fun hotspotAddresses(): List<String> = locals()
        .filter { it.address.size == 4 && PeerGuard.isPrivate(it.address) && !PeerGuard.cellular(it.ifName) && !PeerGuard.isLoopback(it.address) }
        .mapNotNull { InetAddress.getByAddress(it.address).hostAddress }
        .distinct()
}
