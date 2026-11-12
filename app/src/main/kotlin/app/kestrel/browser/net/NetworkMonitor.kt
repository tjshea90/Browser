package app.kestrel.browser.net

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import app.kestrel.browser.Io

/**
 * Connectivity as the tablet sees it: this device lives on a phone hotspot, so the interesting states are
 * "no Wi-Fi at all" (hotspot off / out of range), "Wi-Fi up but Android has not confirmed internet" (phone has no data/signal, OR the carrier
 * blocks Google's connectivity-check URL while everything else works, OR the few seconds after a reconnect), and online.
 * Everything is delivered on the main thread.
 *
 * RULE: only [State.OFFLINE] may change how pages load ([cacheFirst]). [State.NO_INTERNET] is a guess, so it only changes wording and
 * offers an "Open saved copy" button; a working hotspot that fails Android's connectivity check must still browse normally and fresh.
 */
object NetworkMonitor {
    enum class State { OFFLINE, NO_INTERNET, ONLINE }

    @Volatile var state = State.ONLINE
        private set

    /** Serve pages from the HTTP cache (even stale) first: true only when there is no network with INTERNET capability at all. */
    val cacheFirst: Boolean get() = state == State.OFFLINE
    /** The network is metered AND the owner switched a data saver level on: only then does "metered" change what the app does (the owner's hotspot is unmetered, whatever Android's flag says). */
    @Volatile var metered = false
        private set

    /** A VPN is the active network (LinkMeter's Fast Wi-Fi on the tablet): the engine then runs the Fast Wi-Fi profile ([app.kestrel.core.net.NetProfile], research/SPEED3.md). */
    @Volatile var vpn = false
        private set
    /**
     * The VPN is LinkMeter's own Fast Wi-Fi tunnel, not just any VPN: its fixed private addresses sit on the VPN interface ([app.kestrel.core.net.NetProfile.isFastTunnel]). Then the link behind it is the
     * phone's own connection and the profile goes further (no IPv6, a bigger page rate for downloads: research/SPEED4.md). Always false without [vpn].
     */
    @Volatile var fastTunnel = false
        private set
    private var debugVpn = false
    private var debugTunnel = false
    private var vpnTransport = false                 // the default network has TRANSPORT_VPN
    private var tunnelAddr = false                   // and its link carries LinkMeter's tunnel addresses (arrives in a second callback)
    @Volatile private var rawMetered = true // Android's own flag: assume metered until the network says otherwise (a hotspot almost always is)
    @Volatile private var saverOn = false

    fun setSaver(on: Boolean) {
        saverOn = on
        val m = rawMetered && on
        if (m == metered) return
        metered = m
        val copy = synchronized(listeners) { ArrayList(listeners) }
        for (l in copy) l()
    }

    private val listeners = ArrayList<() -> Unit>()
    private var cm: ConnectivityManager? = null
    private var callback: ConnectivityManager.NetworkCallback? = null

    fun addListener(l: () -> Unit) { synchronized(listeners) { listeners.add(l) } }
    fun removeListener(l: () -> Unit) { synchronized(listeners) { listeners.remove(l) } }

    /**
     * Safe to call from any thread (the Gecko build calls it from the IO lane so the binder calls and the first preferences read stay off the UI thread; start and stop go through the same
     * lane, so they stay in order). Listeners are always told on the main thread.
     */
    @Synchronized
    fun start(ctx: Context) {
        if (callback != null) return
        val c = ctx.applicationContext.getSystemService(ConnectivityManager::class.java) ?: return
        cm = c
        saverOn = app.kestrel.browser.data.Prefs.get(ctx).dataSaver != app.kestrel.core.shield.DataSaver.OFF
        val dv = app.kestrel.browser.diag.DebugProps.get(ctx, "debug.kestrel.vpn")        // DEBUG builds only: pretend a VPN is up (the emulator has none); "2" = LinkMeter's own tunnel
        debugVpn = dv == "1" || dv == "2"; debugTunnel = dv == "2"
        val active = c.activeNetwork
        val first = active?.let { c.getNetworkCapabilities(it) }
        val firstLink = addressesOf(active?.let { c.getLinkProperties(it) })
        val begin = { update(first); updateLink(firstLink) }
        if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) begin() else Io.main.post { begin() }
        val cb = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) = update(caps)
            override fun onLinkPropertiesChanged(network: Network, lp: LinkProperties) = updateLink(addressesOf(lp))
            override fun onLost(network: Network) = update(null)
            override fun onUnavailable() = update(null)
        }
        callback = cb
        try { c.registerDefaultNetworkCallback(cb, Io.main) } catch (e: Exception) { callback = null }
    }

    @Synchronized
    fun stop() {
        val c = cm ?: return
        callback?.let { try { c.unregisterNetworkCallback(it) } catch (e: Exception) { /* already gone */ } }
        callback = null
    }

    internal fun classify(caps: NetworkCapabilities?): State = when {
        caps == null || !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) -> State.OFFLINE
        !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) -> State.NO_INTERNET
        else -> State.ONLINE
    }

    /** Every address a network's link carries (its own and its DNS servers) as text, for [app.kestrel.core.net.NetProfile.isFastTunnel]; null when the link is unknown. */
    private fun addressesOf(lp: LinkProperties?): List<String?>? =
        lp?.let { l -> l.linkAddresses.map { it.address?.hostAddress } + l.dnsServers.map { it.hostAddress } }

    internal fun update(caps: NetworkCapabilities?) {
        vpnTransport = caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
        if (caps == null) tunnelAddr = false          // the network is gone, and with it what its link said
        rawMetered = caps == null || !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
        publish(classify(caps))
    }

    /** The default network's link (its addresses) arrived or changed; null = unknown. Comes after [update] for the same network, so [fastTunnel] may turn on a moment after [vpn]. */
    internal fun updateLink(addresses: Iterable<String?>?) {
        tunnelAddr = addresses != null && app.kestrel.core.net.NetProfile.isFastTunnel(addresses)
        publish(state)
    }

    private fun publish(newState: State) {
        val newMetered = rawMetered && saverOn
        val newVpn = debugVpn || vpnTransport
        val newTunnel = newVpn && (debugTunnel || tunnelAddr)
        if (newState == state && newMetered == metered && newVpn == vpn && newTunnel == fastTunnel) return
        state = newState
        metered = newMetered
        vpn = newVpn
        fastTunnel = newTunnel
        val copy = synchronized(listeners) { ArrayList(listeners) }
        for (l in copy) l()
    }
}
