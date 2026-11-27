package app.kestrel.browser.shield

import android.content.Context
import app.kestrel.browser.Io
import app.kestrel.browser.Proc
import app.kestrel.browser.data.Prefs
import app.kestrel.core.shield.ShieldSettings
import app.kestrel.core.shield.SiteShieldStore
import app.kestrel.core.shield.SiteShields
import app.kestrel.core.shield.effective
import java.io.File

/** Per-site Shields overrides (persisted) combined with the global defaults. Thread-safe: read from WebView IO threads. */
object Shields {
    private val lock = Any()
    private var store = SiteShieldStore()
    private var file: File? = null

    fun init(ctx: Context) {
        if (file != null) return
        val f = File(ctx.filesDir, "site_shields.txt")
        file = f
        Io.exec {
            if (f.isFile) {
                val loaded = SiteShieldStore.decode(f.readText())
                synchronized(lock) { store = loaded }
            }
        }
    }

    /** Effective settings for a page on [host] (null host = defaults). */
    fun effective(prefs: Prefs, host: String?): ShieldSettings {
        val g = prefs.globalShields()
        val s = if (host == null) null else synchronized(lock) { store[host] }
        return (s?.applyTo(g) ?: g).effective()
    }

    fun overrideFor(host: String): SiteShields? = synchronized(lock) { store[host] }

    /**
     * PV1 (owner 2026-11-21: nothing saved from a private tab): the Shields panel opened from a PRIVATE tab changes a site's choices for the session only. The change is applied in memory (the page behaves as it was set
     * and every tab of that site follows) but it is NOT written to `site_shields.txt` or to a backup, because that file names sites; [privOverlay] remembers what the saved store held before, and [endPrivate] takes the
     * change back when the last private tab is gone. A change made from a regular tab is a saved choice as before (and replaces a private one for that site).
     */
    fun setOverride(host: String, s: SiteShields, fromPrivate: Boolean = false) {
        val text: String?
        synchronized(lock) {
            if (fromPrivate) {
                if (!privOverlay.containsKey(host)) privOverlay[host] = store[host]
                store.set(host, s); text = null
            } else {
                privOverlay.remove(host)
                store.set(host, s); text = savedText()
            }
        }
        if (text != null) persist(text)
    }

    /** host -> what the SAVED store held before a private tab changed it (null = nothing). Memory only. */
    private val privOverlay = HashMap<String, SiteShields?>()

    /** The text for disk and backups: the store as the saved choices left it, without what a private tab changed. Call with [lock] held. */
    private fun savedText(): String {
        if (privOverlay.isEmpty()) return store.encode()
        val copy = SiteShieldStore(store.entries())
        for ((h, prev) in privOverlay) copy.set(h, prev ?: SiteShields(0, 0))
        return copy.encode()
    }

    /** The last private tab is gone: whatever a private tab changed is taken back (the saved choices were never touched). Returns how many sites went back. */
    fun endPrivate(): Int = synchronized(lock) {
        val n = privOverlay.size
        for ((h, prev) in privOverlay) store.set(h, prev ?: SiteShields(0, 0))
        privOverlay.clear()
        n
    }

    internal fun resetForTest() { synchronized(lock) { store = SiteShieldStore(); privOverlay.clear() }; file = null }

    private fun persist(text: String) {
        val f = file
        if (f == null || Proc.isPrivate) return // private window: changes live for the session only
        Io.exec {
            val tmp = File(f.parentFile, f.name + ".tmp")
            tmp.writeText(text)
            if (!tmp.renameTo(f)) { f.delete(); tmp.renameTo(f) }
        }
    }

    /** Settings > Back up and restore: the per-site choices as text ("" when there are none). */
    fun exportText(): String = synchronized(lock) { savedText() }

    /** Settings > Back up and restore: the backup's per-site choices are added; for a site both have, the backup wins. Returns how many sites came back. */
    fun importText(text: String): Int {
        val incoming = SiteShieldStore.decode(text).entries()
        if (incoming.isEmpty()) return 0
        val out: String
        synchronized(lock) { for ((h, s) in incoming) { store.set(h, s); privOverlay.remove(h) }; out = savedText() }
        persist(out)
        return incoming.size
    }

    fun clearAll() {
        synchronized(lock) { store = SiteShieldStore(); privOverlay.clear() }
        file?.let { f -> Io.exec { f.delete() } }
    }
}
