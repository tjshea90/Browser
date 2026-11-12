package app.kestrel.core.courier

/**
 * The TCP ports of Kestrel Courier on the phone. 8765 is the control port and 8767 the relay port of LinkMeter, the owner's other phone app (it runs all the time on the same
 * phone), so those two are never used here: the Courier's API is on [API] and its page-load proxy on the next port, [API] + [PROXY_OFFSET].
 */
object CourierPorts {
    const val API = 8780
    const val PROXY_OFFSET = 1
    fun proxyFor(apiPort: Int): Int = apiPort + PROXY_OFFSET
}
