package app.kestrel.browser.download

import android.content.Context
import android.os.Handler
import android.os.Looper
import app.kestrel.browser.data.Prefs
import app.kestrel.browser.diag.Health
import app.kestrel.core.courier.PhoneMode
import app.kestrel.core.courier.ProxyLink
import java.net.URLEncoder

/**
 * "Pages through the phone" on the tablet (research/SPEED2.md): while the phone helper is on, the browser engine loads https pages through the Courier's proxy on the phone, which opens
 * the real connection with the phone's own network stack (the carrier caps tethered traffic, not the phone's). [ProxyLink] (in :core) keeps the proxy open for this tablet and reports
 * its state; this object turns the state into "use the proxy" or "go direct" for whichever engine is running ([Applier]), and decides when the owner wants it at all.
 *
 * Anything but an open proxy means direct loading: an unreachable phone, an older Courier, a wrong code and a switched-off helper never end in a failed page.
 */
object PageProxy {
    /** Engine side: Gecko sets a PAC file with a direct fallback; the legacy WebView build has nothing to apply (no applier = pages always load directly). */
    interface Applier { fun apply(host: String, port: Int); fun clear() }

    private val main = Handler(Looper.getMainLooper())
    @Volatile private var link: ProxyLink? = null
    @Volatile private var applier: Applier? = null
    @Volatile private var applied: ProxyLink.State = ProxyLink.State.Off

    fun init(app: Context) {
        synchronized(this) {
            if (link != null) return
            val ctx = app.applicationContext
            link = ProxyLink({ PhoneBridge.clientFor(ctx) }, { wanted(ctx) }, { s -> main.post { apply(s) } }, log = { Health.log("pages", it.take(140)) }).also { it.start() }
        }
    }

    /** The owner wants pages to go through the phone: the helper is on, this switch is on, and a phone is paired. */
    fun wanted(ctx: Context): Boolean = PhoneBridge.mode(ctx) != PhoneMode.OFF && Prefs.get(ctx).pagesViaPhone && PhoneBridge.paired(ctx)

    /** The switch changed, the network changed or the app came back to the front: look again now. */
    fun refresh() { link?.refresh() }

    /** The engine is up (or gone): hand it the current state at once. */
    fun setApplier(a: Applier?) { applier = a; main.post { applyNow() } }

    val state: ProxyLink.State get() = link?.state ?: ProxyLink.State.Off

    /** One plain line for Settings and Diagnostics. */
    fun line(ctx: Context): String = when (val s = state) {
        is ProxyLink.State.Active -> "Pages load through the phone (${s.host}:${s.port})." + (link?.phoneStats?.let { p -> " The phone has opened ${p.tunnels} connections for them (${mb(p.up + p.down)})" + (if (p.refused > 0) ", turned away ${p.refused}" else "") + "." } ?: "")
        is ProxyLink.State.Unavailable -> if (wanted(ctx)) "Pages load directly for now: ${s.why}." else "Pages load directly."
        ProxyLink.State.Off -> if (wanted(ctx)) "Pages: looking for the phone." else "Pages load directly (switched off)."
    }

    private fun mb(b: Long): String = if (b < 1_000_000L) "${b / 1000} KB" else String.format(java.util.Locale.US, "%.1f MB", b / 1_000_000.0)

    private fun apply(s: ProxyLink.State) { applied = s; applyNow() }

    private fun applyNow() {
        val a = applier ?: return
        try {
            when (val s = applied) {
                is ProxyLink.State.Active -> a.apply(s.host, s.port)
                else -> a.clear()
            }
        } catch (e: Exception) { Health.log("pages", "apply failed", e.toString().take(140)) }
    }

    /**
     * The proxy auto-config the engine uses (a `data:` URL, so no file and no server): https and secure web sockets go to the phone with "DIRECT" as the fallback if it stops answering;
     * plain http (rare and small), single-label names, the local network and `.local`-style names never leave the tablet.
     */
    fun pac(host: String, port: Int): String = """function FindProxyForURL(url, host) {
  var p = url.substring(0, 6);
  if (p !== "https:" && url.substring(0, 4) !== "wss:") return "DIRECT";
  if (host === "localhost" || host.indexOf(".") < 0) return "DIRECT";
  if (/^(10\.|127\.|192\.168\.|169\.254\.|172\.(1[6-9]|2[0-9]|3[01])\.|100\.(6[4-9]|[7-9][0-9]|1[01][0-9]|12[0-7])\.)/.test(host)) return "DIRECT";
  if (/\.(local|lan|home|internal|localdomain)${'$'}/.test(host)) return "DIRECT";
  return "PROXY $host:$port; DIRECT";
}"""

    fun pacUrl(host: String, port: Int): String = "data:application/x-ns-proxy-autoconfig;charset=utf-8," + URLEncoder.encode(pac(host, port), "UTF-8").replace("+", "%20")
}
