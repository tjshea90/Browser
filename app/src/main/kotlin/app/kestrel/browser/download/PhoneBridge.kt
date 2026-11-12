package app.kestrel.browser.download

import android.content.Context
import android.content.pm.ApplicationInfo
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import app.kestrel.browser.Io
import app.kestrel.browser.data.Prefs
import app.kestrel.browser.diag.Health
import app.kestrel.core.courier.CourierClient
import app.kestrel.core.courier.CourierException
import app.kestrel.core.courier.LinkReport
import app.kestrel.core.courier.LinkTest
import app.kestrel.core.courier.LocalDownloads
import app.kestrel.core.courier.Pairing
import app.kestrel.core.courier.PhoneBackend
import app.kestrel.core.courier.PhoneMode
import app.kestrel.core.courier.Sign
import java.io.File
import java.net.Inet4Address
import java.util.Locale

/**
 * The Android side of "Download on my phone" (research/PHONE_OFFLOAD.md): where the phone is, the pairing code, the settings, and the one [PhoneBackend] the
 * [DownloadController] hands downloads to. The protocol, the rows and the fallbacks are all in :core.
 */
object PhoneBridge {
    /** Ids of the rows that stand for downloads on the phone: above the single-file engine's ids and below the stream engine's (1e9), so neither mistakes them. */
    const val FIRST_ID = 500_000_000L

    /** What "Test the phone link" downloads the first megabytes of: this project's own release file, so no third party is involved. */
    const val TEST_URL = "https://github.com/tjshea90/Browser/releases/download/kestrel-g-v0.3.0-alpha18/kestrel-g-0.3.0-alpha18.apk"

    @Volatile private var backend: PhoneBackend? = null
    private var client: CourierClient? = null
    private var clientKey = ""

    fun create(app: Context, local: LocalDownloads, onChange: () -> Unit): PhoneBackend {
        val b = PhoneBackend({ clientFor(app) }, local, File(app.filesDir, "phone-jobs.json"), FIRST_ID, { mode(app) }, onChange, log = { Health.log("phone", it.take(140)) })
        backend = b
        return b
    }

    fun backend(): PhoneBackend? = backend

    fun mode(ctx: Context): PhoneMode = PhoneMode.values()[Prefs.get(ctx).phoneMode.coerceIn(0, 2)]

    /** The backend has work (the owner uses the feature, or rows from an earlier run are still in flight). */
    fun wanted(ctx: Context): Boolean = mode(ctx) != PhoneMode.OFF || backend?.needsService() == true

    /** Settings changed the mode: make sure the backend thread runs and looks at the network again. */
    fun modeChanged(ctx: Context) {
        DownloadController.init(ctx)
        val b = backend ?: return
        if (mode(ctx) != PhoneMode.OFF) b.start()
        b.onNetwork()
    }

    fun paired(ctx: Context): Boolean = Pairing.parse(Prefs.get(ctx).phoneCode) != null

    /** Stores a code the owner typed; false = it is not a valid code. */
    fun savePairing(ctx: Context, text: String): Boolean {
        val secret = Pairing.parse(text) ?: return false
        Prefs.get(ctx).phoneCode = Pairing.format(secret)
        synchronized(this) { client = null; clientKey = "" }
        backend?.onNetwork()
        return true
    }

    fun forgetPairing(ctx: Context) {
        Prefs.get(ctx).phoneCode = ""
        synchronized(this) { client = null; clientKey = "" }
    }

    /** The phone's address: a debug override, else the default gateway of the Wi-Fi the tablet is on (which is the phone when the tablet is on its hotspot). */
    fun address(ctx: Context): Pair<String, Int>? {
        debugOverride(ctx)?.let { return it }
        val g = gateway(ctx) ?: return null
        return g to Prefs.get(ctx).phonePort
    }

    /** The client for the paired phone at its current address, or null (not paired, or no Wi-Fi). The same object is returned while nothing changes, so its clock offset is kept. */
    fun clientFor(ctx: Context): CourierClient? {
        val secret = Pairing.parse(Prefs.get(ctx).phoneCode) ?: return null
        val (host, port) = address(ctx) ?: return null
        val key = "$host:$port:" + Prefs.get(ctx).phoneCode
        synchronized(this) {
            if (key != clientKey || client == null) { client = CourierClient(host, port, Sign.deriveKey(secret)); clientKey = key }
            return client
        }
    }

    @Suppress("DEPRECATION")
    fun gateway(ctx: Context): String? {
        val cm = ctx.getSystemService(ConnectivityManager::class.java) ?: return null
        // The phone is the default gateway of the WI-FI network. Not `activeNetwork`: while a VPN is up (LinkMeter's Fast Wi-Fi) that is the VPN, which has no gateway at all, and the
        // phone would be "not found" exactly when the owner uses the VPN. The VPN leaves private addresses alone, so the tablet reaches the phone straight over the Wi-Fi.
        val active = cm.activeNetwork
        var other: String? = null
        for (net in cm.allNetworks) {
            val caps = cm.getNetworkCapabilities(net) ?: continue
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) continue
            if (!caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) && !caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) continue
            val lp = cm.getLinkProperties(net) ?: continue
            for (r in lp.routes) {
                val g = r.gateway
                if (r.isDefaultRoute && g is Inet4Address && !g.isAnyLocalAddress) { if (net == active) return g.hostAddress; if (other == null) other = g.hostAddress }
            }
        }
        return other
    }

    private fun debugProp(ctx: Context, name: String): String? {
        if ((ctx.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) == 0) return null
        return try { (Class.forName("android.os.SystemProperties").getMethod("get", String::class.java).invoke(null, name) as? String)?.takeIf { it.isNotBlank() } } catch (e: Throwable) { null }
    }

    /** The file the link test reads: [TEST_URL], or (DEBUG builds only, `setprop debug.kestrel.phone.testurl http://10.0.2.2:8000/big.bin`) the emulator rig's own website. */
    fun testUrl(ctx: Context): String = debugProp(ctx, "debug.kestrel.phone.testurl") ?: TEST_URL

    /** DEBUG builds only: `adb shell setprop debug.kestrel.phone 127.0.0.1:8780` points the link at a phone that is not on the hotspot (the emulator test rig). */
    private fun debugOverride(ctx: Context): Pair<String, Int>? {
        val v = debugProp(ctx, "debug.kestrel.phone") ?: return null
        val i = v.lastIndexOf(':')
        if (i <= 0) return v to app.kestrel.core.courier.CourierPorts.API
        return v.substring(0, i) to (v.substring(i + 1).toIntOrNull() ?: return null)
    }

    // ---------------------------------------------------------------------------------------------------------------- what the screens show
    fun statusLine(ctx: Context): String {
        val prefs = Prefs.get(ctx)
        if (prefs.phoneCode.isEmpty()) return "Not paired. Open Kestrel Courier on your phone and type the code it shows (Pair with the phone)."
        val addr = address(ctx) ?: return "Paired. Join your phone's hotspot (Wi-Fi) to use it."
        val d = backend?.diag()
        val state = when {
            d == null || d.lastHelloAt == 0L -> "not tried yet"
            d.reachable -> "answering (${d.rttMs} ms)"
            else -> "not answering" + (d.lastError?.let { " ($it)" } ?: "")
        }
        return "Paired. Phone at ${addr.first}:${addr.second}: $state."
    }

    fun diagLines(ctx: Context): List<String> {
        val prefs = Prefs.get(ctx)
        val d = backend?.diag()
        val addr = address(ctx)
        return listOf(
            "Phone link: mode=${mode(ctx).name.lowercase()}, paired=${prefs.phoneCode.isNotEmpty()}, address=${addr?.let { "${it.first}:${it.second}" } ?: "none"}" +
                (if (d != null) ", reachable=${d.reachable}, rtt=${d.rttMs} ms, clock offset=${d.skewMs} ms, last error=${d.lastError ?: "none"}, rows on the phone=${d.activeJobs}" else ", backend not started"),
        )
    }

    /** "Test the phone link": [onDone] gets the report (or a sentence when the test could not even start), on the UI thread. */
    fun testLink(ctx: Context, onDone: (LinkReport?, String?) -> Unit) {
        val c = clientFor(ctx)
        if (c == null) { onDone(null, if (!paired(ctx)) "Not paired yet: type the code the Courier app shows." else "The phone's address is not known: join the phone's hotspot first."); return }
        Io.fast {
            var rep: LinkReport? = null
            var err: String? = null
            try {
                c.hello()
                rep = LinkTest.run(c, UrlConnectionFetcher(), testUrl(ctx))
            } catch (e: CourierException) {
                err = when (e.kind) {
                    CourierException.Kind.UNREACHABLE, CourierException.Kind.TIMEOUT -> "The phone does not answer. Is the hotspot on and the Courier app switched on?"
                    CourierException.Kind.IMPOSTOR, CourierException.Kind.AUTH -> "Something answered, but not with this pairing code. Check the code on the phone."
                    else -> "The phone said: " + e.kind.name.lowercase().replace('_', ' ')
                }
            } catch (e: Exception) { err = "The test failed: ${e.javaClass.simpleName}" }
            Io.ui { onDone(rep, err) }
        }
    }

    /** The report as lines for a dialog. */
    fun describe(r: LinkReport): String {
        fun mb(b: Long) = if (b <= 0) "not measured" else String.format(Locale.US, "%.1f MB/s", b / 1_000_000.0)
        val sb = StringBuilder()
        sb.append("Tablet <-> phone (wifi): ").append(mb(r.lanBps)).append('\n')
        sb.append("Phone's internet: ").append(mb(r.phoneNetBps)).append('\n')
        sb.append("This tablet's internet: ").append(mb(r.tabletNetBps)).append("\n\n")
        sb.append(r.verdict)
        if (r.problems.isNotEmpty()) sb.append("\n\n").append(r.problems.joinToString("; "))
        return sb.toString()
    }
}
