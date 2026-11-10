package app.kestrel.browser.net

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import app.kestrel.browser.Io

/**
 * Connectivity as the tablet sees it: this device lives on a phone hotspot, so the interesting states are
 * "no Wi-Fi at all" (hotspot off / out of range), "Wi-Fi up but the hotspot has no internet" (phone has no data/signal), and online.
 * Everything is delivered on the main thread.
 */
object NetworkMonitor {
    enum class State { OFFLINE, NO_INTERNET, ONLINE }

    @Volatile var state = State.ONLINE
        private set
    @Volatile var metered = true // assume metered until the network says otherwise: a hotspot almost always is
        private set

    private val listeners = ArrayList<() -> Unit>()
    private var cm: ConnectivityManager? = null
    private var callback: ConnectivityManager.NetworkCallback? = null

    fun addListener(l: () -> Unit) { synchronized(listeners) { listeners.add(l) } }
    fun removeListener(l: () -> Unit) { synchronized(listeners) { listeners.remove(l) } }

    fun start(ctx: Context) {
        if (callback != null) return
        val c = ctx.applicationContext.getSystemService(ConnectivityManager::class.java) ?: return
        cm = c
        update(c.activeNetwork?.let { c.getNetworkCapabilities(it) })
        val cb = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) = update(caps)
            override fun onLost(network: Network) = update(null)
            override fun onUnavailable() = update(null)
        }
        callback = cb
        try { c.registerDefaultNetworkCallback(cb, Io.main) } catch (e: Exception) { callback = null }
    }

    fun stop() {
        val c = cm ?: return
        callback?.let { try { c.unregisterNetworkCallback(it) } catch (e: Exception) { /* already gone */ } }
        callback = null
    }

    private fun update(caps: NetworkCapabilities?) {
        val newState = when {
            caps == null || !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) -> State.OFFLINE
            !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) -> State.NO_INTERNET
            else -> State.ONLINE
        }
        val newMetered = caps == null || !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
        if (newState == state && newMetered == metered) return
        state = newState
        metered = newMetered
        val copy = synchronized(listeners) { ArrayList(listeners) }
        for (l in copy) l()
    }
}
